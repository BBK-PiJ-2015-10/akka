package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.language.postfixOps
import scala.concurrent.duration.DurationInt

object IntegratingWithActors extends  App{

  implicit val actorSystem = ActorSystem("IntegratingActors")
  implicit val materializer = ActorMaterializer

  class SimpleActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n : Int =>
        log.info(s"Just received a number: $n")
        sender() ! 2 * n
      case _ =>
    }

  }

  val simpleActor = actorSystem.actorOf(Props[SimpleActor],"simpleActor")

  val numberSource = Source(1 to 10)

  //actor as a flow
  import actorSystem.dispatcher
  implicit val timeOut = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

  //numberSource.via(actorBasedFlow).to(Sink.ignore).run()
  //numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run()

  /*
  Actor as a Source
   */
  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)

  //val materializedActorRef  =
    //actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered flow got number; $number"))).run()

  //materializedActorRef ! 10
  //terminate the stream
  //materializedActorRef ! akka.actor.Status.Success("complete")

  /*
  Actor as a destination/sink minute 12.57
   */

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case StreamInit =>
        log.info(s"Streamed initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream completed")
       context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"Stream failed with $ex")
      case message =>
        log.info(s"Message $message has come to its final resting point.")
        sender() ! StreamAck
    }

  }

  val destinationActor = actorSystem.actorOf(Props[DestinationActor],"destinationActor")

  val actorPowerSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  Source(1 to 10).to(actorPowerSink).run()

  //Sink.actorRef() not capable of providing back pressure








}
