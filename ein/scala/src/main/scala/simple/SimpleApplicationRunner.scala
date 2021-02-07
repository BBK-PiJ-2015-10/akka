package simple

import akka.actor.ActorSystem

import scala.concurrent.duration.DurationInt

//This is a simple application without Routing/Sharding
object SimpleApplicationRunner extends App {

  println("Starting Price System Simulation")

  val system = ActorSystem("prices")
  implicit val ec = system.dispatcher
  val maxPriceUpdatesPerSecond = 100
  val simpleHandler = system.actorOf(SimpleHandler.props(maxPriceUpdatesPerSecond).withDispatcher("simple-loadhandler-dispatcher"),
    "simpleHandler")
  val simpleConsumer = system.actorOf(SimpleConsumer.props(), "simpleConsumer")
  val simpleReducer = system.actorOf(SimpleReducer.props(simpleHandler, simpleConsumer, maxPriceUpdatesPerSecond), "simpleReducer")
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
