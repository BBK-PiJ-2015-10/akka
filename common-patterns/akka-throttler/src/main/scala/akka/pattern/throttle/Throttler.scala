package akka.pattern.throttle


import java.util.concurrent.TimeUnit

import akka.AkkaException
import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration.FiniteDuration

trait Throttler {

  self: Actor =>

}

object Throttler {

  class FailedToSendException(message: String, cause: Throwable) extends AkkaException(message, cause)


  case class Rate(val numberOfCalls: Int, val duration: FiniteDuration){

    def durationInMillis() : Long = duration.toMillis

  }

  case class SetTarget(target: Option[ActorRef])

  case class SetRate(rate: Rate)

  case class Queue(msg: Any)

  class RateInt(numberOfCalls: Int) {

    def msgPer(duration: Int, timeUnit: TimeUnit) = Rate(numberOfCalls,FiniteDuration(duration,timeUnit))

    def msgPer(duration: FiniteDuration) = Rate(numberOfCalls,duration)

    def msgPerSecond = Rate(numberOfCalls,FiniteDuration(1,TimeUnit.SECONDS))

    def msgPerMinute = Rate(numberOfCalls,FiniteDuration(1,TimeUnit.MINUTES))

    def msgPerhour = Rate(numberOfCalls,FiniteDuration(1,TimeUnit.HOURS))

  }

}
