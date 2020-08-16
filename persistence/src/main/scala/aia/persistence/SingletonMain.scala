package aia.persistence

import akka.actor.{ActorSystem,ActorRef}


import aia.persistence.rest.ShoppersServiceSupport

object SingletonMain extends ShoppersServiceSupport {

    //implicit val system = ActorSystem("shoppers")
    //val shoppers : ActorRef = system.actorOf(ShoppersSingleton.props,ShoppersSingleton.name)
    //startService(shoppers)


}
