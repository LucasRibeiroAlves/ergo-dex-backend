package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs._
import org.ergoplatform.dex.tracker.configs.{TxTrackerConfig, UtxoTrackerConfig}
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  cfmmHistoryConsumer: ConsumerConfig,
  cfmmPoolsConsumer: ConsumerConfig,
  cfmmHistoryProducer: ProducerConfig,
  cfmmPoolsProducer: ProducerConfig,
  @promote db: PgConfig,
  @promote kafka: KafkaConfig,
  @promote protocol: ProtocolConfig,
  @promote network: NetworkConfig,
  @promote utxoTracker: UtxoTrackerConfig,
  @promote txTracker: TxTrackerConfig,
  redis: RedisConfig
)

object ConfigBundle extends WithContext.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
