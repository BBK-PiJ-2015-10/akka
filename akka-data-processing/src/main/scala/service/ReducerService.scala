package actors

import actors.common.LogStatus
import akka.actor.{FSM, Props}


trait ReducerState

object Unbooked extends ReducerState

object Accumulating extends ReducerState

case class LogStatusCount(logStatus: Option[LogStatus], count: Int)

case object Reduce

object Reducer {
  def props(): Props = Props(new Reducer)
}

class Reducer extends FSM[ReducerState, LogStatusCount] {

  startWith(Unbooked, LogStatusCount(None, 0))

  when(Unbooked) {
    case Event(logStatus: LogStatus, _) => {
      goto(Accumulating) using LogStatusCount(Some(logStatus), 1)
    }
  }

  when(Accumulating) {
    case Event(_: LogStatus, data: LogStatusCount) => {
      stay() using data.copy(
        logStatus = data.logStatus,
        count = data.count + 1)
    }
    case Event(Reduce, data: LogStatusCount) => {
      sender() ! data
      goto(Unbooked) using data.copy(
        logStatus = None,
        count = 0
      )
    }

  }

}
