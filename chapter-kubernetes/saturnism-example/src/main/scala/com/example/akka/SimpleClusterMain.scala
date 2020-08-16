package com.example.akka

import akka.actor.{ActorSystem,Address}
import akka.stream.ActorMaterializer
import akka.cluster.Cluster

object SimpleClusterMain extends App{

  val clusterName = "ClusterSystem"
  val actorSystem =  ActorSystem(clusterName)
  val listener = actorSystem.actorOf(SimpleClusterListener.props)
  val materializer = ActorMaterializer.create(actorSystem)
  val cluster =  Cluster.get(actorSystem)

  val addresses : Seq[Address] =
    System.getenv().get("SEED_NODES").split(",").map(ip => Address("akka.tcp",clusterName,ip,2551))

  cluster.joinSeedNodes(addresses)


}
