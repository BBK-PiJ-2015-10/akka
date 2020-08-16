package aia.persistence

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}


object ShoppersSingleton {

  def props : Props = Props(new ShoppersSingleton)
  def name : String = "shoppers-singleton"

}

class ShoppersSingleton extends Actor{

  val singletonManager : ActorRef = context.system.actorOf(
    ClusterSingletonManager.props(
      Shoppers.props,
      PoisonPill,
      ClusterSingletonManagerSettings(context.system)
        .withRole(None)
        .withSingletonName(Shoppers.name)
    )
  )

  val shoppers : ActorRef = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonManager.path.child(Shoppers.name).toStringWithoutAddress,
      ClusterSingletonProxySettings(context.system)
        .withRole(None)
        .withSingletonName("shoppers-proxy")
    )
  )

  override def receive: Receive = {

    case command: Shopper.Command => shoppers forward command

  }
}
