package aia.deploy

import akka.actor.{Props,ActorSystem,ActorRef}
import scala.concurrent.duration._

object BootHello  extends App {

  val system : ActorSystem = ActorSystem("hellokernel")

  val actor : ActorRef = system.actorOf(Props[HelloWorld])

  val config = system.settings.config
  val timer = config.getInt("helloWorld.timer")

  val duration = Duration(timer,MILLISECONDS)

  system.actorOf(Props(new HelloWorldCaller(duration,actor)))

}
