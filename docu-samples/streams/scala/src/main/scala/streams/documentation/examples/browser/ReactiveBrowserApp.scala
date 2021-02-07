package streams.documentation.examples.browser

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.scaladsl.Source.actorRefWithAck

import scala.concurrent.Future

case object ReactiveBrowser extends App {

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet,NotUsed] =
    Source(
        Tweet(Author("rolandkuhn"),System.currentTimeMillis(),"#akka rocks!") ::
        Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
        Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
        Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
        Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
        Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
        Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
        Nil
    )

  implicit val actorSystem: ActorSystem = ActorSystem("reactive-tweets")
  implicit val ec = actorSystem.dispatcher

  val done : Future[Done] =
    tweets
      .filterNot(_.hashTags.contains(akkaTag)) // Remove all tweets containing #akka hashtag
      .map(_.hashTags) // Get all sets of hashtags
      .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
      .mapConcat(identity) // Flatten the set of hashtags to a stream of hashtags
      .map(_.name.toUpperCase) // Convert all hashtags to upper case
      .runWith(Sink.foreach(println))


  done.onComplete(
    _ => actorSystem.terminate()
  )



}
