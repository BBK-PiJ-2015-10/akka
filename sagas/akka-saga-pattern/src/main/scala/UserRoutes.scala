
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import EmailRegionFactory.EmailRegion
import UserActor.{UserCommand,UserReply}
import UserActor.UserInfo.ChangeEmailInfo
import UserFormats._
import UserRegionFactory.UserRegion
import UserRequests.{ChangeEmailRequest,RegistrationRequest}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import org.slf4j.LoggerFactory


object UserRoutes extends SprayJsonSupport {


  implicit val timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger(UserRoutes.getClass)

  def apply(userRegion: UserRegion, emailRegion: EmailRegion)
           (implicit context: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {

    path("register"){
      post {
        entity(as[RegistrationRequest]) { req =>
          complete {
            userRegion ? UserCommand.Register(UUID.randomUUID(),req.email,req.password) map {
              case response =>
                log.info(s"response=$response")
                response
            } map {
              case UserReply.RegistrationSucceeded => HttpResponse(OK)
              case UserReply.RegistrationFailed => HttpResponse(BadRequest)
              case _ => HttpResponse(InternalServerError)
            }
          }
        }
      }
    } ~
      pathPrefix("users" / JavaUUID) { userId =>
        path("change-email") {
          post {
            entity(as[ChangeEmailRequest]) { req =>
              complete {
                userRegion ? UserCommand.ChangeEmail(ChangeEmailInfo(userId,req.oldEmail,req.newEmail)) map {
                  case response =>
                    log.info(s"response=$response")
                    response
                } map {
                  case UserReply.ChangeEmailSucceeded => HttpResponse(OK)
                  case UserReply.ChangeEmailFailed => HttpResponse(BadRequest)
                  case _ => HttpResponse(InternalServerError)
                }
              }
            }
          }
        }
      }

  }


}
