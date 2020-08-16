package ticker

import akka.actor.ActorSystem
import akka.grpc.scaladsl.{ServiceHandler,ServerReflection}
import akka.http.scaladsl._
import akka.stream.{SystemMaterializer}

//Source:  https://akka.io/blog/news/2020/06/17/akka-grpc-1.0.0-released
// To call grpcurl -d '{"name": "foo"}' -plaintext     -import-path src/main/protobuf     -proto ticker.proto     localhost:8080 ticker.TickerService.MonitorSymbol
object Main extends App {

  implicit val system = ActorSystem("ticker")
  implicit val mat = SystemMaterializer(system).materializer

  Http().bindAndHandleAsync(
    TickerServiceHandler(new TickerServiceImpl),
    interface = "127.0.0.1",
    8080
  )

}
