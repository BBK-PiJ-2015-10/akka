name := "persistence"

version := "0.1"

scalaVersion := "2.13.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++={
  
  val akkaVersion = "2.5.25"
  val akkaHttpVersion = "10.1.10"
  
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" %akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" %akkaVersion,
    //"com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-http-spray-json" %akkaHttpVersion,

    "ch.qos.logback"    %  "logback-classic"             % "1.2.3",
    
    "org.iq80.leveldb" % "leveldb" % "0.12",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    
    "commons-io" % "commons-io" %"2.6", 
    
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" %"3.2.0-M1" % "test"
    
  )
  
}