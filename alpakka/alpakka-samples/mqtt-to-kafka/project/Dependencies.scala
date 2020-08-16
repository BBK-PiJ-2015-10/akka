import sbt._

object Dependencies {

  val AkkaVersion = "2.6.8"
  val AlpakkaVersion = "2.0.1"
  val AlpakkaKafkaVersion = "2.0.4"
  val JacksonDatabindVersion = "2.11.1"

  val dependencies = List(
    "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" %AlpakkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" %AlpakkaKafkaVersion,
    "com.typesafe.akka" %% "akka-stream" %AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" %AkkaVersion,
    "com.typesafe.akka" %% "akka-actor" %AkkaVersion,
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6",
    // "org.scala-lang.modules" %% "scala-collection-compact" % "2.1.6"

    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % JacksonDatabindVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonDatabindVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonDatabindVersion,

    "com.typesafe.akka" %% "akka-slf4j" %AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    // "org.testcontainers" % "kafka" % "1.14.3"

  )




}
