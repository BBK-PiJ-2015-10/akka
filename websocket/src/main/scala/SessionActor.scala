import akka.actor.typed.{Behavior,SpawnProtocol,ActorRef}
import akka.actor.typed.scaladsl.Behaviors

object SessionActor {

  import CoreChatEvents._
  import WebSocketsEvents._

  def receive(websocket: Option[ActorRef[WebSocketsEvent]]) : Behavior[CoreChatEvent] = Behaviors.receiveMessage {

    // received a message from the user, route to this phone
    case UserMessage(msg,phone) =>
      println(s"Sending message $msg to $phone")
      Behaviors.same

    // received an SMS message from the client, route back to the websocket
    case SMSMessage(sender, message) =>
      println("Received SMS Message!")
      websocket.foreach { socket =>
        socket ! MessageToUser(sender,message)
      }
      Behaviors.same

    case Connected(websocket) =>
      println("Received connection request!")
      receive(Some(websocket))

    case Disconnected =>
      println("Dying now!")
      Behaviors.stopped

    case Failed(ex) =>
      throw new RuntimeException(ex)

  }


}
