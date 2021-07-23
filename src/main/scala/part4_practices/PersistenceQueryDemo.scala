package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

/**
 * User: patricio
 * Date: 23/7/21
 * Time: 10:55
 */
object PersistenceQueryDemo extends App {

  val system = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))

  // read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // give me all persistent IDs
  val persistenceIds = readJournal.persistenceIds()

  implicit val materializer: Materializer = Materializer(system)

  persistenceIds.runForeach { persistenceId =>
    println(persistenceId)
  }

  val events = readJournal.eventsByPersistenceId("persistence-query-id-1",0, Long.MaxValue)
  events.runForeach(event =>
    println(s"Read Event $event")
  )

  // events by tags
  val rockPlaylists = readJournal.eventsByTag("rock", Offset.noOffset)
  rockPlaylists.runForeach(event =>
    println(s"Playlist with rock songs: $event")
  )
}

object App2 extends App {
  class SimplePersistentActor extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case e => log.info(s"Recovered: $e")
    }

    override def receiveCommand: Receive = {
      case m => persist(m) { _ =>
        log.info(s"Persisted: $m")
      }
    }

    override def persistenceId: String = "persistence-query-id-1"
  }

  val genres = Array("pop", "rock", "hip-hop", "jazz", "disco")
  case class Song(artist: String, title: String, genre: String)
  case class Playlist(songs: List[Song])
  case class PlaylistPurchased(id: String, songs: List[Song])

  class MusicStoreCheckoutActor extends PersistentActor with ActorLogging {

    var latestPlaylistId: String = UUID.randomUUID().toString

    override def receiveRecover: Receive = {
      case e @ PlaylistPurchased(id, songs) =>
        log.info(s"Recovered: $e")
        latestPlaylistId = id
    }

    override def receiveCommand: Receive = {
      case Playlist(songs) =>
        persist(PlaylistPurchased(latestPlaylistId, songs)) { e =>
          log.info(s"Persisted $e")
          latestPlaylistId = UUID.randomUUID().toString
        }
    }

    override def persistenceId: String = "music-store-checkout"
  }

  class MusicStoreWriteEventAdapter extends WriteEventAdapter {
    override def manifest(event: Any): String = "music-store"

    override def toJournal(event: Any): Any = event match {
      case event @ PlaylistPurchased(_, songs) =>
        val genres = songs.map(_.genre).toSet
        println("Genres "  + genres)
        Tagged(event, tags = genres)
      case event => event
    }
  }

  val system = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))
  val simpleActor = system.actorOf(Props[SimplePersistentActor])
  val checkOutActor = system.actorOf(Props[MusicStoreCheckoutActor])

  import system.dispatcher
  system.scheduler.scheduleOnce(5 seconds) {
    simpleActor ! "Some message"
  }

  val r = new Random
  for (i <- 1 to 10) {
    val maxSongs = r.nextInt(5)
    val songs = for (i <- 1 to maxSongs) yield {
      Song(s"Artist $i", s"My song $i", genres(r.nextInt(genres.length)))
    }
    checkOutActor ! Playlist(songs.toList)
  }
}
