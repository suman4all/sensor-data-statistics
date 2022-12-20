name := "sensor-data-statistics"

ThisBuild / version := "1.0.0"
ThisBuild / scalaVersion := "2.12.12"
ThisBuild / organization := "com.org"
ThisBuild / organizationName := "XYZ Organization"
ThisBuild / useCoursier := false

val akkaVersion = "2.5.26"
val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

)
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "0.18"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "2.0.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"