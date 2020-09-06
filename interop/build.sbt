name := "interop"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= {

  val akkaVersion = "2.6.8"

  Seq(
    "com.typesafe.akka" %% "akka-stream" %akkaVersion,

    "com.typesafe.akka" %% "akka-testkit" %akkaVersion
  )

}
