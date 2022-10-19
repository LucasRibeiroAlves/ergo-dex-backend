package org.ergoplatform.dex.index.repositories

import cats.data.NonEmptyList
import cats.{Functor, Monad}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.index.db.models.{LiquidityProviderSnapshot, PoolSnapshot}
import org.ergoplatform.dex.index.sql.LiquidityProvidersSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import cats.tagless.syntax.functorK._

@derive(representableK)
trait LiquidityProvidersRepo[F[_]] extends MonoRepo[LiquidityProviderSnapshot, F] {

  def getLatestLiquidityProviderSnapshot(address: String, poolId: PoolId): F[Option[LiquidityProviderSnapshot]]

  def getLatestPoolSnapshot(poolId: PoolId, to: Long): F[Option[PoolSnapshot]]
}

object LiquidityProvidersRepo {

  def make[I[_]: Functor, F[_], D[_]: Monad: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D],
    txr: Txr.Aux[F, D]
  ): I[LiquidityProvidersRepo[F]] =
    logs.forService[LiquidityProvidersRepo[D]].map { implicit __ =>
      elh
        .embed(implicit lh => new Tracing[D] attach new Live().mapK(LiftConnectionIO[D].liftF))
        .mapK(txr.trans)
    }

  final private class Live(implicit lh: LogHandler) extends LiquidityProvidersRepo[ConnectionIO] {

    def getLatestLiquidityProviderSnapshot(
      address: String,
      poolId: PoolId
    ): ConnectionIO[Option[LiquidityProviderSnapshot]] =
      LiquidityProvidersSql.getLatestLiquidityProviderSnapshot(address, poolId).option

    def getLatestPoolSnapshot(poolId: PoolId, to: Long): ConnectionIO[Option[PoolSnapshot]] =
      LiquidityProvidersSql.getLatestPoolSnapshot(poolId, to).option

    def insert(entities: NonEmptyList[LiquidityProviderSnapshot]): ConnectionIO[Int] =
      LiquidityProvidersSql.insertNoConflict.updateMany(entities)
  }

  final private class Tracing[F[_]: Monad: Logging] extends LiquidityProvidersRepo[Mid[F, *]] {

    def getLatestPoolSnapshot(poolId: PoolId, to: Long): Mid[F, Option[PoolSnapshot]] =
      for {
        _ <- info"getLatestPoolSnapshot($poolId, $to)"
        r <- _
        _ <- info"getLatestPoolSnapshot($poolId, $to) -> $r"
      } yield r

    def getLatestLiquidityProviderSnapshot(address: String, poolId: PoolId): Mid[F, Option[LiquidityProviderSnapshot]] =
      for {
        _ <- info"getLatestLiquidityProviderSnapshot($address, $poolId)"
        r <- _
        _ <- info"getLatestLiquidityProviderSnapshot($address, $poolId) -> $r"
      } yield r

    def insert(entities: NonEmptyList[LiquidityProviderSnapshot]): Mid[F, Int] =
      trace"Going to insert new lp state ${entities.toList}" *> _.flatTap(r => trace"Insert finished for $r entities.")
  }

}