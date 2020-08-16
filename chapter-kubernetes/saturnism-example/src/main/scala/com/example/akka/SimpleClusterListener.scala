package com.example.akka

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object SimpleClusterListener {

  def props:  Props = Props(new SimpleClusterListener)

}

class SimpleClusterListener extends Actor with ActorLogging{

  val cluster : Cluster = Cluster(context.system)


  override def preStart(): Unit = {
    cluster.subscribe(self,InitialStateAsEvents,classOf[MemberEvent],classOf[UnreachableMember])
    super.preStart()
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    super.postStop()
  }

  override def receive: Receive = {
    case MemberUp(member) => log.info(s"fuckers $member is UP")
    case MemberRemoved(member,_) => log.info(s"suckers $member is REMOVED")
    case UnreachableMember(member) => log.info(s"culons $member is UNREACHABLE")
    case s => log.info(s"Got something else : "+s)
  }

}
