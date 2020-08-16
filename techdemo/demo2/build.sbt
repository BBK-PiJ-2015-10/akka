name := "record-analyzer"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "streamz at bintray" at "http://dl.bintray.com/streamz/maven"

libraryDependencies ++={

  //val akkaVersion = "2.5.29"
  val akkaVersion = "2.5.31"
  val akkaCamelVersion = "2.5.31"
  val akkaHttpVersion = "10.1.11"
  val camelVersion = "2.25.0"
  val streamzVersion = "0.11-RC1"

  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" %akkaVersion,
    "com.typesafe.akka" %% "akka-camel" %akkaVersion,
    //"com.typesafe.akka" %% "akka-camel" %akkaCamelVersion,

    //"com.typesafe.akka" %% "akka-http-core" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" %akkaHttpVersion,
    "com.typesafe.akka" %% "akka-persistence" %akkaVersion,
    "com.typesafe.akka" %% "akka-stream" %akkaVersion,

    // "com.github.krasserm" %% "streamz-camel-akka" % streamzVersion,
     "org.apache.camel" %"camel-http4" %camelVersion,

    "net.liftweb" % "lift-json_2.13" % "3.4.0",

    "org.iq80.leveldb" % "leveldb" % "0.12",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

    "org.scalatest" %% "scalatest" % "3.2.0-M1" % "test",
    "com.typesafe.akka" %% "akka-testkit" %akkaVersion

  )


}