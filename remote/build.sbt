name := "goticks"

version := "0.1"

scalaVersion := "2.13.1"

organization := "com.goticks"

libraryDependencies ++= {
  
  val akkaVersion = "2.5.25"
  val akkaHttpVersion = "10.1.10"
  
  Seq(

    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    
    "com.typesafe.akka"       %%  "akka-remote"                    % akkaVersion,
    "com.typesafe.akka"       %%  "akka-multi-node-testkit"        % akkaVersion % "test",
    
    
    "com.typesafe.akka"       %% "akka-testkit"                    % akkaVersion % "test",
    "org.scalatest"           %% "scalatest"                       % "3.2.0-M1" % "test",
    
    "com.typesafe.akka"       %% "akka-http-core"                  % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-http"                       % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-http-spray-json"            % akkaHttpVersion,
    "ch.qos.logback"          %  "logback-classic"                 % "1.2.3"
    
   )
  
}

//Assembly settings