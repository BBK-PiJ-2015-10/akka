import akka.actor.typed.{ActorSystem,SpawnProtocol}


object ChatSessionMap {

  private var sessions: Map[String,ChatSession] = Map.empty[String,ChatSession]

  def findOrCreate(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) =
    sessions.getOrElse(userId,create(userId))

  private def create(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) = {
    val session = ChatSession(userId)
    sessions += userId -> session
    session
  }

}


