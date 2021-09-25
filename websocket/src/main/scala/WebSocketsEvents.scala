object WebSocketsEvents {

  sealed trait WebSocketsEvent

  final case class MessageToUser(phoneNumber: String, message: String) extends WebSocketsEvent

  final case object Complete extends WebSocketsEvent

  final case class Failure(ex: Throwable) extends WebSocketsEvent

}
