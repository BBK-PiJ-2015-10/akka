package akkapatterns

import akka.actor.{Actor,ActorRef,ActorLogging}

import scala.concurrent.Future
import scala.reflect.ClassTag

import WorkPullingPattern._

abstract class Worker[T: ClassTag](val master: ActorRef)(implicit manifest: Manifest[T]) extends Actor{

  implicit val ec = context.dispatcher

  override def preStart(): Unit = {
    master ! RegisterWorker(self)
    master ! GimmeWork
  }

  override def receive: Receive = {
    case WorkAvailable => master ! GimmeWork
    case Work(work: T) =>  doWork(work) onComplete {
      case _ => master ! GimmeWork
    }

  }

  def doWork(work: T): Future[_]

}
