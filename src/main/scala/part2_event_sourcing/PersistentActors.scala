package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

/**
 * User: patricio
 * Date: 13/7/21
 * Time: 10:33
 */
object PersistentActors extends App {

  /*
  Scenario: we have a business and an accountant which  keeps track of our invoices
   */

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)

  // EVENT
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant" // best practice: make this unique

    /**
     * The "normal" receive method
     */
    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        /*
        When you receive a commands
        1) you create an EVENT to persist into the store
        2) you persist the event, the pass in a callback that will get triggered once the event is written
        3) we update the actor state when the event has persisted
         */
        log.info(s"Receive invoice for amount: $amount")
        val event = InvoiceRecorded(latestInvoiceId, recipient, date, amount)
        // SAFE to access mutable state here

        // update state
        persist(event) /* time gap: all other messages send to this actor are STASHED */ { e =>
          latestInvoiceId += 1
          totalAmount += amount
          //correctly identify the sender of the command
          sender() ! "PersistenceACK"
          log.info(s"Persisted $e as invoice #${e.id}, for a total amount $totalAmount")
        }
        // act like a normal actor
      case "print" =>
        log.info(s"Latest invoice #$latestInvoiceId, for a total amount $totalAmount")
    }

    /**
     * Handler that would be called on recovery
     */
    override def receiveRecover: Receive = {
      /*
      best practice: follow the logic in the persistent steps of receiveCommand
       */
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Revered invoice #$id for amount $amount | Total amount: $totalAmount")
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  /*for (i <- 1 to 10) {
    accountant ! Invoice("The sofa company", new Date, i*1000)
  }*/

}
