package aia.persistence.sharded

import scala.concurrent.duration._

import akka.actor.{ActorSystem}

import aia.persistence.rest.ShoppersServiceSupport

object ShardedMain extends App with ShoppersServiceSupport {

  implicit val system = ActorSystem("shoppers")

  val shoppers = system.actorOf(ShardedShoppers.props,ShardedShoppers.name)

  startService(shoppers)

}
