package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs._
import tofu.{Context, WithContext}
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
                               @promote commitPolicy: CommitPolicy,
                               ordersProducer: ProducerConfig,
                               cfmmOpsProducer: ProducerConfig,
                               cfmmPoolsProducer: ProducerConfig,
                               @promote kafka: KafkaConfig,
                               @promote protocol: ProtocolConfig,
                               @promote network: NetworkConfig,
                               @promote tracker: UtxoTrackerConfig,
                               @promote monetary: MonetaryConfig,
                               redis: RedisConfig
)

object ConfigBundle extends WithContext.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
