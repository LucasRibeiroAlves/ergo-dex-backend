package org.ergoplatform.dex.tracker

import cats.Functor
import org.ergoplatform.ergo.domain.{Block, Output, SettledOutput, SettledTransaction, Transaction}
import tofu.syntax.monadic._

package object handlers {
  type BoxHandler[F[_]]        = F[Output] => F[Unit]
  type SettledBoxHandler[F[_]] = F[SettledOutput] => F[Unit]
  type TxHandler[F[_]]         = F[Transaction] => F[Unit]
  type SettledTxHandler[F[_]]  = F[SettledTransaction] => F[Unit]
  type BlockHandler[F[_]]      = F[Block] => F[Unit]

  def lift[F[_]: Functor](bh: BoxHandler[F]): SettledBoxHandler[F] = fa => bh(fa.map(_.output))
}
