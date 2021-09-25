name := "websocket"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++={

  val akkaVersion = "2.6.16"
  val akkaHttpVersion = "10.2.6"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" %akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" %akkaVersion,
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-core" %akkaHttpVersion
  )

}
