import xerial.sbt.Sonatype._

ThisBuild / name := "fetch-file"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true

ThisBuild / publishTo := sonatypePublishToBundle.value

val catsEffectVersion = "2.1.2"
val fs2Version = "2.2.2"
val http4sVersion = "0.21.1"

val scalaTestVersion = "3.1.1"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", "fetch-file", "gabor.pihaj@gmail.com")),
)

lazy val fetchfile = (project in file("fetch-file"))
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,

      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalacheck" %% "scalacheck" % "1.14.1" % Test,
      "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )

lazy val fetchfileHttp4s = (project in file("fetch-file-http4s"))
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,

      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )
  .dependsOn(fetchfile)

lazy val examples = (project in file("examples"))
  .settings(
    skip in publish := true,
  )
  .dependsOn(fetchfile)

lazy val root = (project in file("."))
  .settings(
    skip in publish := true,
  )
  .aggregate(fetchfile, fetchfileHttp4s, examples)
