package models

import java.sql.Connection

import anorm._
import anorm.SqlParser._
import dig.AnormExtension._

case class EventData(id: Pk[Long], event: Event, key: String, value: String)

object EventData {

  private def simple(implicit connection: Connection) =
    get[Pk[Long]]("id")  ~
      get[Long]("event_id") ~
      get[String]("key") ~
      get[String]("value") map {
      case id ~ eventId ~ key ~ value =>
        EventData(id, Event.findById(eventId).get, key, value)
    }

  def findById(id: Long)(implicit connection: Connection): Option[EventData] = {
    SQL("select * from event_data where id = {id}") on("id" -> id) as simple.singleOpt
  }

  def findByEventId(eventId: Long)(implicit  connection: Connection): List[EventData] = {
    SQL("select * from event_data where event_id = {eventId}").on("eventId" -> eventId).as(simple *)
  }

}
