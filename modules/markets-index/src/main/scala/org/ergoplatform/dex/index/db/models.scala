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

object models {

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
    redeemer: Option[PubKey],
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion,
    redeemerErgoTree: Option[SErgoTree]
  )

  implicit val swapQs: QuerySet[DBSwap] = SwapOrdersSql

  implicit val swapView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnySwap, SwapEvaluation], DBSwap] = {
    case EvaluatedCFMMOrder(swap: AnySwap, ev, pool, _) =>
      val (minerFee, params, redeemer, ergoTree) = swap match {
        case swap: SwapV0 => (None, swap.params, swap.params.redeemer.some, none)
        case swap: SwapV1 => (swap.maxMinerFee.some, swap.params, swap.params.redeemer.some, none)
        case swap: SwapV2 => (swap.maxMinerFee.some, swap.params, none, swap.params.redeemer.some)
        case swap: SwapV3 => (swap.maxMinerFee.some, swap.params, none, swap.params.redeemer.some)
      }
      DBSwap(
        OrderId.fromBoxId(swap.box.boxId),
        swap.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        minerFee,
        swap.timestamp,
        params.baseAmount.id,
        params.baseAmount.value,
        params.minQuoteAmount.id,
        params.minQuoteAmount.value,
        ev.map(_.output.value),
        params.dexFeePerTokenNum,
        params.dexFeePerTokenDenom,
        redeemer,
        ProtocolVersion.Initial,
        swap.version,
        ergoTree
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
    redeemer: Option[PubKey],
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion,
    redeemerErgoTree: Option[SErgoTree]
  )

  implicit val redeemQs: QuerySet[DBRedeem] = RedeemOrdersSql

  implicit val redeemView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyRedeem, RedeemEvaluation], DBRedeem] = {
    case EvaluatedCFMMOrder(redeem, ev, pool, _) =>
      val (minerFee, params, redeemer, ergTree) = redeem match {
        case redeem: RedeemV0 => (None, redeem.params, Some(redeem.params.redeemer), None)
        case redeem: RedeemV1 => (redeem.maxMinerFee.some, redeem.params, Some(redeem.params.redeemer), None)
        case redeem: RedeemV3 => (redeem.maxMinerFee.some, redeem.params, None, Some(redeem.params.redeemer))
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
        redeemer,
        ProtocolVersion.Initial,
        redeem.version,
        ergTree
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
    redeemer: Option[PubKey],
    protocolVersion: ProtocolVersion,
    contractVersion: CFMMOrderVersion,
    redeemerErgoTree: Option[SErgoTree]
  )

  implicit val depositQs: QuerySet[DBDeposit] = DepositOrdersSql

  implicit val depositView: Extract[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyDeposit, DepositEvaluation], DBDeposit] = {
    case EvaluatedCFMMOrder(deposit, ev, pool, _) =>
      val (minerFee, params, redeemer, ergTree) = deposit match {
        case deposit: DepositV0 => (None, deposit.params, Some(deposit.params.redeemer), None)
        case deposit: DepositV1 => (None, deposit.params, Some(deposit.params.redeemer), None)
        case deposit: DepositV2 => (deposit.maxMinerFee.some, deposit.params, Some(deposit.params.redeemer), None)
        case deposit: DepositV3 => (deposit.maxMinerFee.some, deposit.params, None, Some(deposit.params.redeemer))
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
        redeemer,
        ProtocolVersion.Initial,
        deposit.version,
        ergTree
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
}
