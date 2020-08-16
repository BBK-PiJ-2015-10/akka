package aia.channels

import java.util.Date

class CancelOrder(time: Date, override val customerId: String, override val productId: String, override val number: Int)
  extends Order(customerId,productId,number) {

}
