package org.ergoplatform.dex.index.processes

import cats.Applicative
import cats.data.NonEmptyList
import cats.syntax.option.none
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder.{AnyDeposit, AnyRedeem, AnySwap}
import org.ergoplatform.dex.domain.amm.{CFMMVersionedOrder, EvaluatedCFMMOrder}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBOrderExecutorFee, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repositories.{MonoRepo, RepoBundle}
import tofu.syntax.monadic._

trait AnyOrdersHandler[F[_]] {
  def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int]
}

object AnyOrdersHandler {

  def makeOrdersHandlers[F[_]: Applicative](implicit
    repos: RepoBundle[F]
  ): List[AnyOrdersHandler[F]] =
    new SwapHandler[F](repos.swaps) :: new RedeemHandler[F](repos.redeems) :: new DepositHandler[F](
      repos.deposits
    ) :: new OrderExecutorFeeHandler[F](repos.orderExecutorFee) :: Nil

  final private class OrderExecutorFeeHandler[F[_]: Applicative](offChain: MonoRepo[DBOrderExecutorFee, F])
    extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      anyOrders
        .flatMap(_.orderExecutorFee)
        .map(_.extract[DBOrderExecutorFee]) match {
        case Nil          => 0.pure
        case ::(head, tl) => offChain.insert(NonEmptyList(head, tl))
      }
  }

  final class SwapHandler[F[_]: Applicative](repo: MonoRepo[DBSwap, F]) extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV3, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV2, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV1, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV0, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV1, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV2, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV3, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
      }) match {
        case Some(nel) => repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

  final class RedeemHandler[F[_]: Applicative](repo: MonoRepo[DBRedeem, F]) extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV3, Some(ev: RedeemEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, Some(ev), p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV1, Some(ev: RedeemEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, Some(ev), p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV0, Some(ev: RedeemEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, Some(ev), p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV3, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, none[RedeemEvaluation], p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV1, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, none[RedeemEvaluation], p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, none[RedeemEvaluation], p, r).extract[DBRedeem]
      }) match {
        case Some(nel) => repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

  final class DepositHandler[F[_]: Applicative](repo: MonoRepo[DBDeposit, F]) extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV0, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV1, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV2, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV3, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV1, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV2, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV3, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
      }) match {
        case Some(nel) => repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

}
