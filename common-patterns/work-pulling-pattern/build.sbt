name := "work-pulling-pattern"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= {

  val akkaVersion = "2.6.4"

  Seq(
    "com.typesafe.akka" %% "akka-actor" %akkaVersion,

    "com.typesafe.akka" %% "akka-testkit" %akkaVersion %"test",
    "org.scalatest" %% "scalatest" %"3.2.0-M4" %"test"

  )

}
