package part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.{Date, UUID}

/**
 * User: patricio
 * Date: 14/7/21
 * Time: 06:29
 */
object MultiplePersist extends App {

  /*
  Diligent accountant: with every invoice, will persist TWO events
    - a tax record for the fiscal authority
    - an invoice record for personal logs or some auditing authority
   */
  // COMMAND
  case class Invoice(recipient: String, date: Date, amount: Double)

  // EVENTS
  case class TaxRecord(taxId: String, recordId: String, date: Date, totalAmount: Double)
  case class InvoiceRecord(invoiceRecordId: String, recipient: String, date: Date, amount: Double)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef): Props = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestRecordId: String = UUID.randomUUID().toString
    var latestInvoiceRecordId: String = UUID.randomUUID().toString

    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        persist(TaxRecord(taxId, latestRecordId, date, amount / 3)) { record =>
          taxAuthority ! record
          latestRecordId = UUID.randomUUID().toString
          persist("I hereby declare this tax record to be true and complete.") { declaration =>
            taxAuthority ! declaration
          }
        }
        persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestInvoiceRecordId = UUID.randomUUID().toString
          persist("I hereby declare this invoice record to be true.") { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def persistenceId: String = "diligent-accountant"
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received: $message")
    }
  }


  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("SOME_UK_TAX_ID", taxAuthority))

  accountant ! Invoice("The sofa company", new Date, 2000)

  /*
  The message ordering (TaxRecord -> InvoiceRecord) is GUARANTEED
   */
  /**
   * PERSISTENCE IS ALSO BASED ON MESSAGE PASSING.
   *
   */

  // nested persisting

  accountant ! Invoice("The supercart company", new Date, 20004302)


}
