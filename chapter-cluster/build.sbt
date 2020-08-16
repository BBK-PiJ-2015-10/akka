name := "words-cluster"

version := "0.1"

scalaVersion := "2.13.1"


libraryDependencies ++={
  
  val akkaVersion = "2.5.24"
  
  Seq(
    
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" %akkaVersion,
    "com.typesafe.akka" %% "akka-remote" %akkaVersion,

    "ch.qos.logback" % "logback-classic" %"1.2.3" % "test",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion %"test",
  
    "org.scalatest" %% "scalatest" %"3.2.0-M1" % "test"
    
  )
  
}


mainClass in assembly := Some("aia.cluster.words.Main")

assemblyJarName in assembly := "words-node.jar"


