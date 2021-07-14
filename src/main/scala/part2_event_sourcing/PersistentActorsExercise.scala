package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import scala.annotation.tailrec
import scala.util.Random

/**
 * User: patricio
 * Date: 14/7/21
 * Time: 04:19
 */
object PersistentActorsExercise extends App {
  /*
  Persistent actor for a voting extension
  Keep:
    - the citizens who voted
    - the poll: mapping between a candidate and the number of received votes so far
   The actor must be able to recover its state if it's shut down or restarted
   */
  case class Vote(citizenID: String, candidate: String)

  // Just for semantic accuracy
  /*case class Pool(citizenID: String, candidate: String)*/

  class AntiDemocraticPoolingStation(stationNumber: Int) extends PersistentActor with ActorLogging {

    var pooledCitizens: List[String] = List()
    var cheatingCitizens: List[String] = List()
    var results: Map[String, Int] = Map()

    override def persistenceId: String = s"polling-station-$stationNumber"

    override def receiveCommand: Receive = {
      case vote @ Vote(_, __) =>
        persist(vote) { e =>
          if (pooledCitizens.contains(e.citizenID)) cheatingCitizens + e.citizenID
          else {
            pooledCitizens + e.citizenID
            results = results + Tuple2(e.candidate, results.getOrElse[Int](e.candidate, 0) + 1)
            log.info(s"Citizen ${e.citizenID} has voted ${e.candidate}")
            log.info(s"Partial results: ${stringifyResults(results)}")
          }
        }
      case "PrintPartials" =>
        log.info(s"Partial results: ${stringifyResults(results)}")
      case "FinishPooling" =>
        log.info(s"Final results: ${stringifyResults(results)}")
        val winner = results.maxBy(reg => reg._2)
        log.info(s"The winner is ${winner._1} with ${winner._2} votes")
        context.stop(self)
    }

    override def receiveRecover: Receive = {
      case vote @ Vote(_, __) => // COMMAND sourcing against EVENT sourcing
        if (pooledCitizens.contains(vote.citizenID)) cheatingCitizens + vote.citizenID
      else {
        pooledCitizens + vote.citizenID
        results = results + Tuple2(vote.candidate, results.getOrElse[Int](vote.candidate, 0) + 1)
        log.info(s"Recover: Citizen ${vote.citizenID} has voted ${vote.candidate}")
      }
    }

    /**
     * This method shouldn't live here, for this purposes only
     * @param results
     */
    private def stringifyResults(results: Map[String, Int]): String = {
      @tailrec
      def aux(pending: Map[String, Int], stringifies: String = ""): String = {
        if (pending.isEmpty) stringifies
        else aux(pending.tail, stringifies + s" | ${pending.head._1} | ${pending.head._2} | \n")
      }
      s" | Candidate | Votes | \n" + aux(results)
    }
  }

  val system = ActorSystem("PollSystem")
  val poolStation1 = system.actorOf(Props(new AntiDemocraticPoolingStation(1)), "polling-station-1")

  val candidates = List("El Peter", "El Vladi", "El Wig", "El Perverse Megalomaniac", "El hubris guy", "El tito", "El bigotes", "El bigotito")
  val random = new Random

  /*for (_ <- 1 to 10) {
    poolStation1 ! Vote(UUID.randomUUID().toString, candidates(random.nextInt(candidates.length)))
  }*/

  poolStation1 ! "PrintPartials"
  poolStation1 ! "FinishPooling"

}
