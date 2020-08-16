package aia.cluster.words

import akka.actor.{Actor,ActorRef}
import akka.routing.BroadcastGroup
import akka.cluster.routing.{ClusterRouterGroup,ClusterRouterGroupSettings}

trait ReceptionistRouterLookup { this: Actor =>
  def receptionistRouter : ActorRef = context.actorOf(
    ClusterRouterGroup(
      BroadcastGroup(Nil),
      ClusterRouterGroupSettings(
        totalInstances = 100,
        routeesPaths = List("/user/receptionist"),
        allowLocalRoutees = true,
        useRole = Some("master")
      )
    ).props(),
    name = "receptionist-router")
}
