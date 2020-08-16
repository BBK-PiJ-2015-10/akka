name := "akka-saga-pattern"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {
  
  val akkaVersion = "2.5.25"
  val akkaHttpVersion = "10.1.10"
  val akkaDynamoVersion = "1.1.1"
  
  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" %akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" %akkaHttpVersion,

   "com.typesafe.akka"  % "akka-persistence-dynamodb_2.12"     % akkaDynamoVersion,

    
    
    
    
  )
  
}