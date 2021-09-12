name := "streams-documentation-examples"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= {

  val akkaVersion = "2.6.11"

  Seq(
    "com.typesafe.akka" %% "akka-stream" %akkaVersion
  )

}
