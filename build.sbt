import xerial.sbt.Sonatype._

ThisBuild / name := "fetch-file"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.3.2"

ThisBuild / publishTo := sonatypePublishToBundle.value

val catsEffectVersion = "3.3.12"
val fs2Version = "3.2.8"
val http4sVersion = "0.23.12"

val scalaTestVersion = "3.2.12"

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
      "org.scalacheck"    %% "scalacheck"      % "1.16.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-16" % "3.2.12.0" % Test,
      "org.typelevel"     %% "claimant"        % "0.2.0" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
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
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
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
