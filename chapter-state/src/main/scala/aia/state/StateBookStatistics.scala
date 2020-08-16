package aia.state

case class StateBookStatistics(val sequence: Long, books: Map[String,BookStatistics])
