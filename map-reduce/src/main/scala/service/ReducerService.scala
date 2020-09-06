package service

import service.common.LogStatus
import akka.actor.{FSM, Props}


trait ReducerState

object Unbooked extends ReducerState
object Accumulating extends ReducerState

case class LogStatusCount(logStatus: Option[LogStatus], count: Int)

case object Reduce

object ReducerService {
  def props(): Props = Props(new ReducerService)
}

class ReducerService extends FSM[ReducerState, LogStatusCount] {

  startWith(Unbooked, LogStatusCount(None, 0))

  when(Unbooked) {
    case Event(logStatus: LogStatus, _) => {
      log.info(s"Received first ${logStatus} from ${sender().path.name}")
      goto(Accumulating) using LogStatusCount(Some(logStatus), 1)
    }
  }

  when(Accumulating) {
    case Event(logStatus: LogStatus, data: LogStatusCount) => {
      log.info(s"Received subsequent ${logStatus} from ${sender().path.name}")
      stay() using data.copy(
        logStatus = data.logStatus,
        count = data.count + 1)
    }
    case Event(Reduce, data: LogStatusCount) => {
      log.info(s"Received ${Reduce} from ${sender().path.name} will submit ${data}")
      sender() ! data
      goto(Unbooked) using data.copy(
        logStatus = None,
        count = 0
      )
    }

  }

}
