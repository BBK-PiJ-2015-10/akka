package aia.persistence

import akka.actor.{Props,Actor,ActorLogging}

import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink


object PaymentHistory {

  def props(shopperId: Long) : Props = Props(new PaymentHistory(shopperId))
  def name(shopperId : Long) : String = s"payment_history_${shopperId}"

  case object GetHistory

  case class History(items: List[Item] = Nil){

    def paid(paidItems: List[Item]) : History = {
      History(paidItems ++ items)
    }

  }

}

class PaymentHistory(shopperId: Long) extends Actor with ActorLogging {
  import PaymentHistory._
  import Basket._

  var history = History()

  val queries = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
  implicit val materializer = ActorMaterializer()
  queries.eventsByPersistenceId(Wallet.name(shopperId)).runWith(Sink.actorRef(self,None))

  override def receive: Receive = {
    case Wallet.Paid(items,_) => history = history.paid(items)
    case GetHistory => sender() ! history
  }

}
