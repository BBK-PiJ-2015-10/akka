name := "com.palacios.pricing"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++={

  val akkaVersion ="2.6.12"

  Seq(
    "com.typesafe.akka" %%  "akka-actor" %akkaVersion
  )


}
