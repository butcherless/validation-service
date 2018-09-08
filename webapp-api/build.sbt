import sbt.Keys.libraryDependencies

lazy val projectVersion = "1.0.0-SNAPSHOT"

lazy val commonSettings = Seq(
  organization := "com.cmartin.learn",
  version := projectVersion,
  scalaVersion := "2.12.6"
)

// third party deps

lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion = "2.5.16"
lazy val catsVersion = "1.3.1"
lazy val logbackVersion = "1.2.3"
lazy val playJsonVersion = "2.6.10"
lazy val scalaLoggingVersion = "3.9.0"
lazy val scalazVersion = "7.2.26"
lazy val scalatestVersion = "3.0.5"
lazy val specs2Version = "4.3.4"
lazy val utestVersion = "0.6.4"

lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
lazy val akkaJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
lazy val cats = "org.typelevel" %% "cats-core" % catsVersion
lazy val catsFree = "org.typelevel" %% "cats-free" % catsVersion
lazy val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
lazy val playJson = "com.typesafe.play" % "play-json_2.12" % playJsonVersion
lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
lazy val scalaz = "org.scalaz" %% "scalaz-core" % scalazVersion

lazy val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
lazy val scalaTest = "org.scalatest" %% "scalatest" % scalatestVersion % "test"
lazy val specs2 = "org.specs2" %% "specs2-core" % specs2Version % "test"
lazy val uTest = "com.lihaoyi" %% "utest" % utestVersion % "test"

// app modules

lazy val common = (project in file("common"))
  .settings(
    commonSettings,
    name := "common",
    libraryDependencies ++= Seq(scalaTest)
  )

lazy val controller = (project in file("controller"))
  .settings(
    commonSettings,
    name := "controller",
    libraryDependencies ++= Seq(scalaTest)
  ).dependsOn(common, service)

lazy val repository = (project in file("repository"))
  .settings(
    commonSettings,
    name := "repository",
    libraryDependencies ++= Seq(scalaTest)
  ).dependsOn(common)

lazy val service = (project in file("service"))
  .settings(
    commonSettings,
    name := "service",
    libraryDependencies ++= Seq(playJson, scalaTest)
  ).dependsOn(common, repository)

lazy val web = (project in file("web"))
  .settings(
    commonSettings,
    name := "web",
    libraryDependencies ++= Seq(scalaTest)
  ).dependsOn(common, controller)
