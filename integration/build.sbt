name := "integration"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++={
  
  val akkaVersion = "2.5.25"
  val camelVersion = "2.24.2"
  val akkaHttpVersion = "10.1.10"
  val activeMQVersion= "5.15.10"
    
  
  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" % "akka-camel_2.13" % "2.5.26",
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" %akkaVersion,
    
    "net.liftweb" % "lift-json_2.13" % "3.4.0",
    
    "commons-io" % "commons-io" % "2.6",
    
    "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" %"10.1.10" % "test",
    "org.scalatest" %% "scalatest" % "3.2.0-M1" % "test",
    "org.apache.camel" %"camel-mina" %camelVersion % "test", 
    "org.apache.camel" %"camel-jetty" %camelVersion % "test",
    
    "org.apache.activemq" %"activemq-camel" %activeMQVersion %"test",
    "org.apache.activemq" %"activemq-client" %activeMQVersion %"test",
    "org.apache.activemq" %"activemq-broker" %activeMQVersion %"test"
    
  )
  
}
