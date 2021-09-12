import akka.actor.{ActorRef, ActorSystem, FSM}
import akka.event.Logging
import akka.pattern.pipe

import scala.concurrent.Future
import scala.util.{Failure, Success}
//import akka.pattern.StatusReply.Success
import akka.pattern.ask

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import akka.util.Timeout

object PriorityDispatcherRunner extends App {


  val system = ActorSystem("exampleSystem")
  implicit val ec : ExecutionContext = system.getDispatcher
  implicit val timeout = Timeout(5.seconds)

  //val log = Logging(system.eventStream,"processor")

  //log.info("FUCKER FUCKER")

  //val excl : ExecutionContext = system.dispatchers.lookup("custom-dedicated-dispatcher");

  //def work(actorRef: ActorRef,msg: String): Unit = {
    //actorRef  ! msg
  //}

  //val duration : FiniteDuration =  FiniteDuration.apply(500,TimeUnit.MILLISECONDS)

  //val priorityLogger: ActorRef =
    //system.actorOf(LoggerActor.props().withDispatcher("prio-dispatcher"))
  //val msg = "highpriority"
  //system.scheduler.schedule(duration,duration)(work(priorityLogger,msg))(ec)
  //val sender = system.actorOf(Sender.props(priorityLogger).withDispatcher("custom-dedicated-dispatcher"))
  //val msg1 = "send";
  //sender ! msg1
  //priorityLogger ! msg1

  var controller = 0;

  val f1 = Future {
    "Hello" + "World"
  }
  val f2 = f1.map { x =>
    if (controller < 2) {

    }
    x.toUpperCase+controller
  }
  f2.foreach(println)


/*
  var count = 0;
  //count +=1

  println("CULON "+count)

  val echoActor = system.actorOf(EchoActor.props(),"echo")

  val response = (echoActor ? "fucker").pipeTo(echoActor)

  echoActor

  response.onComplete({
    case Success(value) => {
      log.info(s"Received ${value}")

      count += 1
      log.info(s"Fucking count is at ${count}")
      if (count < 2){
        log.info(s"Sending this shit again")
        echoActor ! "fucker"
      } else {
        log.info(s"Now I am at ${count}")
        log.info(s"Done for today mofos")
      }

    }
    case Failure(exception) => log.info(s"Damn it ${exception.getMessage}")
    //case Success(value) => log.info(s"Received ")
  })

  def myPiper(response: Future[Any], actor: ActorRef, count: Int, max: Int) = {
    if (count < max){
      response.pipeTo(actor)
    }
  }

  println("Mimado")
  println(count)

*/



}

