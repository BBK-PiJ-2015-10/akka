
import java.util.UUID

import scala.concurrent.duration._

import akka.actor.{ActorLogging, ActorRef, ReceiveTimeout}
import akka.persistence.{PersistentActor, SnapshotMetadata, SnapshotOffer}
import akka.event.LoggingReceive
import akka.cluster.sharding.ShardRegion

import UserActor.UserInfo

object EmailActor {

  type Email = String
  type UserId = UUID
  type DeliveryId = Long

  sealed trait EmailReply
  object EmailReply {
    final case class EmailAlreadyExists(deliveryId: DeliveryId, client: ActorRef, userInfo: UserInfo) extends EmailReply
    final case class EmailCreated(deliveryId: DeliveryId, client: ActorRef, userInfo: UserInfo) extends EmailReply
    final case class EmailDeleted(deliveryId: DeliveryId, client: ActorRef, userInfo: UserInfo) extends EmailReply
  }

  sealed trait EmailCommand { def email: Email}
  object EmailCommand {
    final case class CreateEmail(email: Email, deliveryId: DeliveryId, client: ActorRef, userInfo: UserInfo) extends EmailCommand
    final case class DeleteEmail(email: Email, deliveryId: DeliveryId, client: ActorRef, userInfo: UserInfo) extends  EmailCommand
  }

  sealed trait EmailEvent
  object EmailEvent {
    final case class EmailCreated(user: UserId, email: Email) extends EmailEvent
    final case class EmailDeleted(user: UserId, email: Email) extends EmailEvent
  }

  case object Stop

  case class EmailState(userId: UserId, email: Email)

  val idExtractor: ShardRegion.ExtractEntityId = {
    case c: EmailCommand => (c.email,c)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case c: EmailCommand => (math.abs(c.email.hashCode) % 50).toString
  }

}

class EmailActor extends PersistentActor with ActorLogging {

  import EmailActor._
  import ShardRegion.Passivate

  context.setReceiveTimeout(30.seconds)

  var maybeState: Option[EmailState] = None

  var snapshotMetadata: Option[SnapshotMetadata] = None

  override def persistenceId: String = s"email-${self.path.name}"


  override def receiveRecover: Receive = LoggingReceive {

    case EmailCommand.CreateEmail(email,deliveryId,client,userInfo) =>
      maybeState match {
        case Some(state) =>
          if (state.userId == userInfo.userId){
            log.info(s"Duplicate create command for: ${email} -> ${userInfo.userId}")
            sender() ! EmailReply.EmailCreated(deliveryId,client,userInfo)
          } else {
            log.info(s"Bad created command for ${email} -> ${userInfo.userId} . See ${state.userId}")
            sender() ! EmailReply.EmailAlreadyExists(deliveryId,client,userInfo)
          }
        case None =>
          persist(EmailEvent.EmailCreated(userInfo.userId,email)) { event =>
            log.info(s"Created command for : ${email} -> ${userInfo.userId}")
            //update(event)
            sender() ! EmailReply.EmailCreated(deliveryId,client,userInfo)

          }
      }

    case EmailCommand.DeleteEmail(email,deliveryId,client,userInfo) =>
      maybeState match {
        case None =>
          log.info(s"Duplicate delete command for : ${email} -> ${userInfo.userId}")
          sender() ! EmailReply.EmailDeleted(deliveryId,client,userInfo)
        case Some(state) =>
          // TODO Use a better error than EmailAlreadyExists.
          if (state.userId != userInfo.userId) {
            log.info(s"Bad delete command for : ${email} -> ${userInfo.userId}. See ${state.userId}")
            sender() ! EmailReply.EmailAlreadyExists(deliveryId,client,userInfo)
          } else {
            persist(EmailEvent.EmailDeleted(userInfo.userId,email)) { event =>
              log.info(s"Delete command for ${email} -> ${userInfo.userId}")
              update(event)
              sender() ! EmailReply.EmailDeleted(deliveryId,client,userInfo)
            }
          }
      }

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      context.stop(self)

  }


  override def receiveCommand: Receive = ???

  def update(event: EmailEvent): Unit = {
    log.debug(s"update($event")
    event match {
      case EmailEvent.EmailCreated(userId,email) => maybeState = Some(EmailState(userId,email))
      case EmailEvent.EmailDeleted(_,_) => maybeState = None
    }
  }

  def restore(offer: SnapshotOffer): Unit = {
    log.debug(s"restore ($offer)")
    snapshotMetadata = Some(offer.metadata)
    offer.snapshot match {
      case Some(state: EmailState) => maybeState = Some(state)
    }
  }

}







