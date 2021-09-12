//import PriorityDispatcherRunner.priorityLogger
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Sender {
  def props(handler: ActorRef) = Props(new Sender(handler))
}


class Sender(handler: ActorRef) extends Actor with ActorLogging{

  override def receive: Receive = {
    case "send" => {
      log.info("Starting to send")
      for (i <- 0 to 1000){

        //priorityLogger ! "lowpriority"
        //priorityLogger ! "lowpriority"
        //priorityLogger ! "lowpriority"
        //priorityLogger ! "lowpriority"
        //priorityLogger ! "pigdog2"
        //priorityLogger ! "pigdog3"
        //priorityLogger ! "fucker"

      }

    }
  }

}
