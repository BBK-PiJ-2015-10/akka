package example1

import akka.actor.{Actor, ActorLogging, Props}


object Translator {
  def props() = Props(new Translator)
}

class Translator extends Actor with ActorLogging{

  override def receive: Receive = {
    case word: String  =>
      val reply = word.toUpperCase
      log.info(s"Received : ${word} Responding:  ${reply}")
      sender() ! reply
  }

}
