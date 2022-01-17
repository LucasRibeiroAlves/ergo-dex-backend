package org.ergoplatform.dex.matcher

import cats.effect.{Blocker, Resource}
import fs2.Chunk
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.db.{PostgresTransactor, doobieLogging}
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.domain.orderbook._
import org.ergoplatform.dex.matcher.configs.ConfigBundle
import org.ergoplatform.dex.matcher.configs.ConfigBundle.promoteContextStructure
import org.ergoplatform.dex.matcher.processes.Matcher
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import org.ergoplatform.dex.matcher.services.{LimitOrderBook, OrderBook}
import org.ergoplatform.dex.matcher.streaming.StreamingBundle
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer, Producer}
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.doobie.instances.implicits._
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.Logs
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[ConfigBundle] {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    resources(args.headOption).use { case (matcher, ctx) =>
      val appF = matcher.run.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def resources(configPathOpt: Option[String]): Resource[InitF, (Matcher[StreamF], ConfigBundle)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      trans   <- PostgresTransactor.make("matcher-pool", configs.db)
      implicit0(xa: Txr.Contextual[RunF, ConfigBundle]) = Txr.contextual[RunF](trans)
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <-
        Resource.eval(doobieLogging.makeEmbeddableHandler[InitF, RunF, xa.DB]("matcher-db-logging"))
      implicit0(logsDb: Logs[InitF, xa.DB]) = Logs.sync[InitF, xa.DB]
      implicit0(isoKRun: IsoK[RunF, InitF]) = IsoK.byFunK(wr.runContextK(configs))(wr.liftF)
      implicit0(ordersRepo: OrdersRepo[xa.DB]) <- Resource.eval(OrdersRepo.make[InitF, xa.DB])
      implicit0(orderBook: OrderBook[RunF])    <- Resource.eval(LimitOrderBook.make[InitF, RunF, xa.DB])
      implicit0(mc: MakeKafkaConsumer[RunF, OrderId, AnyOrder]) = MakeKafkaConsumer.make[InitF, RunF, OrderId, AnyOrder]
      consumer                                                  = Consumer.make[StreamF, RunF, OrderId, AnyOrder](configs.consumer)
      producer <- Producer.make[InitF, StreamF, RunF, TradeId, AnyTrade](configs.producer)
      implicit0(bundle: StreamingBundle[StreamF, RunF]) = StreamingBundle(consumer, producer)
      matcher <- Resource.eval(Matcher.make[InitF, StreamF, RunF, Chunk])
    } yield matcher -> configs
}
