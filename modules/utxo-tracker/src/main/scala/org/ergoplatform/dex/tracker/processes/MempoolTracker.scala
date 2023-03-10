package org.ergoplatform.dex.tracker.processes

import cats.effect.{Clock, Timer}
import cats.{Defer, FlatMap, Monad, MonoidK}
import org.ergoplatform.common.data.TemporalFilter
import org.ergoplatform.dex.tracker.configs.MempoolTrackingConfig
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.modules.MempoolStreaming
import org.ergoplatform.ergo.services.explorer.models.Transaction
import org.ergoplatform.ergo.services.node.ErgoNode
import tofu.Catches
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.handle._
import tofu.syntax.streams.all.{eval, _}
import cats.syntax.traverse._

import scala.concurrent.duration._

/** Tracks UTxOs from mempool.
  */
final class MempoolTracker[
  F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
  G[_]: Monad: Logging: Timer
](conf: MempoolTrackingConfig, filter: TemporalFilter[G], handlers: List[BoxHandler[F]])(implicit
  mempool: MempoolStreaming[F]
) extends UtxoTracker[F] {

  def run: F[Unit] = {
    def sync: F[Unit] =
      for {
        output  <- mempool.streamUnspentOutputs
        known   <- eval(filter.probe(output.boxId))
        (n, mx) <- eval(filter.inspect)
        _       <- eval(debug"MempoolFilter{N=$n, MX=$mx}")
        _ <- if (!known)
               eval(debug"Scanning unconfirmed output $output") >>
               emits(handlers.map(_(output.pure[F]))).parFlattenUnbounded
             else unit[F]
      } yield ()

    sync.repeat
      .throttled(conf.samplingInterval)
      .handleWith[Throwable](e => eval(warnCause"Mempool Tracker failed, restarting .." (e)) >> run)
  }
}

object MempoolTracker {

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: MempoolTrackingConfig.Has: Catches,
    G[_]: Monad: Clock: Timer
  ](handlers: BoxHandler[F]*)(implicit
    mempool: MempoolStreaming[F],
    logs: Logs[I, G],
    makeRef: MakeRef[I, G]
  ): I[UtxoTracker[F]] =
    for {
      implicit0(l: Logging[G]) <- logs.forService[MempoolTracker[F, G]]
      filter                   <- TemporalFilter.make[I, G](30.minutes, 12)
      tracker = MempoolTrackingConfig.access
                  .map(conf => new MempoolTracker[F, G](conf, filter, handlers.toList): UtxoTracker[F])
                  .embed
    } yield tracker
}
