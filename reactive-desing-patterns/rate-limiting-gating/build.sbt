name := "rate-limiting-gating"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++={

  val akkaVersion = "2.6.16"
  val akkaHttpVersion = "10.2.6"

  Seq(

    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" %akkaVersion,
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" %akkaHttpVersion,

    "ch.qos.logback" % "logback-classic" % "1.2.6"
  )

}
