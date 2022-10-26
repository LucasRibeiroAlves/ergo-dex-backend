package org.ergoplatform.dex.index.db

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.domain.Ticker
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.index.sql._
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.{Block, LedgerMetadata}
import org.ergoplatform.ergo.services.explorer.models.TokenInfo
import org.ergoplatform.ergo.state.ConfirmedIndexed
import CFMMVersionedOrder._
import cats.syntax.option._
import derevo.derive
import tofu.logging.derivation.loggable

object models {

  @derive(loggable)
  final case class UnresolvedState(
    address: String,
    poolId: PoolId,
    lpId: TokenId,
    balance: BigDecimal,
    lpErg: BigDecimal,
    timestamp: Long,
    boxId: String
  )

  @derive(loggable)
  final case class PoolSnapshot(
    lpAmount: BigDecimal,
    xId: String,
    xAmount: BigDecimal,
    yId: String,
    yAmount: BigDecimal,
    stateId: String
  )

  final case class DBBlock(
    id: String,
    height: Int,
    timestamp: Long
  )

  implicit val blockQs: QuerySet[DBBlock] = BlocksSql

  implicit val dbBlockView: Extract[Block, DBBlock] = { case Block(id, height, timestamp) =>
    DBBlock(id, height, timestamp)
  }

  final case class DBLiquidityLock(
    id: LockId,
    deadline: Int,
    tokenId: TokenId,
    amount: Long,
    redeemer: Address
  )

  implicit val lqLockQs: QuerySet[DBLiquidityLock] = LqLocksSql

  implicit val lqLockView: Extract[LiquidityLock, DBLiquidityLock] = {
    case LiquidityLock(id, deadline, amount, redeemer) =>
      DBLiquidityLock(id, deadline, amount.id, amount.value, redeemer)
  }

  final case class DBPoolSnapshot(
    stateId: PoolStateId,
    poolId: PoolId,
    lpId: TokenId,
    lpAmount: Long,
    xId: TokenId,
    xAmount: Long,
    yId: TokenId,
    yAmount: Long,
    feeNum: Int,
    globalIndex: Long,
    settlementHeight: Int,
    protocolVersion: ProtocolVersion
  )

  implicit val poolQs: QuerySet[DBPoolSnapshot] = CFMMPoolSql

  implicit val poolView: Extract[ConfirmedIndexed[CFMMPool], DBPoolSnapshot] = {
    case ConfirmedIndexed(pool, LedgerMetadata(gix, height)) =>
      DBPoolSnapshot(
        PoolStateId.fromBoxId(pool.box.boxId),
        pool.poolId,
        pool.lp.id,
        pool.lp.value,
        pool.x.id,
        pool.x.value,
        pool.y.id,
        pool.y.value,
        pool.feeNum,
        gix,
        height,
        ProtocolVersion.Initial
      )
  }

  final case class DBSwap(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Option[Long],
    timestamp: Long,
    inputId: TokenId,
    inputValue: Long,
    minOutputId: TokenId,
    minOutputAmount: Long,
    outputAmount: Option[Long],
    dexFeePerTokenNum: Long,
    dexFeePerTokenDenom: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion
  )

  implicit val swapQs: QuerySet[DBSwap] = SwapOrdersSql

