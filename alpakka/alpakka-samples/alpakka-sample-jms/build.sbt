name := "alpakka-sample-jms"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++={

  val AkkaVersion ="2.6.4"
  val AkkaHttpVersion = "10.1.11"
  val AlpakkaVersion = "1.1.2"

  Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-jms" % AlpakkaVersion,
    "com.typesafe.akka"  %% "akka-stream" %AkkaVersion,
    "com.typesafe.akka"  %% "akka-actor" %AkkaVersion,

    //Logging
    "com.typesafe.akka" %% "akka-slf4j" %AkkaVersion,
    "ch.qos.logback" % "logback-classic" %"1.3.0-alpha5"


  )

}
