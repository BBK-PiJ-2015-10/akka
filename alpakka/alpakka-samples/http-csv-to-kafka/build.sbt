name := "http-csv-to-kafka"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++={

  val AkkaVersion = "2.6.4"
  val AkkaHttpVersion = "10.1.11"
  val AlpakkaVersion = "1.1.2"
  val AlpakkaKafkaVersion = "2.0.2"

  Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" %AlpakkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" %AlpakkaKafkaVersion,
    "com.typesafe.akka" %% "akka-stream" %AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" %AkkaVersion,

    "com.typesafe.akka" %% "akka-http" %AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" %AkkaHttpVersion,

    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3"

  )

}
