package de.innfactory.svc1

import akka.actor.ActorRef
import akka.stream.ActorMaterializer
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.Future

import de.innfactory.svc1.grpc._

class GreetersServiceImpl(greeterRef: ActorRef)
                         (implicit mat: ActorMaterializer, timeout: Timeout) extends GreeterService {

  override def sayHello(in: HelloRequest): Future[HelloReply] = {
    println(s"sayHello to ${in.name}")
    (greeterRef ? in).mapTo[HelloReply]
  }

}
