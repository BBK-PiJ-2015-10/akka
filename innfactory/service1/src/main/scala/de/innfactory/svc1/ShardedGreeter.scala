package de.innfactory.svc1

import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.{ShardRegion}
import akka.cluster.sharding.ShardRegion.Passivate
import de.innfactory.svc1.grpc.HelloReply

import scala.concurrent.duration._

object ShardedGreeter {

  def props: Props = Props(new ShardedGreeter)

  case object StopGreeters

  val shardName : String = "greeters"

  val extractEntityId : ShardRegion.ExtractEntityId = {
    case cmd : HelloReply => (cmd.message,cmd)
  }

  val extractShardId : ShardRegion.ExtractShardId = {
    case cmd:  HelloReply => (math.abs(cmd.message.hashCode) % 12).toString
  }


}


class ShardedGreeter extends GreeterActor {
  import ShardedGreeter._

  context.setReceiveTimeout(10.seconds)

  override def unhandled(message: Any): Unit = {
    message match {
      case ReceiveTimeout =>
        context.parent ! Passivate(stopMessage = StopGreeters)
      case _ => println("unhandled sharded role")
    }
  }
}
