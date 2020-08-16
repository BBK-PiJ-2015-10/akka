name := "record-analyzer"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++={

  //val akkaVersion = "2.5.27"
  val akkaVersion = "2.5.29"
  val akkaHttpVersion = "10.1.11"
  val camelVersion = "2.25.0"
  //val camelVersion= "3.0.0-M4"

  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
   // "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-camel" %akkaVersion,
    "com.typesafe.akka" %% "akka-http-xml" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-persistence" %akkaVersion,

    "org.apache.camel" %"camel-http4" %camelVersion,

    "net.liftweb" % "lift-json_2.13" % "3.4.0",

    "org.iq80.leveldb" % "leveldb" % "0.12",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

    "org.scalatest" %% "scalatest" % "3.2.0-M1" % "test",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion

  )


}