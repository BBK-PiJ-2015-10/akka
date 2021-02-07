package sharded

import akka.actor.ActorSystem
import akka.routing.ConsistentHashingPool

import scala.concurrent.duration.DurationInt

//TODO: WIP
object ShardedApplicationRunner extends App{

  println("Starting Price System Simulation")

  val system = ActorSystem("prices")
  implicit val ec = system.dispatcher
  val maxPriceUpdatesPerSecond = 100
  val simpleHandler = system.actorOf(SimpleHandler.props(maxPriceUpdatesPerSecond).withDispatcher
  ("sharded-loadhandler-dispatcher"),
    "simpleHandler")
  //val routerHandler = system.actorOf(
    //ConsistentHashingPool(10,
      //virtualNodesFactor = 10,
      //hashMapping = ConsistentHashMapper.priceUpdateConsistentHashMapperByFirtLetter()
    //).props(SimpleHandler.props(maxPriceUpdatesPerSecond)))
    //).props(SimpleHandler.props(maxPriceUpdatesPerSecond).withDispatcher(
      //("sharded-loadhandler-dispatcher"))),name="routerHandler"
  //)
  val simpleConsumer = system.actorOf(SimpleConsumer.props(), "simpleConsumer")
  val simpleReducer = system.actorOf(SimpleReducer.props(simpleHandler, simpleConsumer, maxPriceUpdatesPerSecond),
    "simpleReducer")
  system.scheduler.schedule(0 milliseconds, 1000 milliseconds, simpleReducer, Submit)

  val prices: List[PriceUpdate] = List(
    PriceUpdate("Apple", 97.85),
    PriceUpdate("Google", 160.71),
    PriceUpdate("Facebook", 91.66),
    PriceUpdate("Google", 160.73),
    PriceUpdate("Facebook", 91.71),
    PriceUpdate("Google", 160.76),
    PriceUpdate("Apple", 97.85),
    PriceUpdate("Google", 160.71),
    PriceUpdate("Facebook", 91.63),
  )
  val simpleProducer = SimpleProducer(prices, simpleHandler, 1000000, system)
  simpleProducer.run()

  println("say good bye to this thread context")
  println("but the actor system is still alive and waiting for more action")

}
