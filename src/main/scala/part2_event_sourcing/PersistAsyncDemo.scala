package part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

/**
 * User: patricio
 * Date: 20/7/21
 * Time: 17:26
 */
object PersistAsyncDemo extends App {

  case class Command(contents: String)
  case class Event(contents: String)

  object CriticalStreamProcessor {
    def props(eventAggregator: ActorRef): Props = Props(new CriticalStreamProcessor(eventAggregator))
  }
  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def persistenceId: String = "critical-stream-processor"

    override def receiveRecover: Receive = {
      case message => log.info(s"Recovering: $message")
    }

    override def receiveCommand: Receive = {
      case Command(contents) => /*                                                   time gap*/{
        eventAggregator ! s"Processing $contents"
        persistAsync(Event(contents)) { e =>
          eventAggregator ! e
        }
        // some actual computation
        val persistContents = contents + "_processed"
        persistAsync(Event(persistContents)) /*                                                   time gap*/ { e =>
          eventAggregator ! e
        }
      }
    }
  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"$message")
    }
  }

  val system = ActorSystem("PersistAsyncDemo")

  val eventAggregator = system.actorOf(Props[EventAggregator], "eventAggregator")
  val streamProcessor = system.actorOf(CriticalStreamProcessor.props(eventAggregator), "streamProcessor")

  streamProcessor ! Command("command1")
  streamProcessor ! Command("command2")

  /**
   * persistAsync vs persist
   * - performance: high-throughput environment
   *
   * persist vs persisAsync
   * - ordering guarantee
   */



}
