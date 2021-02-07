import akka.actor.{ActorRef, FSM, Props}
import simple.PriceUpdate

sealed trait LoadReducerState

case object Reducing extends LoadReducerState
case object Resetting extends LoadReducerState

case class LoadReducerStateData(submissions: Int, prices: Set[PriceUpdate])

object LoadReducer {
  def props(consumer: ActorRef, numberOfHandlers: Int, maxCapacity: Int) : Props = Props(new LoadReducer(consumer,
    numberOfHandlers,maxCapacity))
}

//This is the guy who should have the timer
class LoadReducer(consumer: ActorRef, numberOfHandlers: Int, maxCapacity: Int) extends FSM[LoadReducerState,
  LoadReducerStateData] {

  startWith(Reducing, LoadReducerStateData(0, Set()))

  when(Reducing) {

    case Event(priceUpdates: Set[PriceUpdate], data: LoadReducerStateData) => {
      val newPrices = data.prices.concat(priceUpdates)
      val newSubmissions = data.submissions + 1
      log.info(s"Reducer received a price Basket, new BasketSize is ${newPrices.size} and submission are " +
        s"${newSubmissions}")
      val overSized = newPrices.size - maxCapacity
      if (newSubmissions == numberOfHandlers) {
        val pricesToSend = newPrices.drop(overSized)
        pricesToSend.foreach(price => consumer ! price)
        // no need to reset all workers submitted
        stay() using LoadReducerStateData(0, Set())
      } else {
        if (overSized >= 0) {
          val pricesToSend = newPrices.drop(overSized)
          pricesToSend.foreach(price => consumer ! price)
          // opted to reset to avoid sending anything that could be more than 1 second old
          goto(Resetting) using LoadReducerStateData(newSubmissions, Set())
        } else {
          // don't have enough submissions or not all workers done
          stay() using LoadReducerStateData(newSubmissions, newPrices)
        }
      }
    }
  }

  when(Resetting) {
    case Event(_: Set[PriceUpdate], data: LoadReducerStateData) => {
      val newSubmissions = data.submissions + 1
      if (newSubmissions == numberOfHandlers) {
        goto(Reducing) using LoadReducerStateData(0, Set())
      } else {
        stay() using LoadReducerStateData(newSubmissions, Set())
      }
    }
  }

  initialize()

}
