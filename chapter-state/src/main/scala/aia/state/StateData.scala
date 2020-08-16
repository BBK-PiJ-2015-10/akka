package aia.state

case class StateData(nrBooksInStore: Int, pendingRequests:Seq[BookRequest])
