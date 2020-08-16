package aia.deploy

import akka.actor.{Actor,ActorRef,ActorLogging}
import scala.concurrent.duration._

class HelloWorld extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg: String =>
      val hello  = "Hello %s".format(msg)
      sender() ! hello
      log.info("Sent response {}",hello)
  }

}

class HelloWorldCaller(timer: FiniteDuration, actor: ActorRef) extends Actor with ActorLogging {

  case class TimerTick(msg: String)


  override def preStart(): Unit = {
    super.preStart()
    log.info("FUCK IT with timer: {}",timer.toMillis)
    implicit val ec = context.dispatcher
    context.system.scheduler.schedule(timer,timer,self,new TimerTick("everybody"))
  }

  override def receive: Receive = {
    case msg: String => log.info("received {}",msg)
    case tick: TimerTick => actor ! tick.msg
  }
}