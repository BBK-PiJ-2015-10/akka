
// Source: https://github.com/calvinlfer/rate-limiting-and-gating-akka

import akka.actor.{ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import rest.RestApi
import service.User

object ApplicationRunner extends App with RestApi {

  println("Rate Limiter and Gating example")

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(3.seconds)
  val config = system.settings.config

  val userShardingRegion = ClusterSharding(system).start(
    typeName = User.Sharding.shardName,
    entityProps = Props[User],
    settings = ClusterShardingSettings(system),
    extractEntityId = User.Sharding.extractEntityId,
    extractShardId = User.Sharding.shardIdExtractor(5)
  )

  val bindingFuture = Http().bindAndHandle(routes,"localhost",9001)

  bindingFuture.onComplete {
    case Success(serverBinding) =>
      system.log.info(s"Bound to {}",serverBinding.localAddress)
    case Failure(exception) =>
      system.log.error(exception.getMessage)
      system.terminate()
  }

}
