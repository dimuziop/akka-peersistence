package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}

/**
 * User: patricio
 * Date: 20/7/21
 * Time: 10:39
 */
object RecoveryDemo extends App {

  case class Command(contents: String)

  case class Event(id: Int , contents: String)

  class RecoveryActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "recovery_actor"

    override def receiveCommand: Receive = online(0)

    def online(latestPersistedId: Int): Receive = {
      case Command(contents) =>
        persist(Event(latestPersistedId, contents)) { event =>
          log.info(s"successfully persisted $event, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished.")
          context.become(online(latestPersistedId + 1))
        }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        // additional initialization
        log.info("I have finished recovery")
      case Event(id, contents) =>
        /*if (contents.contains("314"))
          throw new RuntimeException("I can't take this anymore")*/
        log.info(s"Recovered $contents, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished.")
        context.become(online(id + 1))
        /*
        THIS WILL NOT CHANGE the event handler during recovery
        after recovery the normal handler will be the result of ALL thew stacking od contexts.become
         */
    }

    override protected def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error("I failed from recovery")
      super.onRecoveryFailure(cause, event)
    }

    //override def recovery: Recovery = Recovery(toSequenceNr = 100)
    //override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)
    //override def recovery: Recovery = Recovery.none
  }

  val system = ActorSystem("RecoveryDemo")
  val recoveryActor = system.actorOf(Props[RecoveryActor], "recoveryActor")
  /**
   * Stashing commands
   */

  /*for (i <- 1 to 1000) {
    recoveryActor ! Command(s"command $i")
  }*/
  // ALL COMMANDS SENT DURING RECOVERY ARE STASHED

  /*
  2 - Failure on recovery
    - onRecoveryFailure(cause, event)
   */

  /**
   * 3 - Customizing recovery
   *  - DO NOT persist more events after a customized _incomplete_ recovery
   */

  /**
   * 4 - recovery status or KNOWING when you're done recovery
   * - getting a signal when you're done recovering
   */

  /*
  5 - stateless actors
   */
  recoveryActor ! Command(s"Special command 1")
  recoveryActor ! Command(s"Special command 2")

}
