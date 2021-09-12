package streams.documentation.examples.browser

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.scaladsl.Source.actorRefWithAck

import scala.concurrent.Future

case object ReactiveBrowserApp extends App {

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

  /*
  val done : Future[Done] =
    tweets
      .filterNot(_.hashTags.contains(akkaTag)) // Remove all tweets containing #akka hashtag
      .map(_.hashTags) // Get all sets of hashtags
      .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
      .mapConcat(identity) // Flatten the set of hashtags to a stream of hashtags
      .map(_.name.toUpperCase) // Convert all hashtags to upper case
      .runWith(Sink.foreach(println))  // Attach the Flow to a Sink that will finally print the hashtags
   */

  //done.onComplete(
    //_ => actorSystem.terminate()
  //)

  /*
  val authors: Source[Author,NotUsed] =
    tweets.filter(_.hashTags.contains(akkaTag))
      .map(_.author)

  val done1 = authors.runWith(Sink.foreach(println))

  done1.onComplete(
    _ => actorSystem.terminate()
  )
  */

  /*
  val hashtags: Source[Hashtag,NotUsed] = tweets.mapConcat(_.hashTags.toList)

  val writeAuthors: Sink[Author,NotUsed] = ???

  val writeHashTags: Sink[Hashtag,NotUsed] = ???

  val g = RunnableGraph.fromGraph(GraphDSL.create(){ implicit b =>
    import GraphDSL.Implicits._

    val bcas = b.add(Broadcast[Tweet](2))
    tweets ~> bcas.in
    bcas.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcas.out(1) ~> Flow[Tweet].mapConcat(_.hashTags.toList) ~> writeHashTags
    ClosedShape
  })

  g.run()

   */

  /*
  tweets.buffer(10,OverflowStrategy.dropHead)
    .map()
    .runWith(Sink.ignore)
 */

  val count: Flow[Tweet,Int,NotUsed] = Flow[Tweet].map(_ => 1)

  val sumSink: Sink[Int,Future[Int]] = Sink.fold[Int,Int](0)(_ + _)

  val counterGraph : RunnableGraph[Future[Int]] = tweets
    .via(count)
    .toMat(sumSink)(Keep.right)

  val sum: Future[Int] = counterGraph.run()

  sum.foreach(c => println(s"Total tweets processed: $c"))

  sum.onComplete(
    _ => actorSystem.terminate()
  )



}
