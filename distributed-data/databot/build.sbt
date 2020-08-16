name := "databot"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {

  val akkaVersion = "2.6.4"

  Seq(
    "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion
  )

}


