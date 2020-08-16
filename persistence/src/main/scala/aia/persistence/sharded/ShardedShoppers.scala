package aia.persistence.sharded

import akka.actor.{Props,Actor,ActorRef}
import akka.cluster.sharding.{ClusterSharding,ClusterShardingSettings}

import aia.persistence.Shopper

object ShardedShoppers {

  def props: Props = Props(new ShardedShoppers)
  def name : String = "sharded-shoppers"

}

class ShardedShoppers extends Actor{

  ClusterSharding(context.system)
    .start(
      ShardedShopper.shardName
      ,ShardedShopper.props,
      ClusterShardingSettings(context.system),
      ShardedShopper.extractEntityId,
      ShardedShopper.extractShardId)


  def shardedShopper : ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedShopper.shardName)
  }

  override def receive: Receive = {
    case cmd:  Shopper.Command => shardedShopper forward cmd
  }

}
