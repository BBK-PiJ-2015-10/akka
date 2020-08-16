package record.analyzer.state

import akka.actor.{Actor, ActorRef, DeadLetter, FSM, PoisonPill, Props}
import akka.routing.{BalancingPool, BroadcastPool, RoundRobinPool, SmallestMailboxPool}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import record.analyzer.channel.DoneSourceMessageBus
import record.analyzer.model.{ProcessedRecord, SubmittedRecord}
import record.analyzer.service.RecordProcessor.{DoneProcessingOrphanRecords, ProcessOrphanRecords}
import record.analyzer.service.RecordProcessor
import record.analyzer.service.RecordSink
import record.analyzer.service.sharded.ShardedRecordSinks
import record.analyzer.state

sealed trait CoordinatorState

case object Bored extends CoordinatorState
case object ProcessingRecords extends CoordinatorState
case object ProcessingOrphanRecords extends CoordinatorState

case class CoordinatorStateData()

case object DoneSources


object Coordinator {

  def props(sources: Set[String], destination: String) = Props(new Coordinator(sources,destination))

}


class Coordinator(sources: Set[String], destination: String) extends FSM[CoordinatorState,CoordinatorStateData]{

  val mediator : ActorRef = DistributedPubSub(context.system).mediator

  //context.system.eventStream.subscribe(self,classOf[DoneSource])
  //sources.foreach(source => DoneSourceMessageBus.subscribe(self,source))
  mediator ! Subscribe("donesource",self)

  context.system.eventStream.subscribe(self,classOf[DeadLetter])

  //val sink = context.actorOf(SmallestMailboxPool(9()).props(RecordSink.props(destination)))


  val sink = context.actorOf(
    ClusterRouterPool(RoundRobinPool(9),ClusterRouterPoolSettings(
      totalInstances = 9,
      maxInstancesPerNode = 2,
      allowLocalRoutees = false
    )).props(RecordSink.props(destination)),"sink")


  //val sink = ClusterSharding(context.system).shardRegion(ShardedRecordSinks.shardName)

  val processor = context.actorOf(RecordProcessor.props(sources),name="processor")
  val controllers : Set[ActorRef] = initializeControllers(sources)
  var doneControllers : Set[String] = sources

  private def initializeControllers(sources: Set[String]): Set[ActorRef] = {
    //sources.map(source => context.actorOf(SourceController.props(source)))
    sources.map(source =>
      context.system.actorOf(
        ClusterSingletonProxy.props(
          singletonManagerPath = "/user/controller-" +source.substring(22),
          settings = ClusterSingletonProxySettings(context.system)
        ), name = "proxy-controller-" +source.substring(22)
      )
    )
  }

  startWith(Bored,CoordinatorStateData())

  initialize()

  when(Bored){
    case Event(FetchRecords,_) => {
      controllers.foreach(controller => controller ! FetchRecords)
      goto(ProcessingRecords)
    }
  }

  when(ProcessingRecords){
    case Event(DoneSource(source,_), _) => {
      doneControllers -= source
      log.info(s"Actor with name ${self.path.name} got Done received from ${source}, controllers size is ${doneControllers.size}")
      //actorRef ! PoisonPill
      if (doneControllers.isEmpty) goto (ProcessingOrphanRecords) else stay()
    }
    case Event(processedRecord: ProcessedRecord, _) => {
      sink ! processedRecord
      stay()
    }
    case Event(SubmittedRecord, _) => {
      stay()
    }
  }

  when(ProcessingOrphanRecords){
    case Event(processedRecord: ProcessedRecord, _) => {
      sink ! processedRecord
      stay()
    }
    case Event(DoneProcessingOrphanRecords, _) => {
      //sender() ! PoisonPill
      goto(Bored)
    }
  }

  onTransition {
    case ProcessingRecords -> ProcessingOrphanRecords => {
      //processor ! ProcessOrphanRecords
      mediator ! Publish("records",ProcessOrphanRecords)
    }
    case ProcessingOrphanRecords -> Bored => {
      doneControllers = sources
      //sink ! PoisonPill
      //context.parent ! Done
      //self ! PoisonPill
    }
  }

  whenUnhandled {
    case Event(deadLetter: DeadLetter, _) => {
      log.warning(s"ItOps save me please, need to deal with message ${deadLetter.message} sentBy ${deadLetter.sender} sentTo ${deadLetter.recipient}")
      // FORWARD THIS HOT POTATO TO ItOps
      stay()
    }
    case Event(SubmittedRecord,_) => {
      log.info(s"Received a SubmittedRecord from actor ${sender()}")
      stay()
    }
    case Event(message,_) => {
      log.warning(s"Coordinator While on ${stateName} received a weird message ${message} from  ${sender()}")
      stay()
    }
  }

}





