package org.baku.cb

import akka.actor.{Actor,ActorSystem,Props}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  val system = ActorSystem("CB")
  implicit val ec = system.dispatcher

  implicit val timeout = Timeout(2 seconds)
  val db = system.actorOf(Props[DbActor])
  Await.result(db ? Set("key","value"), 2 seconds)

  (1 to 1000).foreach({ num =>
    Thread.sleep(900)
    val resF = db ? Get("key")
    resF.map(x => "Received: " +x).recover({
      case t => "Received Error: " +t
    }).foreach(x => println(x))
  })

}
