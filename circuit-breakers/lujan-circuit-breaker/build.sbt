name := "lujan-circuit-breaker"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++={

  val akkaVersion = "2.6.6"

  Seq (
    "com.typesafe.akka" %% "akka-actor" %akkaVersion
  )



}
