package part3_stores_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import part3_stores_serialization.LocalStores.SimplePersistentActor

/**
 * User: patricio
 * Date: 21/7/21
 * Time: 15:55
 */
object Postgres extends App {

  val postgresStoresActorSystem = ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("postgresDemo"))
  val persistentActor = postgresStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love akka [$i]"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20) {
    persistentActor ! s"I love akka [$i]"
  }

}
