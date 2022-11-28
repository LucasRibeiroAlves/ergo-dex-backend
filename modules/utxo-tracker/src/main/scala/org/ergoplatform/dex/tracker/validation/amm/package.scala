package org.ergoplatform.dex.tracker.validation

import cats.{FlatMap, Monad}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.higherKind.Embed
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

package object amm {

  type RuleViolation = String

  type CFMMRules[F[_]] = CFMMOrder.AnyOrder => F[Option[RuleViolation]]

  implicit def embed: Embed[CFMMRules] =
    new Embed[CFMMRules] {

      def embed[F[_]: FlatMap](ft: F[CFMMRules[F]]): CFMMRules[F] =
        op => ft >>= (rules => rules(op))
    }

  object CFMMRules {

    def make[F[_]: Monad: MonetaryConfig.Has]: CFMMRules[F] =
      (MonetaryConfig.access map (conf => new CfmmRuleDefs[F](conf).rules)).embed
  }
}
