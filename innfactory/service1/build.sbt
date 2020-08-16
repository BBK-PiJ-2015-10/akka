name := "service-1"

version := "0.1"

scalaVersion := "2.13.1"


libraryDependencies ++= ReflectionResolver.getDefaultDependencies()



enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AkkaGrpcPlugin)

packageName in Docker := "innfactory-test/service1"
version in Docker := "0.1"
dockerExposedPorts := Seq(2552,8558)
