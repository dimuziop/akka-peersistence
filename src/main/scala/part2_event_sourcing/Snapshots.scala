package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

/**
 * User: patricio
 * Date: 15/7/21
 * Time: 03:57
 */
object Snapshots extends App {

  //commands
  case class ReceivedMessage(contents: String)

  case class SentMessage(contents: String)

  //events
  case class ReceivedMessageRecord(id: Int, contents: String)

  case class SentMessageRecord(id: Int, contents: String)

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {
    val MAX_MESSAGES = 10

    var currentMessageId = 0
    var commandWithoutCheckpoint = 0
    val lastMessages = new mutable.Queue[(String, String)]()

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered received message: $id: $contents")
        maybeReplaceMessage(contact, contents)
        currentMessageId = id
      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered sent message: $id: $contents")
        maybeReplaceMessage(owner, contents)
        currentMessageId = id
      case SnapshotOffer(metadata, contents) =>
        log.info(s"Recovered snapshot: $metadata")
        contents.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue)
    }

    def maybeCheckpoint(): Unit = {
      commandWithoutCheckpoint += 1
      if (commandWithoutCheckpoint >= MAX_MESSAGES) {
        log.info("Saving checkpoint....")
        saveSnapshot(lastMessages) // asynchronous operation
        commandWithoutCheckpoint = 0
      }
    }

    override def receiveCommand: Receive = {
      case ReceivedMessage(contents) => {
        persist(ReceivedMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Received message: $contents")
          maybeReplaceMessage(contact, contents)
          currentMessageId += 1
          maybeCheckpoint()
        }
      }
      case SentMessage(contents) => {
        persist(SentMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Sent message: $contents")
          maybeReplaceMessage(owner, contents)
          currentMessageId += 1
        }
      }
      case "print" => log.info("Most recent messages")
        // snapshot-related messages
      case SaveSnapshotSuccess(metadata) => log.info(s"saving snapshot succeeded $metadata")
      case SaveSnapshotFailure(metadata, reason) => log.info(s"saving snapshot $metadata failed due to $reason")
    }

    override def persistenceId: String = s"$owner-$contact-chat"

    private def maybeReplaceMessage(sender: String, contents: String) = {
      if (lastMessages.size >= MAX_MESSAGES) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }

  }

  object Chat {
    def props(owner: String, contact: String): Props = Props(new Chat(owner, contact))
  }

  val system = ActorSystem("SnapshotDemo")
  val chat = system.actorOf(Chat.props("pat123", "daniel123"))

  /*for (i <- 1 to 100000) {
    chat ! ReceivedMessage(s"Akka rocks $i")
    chat ! SentMessage(s"Akka Rules $i")
  }*/

  chat ! "print"

 /*
 event 1
 event 2
 event 3
 snapshot 1
 event 4
 snapshot 2
 event 5
 event 6
  */

  /*
  pattern:
  - after each persist, maybe save a snapshot (logic is up to you)
  - if you save a snapshot, handle the SnapshotOffer message in receiveRecover
  - (optional, but BP) handle SaveSnapshotSuccess and SaveSnapshotFailure in ReceiveCommand
  - profit for the extra speed
   */


}
