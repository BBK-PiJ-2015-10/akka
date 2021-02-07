package simple

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging

import java.util.concurrent.TimeUnit

case class SimpleProducer(prices: List[PriceUpdate], handler: ActorRef, loops: Int, actorSystem: ActorSystem) {

  val log = Logging(actorSystem.eventStream, "producer")

  var sents: Int = 0

  def run() = {
    val start = System.nanoTime
    while (sents < loops) {
      sents += 1
      prices.foreach(price => {
        log.debug(s"Sending while on loop ${sents}")
        handler ! price
      })
      log.debug(s"Finished sending batch ${sents}")
    }
    val elapsedTime = System.nanoTime - start
    val productionTime = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
    log.info(s"Done producing ${loops} batches in ${productionTime} milliseconds")

  }

}
