package org.ergoplatform.dex.markets.services

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.syntax.option._
import org.ergoplatform.common.caching.Memoize
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.services.FiatRates.ErgoOraclesRateSource
import org.ergoplatform.dex.protocol.constants.ErgoAssetClass
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{SttpBackend, UriContext}
import tofu.logging.{Logging, Logs}
import tofu.{Context, WithContext}
import tofu.fs2Instances._
import scala.concurrent.duration.FiniteDuration

class FiatRatesSpec extends AnyPropSpec with should.Matchers with CatsPlatform {

  implicit val conf: WithContext[IO, NetworkConfig] =
    Context.const(NetworkConfig(uri"https://api.ergoplatform.com", uri"http://127.0.0.1:9053"))

  implicit val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  def constantMemo[A](const: Option[A]): Memoize[IO, A] = new Memoize[IO, A] {
    override def read: IO[Option[A]]                          = IO.pure(const)
    override def memoize(a: A, ttl: FiniteDuration): IO[Unit] = IO.unit
  }

  property("Get ERG/USD rate") {
    type S[A] = fs2.Stream[IO, A]
    val test = for {
      implicit0(back: SttpBackend[IO, Any]) <- AsyncHttpClientCatsBackend[IO]()
      network                               <- ErgoExplorer.make[IO, IO]
      ref                                   <- Ref.of[IO, Option[BigDecimal]](none)
      implicit0(logging: Logging[IO])       <- logs.forService[Unit]
      rates = new ErgoOraclesRateSource[IO](network, ref)
      ergUsdRate <- rates.rateOf(ErgoAssetClass, UsdUnits)
      _          <- IO.delay(println(ergUsdRate))
    } yield ()
    test.unsafeRunSync()
  }
}
