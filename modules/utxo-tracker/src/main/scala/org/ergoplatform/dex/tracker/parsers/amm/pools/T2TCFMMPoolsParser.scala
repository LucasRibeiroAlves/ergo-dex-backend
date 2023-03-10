package org.ergoplatform.dex.tracker.parsers.amm.pools

import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.domain.{AssetAmount, BoxInfo}
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.ergo.domain.SConstant.IntConstant
import org.ergoplatform.ergo.domain.{Output, RegisterId}

object T2TCFMMPoolsParser extends CFMMPoolsParser[T2T_CFMM] {

  def pool(box: Output): Option[CFMMPool] =
    for {
      nft <- box.assets.lift(constants.cfmm.t2t.IndexNFT)
      lp  <- box.assets.lift(constants.cfmm.t2t.IndexLP)
      x   <- box.assets.lift(constants.cfmm.t2t.IndexX)
      y   <- box.assets.lift(constants.cfmm.t2t.IndexY)
      fee <- box.additionalRegisters.get(RegisterId.R4).collect { case IntConstant(x) => x }
    } yield CFMMPool(
      PoolId(nft.tokenId),
      AssetAmount.fromBoxAsset(lp),
      AssetAmount.fromBoxAsset(x),
      AssetAmount.fromBoxAsset(y),
      fee,
      BoxInfo.fromBox(box)
    )
}
