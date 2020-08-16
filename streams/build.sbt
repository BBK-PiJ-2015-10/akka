name := "streams"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {
  
  val akkaVersion = "2.5.25"
  val akkaHttpVersion = "10.1.10"
  
  Seq(
    "com.typesafe.akka" %% "akka-stream" %akkaVersion,
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-http-core" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" %akkaHttpVersion,
    
    "com.typesafe.akka" %% "akka-stream-testkit" %akkaVersion %"test",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion %"test",
    "org.scalatest" %%"scalatest" %"3.2.0-M1" %"test"
    
  )
  
}