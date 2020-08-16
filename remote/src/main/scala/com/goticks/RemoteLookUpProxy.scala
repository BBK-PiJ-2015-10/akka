package com.goticks

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, ReceiveTimeout, Terminated}
import scala.language.postfixOps

import scala.concurrent.duration._


class RemoteLookUpProxy(path: String) extends Actor with ActorLogging{

  context.setReceiveTimeout(3 seconds)


  def sendIdentifyRequest() : Unit  = {
    val selection = context.actorSelection(path)
    selection ! identify
  }

  override def receive: Receive = identify


  def identify : Receive = {

    case ActorIdentity(`path`,Some(actor)) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("switching to active state")
      context.become(active(actor))
      context.watch(actor)

    case ActorIdentity(`path`,None) =>
      log.error(s"Remote actor with path $path is not available.")

    case ReceiveTimeout => sendIdentifyRequest()

    case msg: Any =>
      log.error(s"Ignoring message $msg, remote actor is not ready yet")

  }


  def active(actor: ActorRef) : Receive = {

    case Terminated(actorRef) =>
      log.info(s"Actor $actorRef terminated.")
      log.info("switching identity state")
      context.become(identify)
      context.setReceiveTimeout(3 seconds)
      sendIdentifyRequest()

    case msg: Any => actor forward msg

  }

}
