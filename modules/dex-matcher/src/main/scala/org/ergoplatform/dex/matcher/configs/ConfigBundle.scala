package org.ergoplatform.dex.matcher.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ConsumerConfig, KafkaConfig, ProducerConfig}
import tofu.{Context, WithContext}
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote matcher: MatcherConfig,
  @promote db: PgConfig,
  @promote commitPolicy: CommitPolicy,
  @promote consumer: ConsumerConfig,
  producer: ProducerConfig,
  @promote kafka: KafkaConfig
)

object ConfigBundle extends WithContext.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
