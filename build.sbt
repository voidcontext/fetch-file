import xerial.sbt.Sonatype._

ThisBuild / name := "fetch-file"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true

ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", "fetch-file", "gabor.pihaj@gmail.com")),
)

lazy val fetchfile = (project in file("fetch-file"))
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.1.2",
      "co.fs2" %% "fs2-core" % "2.2.2",
      "co.fs2" %% "fs2-io" % "2.2.2",

      "org.scalatest" %% "scalatest" % "3.1.1" % "test",
      "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
      "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test
    )
  )

lazy val examples = (project in file("examples"))
  .settings(
    skip in publish := true,
  )
  .dependsOn(fetchfile)

lazy val root = (project in file("."))
  .settings(
    skip in publish := true,
  )
  .aggregate(fetchfile, examples)
