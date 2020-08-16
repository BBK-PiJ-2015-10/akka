name := "service1-api"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= ReflectionResolver.getDefaultDependencies()

enablePlugins(AkkaGrpcPlugin)


