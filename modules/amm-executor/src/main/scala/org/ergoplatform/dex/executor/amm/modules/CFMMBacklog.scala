package org.ergoplatform.dex.executor.amm.modules

import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}

trait CFMMBacklog[F[_]] {

  /** Put an order to the backlog.
    */
  def put(order: CFMMOrder.AnyOrder): F[Unit]

  /** Get candidate order for execution. Blocks until an order is available.
    */
  def get: F[CFMMOrder.AnyOrder]

  /** Put an order from the backlog.
    */
  def drop(id: OrderId): F[Unit]
}
