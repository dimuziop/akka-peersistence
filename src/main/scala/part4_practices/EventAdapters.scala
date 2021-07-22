package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

/**
 * User: patricio
 * Date: 22/7/21
 * Time: 08:07
 */
object EventAdapters extends App {
  // store for acoustic guitars
  val ACOUSTIC = "acoustic"
  val ELECTRIC = "electric"

  // data structure
  case class Guitar(id: String, model: String, make: String, guitarType: String = ACOUSTIC)

  //command
  case class AddGuitar(guitar: Guitar, quantity: Int)

  // event
  case class GuitarAdded(guitarId: String, guitarModel: String, guitarMake: String, quantity: Int)
  case class GuitarAddedV2(guitarId: String, guitarModel: String, guitarMake: String, quantity: Int, guitarType: String)

  class InventoryManager extends PersistentActor with ActorLogging {
    override def persistenceId: String = "guitar-inventory-manager"

    val inventory: mutable.Map[Guitar, Int] = new mutable.HashMap[Guitar, Int]()

    override def receiveCommand: Receive = {
      case AddGuitar(guitar@Guitar(id, model, make, guitarType), quantity) =>
        persist(GuitarAddedV2(id, model, make, quantity, guitarType)) { _ =>
          updateInventory(guitar, quantity)
          log.info(s"Added the $quantity x $guitar to inventory")
        }
      case "print" =>
        log.info(s"Current inventory is $inventory")
    }

    override def receiveRecover: Receive = {
      case GuitarAdded(id, model, make, quantity) =>
        val guitar: Guitar = Guitar(id, model, make, ACOUSTIC)
        updateInventory(guitar, quantity)
        log.info(s"Recovered the $quantity x $guitar to inventory")
      case event @ GuitarAddedV2(id, model, make, quantity, guitarType) =>
        val guitar: Guitar = Guitar(id, model, make, guitarType)
        updateInventory(guitar, quantity)
        log.info(s"Recovered the $event")
    }

    private def updateInventory(guitar: Guitar, quantity: Int): Unit = {
      val existingQuantity = inventory.getOrElse(guitar, 0)
      inventory.put(guitar, existingQuantity + quantity)
    }
  }

  val system = ActorSystem("eventAdapters", ConfigFactory.load().getConfig("eventAdapters"))
  val inventoryManager = system.actorOf(Props[InventoryManager], "inventoryManager")

  /*val guitars = for (i <- 1 to 10) yield Guitar(UUID.randomUUID().toString, s"JVMBlueSpecial $i", "Brian June")
  guitars.foreach { guitar =>
    inventoryManager ! AddGuitar(guitar, 5)
  }*/

  inventoryManager ! "print"



}
