package service

import akka.actor.{Actor,ActorRef, Props}

import service.common.LogStatus

object ReducerRouter {
  def props(): Props = Props(new ReducerRouter)
}

class ReducerRouter extends Actor {

  var reducerWorkers: Map[LogStatus,ActorRef] = Map()

  override def receive: Receive = {
    case logStatus: LogStatus => {
      reducerWorkers.get(logStatus) match {
        case  Some(actor) => actor forward logStatus
        case  None =>  {
          val newWorker = context.actorOf(ReducerService.props(),"Service+"+logStatus.value)
          reducerWorkers += (logStatus -> newWorker)
          newWorker forward logStatus
        }
      }
    }
    case Reduce => reducerWorkers.values.foreach(worker => worker forward  Reduce)
  }

}
