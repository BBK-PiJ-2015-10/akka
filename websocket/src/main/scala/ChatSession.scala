import scala.concurrent.{Future, ExecutionContext}


import akka.actor.typed.{ActorRef,ActorSystem,SpawnProtocol,Props,DispatcherSelector}
import akka.actor.typed.SpawnProtocol.Spawn

import akka.http.scaladsl.model.ws.{TextMessage,Message}
import akka.stream.{FlowShape,OverflowStrategy}
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl.{ActorSink,ActorSource}
import akka.util.Timeout

import CoreChatEvents._
import WebSocketsEvents._


class ChatSession(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) {

  println("Spawning new chat session")

  import akka.actor.typed.scaladsl.AskPattern._
  import scala.concurrent.duration._
  import ChatSession._

  implicit val timeout: Timeout = Timeout(3.seconds)
  implicit val ec = system.dispatchers.lookup(DispatcherSelector.default())

  // asks to spawn an actor outside of the system
  private[this] val sessionActor: Future[ActorRef[CoreChatEvent]] =
    system.ask[ActorRef[CoreChatEvent]] { ref =>
      Spawn[CoreChatEvent](
        behavior = SessionActor.receive(None),
        name = s"$userId-chat-session",
        props = Props.empty,
        replyTo = ref
      )
    }

  def webflow(): Future[Flow[Message,Message,_]] = sessionActor.map { session =>

    Flow.fromGraph(

      GraphDSL.create(webSocketsActor) { implicit builder => socket =>

        import GraphDSL.Implicits._

        val webSocketSource = builder.add(
          Flow[Message].collect {
            case TextMessage.Strict(txt) =>
              UserMessage(txt,"111-111-1111")
          }
        )


        val webSocketSink = builder.add(
          Flow[WebSocketsEvent].collect {
            case MessageToUser(p,t) =>
              TextMessage(p + t)
          }
        )


        val routeToSession = builder.add(ActorSink.actorRef[CoreChatEvent](
          ref = session,
          onCompleteMessage = Disconnected,
          onFailureMessage = Failed.apply
        ))

        val materializedActorSource = builder.materializedValue.map(ref => Connected(ref))

        val merge = builder.add(Merge[CoreChatEvent](2))

        webSocketSource ~> merge.in(0)
        materializedActorSource ~> merge.in(1)

        merge ~> routeToSession
        socket ~> webSocketSink

        FlowShape(webSocketSource.in,webSocketSink.out)

      }

    )

  }



}

object ChatSession {

  def apply(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) : ChatSession =
    new ChatSession(userId)

  val webSocketsActor = ActorSource.actorRef[WebSocketsEvent](completionMatcher = {
    case Complete =>
  }, failureMatcher = {
    case WebSocketsEvents.Failure(ex) => throw ex
  }, bufferSize = 5, OverflowStrategy.fail)

}

