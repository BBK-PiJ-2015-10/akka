package aia.persistence.rest


import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure,Success}

import akka.actor._
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import aia.persistence._

trait ShoppersServiceSupport extends RequestTimeOut {

  def startService(shoppers: ActorRef)(implicit system: ActorSystem) = {
    val config = system.settings.config
    val host = config.getString("http.host")
    val port = config.getInt("http.port")

    implicit val ec = system.dispatcher //bindAndHandle requires an implicit ExecutionContext

    val api = new ShopperService(shoppers,system,requestTimeout(config)).routes

    implicit val materializer = ActorMaterializer()

    val bindingFuture : Future[ServerBinding] =
      Http().bindAndHandle(api,host,port)

    val log = Logging(system.eventStream,"shoppers")

    bindingFuture.map{ serverBinding =>
      log.info(s"Shoppers API bound to ${serverBinding.localAddress} ")
    }.onComplete({
      case Failure(ex) => {
        log.error(ex,"Failed to bind {}:{} ",port,host)
        system.terminate() }
      case Success(_)  => log.info("All set to go cowboy")
    })

  }

}

trait RequestTimeOut {

  import scala.concurrent.duration._

  def requestTimeout(config: Config) : Timeout = {
    val t  =config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length,d.unit)
  }

}
