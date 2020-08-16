name := "akka-example"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(JavaServerAppPackaging,DockerPlugin)

dockerBaseImage := "openjdk:8"


libraryDependencies ++= {
  
  val akkaVersion = "2.5.24"
  
  Seq (
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion
  )
  
}