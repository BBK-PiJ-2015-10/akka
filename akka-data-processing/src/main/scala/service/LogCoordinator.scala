package actors

import actors.common.{StartFetching, LogStatus}
import akka.actor.FSM

sealed trait CoordinatorState
case object Idle extends CoordinatorState
case object Processing extends CoordinatorState
case object Consolidating extends CoordinatorState
case object Finalized extends CoordinatorState

case object CoordinatorData



object LogCoordinator {





}


class LogCoordinator extends FSM[CoordinatorState,Any]{

  startWith(Idle,CoordinatorData)


  when(Idle){
    case Event(startFetching: StartFetching, _) => {
      val sourceActor  = context.actorOf(LogSource.props(startFetching.source))
      sourceActor ! startFetching
      goto(Processing)
    }
  }

  when(Processing){
    case Event(status: LogStatus, _) => {
      //Send to Reducer
      stay()
    }
  }






}
