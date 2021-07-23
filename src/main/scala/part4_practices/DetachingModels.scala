package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory
import part4_practices.DataModel.{WrittenCouponApplied, WrittenCouponApplied2}
import part4_practices.DomainModel.{CouponApplied, User}

import scala.collection.mutable

/**
 * User: patricio
 * Date: 22/7/21
 * Time: 10:47
 */
object DetachingModels extends App {
  import DomainModel._

  class CouponManager extends PersistentActor with ActorLogging {

    val coupons: mutable.Map[String, User] = new mutable.HashMap[String, User]()

    override def persistenceId: String = "coupon-manager"

    override def receiveRecover: Receive = {
      case event @ CouponApplied(code, user) =>
        coupons.put(code, user)
        log.info(s"Recovered $event")
    }

    override def receiveCommand: Receive = {
      case ApplyCoupon(coupon, user) =>
        persist(CouponApplied(coupon.code, user)) { e =>
          if (!coupons.contains(coupon.code))
            coupons.put(coupon.code, user)
            log.info(s"Persisted $e")
        }
    }
  }

  val system = ActorSystem("DetachingModels", ConfigFactory.load().getConfig("detachingModels"))
  val couponManagerActor = system.actorOf(Props[CouponManager], "couponManager")

 /* for (i <- 1 to 5) {
    val coupon = Coupon(UUID.randomUUID().toString, 100)
    val user = User(UUID.randomUUID().toString, s"user$i@dev.com", s"name$i")

    couponManagerActor ! ApplyCoupon(coupon, user)
  }*/


}

object DomainModel {
  case class User(id: String, email: String, name: String)

  case class Coupon(code: String, promotionAmount: Int)

  //command
  case class ApplyCoupon(coupon: Coupon, user: User)

  // event
  case class CouponApplied(code: String, user: User)
}

object DataModel {
  case class WrittenCouponApplied(code: String, userId: String, userEmail: String)
  case class WrittenCouponApplied2(code: String, userId: String, userEmail: String, userName: String)
}

class ModelAdapter extends EventAdapter {
  override def manifest(event: Any): String = "CMA"

  override def toJournal(event: Any): Any = event match {
    case event @ CouponApplied(code, user) =>
      println(s"I'm converting $event to Data model")
      WrittenCouponApplied2(code, user.id, user.email, user.name)
      //WrittenCouponApplied(code, user.id, user.email)
  }

  // journal -> serializer -> fromJournal -> actor
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case event @ WrittenCouponApplied(code, userId, userEmail) =>
      println(s"I'm converting $event to DOMAIN model")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, "")))
    case event @ WrittenCouponApplied2(code, userId, userEmail, userName) =>
      println(s"I'm converting $event to DOMAIN model")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, userName)))
    case other => EventSeq.single(other)
  }
}