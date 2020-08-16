package aia.state


case object PublisherRequest
case class BookReply(context: AnyRef, reservedId: Either[String,Int])
