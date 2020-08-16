package de.innfactory.svc1

import akka.actor.{Actor}

import de.innfactory.svc1.grpc._

class GreeterActor extends Actor {

  var state :  Map[String,Int] = Map()

  override def receive: Receive = {
    case HelloRequest(from: String) => {
      val newCount : Int = state.getOrElse(from,0) + 1
      state = state.filter(_._1 == from).+((from,newCount))
      sender ! HelloReply(from,newCount)
    }
  }

}


