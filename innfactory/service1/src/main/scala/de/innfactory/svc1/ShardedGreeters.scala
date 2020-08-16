package de.innfactory.svc1


import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import de.innfactory.svc1.grpc.{HelloRequest}

object ShardedGreeters {

  def props : Props = Props(new ShardedGreeters)
  def name : String = "sharded-users"
}

class ShardedGreeters extends Actor {

  import ShardedGreeter._

  ClusterSharding(context.system)
    .start(ShardedGreeter.shardName,ShardedGreeter.props,ClusterShardingSettings(context.system),ShardedGreeter.extractEntityId,ShardedGreeter.extractShardId)

  def shardedGreeters : ActorRef = ClusterSharding(context.system).shardRegion(ShardedGreeter.shardName)

  override def receive: Receive = {
    case cmd: HelloRequest => shardedGreeters forward cmd
  }


}
