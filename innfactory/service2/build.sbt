name := "service-2"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= ReflectionResolver.getDefaultDependencies()

enablePlugins(AkkaGrpcPlugin)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

packageName in Docker := "innfactory-test/service2"
version in Docker := "0.1"
dockerExposedPorts := Seq(2552,8558,8090)