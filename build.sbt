ThisBuild / name := "fetch-file"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.1"

lazy val fetchfile = (project in file("fetch-file"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.1.2",
      "co.fs2" %% "fs2-core" % "2.2.2",
      "co.fs2" %% "fs2-io" % "2.2.2",

      "org.scalatest" %% "scalatest" % "3.1.1" % "test"
    )
  )

lazy val root = (project in file("."))
  .aggregate(fetchfile)
