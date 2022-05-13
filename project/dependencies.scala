import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  import versions._

  val SttpCore =
    List(
      "com.softwaremill.sttp.client3" %% "core"           % SttpVersion,
      "com.softwaremill.sttp.client3" %% "circe"          % SttpVersion,
      "com.softwaremill.sttp.client3" %% "okhttp-backend" % SttpVersion % Test
    )

  val OkHttp = List("com.softwaremill.sttp.client3" %% "okhttp-backend" % SttpVersion)

  val SttpClientFs2 =
    List(
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2-ce2" % SttpVersion
    )

  val SttpClientCE =
    List(
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats-ce2" % SttpVersion
    )

  val TapirCore = List(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion
  )

  val Tapir = List(
    "com.softwaremill.sttp.tapir" %% "tapir-core"       % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion
  )

  val TapirDocs = List(
    "com.softwaremill.sttp.tapir" %% "tapir-core"               % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-redoc-http4s"       % TapirVersion
  )

  val TapirHttp4s = List(
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % TapirVersion
  )

  val Http4sServer = List("org.http4s" %% "http4s-blaze-server" % Http4sVersion)

  val Circe: List[ModuleID] =
    List(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-refined"
    ).map(_ % CirceVersion)

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core"           % CatsVersion,
    "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
    "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
    "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
    "org.typelevel" %% "mouse"               % MouseVersion
  )

  val Redis = List(
    "dev.profunktor" %% "redis4cats-effects" % Redis4CatsVersion
  )

  val ScodecCore = List(
    "org.scodec" %% "scodec-core" % ScodecCoreVersion
  )

  val Scodec = List(
    "org.scodec" %% "scodec-core" % ScodecCoreVersion,
    "org.scodec" %% "scodec-bits" % ScodecBitsVersion,
    "org.scodec" %% "scodec-cats" % ScodecCatsVersion
  )

  val Monocle: List[ModuleID] = List(
    "com.github.julien-truffaut" %% "monocle-core"  % MonocleVersion,
    "com.github.julien-truffaut" %% "monocle-macro" % MonocleVersion
  )

  val Fs2: List[ModuleID] = List("co.fs2" %% "fs2-core" % Fs2Version)

  val JawnFs2: List[ModuleID] = List("org.http4s" %% "jawn-fs2" % JawnFs2Version)

  val Kafka: List[ModuleID] = List(
    "com.github.fd4s"           %% "fs2-kafka"        % Fs2KafkaVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion
  )

  val RocksDB = List(
    "io.github.oskin1" %% "rocks4cats-scodec" % Rocks4CatsVersion
  )

  val Tofu: List[ModuleID] = List(
    "tf.tofu" %% "tofu-core"         % TofuVersion,
    "tf.tofu" %% "tofu-concurrent"   % TofuVersion,
    "tf.tofu" %% "tofu-optics-core"  % TofuVersion,
    "tf.tofu" %% "tofu-optics-macro" % TofuVersion,
    "tf.tofu" %% "tofu-derivation"   % TofuVersion,
    "tf.tofu" %% "tofu-logging"      % TofuVersion,
    "tf.tofu" %% "tofu-doobie"       % TofuVersion,
    "tf.tofu" %% "tofu-streams"      % TofuVersion,
    "tf.tofu" %% "tofu-fs2-interop"  % TofuVersion,
    "tf.tofu" %% "tofu-zio-interop"  % TofuVersion
  )

  val Magnolia = List("com.propensive" %% "magnolia" % MagnoliaVersion)

  val Derevo: List[ModuleID] = List(
    "tf.tofu" %% "derevo-cats"              % DerevoVersion,
    "tf.tofu" %% "derevo-cats-tagless"      % DerevoVersion,
    "tf.tofu" %% "derevo-circe"             % DerevoVersion,
    "tf.tofu" %% "derevo-pureconfig-legacy" % DerevoVersion
  )

  val Ergo: List[ModuleID] = List(
    "org.scorexfoundation" %% "sigma-state" % SigmaStateVersion,
    "org.ergoplatform"     %% "ergo-wallet" % ErgoWalletVersion,
    "org.ergoplatform"     %% "contracts"   % ErgoContractsVersion
  )

  val Db: List[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"      % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres"  % DoobieVersion,
    "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % DoobieVersion,
    "org.tpolecat" %% "doobie-refined"   % DoobieVersion,
    "org.flywaydb"  % "flyway-core"      % FlywayVersion
  )

  val Testing: List[ModuleID] = List(
    "org.tpolecat"               %% "doobie-scalatest"          % DoobieVersion                 % Test,
    "org.scalatest"              %% "scalatest"                 % ScalaTestVersion              % Test,
    "org.scalatestplus"          %% "scalacheck-1-14"           % ScalaTestPlusVersion          % Test,
    "org.testcontainers"          % "postgresql"                % TestContainersPostgresVersion % Test,
    "com.dimafeng"               %% "testcontainers-scala"      % TestContainersScalaVersion    % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % ScalaCheckShapelessVersion    % Test
  )

  val Typing: List[ModuleID] = List(
    "io.estatico" %% "newtype"      % NewtypeVersion,
    "eu.timepit"  %% "refined"      % RefinedVersion,
    "eu.timepit"  %% "refined-cats" % RefinedVersion
  )

  val Enums: List[ModuleID] = List(
    "com.beachape" %% "enumeratum"       % EnumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion
  )

  val Config: List[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion
  )

  val Simulacrum: List[ModuleID] = List(
    "com.github.mpilquist" %% "simulacrum" % SimulacrumVersion
  )

  val CompilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full
      ),
      compilerPlugin(
        "org.scalamacros" % "paradise" % MacroParadiseVersion cross CrossVersion.full
      ),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
}
