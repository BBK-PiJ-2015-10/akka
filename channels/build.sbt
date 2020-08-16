import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

name := "channels"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.5.25"


lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  

libraryDependencies ++= {
  
  val akkaVersion = "2.5.25"
  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-remote" %akkaVersion,
    "com.typesafe.akka" %% "akka-multi-node-testkit" %akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" %akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "3.2.0-M1" % "test"
  )
  
}







  
  


