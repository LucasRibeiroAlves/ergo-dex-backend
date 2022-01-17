package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.RotationConfig
import org.ergoplatform.dex.configs._
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader, loggable)
@ClassyOptics
final case class ConfigBundle(
  @promote rotation: RotationConfig,
  @promote exchange: ExchangeConfig,
  @promote execution: ExecutionConfig,
  @promote monetary: MonetaryConfig,
  @promote protocol: ProtocolConfig,
  orders: ConsumerConfig,
  ordersRetryIn: ConsumerConfig,
  ordersRetryOut: ProducerConfig,
  @promote kafka: KafkaConfig,
  @promote network: NetworkConfig,
  @promote resolver: ResolverConfig
)

object ConfigBundle extends WithContext.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle]
