package aia.persistence.sharded

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import aia.persistence.Shopper



object ShardedShopper {

  def props : Props = Props(new ShardedShopper)
  def name(shopperId: Long) : String = shopperId.toString

  case object StopShopping

  val shardName : String = "shoppers"

  val extractEntityId : ShardRegion.ExtractEntityId = {
    case cmd : Shopper.Command => (cmd.shopperId.toString,cmd)
  }

  val extractShardId : ShardRegion.ExtractShardId = {
    case cmd : Shopper.Command => (cmd.shopperId % 12).toString
  }


}

class ShardedShopper extends Shopper {
  import ShardedShopper._

  val t : Long = context.system.settings.config.getDuration("passivate-timeout",TimeUnit.SECONDS)
  val d : Duration = Duration(t.toString)
  context.setReceiveTimeout(d)

  override def unhandled(message: Any): Unit = message match {

    case ReceiveTimeout => context.parent ! Passivate(stopMessage = ShardedShopper.StopShopping)
    case StopShopping => context.stop(self)

  }
}
