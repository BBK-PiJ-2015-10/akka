package aia.persistence.rest

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.{PathMatcher1,Route}

import spray.json._

import aia.persistence._


class ShopperService(val shoppers: ActorRef, val system: ActorSystem, val requestTimeOut: Timeout)
  extends ShopperRoutes {

    val executionContext = system.dispatcher

}



trait ShopperRoutes extends ShopperMarshalling {

  def shoppers: ActorRef

  implicit def requestTimeOut: Timeout
  implicit def executionContext: ExecutionContext

  val ShopperIdSegment : PathMatcher1[Long] = Segment.flatMap{id => Try(id.toLong).toOption}
  val ProductIdSegment : PathMatcher1[String] = Segment.flatMap{id => if(!id.isEmpty) Some(id) else None}


  def routes : Route = deleteBasket ~ getBasket ~ updateBasket ~ pay ~ deleteItem ~ updateItem

  def deleteBasket : Route = {
    delete {
      pathPrefix("shopper" / ShopperIdSegment / "basket"){ shopperId =>
        pathEnd {
          shoppers ! Basket.Clear(shopperId)
          complete(OK)
        }
      }
    }
  }

  def getBasket : Route = {
    get {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        pathEnd {
         onSuccess(shoppers.ask(Basket.GetItems(shopperId)).mapTo[Items]) {
           case Items(Nil) => complete(NotFound)
           case items: Items => complete(items)
         }
        }
      }
    }
  }

  def updateBasket : Route = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        pathEnd {
          entity(as[Items]) { items =>
            shoppers ! Basket.Replace(items, shopperId)
            complete(OK)
          } ~
            entity(as[Item]) { item =>
              shoppers ! Basket.Add(item, shopperId)
              complete(OK)
            }
        }
      }
    }
  }

  def pay : Route = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "pay"){ shopperId =>
        shoppers ! Shopper.PayBasket(shopperId)
        complete(OK)
      }
    }
  }

  def deleteItem : Route = {
    delete {
      pathPrefix("shopper" / ShopperIdSegment / "basket" / ProductIdSegment){
        (shopperId,productId) =>
          pathEnd {
            val removeItem = Basket.RemoveItem(productId,shopperId)
            onSuccess(shoppers.ask(removeItem).mapTo[Option[Basket.ItemRemoved]]){
              case Some(_) => complete(OK)
              case None => complete(NotFound)
            }
          }
      }
    }
  }


  def updateItem : Route = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket" / ProductIdSegment) {
        (shopperId,productId) =>
          pathEnd {
            entity(as[ItemNumber]) { itemNumber =>
              val ItemNumber(number) = itemNumber
              val updateItem = Basket.UpdateItem(productId,number,shopperId)
              onSuccess(shoppers.ask(updateItem).mapTo[Option[Basket.ItemUpdated]]){
                case Some(_) => complete(OK)
                case None => complete(NotFound)
              }
            }
          }
      }
    }
  }

}
