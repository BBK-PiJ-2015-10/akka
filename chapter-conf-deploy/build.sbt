name := "deploy"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(JavaAppPackaging)

scriptClasspath +="../conf"

libraryDependencies ++= {
  
  val akkaVersion = "2.5.25"
  
  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "ch.qos.logback" % "logback-classic" %"1.2.3",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion %"test",
    "org.scalatest" %% "scalatest" %"3.2.0-M1" %"test" 
  )
  
}