package actors.interop.example2

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

object ApplicationRunnerExample2 extends App{

  val system = ActorSystem("example2")
  implicit val ec = system.getDispatcher
  implicit val materializer = Materializer(system)

  val words : Source[String,NotUsed] = Source(List("hello,hi"))

  val names : Source[String,NotUsed] = Source(List("alejandro,palacios"))

  val AckMessage = AckingReceiver.Ack

  val InitMessage = AckingReceiver.StreamInitialized

  val OnCompleteMessage = AckingReceiver.StreamCompleted

  val onErrorMessage = (ex: Throwable) => AckingReceiver.StreamFailure(ex)

  val echo = system.actorOf(EchoActor.props())

  val receiver = system.actorOf(AckingReceiver.props(echo,ackWith = AckMessage))

  val sink = Sink.actorRefWithBackpressure(
    receiver,
    onInitMessage = InitMessage,
    ackMessage = AckMessage,
    onCompleteMessage = OnCompleteMessage,
    onFailureMessage = onErrorMessage
  )

  words.map(_.toLowerCase).runWith(sink)

  //names.map(_.toLowerCase).runWith(sink)

}
