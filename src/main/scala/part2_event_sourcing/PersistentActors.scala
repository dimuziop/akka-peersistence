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
  case class InvoiceBulk(invoices: List[Invoice])
  case class Shutdown()

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
      case InvoiceBulk(invoices) =>
        /*
        1) create events (Plural)
        2) persist al events
        3) update the actor state when each item persist
         */
        val invoicesIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoicesIds).map(pair => {
          val id = pair._2
          val invoice = pair._1
          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        })
        persistAll(events) { e =>
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice #${e.id}, for a total amount $totalAmount")
        }
        // act like a normal actor
      case "print" =>
        log.info(s"Latest invoice #$latestInvoiceId, for a total amount $totalAmount")
      case Shutdown => context.stop(self)
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

    /*
    This method is called if persistence failed
    The actor will be STOPPED

    Best practice: start the actor again after a while
    (use Backoff supervisor)
     */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /*
    Called if the JOURNAL fails to persist the event
    The actor is RESUMED.
     */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected for $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  /*for (i <- 1 to 10) {
    accountant ! Invoice("The sofa company", new Date, i*1000)
  }*/
  /*
  Persistence failures
   */

  /*
  Persisting Multiple Events
  Persist All
   */
  val newInvoices = for(i <- 1 to 5) yield Invoice("The sofa company", new Date, i*1000)
  accountant ! InvoiceBulk(newInvoices.toList)

  /*
  NEVER EVER CALL PERSIST OR PERSISTALL FROM FUTURES
   */

  /**
   * Shutdown of persistent actors
   * Best practice; Define your own shutdown messages
   */
  accountant ! Shutdown



}
