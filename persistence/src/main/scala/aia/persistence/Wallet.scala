package aia.persistence


import akka.actor.{Props}
import akka.persistence.{PersistentActor}

object Wallet {

  def props(shopperId: Long, cash: BigDecimal) : Props = Props(new Wallet(shopperId,cash))

  def name(shopperId: Long) = s"Wallet_${shopperId}"

  sealed trait Command extends Shopper.Command
  case class Pay(items: List[Item], shopperId: Long) extends Command
  case class Check(shopperId: Long) extends Command
  case class SpentHowMuch(shopperId: Long) extends Command

  case class AmountSpent(amount: BigDecimal)
  case class NotEnoughCash(left: BigDecimal)
  case class Cash(left: BigDecimal)

  sealed trait Event
  case class Paid(list: List[Item], shopperId: Long) extends Event

}


class Wallet(shopperId: Long, cash: BigDecimal) extends PersistentActor {
  import Wallet._

  override def persistenceId: String = s"${self.path.name}"

  var amountSpent : BigDecimal = 0

  private def addSpending(items: List[Item]) :BigDecimal = {
    amountSpent + items.foldLeft(BigDecimal(0)){(total,item) => total + (item.unitPrice * item.number)}
  }

  private val updateState : (Event => Unit) = {
    case paidItems @ Paid(items,_) => amountSpent = addSpending(items)
  }

  override def receiveCommand: Receive = {
    case Pay(items,_) =>
      val totalSpent = addSpending(items)
      if (cash - totalSpent > 0){
        persist(Paid(items,shopperId)) { paid =>
          updateState(paid)
          sender() ! paid
        }
      } else {
        context.system.eventStream.publish(NotEnoughCash(cash-amountSpent))
      }
    case Check(_) => sender() ! Cash(cash - amountSpent)
    case SpentHowMuch(_) => sender() ! AmountSpent(amountSpent)
  }

  override def receiveRecover: Receive = {
    case event: Event => updateState(event)
  }

}
