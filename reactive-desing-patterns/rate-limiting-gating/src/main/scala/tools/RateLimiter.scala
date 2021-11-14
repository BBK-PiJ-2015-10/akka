package tools

import tools.RateLimiter.RateLimitExceeded

import scala.concurrent.Future
import scala.concurrent.duration.{Deadline, FiniteDuration}

class RateLimiter(requests: Int, period: FiniteDuration) {

  private val startTimes : Array[Deadline] = {
    val onPeriodAgo : Deadline = Deadline.now - period
    Array.fill(requests)(onPeriodAgo)
  }

  private var position = 0
  private def lastTime: Deadline = startTimes(position)

  private def enqueue(time: Deadline) = {
    startTimes(position) = time
    position +=1
    if (position == requests) position = 0
  }

  def call[T](block: => Future[T]): Future[T] = {
    val now = Deadline.now
    if (((now - lastTime)) < period) Future.failed(RateLimitExceeded)
    else {
      enqueue(now)
      block
    }
  }


}

object RateLimiter {

  case object RateLimitExceeded extends RuntimeException

}
