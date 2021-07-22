package part3_stores_serialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

import java.util.UUID

/**
 * User: patricio
 * Date: 22/7/21
 * Time: 04:39
 */

case class RegisterUser(email: String, name: String)
case class UserRegistered(id: String, email: String, name: String)

// serializer
class UserRegistrationSerializer extends Serializer {

  val SEPARATOR= "//"
  override def identifier: Int = 57000

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event @ UserRegistered(id, email, name) =>
      println(s"Serializing $event")
      s"[$id$SEPARATOR$email$SEPARATOR$name]".getBytes()
    case _ => throw new IllegalArgumentException("only user registered events are allowed in this serializer")
  }

  override def includeManifest: Boolean = false

  // since we always return false at include manifest manifest will be None
  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val serialized: String = new String(bytes)
    val values = serialized.substring(1, serialized.length() - 1).split(SEPARATOR)
    val result = UserRegistered(values(0), values(1), values(2))

    println(s"deserialized $serialized to $result")

    result
  }
}

class UserRegistrationActor extends PersistentActor with ActorLogging {
  override def persistenceId: String = "user-registration"
  var currentId: String = UUID.randomUUID().toString

  override def receiveCommand: Receive = {
    case RegisterUser(email, name) =>
      persist(UserRegistered(currentId, email, name)) { event =>
        currentId = UUID.randomUUID().toString
        log.info(s"Persisted $event")
      }
  }

  override def receiveRecover: Receive = {
    case event @ UserRegistered(id, _,_) =>
      currentId = id
      log.info(s"Recovered: $event")
  }
}

object CustomSerialization extends App {
  /*
    send command to actor
     actor calls persist
     serializer serializes the event into bytes
     the journal write the bytes
   */

  val system = ActorSystem("CustomSerialization", ConfigFactory.load().getConfig("customSerializerDemo"))
  val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "userRegistration")

 /* for (i <- 1 to 10) {
    userRegistrationActor ! RegisterUser(s"user_$i@dmz.dev", s"User $i")
  }*/



}
