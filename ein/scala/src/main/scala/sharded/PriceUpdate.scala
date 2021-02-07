package sharded

case class PriceUpdate(companyName: String, price: Double) {

  override def hashCode(): Int = companyName.hashCode

  override def equals(obj: Any): Boolean = {
    obj match {
      case PriceUpdate(cn, pr) => cn.equals(this.companyName)
      case _ => false
    }
  }
}
