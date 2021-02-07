import akka.actor.{ActorRef, FSM, Props}

case object Submit

trait ReducerState

case object Idle extends ReducerState
case object Soliciting extends ReducerState

case class ReducerData(lastTimeOut: Long)

object SimpleReducer {
  def props(handlers: ActorRef, consumer: ActorRef, maxCapacity: Int) : Props = Props(new SimpleReducer(handlers,
    consumer,maxCapacity))
}

class SimpleReducer(handlers: ActorRef, consumer: ActorRef, maxCapacity: Int) extends FSM[ReducerState,ReducerData] {

  startWith(Idle,ReducerData(0))

  when(Idle){
    case Event(Submit,_) => {
      log.debug(s"Moving Reducer to soliciting")
      handlers ! Submit
      goto(Soliciting)
    }
  }

  when(Soliciting){
    case Event(priceUpdate: Set[PriceUpdate],_) => {
      val toDrop = priceUpdate.size - maxCapacity
      val priceToSend = priceUpdate.drop(toDrop)
      priceToSend.foreach(price => consumer ! price)
      log.debug(s"Have sent to consumer a basket of size ${priceUpdate.size}")
      stay()
    }
    case Event(Submit,_) => {
      log.debug(s"Trying my best to${Submit}")
      handlers ! Submit
      stay()
    }
  }

  initialize()

}
