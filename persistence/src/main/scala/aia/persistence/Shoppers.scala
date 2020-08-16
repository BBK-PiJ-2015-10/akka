package aia.persistence

import akka.actor.{Props,Actor}
import akka.persistence.{PersistentActor}

object Shoppers {

  def props : Props =  Props(new Shoppers)
  def name : String = "shoppers"

  sealed trait Event
  case class ShopperCreated(shopperId: Long)

}


class Shoppers extends PersistentActor
  with ShopperLookup {

  import Shoppers._

  override def persistenceId: String = "shoppers"

  override def receiveRecover: Receive = {
    case ShopperCreated(shopperId) => {
      context.child(Shopper.name(shopperId)).getOrElse(createShopper(shopperId))
    }
  }

  override def receiveCommand: Receive = forwardToShopper

  override def createAndForward(cmd: Shopper.Command, shopperId: Long): Unit = {
    val shopper = createShopper(shopperId)
    persistAsync(ShopperCreated(shopperId)){ _ =>
      forwardCommand(cmd)(shopper)
    }
  }
}