  implicit val swapView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnySwap, SwapEvaluation], DBSwap] = {
    case EvaluatedCFMMOrder(swap: AnySwap, ev, pool, _) =>
      val (minerFee, params) = swap match {
        case swap: SwapV0 => (None, swap.params)
        case swap: SwapV1 => (swap.maxMinerFee.some, swap.params)
      }
      DBSwap(
        OrderId.fromBoxId(swap.box.boxId),
        swap.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        minerFee,
        swap.timestamp,
        params.input.id,
        params.input.value,
        params.minOutput.id,
        params.minOutput.value,
        ev.map(_.output.value),
        params.dexFeePerTokenNum,
        params.dexFeePerTokenDenom,
        params.redeemer,
        ProtocolVersion.Initial,
        swap.version
      )
  }

  final case class DBRedeem(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Option[Long],
    timestamp: Long,
    lpId: TokenId,
    lpAmount: Long,
    outputAmountX: Option[Long],
    outputAmountY: Option[Long],
    dexFee: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion
  )

  implicit val redeemQs: QuerySet[DBRedeem] = RedeemOrdersSql

  implicit val redeemView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyRedeem, RedeemEvaluation], DBRedeem] = {
    case EvaluatedCFMMOrder(redeem, ev, pool, _) =>
      val (minerFee, params) = redeem match {
        case redeem: RedeemV0 => (None, redeem.params)
        case redeem: RedeemV1 => (redeem.maxMinerFee.some, redeem.params)
      }
      DBRedeem(
        OrderId.fromBoxId(redeem.box.boxId),
        redeem.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        minerFee,
        redeem.timestamp,
        params.lp.id,
        params.lp.value,
        ev.map(_.outputX.value),
        ev.map(_.outputY.value),
        params.dexFee,
        params.redeemer,
        ProtocolVersion.Initial,
        redeem.version
      )
  }

  final case class DBDeposit(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Option[Long],
    timestamp: Long,
    inputIdX: TokenId,
    inputAmountX: Long,
    inputIdY: TokenId,
    inputAmountY: Long,
    outputAmountLP: Option[Long],
    dexFee: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion
  )

  implicit val depositQs: QuerySet[DBDeposit] = DepositOrdersSql

  implicit val depositView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyDeposit, DepositEvaluation], DBDeposit] = {
    case EvaluatedCFMMOrder(deposit, ev, pool, _) =>
      val (minerFee, params) = deposit match {
        case deposit: DepositV0 => (None, deposit.params)
        case deposit: DepositV1 => (None, deposit.params)
        case deposit: DepositV2 => (deposit.maxMinerFee.some, deposit.params)
      }
      DBDeposit(
        OrderId.fromBoxId(deposit.box.boxId),
        deposit.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        minerFee,
        deposit.timestamp,
        params.inX.id,
        params.inX.value,
        params.inY.id,
        params.inY.value,
        ev.map(_.outputLP.value),
        params.dexFee,
        params.redeemer,
        ProtocolVersion.Initial,
        deposit.version
      )
  }

  final case class DBAssetInfo(
    tokenId: TokenId,
    ticker: Option[Ticker],
    decimals: Option[Int]
  )

  implicit val extractAssets: Extract[TokenInfo, DBAssetInfo] =
    ti => DBAssetInfo(ti.id, ti.name.map(Ticker.apply), ti.decimals)

  final case class DBOrderExecutorFee(
    poolId: PoolId,
    orderId: OrderId,
    outputId: BoxId,
    address: String,
    operatorFee: Long,
    timestamp: Long
  )

  implicit val orderExecutorFeeQs: QuerySet[DBOrderExecutorFee] = OrderExecutorFeeSql

  implicit val orderExecutorFeeView: Extract[OrderExecutorFee, DBOrderExecutorFee] =
    orderExecutorFee =>
      DBOrderExecutorFee(
        orderExecutorFee.poolId,
        orderExecutorFee.orderId,
        orderExecutorFee.outputId,
        orderExecutorFee.address,
        orderExecutorFee.operatorFee,
        orderExecutorFee.timestamp
      )

  @derive(loggable)
  final case class LiquidityProviderSnapshot(
    address: String,
    poolId: PoolId,
    lpId: TokenId,
    boxId: String,
    txId: String,
    blockId: String,
    balance: BigDecimal,
    timestamp: Long,
    weight: BigDecimal,
    op: String,
    amount: BigDecimal,
    gap: Long,
    lpErg: BigDecimal,
    txHeight: Long,
    poolStateId: String
  )

  object LiquidityProviderSnapshot {

    def initial(
      address: String,
      poolId: PoolId,
      lpId: TokenId,
      boxId: String,
      ts: Long,
      txId: String,
      blockId: String,
      txHeight: Long
    ): LiquidityProviderSnapshot =
      LiquidityProviderSnapshot(
        address     = address,
        poolId      = poolId,
        lpId        = lpId,
        boxId       = boxId,
        balance     = BigDecimal(0),
        timestamp   = ts,
        weight      = BigDecimal(0),
        op          = "init",
        amount      = 0,
        gap         = 0,
        lpErg       = 0,
        txHeight    = txHeight,
        txId        = txId,
        blockId     = blockId,
        poolStateId = ""
      )

    def resolved(
      address: String,
      poolId: PoolId,
      lpId: TokenId,
      ts: Long,
      weight: BigDecimal,
      gap: Long
    ): LiquidityProviderSnapshot =
      LiquidityProviderSnapshot(
        address     = address,
        poolId      = poolId,
        lpId        = lpId,
        boxId       = "resolved",
        txId        = "resolved",
        blockId     = "resolved",
        balance     = BigDecimal(0),
        timestamp   = ts,
        weight      = weight,
        op          = "Resolve",
        amount      = BigDecimal(0),
        gap         = gap,
        lpErg       = BigDecimal(0),
        txHeight    = 0,
        poolStateId = "resolved"
      )
  }
}
