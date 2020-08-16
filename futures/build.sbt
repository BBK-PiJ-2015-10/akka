name := "futures"

version := "0.1"

scalaVersion := "2.13.1"

organization := "com.goticks"

libraryDependencies ++= {

  val akkaVersion = "2.5.25"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion      % "test" ,
    "org.scalatest"           %%  "scalatest"                      % "3.2.0-M1"       % "test",
    "com.github.nscala-time"  %%  "nscala-time"                    % "2.22.0"
  )

}