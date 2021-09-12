package part4_techniques

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import scala.language.postfixOps;

import scala.concurrent.Future

object IntegratingWithExternalServices extends App {

  implicit val system = ActorSystem("IntegratingWithExternalSystem")
  implicit val materializer = ActorMaterializer

  //import system.dispatcher // not recommended for future
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExternalService[A,B](element: A) : Future[B] = ???
    //example pagerDuty
    case class PagerEvent(application: String, description: String, date: Date)

    val eventSource = Source(List(
      PagerEvent("AkkaInfra","Infra broke",new Date),
      PagerEvent("FastDataPipeline","Illegal elements in the data pipeline",new Date),
      PagerEvent("AkkaInfra","A service stopped responding",new Date()),
      PagerEvent("SuperFrontEnd","A button doesn't work",new Date)
    ))


    object PagerService {
      private val engineers = List("Rabbo","Leon","Lady Gaga")
      private val emails = Map (
        "Rabbo" -> "rabbo@hotmail.com",
        "Leon"  -> "leon@gmail.com",
        "Lady Gaga" -> "ladyGaga@gmail.com"
      )

      def processEvent(pagerEvent: PagerEvent) = Future {
        val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length
        val engineer = engineers(engineerIndex.toInt)
        val engineerEmail = emails(engineer)

        println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
        Thread.sleep(1000)
        engineerEmail

      }
    }


  val infraEvents = eventSource.filter(_.application == "AkkaInfra")
  //mapAsyncUnordeered
  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
  //guarantee the relative order of elements
 val pagedEmailsSink = Sink.foreach[String](email => println(s"Successfully sent notification to email $email "))

  //pagedEngineerEmails.to(pagedEmailsSink).run()

  //left on 14.21 minute

  class PagerActor extends Actor with ActorLogging {

    private val engineers = List("Rabbo","Leon","Lady Gaga")
    private val emails = Map (
      "Rabbo" -> "rabbo@hotmail.com",
      "Leon"  -> "leon@gmail.com",
      "Lady Gaga" -> "ladyGaga@gmail.com"
    )

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        val response = processEvent(pagerEvent)
        sender() ! response
    }

    def processEvent(pagerEvent: PagerEvent) =  {
      val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      println(s"Actor Sending engineer  $engineerEmail a high priority notification: $pagerEvent")
      Thread.sleep(1000)
      engineerEmail

    }
  }

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(4 seconds)
  val pagerActor = system.actorOf(Props[PagerActor],"pagerActor")
  val alternativePageEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])
  alternativePageEngineerEmails.to(pagedEmailsSink).run()

}
