name := "akka-map-reduce"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++={

  val akkaVersion = "2.6.8"
  val akkaHttpVersion = "10.2.0"

  Seq(
    "com.typesafe.akka" %%"akka-stream" %akkaVersion,
    "com.typesafe.akka" %%"akka-actor" %akkaVersion,
    "com.typesafe.akka" %%"akka-http" %akkaHttpVersion,

    "ch.qos.logback" %"logback-classic" % "1.2.3"


  )

}
