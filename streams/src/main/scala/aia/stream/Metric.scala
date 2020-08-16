package aia.stream

import java.time.ZonedDateTime
import scala.concurrent.duration.FiniteDuration

case class Metric(serviceTime: String, time: ZonedDateTime, metric: Double, tag: String, drift: Int = 0)


