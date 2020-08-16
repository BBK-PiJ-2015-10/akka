name := "cluster-samples"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies ++= {

  val akkaVersion = "2.6.5"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" %akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" %akkaVersion,
    "ch.qos.logback"    %  "logback-classic"             % "1.2.3",
  )

}
