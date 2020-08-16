import sbt._
import scala.reflect.runtime.universe._

object Version {
  val akkaVersion = "2.5.26"
  val akkaHttpVersion = "10.1.10"
  val akkaManagementVersion = "1.0.5"
  val circeVersion = "0.12.3"
}


object Dependencies {
  import Version._
  
  val circeCore = "io.circe" %% "circe-core" %circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" %circeVersion
  val circeParser = "io.circe" %% "circe-parser" %circeVersion
  
  val akkaHttp = "com.typesafe.akka" %% "akka-http" %akkaHttpVersion
  val akkaStreams = "com.typesafe.akka" %%"akka-stream" %akkaVersion
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" %akkaVersion
  val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" %akkaVersion
  
  val akkaManagement = "com.lightbend.akka.management" %%"akka-management" %akkaManagementVersion
  val akkaManagementClusterBootstrap = "com.lightbend.akka.management" %%"akka-management-cluster-bootstrap" %akkaManagementVersion
  val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %%"akka-discovery-kubernetes-api" %akkaManagementVersion
  
}


object ReflectionResolver {
  
  private val runtimeMirror = scala.reflect.runtime.currentMirror
  
  private val accessors = runtimeMirror.classSymbol(Dependencies.getClass).toType.members.collect{
    case m: MethodSymbol if m.isGetter && m.isPublic => m
  }
  
  private val instanceMirror = runtimeMirror.reflect(Dependencies)
  
  private def filterUselessDependencies(methodSymbol: MethodSymbol, endsWith: Option[String]): Boolean = {
    endsWith match {
      case Some(filterString) => methodSymbol.fullName.endsWith(filterString)  
      case None => true
    }
  }
  
  def getDefaultDependencies(endsWith: Option[String] = None): Seq[ModuleID] = accessors
    .filter(filterUselessDependencies(_,endsWith))
    .map(dep => instanceMirror.reflectMethod(dep).apply().asInstanceOf[ModuleID])
    .toSeq
  
}