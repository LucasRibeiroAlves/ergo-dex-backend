package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, NetworkConfig, ProducerConfig, ProtocolConfig}
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  @promote producer: ProducerConfig,
  topics: Topics,
  @promote protocol: ProtocolConfig,
  @promote network: NetworkConfig,
  @promote tracker: TrackerConfig,
  @promote fees: Fees
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
