import sbt._

object Dependencies {

  val scalaVersion = "2.13.1"
  val akkaVersion = "2.5.25"
  val akkaHttpVersion = "10.1.10"
  val circleVersion = "0.13.0"

  object Compile {

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  // val akkaCluster =  "com.typesafe.akka" %% "akka-cluster" %akkaVersion
  val akkaDistData = "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  val httpCircle = "de.heikoseeberger" %% "akka-http-circe" % "1.28.0"

    val circleCore = "io.circe" %% "circe-core" % circleVersion
    val circleGeneric = "io.circe" %% "circe-generic" % circleVersion
    val circleParser = "io.circe" %% "circe-parser" % circleVersion

  val all = Seq(akkaActor,akkaRemote,akkaDistData,akkaHttpCore,akkaHttp,circleCore,circleGeneric,circleParser,httpCircle)

  }

  object Test {

    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0-M1" % "test"

    val all= Seq(akkaTestKit,scalaTest)

  }

}
