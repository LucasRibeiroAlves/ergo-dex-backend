package org.ergoplatform.dex.tracker.validation.amm

import cats.Applicative
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType.ErgFee
import org.ergoplatform.dex.domain.amm.CFMMOrderType.SwapType
import org.ergoplatform.ergo.PubKey
import tofu.syntax.embed._
import tofu.syntax.monadic._

import scala.{PartialFunction => ?=>}

final class CfmmRuleDefs[F[_]: Applicative](conf: MonetaryConfig) {

  type CFMMRule = CFMMOrder.Any ?=> Option[RuleViolation]

  def rules: CFMMRules[F] = op => allRules.lift(op).flatten.pure

  private val allRules = sufficientValueDepositRedeem orElse sufficientValueSwap

  private def sufficientValueDepositRedeem: CFMMRule = { //todo
    case s:Deposit[ErgFee, PubKey]  => checkFee(s.params.dexFee)
    case s:Deposit[ErgFee, PubKey]  => checkFee(s.params.dexFee)
  }

  private def sufficientValueSwap: CFMMRule = { case s:Swap[SwapType.SwapErgFee, PubKey] =>
//    val minDexFee   = BigInt(params.dexFeePerTokenNum) * params.minQuoteAmount.value / params.dexFeePerTokenDenom
//    val nativeInput = if (params.baseAmount.isNative) params.baseAmount.value else 0L
//    val minerFee    = conf.minerFee min maxMinerFee
//    val maxDexFee   = box.value - conf.minBoxValue - nativeInput
//    val insufficientValue =
//      if (maxDexFee >= minDexFee) None
//      else Some(s"Actual fee '$maxDexFee' is less than declared minimum '$minDexFee'")
//    val maxDexFeeNet    = maxDexFee - minerFee
//    val insufficientFee = checkFee(maxDexFeeNet)
//    insufficientFee orElse insufficientValue
    ???
  }

  private def checkFee(givenFee: BigInt): Option[RuleViolation] =
    if (givenFee >= conf.minDexFee) None
    else Some(s"Declared fee '$givenFee' is less than configured minimum '${conf.minDexFee}'")
}
