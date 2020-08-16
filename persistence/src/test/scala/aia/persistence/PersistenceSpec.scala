package aia.persistence

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import org.apache.commons.io.FileUtils
import com.typesafe.config.Config
import akka.testkit.{ImplicitSender, TestKit}

import scala.util.Try
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike,AnyWordSpec}


abstract class PersistenceSpec(system: ActorSystem) extends TestKit(system)
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with PersistanceCleanUp {

  def this(name: String, config: Config) = this(ActorSystem(name,config))

  override protected def beforeAll(): Unit = {
    deleteStorageLocations
  }

  override protected def afterAll(): Unit = {
    deleteStorageLocations
    TestKit.shutdownActorSystem(system);
  }

  def killActors(actors: ActorRef*): Unit ={
    actors.foreach { actor =>
      watch(actor)
      system.stop(actor)
      expectTerminated(actor)
      Thread.sleep(1000)
    }
  }
}

trait PersistanceCleanUp {

  def system: ActorSystem

  val storageLocation = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.journal.leveldb-shared.store.dir",
    "akka.persistence.snapshot-store.local.dir").map { s =>
    new File(system.settings.config.getString(s))}

  def deleteStorageLocations = {
    storageLocation.foreach(dir => Try(FileUtils.deleteDirectory(dir)))
  }

}