import xerial.sbt.Sonatype._

ThisBuild / name := "fetch-file"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.3.2"

ThisBuild / publishTo := sonatypePublishToBundle.value

val catsEffectVersion = "2.1.2"
val fs2Version = "2.2.2"
val http4sVersion = "0.21.1"

val scalaTestVersion = "3.1.1"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", "fetch-file", "gabor.pihaj@gmail.com"))
)

lazy val fetchfile = (project in file("fetch-file"))
  .settings(publishSettings)
  .settings(
    name := "fetch-file",
    libraryDependencies ++= Seq(
      "org.typelevel"     %% "cats-effect"     % catsEffectVersion,
      "co.fs2"            %% "fs2-core"        % fs2Version,
      "co.fs2"            %% "fs2-io"          % fs2Version,
      "org.scalatest"     %% "scalatest"       % scalaTestVersion % Test,
      "org.scalacheck"    %% "scalacheck"      % "1.14.1" % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % "3.1.1.1" % Test,
      "org.typelevel"     %% "claimant"        % "0.1.3" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    addCompilerPlugin(scalafixSemanticdb)
  )

lazy val fetchfileHttp4s = (project in file("fetch-file-http4s"))
  .settings(publishSettings)
  .settings(
    name := "fetch-file-http4s",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"         % catsEffectVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.http4s"    %% "http4s-blaze-client" % http4sVersion,
      "org.scalatest" %% "scalatest"           % scalaTestVersion % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    addCompilerPlugin(scalafixSemanticdb)
  )
  .dependsOn(fetchfile)

lazy val examples = (project in file("examples"))
  .settings(
    skip in publish := true,
    addCompilerPlugin(scalafixSemanticdb)
  )
  .dependsOn(fetchfile)

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(fetchfile, fetchfileHttp4s, examples)

addCommandAlias("prePush", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt; reload ;clean ;test")
