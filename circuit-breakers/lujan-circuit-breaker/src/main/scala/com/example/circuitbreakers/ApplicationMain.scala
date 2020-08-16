package com.example.circuitbreakers

import akka.actor.ActorSystem

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.StdIn


//Source of example https://github.com/alejandrolujan/AkkaCircuitBreakers
//https://www.youtube.com/watch?v=FLw95uX3mkU

object ApplicationMain extends App {

  val system = ActorSystem("MySystem")

  val service = system.actorOf(ServiceWithCB.props,"service")

  val userCount = 10
  (1 to userCount)

  println("System running, press enter to shutdown")
  StdIn.readLine()

  Await.result(system.terminate(),3 seconds)

  private def createUser(i: Int) : Unit ={
    import system.dispatcher
    system.scheduler.scheduleOnce(i seconds) {
      system.actorOf(UserActor.props(service),s"User$i")
    }

  }


}
