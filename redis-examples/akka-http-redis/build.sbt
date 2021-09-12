name := "akka-http-redis"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++={

  val akkaVersion = "2.6.14"
  val akkaHttp = "10.2.4"
  val akkaCirceV = "1.36.0"
  val logV= "1.2.3"
  val xtractV = "2.2.1"
  val circeV = "0.13.0"
  val scalaTestV = "3.2.9"


  Seq(

    //REDIS
    "com.github.scredis" %% "scredis" % "2.4.3",

    //HTTP SERVER
    //"com.typesafe.akka" %% "akka-http"  %akkaHttp,
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-stream" %akkaVersion,

    //LOGGING
    "ch.qos.logback" % "logback-classic" % logV,

    // XML serializer Xtract
    "com.lucidchart" %% "xtract" % xtractV,


    // JSON serialization library
    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser" % circeV,

    // Sugar for serialization and deserialization in akka-http with circe
    "de.heikoseeberger" %% "akka-http-circe" % akkaCirceV,


    "org.scalatest" %% "scalatest" % "3.2.9" % Test

  )




}
