package streams.documentation.examples.browser

case class Tweet(author: Author, timestamp: Long, body: String){

  def hashTags: Set[Hashtag] =
    body
      .split(" ")
      .collect {
        case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
      }
      .toSet


}
