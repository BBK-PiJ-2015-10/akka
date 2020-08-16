package akkapatterns

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akkapatterns.WorkPullingPattern.{Epic, GimmeWork, RegisterWorker, Work}

import scala.collection.mutable

import WorkPullingPattern._

class Master[T] extends Actor with ActorLogging {

  val workers = mutable.Set.empty[ActorRef]

  var currentEpic: Option[Epic[T]] = None


  override def receive: Receive = {

    case epic: Epic[T] => {
      if (currentEpic.isDefined){
        sender ! CurrentlyBusy
      } else if (workers.isEmpty){
        log.error("Got work, but there are no workers registered")
      } else {
        currentEpic = Some(epic)
        workers foreach { _ ! WorkAvailable }
      }
    }

    case RegisterWorker(worker) => {
      log.info(s"worker $worker registered")
      context.watch(worker)
      workers += worker
    }

    case Terminated(worker) => {
      log.info(s"work $worker died - taking off the set of workers")
      workers.remove(worker)
    }

    case GimmeWork => {
      currentEpic match {
        case None => log.info("worker asked for work but there is nothing to do")
        case Some(epic) => {
          val iter = epic.iterator
          if (iter.hasNext) {
            sender ! Work(iter.next())
          } else {
            log.info(s"done with the current $epic")
            currentEpic = None
          }
        }
      }
    }

  }

}