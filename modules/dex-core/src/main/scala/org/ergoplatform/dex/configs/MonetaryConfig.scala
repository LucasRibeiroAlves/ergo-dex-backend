package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class MonetaryConfig(minerFee: Long, minDexFee: Long, minBoxValue: Long)

object MonetaryConfig extends WithContext.Companion[MonetaryConfig]
