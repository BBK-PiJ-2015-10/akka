package service

import service.common.{LogStatus, StartFetching}
import akka.actor.{ActorRef, FSM, Props}

sealed trait CoordinatorState

case object Reset

case object Idle extends CoordinatorState
case object Processing extends CoordinatorState
case object Consolidating extends CoordinatorState
case object Reporting extends CoordinatorState
case object Finalized extends CoordinatorState

case class LogCoordinatorStateData(results: Set[LogStatusCount])

object LogCoordinator {
  def props(): Props = Props(new LogCoordinator)
}

class LogCoordinator extends FSM[CoordinatorState,LogCoordinatorStateData]{

  var requestor : Option[ActorRef] = None

  val router = context.actorOf(ReducerRouter.props(),"reducerRouter")

  startWith(Idle,LogCoordinatorStateData(Set()))

  when(Idle){
    case Event(startFetching: StartFetching, _) =>{
      requestor = Some(sender())
      log.info(s"Starting job ${startFetching}")
      val source = context.actorOf(LogSource.props(startFetching.source),"sourceLog")
      log.info(s"Created source service ${source.path}")
      source ! Consume
      goto(Processing)
    }
  }

  when(Processing){
    case Event(logStatus: LogStatus,_) =>{
      log.info(s"Received ${logStatus} from ${sender().path.name} with a hashKey ${logStatus}")
      router ! logStatus
      stay()
    }
    case Event(DoneConsuming, _) => {
      log.info(s"Received ${DoneConsuming} from ${sender().path.name}")
      router ! Reduce
      goto(Consolidating)
    }
  }

  when(Consolidating) {
    case Event(logStatusCount: LogStatusCount, data) => {
      if (data.results.size < 5){
        log.info(s"Received ${logStatusCount} from ${sender().path.name} my size is ${data.results.size} ")
        log.info(s"Completed resuts were ${data.results.size}")
        if (data.results.size == 4){
          log.info(s"Going to idle with ${data.results.size}")
          goto(Reporting) using data.copy(
            results = data.results + logStatusCount
          )
        } else {
          stay() using data.copy(
            results = data.results + logStatusCount
          )
        }
      } else {
        log.info("Done with this job")
        goto(Idle)
      }
    }
  }

  when(Reporting) {
    case Event(Reset, _) => {
      log.info(s"Received a Reset")
    }
    goto(Idle) using LogCoordinatorStateData(Set())
  }

  onTransition{
    case Consolidating -> Reporting => {
      val response = nextStateData.results.map(
        value => (value.logStatus.get.value,value.count)
      ).toSeq.sortBy(_._2)
      requestor.get ! response
      log.info(s"Sent ${response} to requestor ${requestor}")
      requestor = None
      self ! Reset
    }
  }

  onTransition {
    case Reporting -> Idle =>{
      log.info(s"My requestor is ${requestor} and my state is ${nextStateData.results.size}")
    }
  }

}
