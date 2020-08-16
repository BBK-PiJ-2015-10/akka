name := "akka-simple-cluster-k8s"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Resolver.bintrayRepo("tanukkii007", "maven")

enablePlugins(JavaServerAppPackaging, DockerPlugin)

dockerBaseImage := "openjdk:8"
dockerUsername := Some("softwaremill")


libraryDependencies ++= {
  
  val akkaVersion = "2.5.24"
  val akkaHttpVersion = "10.1.10"
  val akkaManagementVersion = "1.0.4"
  
  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-cluster" %akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    
    "com.typesafe.akka" %% "akka-http-core" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" %akkaHttpVersion,
    
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" %akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management" %akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" %akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" %akkaManagementVersion,
    
    "com.typesafe.akka" %% "akka-discovery" %"2.6.1",
    
    "com.github.TanUkkii007" %%"akka-cluster-custom-downing" % "0.0.13"
    
    
  )
  
  
}






