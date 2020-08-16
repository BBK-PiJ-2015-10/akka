package greeter

import scala.concurrent.Future
import akka.NotUsed
import akka.actor.ActorLogging
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
//import example.myapp.helloworld._

import greeter._

import akka.event.Logging

class GreeterServiceImpl(implicit mat: Materializer) extends GreetingService  {

  import mat.executionContext

  //val log = Logging.getLogger(mat.)

  override def sayHello(in: HelloRequest): Future[HelloReply] = {
    println(s"sayHello to ${in.name}")
    Future.successful(HelloReply(s"Hello, ${in.name}"))
  }

  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = {
    println(s"sayHello to in stream..")
    in.runWith(Sink.seq).map(elements => HelloReply(s"Hello, ${elements.map(_.name).mkString(", ")}"))
  }

  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = {
    println(s"sayHello to ${in.name} with streatm of chars...")
    Source(s"Hello, ${in.name}".toList).map(character => HelloReply(character.toString))
  }

  override def sayHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
    println(s"sayHello to stream...")
    in.map(request => HelloReply(s"Hello, ${request.name}"))
  }


}
