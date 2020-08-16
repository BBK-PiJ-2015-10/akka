package aia.persistence


import akka.actor.{Actor,Props}

object LocalShoppers {

  def props : Props =  Props(new LocalShoppers)

  def name : String = "local-shoppers"

}

class LocalShoppers extends Actor with ShopperLookup {

  override def receive: Receive = forwardToShopper

}




