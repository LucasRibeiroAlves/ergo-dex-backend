package org.ergoplatform.dex.executor.amm.interpreters.deposits.n2t

import cats.Functor
import cats.effect.concurrent.Ref
import cats.syntax.semigroup._
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.DepositFee._
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreterHelpers
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._

final class N2TDepositTokenFeeInterpreter[F[_]: Functor](
  exchange: ExchangeConfig,
  execution: MonetaryConfig,
  ref: Ref[F, NetworkContext],
  helpers: CFMMInterpreterHelpers
)(implicit contracts: AMMContracts[N2T_CFMM]) {

  import helpers._

  def deposit(
    deposit: DepositTokenFee,
    pool: CFMMPool,
    dexFeeOutput: Output
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = ref.get.map { ctx =>
    val poolBox: BoxInfo   = pool.box
    val depositBox: Output = deposit.box

    val depositIn = new Input(depositBox.boxId.toErgo, ProverResult.empty)
    val poolIn    = new Input(poolBox.boxId.toErgo, ProverResult.empty)
    val dexFeeIn  = new Input(dexFeeOutput.boxId.toErgo, ProverResult.empty)

    val inX = deposit.params.inX
    val inY = deposit.depositFee match {
      case FeeFromY => deposit.params.inY - deposit.params.dexFee
      case _        => deposit.params.inY
    }

    val (rewardLP, change) = pool.rewardLP(inX, inY)
    val (changeX, changeY) =
      (change.filter(_.id == inX.id).map(_.value).sum, change.filter(_.id == inY.id).map(_.value).sum)

    val poolOut = new ErgoBoxCandidate(
      value          = poolBox.value + inX.value - changeX,
      ergoTree       = contracts.pool,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkPoolTokens(
        pool,
        amountLP = pool.lp.value - rewardLP.value,
        amountY  = pool.y.value + inY.value - changeY
      ),
      additionalRegisters = mkPoolRegs(pool)
    )

    val minerFee    = execution.minerFee min deposit.maxMinerFee
    val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)

    val dexFeeTokensReturn: Seq[(ergo.TokenId, Long)] =
      (Map(exchange.spectrumToken -> deposit.params.dexFee) |+| dexFeeOutput.assets
        .map(asset => asset.tokenId -> asset.amount)
        .toMap).toSeq

    val dexFeeBox = new ErgoBoxCandidate(
      dexFeeOutput.value,
      dexFeeProp,
      ctx.currentHeight,
      additionalTokens = mkTokens(dexFeeTokensReturn: _*)
    )

    val returnBox = new ErgoBoxCandidate(
      value          = depositBox.value - inX.value - minerFeeBox.value + changeX,
      ergoTree       = deposit.params.redeemer.toErgoTree,
      creationHeight = ctx.currentHeight,
      additionalTokens =
        if (changeY > 0)
          mkTokens(rewardLP.id -> rewardLP.value, inY.id -> changeY)
        else
          mkTokens(rewardLP.id -> rewardLP.value)
    )

    val dexInput =
      Vector(
        ErgoUnsafeProver
          .prove(UnsignedErgoLikeTransaction(IndexedSeq(dexFeeIn), IndexedSeq.empty), exchange.sk)
          .inputs
          .headOption
      ).flatten

    val inputs      = Vector(poolIn, depositIn) ++ dexInput
    val outs        = Vector(poolOut, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolOut.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
    val nextPool    = pool.deposit(inX, inY, boxInfo)

    (tx, nextPool)
  }
}