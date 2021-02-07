import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import simple.PriceUpdate

import scala.concurrent.duration.DurationInt


sealed trait LoadHandlerState

case object Loading extends LoadHandlerState
case object Unloading extends LoadHandlerState

case class LoadHandlerStateData(prices: Set[PriceUpdate])

object LoadHandler {
  def props(reducer: ActorRef, maxCapacity: Int) : Props = Props(new LoadHandler(reducer,maxCapacity))
}

class LoadHandler(reducer: ActorRef, maxCapacity: Int) extends FSM[LoadHandlerState,LoadHandlerStateData] with
  ActorLogging{

  var receivedCount : Int = 0

  startWith(Loading,LoadHandlerStateData(Set()))

  //no time out in this guy dummy. However, it should have a priority mailbox. And Sharded later.
  //Research a queue with bounded context
  //can you a last in first out on an actor queue
  //Stream with a bounded buffer of drop first
  when(Loading, stateTimeout = 1.second){
  //when(Loading){

    case Event(priceUpdate: PriceUpdate,data: LoadHandlerStateData) =>{
      receivedCount +=1
      val currentPrices = data.prices
      if (currentPrices.contains(priceUpdate)){
        val newPrices = currentPrices + priceUpdate
        val newStateData  = data.copy(prices = newPrices)
        log.info(s"Got an update for ${priceUpdate.companyName}")
        stay() using newStateData
      } else {
        if (currentPrices.size<=maxCapacity){
          val newPrices = currentPrices + priceUpdate
          val newStateData  = data.copy(prices = newPrices)
          log.info(s"Got a new stock for ${priceUpdate.companyName}")
          stay() using newStateData
        }
        else {
          stay()
        }
      }
    }

    case Event(culon: String,data: LoadHandlerStateData) => {
      //self ! StateTimeout
      val currentPrices = data.prices
      val toDrop = currentPrices.size - maxCapacity
      val sentPrices = currentPrices.drop(toDrop)
      log.info(s"LoadHandler timer on 1 second has triggered, currentBasket has ${currentPrices.size} prices, " +
        s"submitting only ${sentPrices.size} and cleaning basket to empty")
      reducer ! sentPrices
      val newStateData  = data.copy(prices = Set())
      stay() using newStateData
    }
  }

  initialize()

}
