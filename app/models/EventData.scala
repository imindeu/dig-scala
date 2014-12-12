package models

import java.sql.Connection

import anorm._
import anorm.SqlParser._
import anorm.{NotAssigned, Pk}
import play.api.data.Form
import play.api.data._
import Forms._
import dig.AnormExtension._

case class EventData(id: Pk[Long], key: String, value: String)

object EventData {

  private def simple(implicit connection: Connection) =
    get[Pk[Long]]("id")  ~
      get[String]("key") ~
      get[String]("value") map {
      case id ~ key ~ value =>
        EventData(id, key, value)
    }

  def findById(id: Long)(implicit connection: Connection): Option[EventData] = {
    SQL("select * from event_data where id = {id}") on("id" -> id) as simple.singleOpt
  }

  def findByEventId(eventId: Long)(implicit  connection: Connection): List[EventData] = {
    SQL("select * from event_data where event_id = {eventId}").on("eventId" -> eventId).as(simple *)
  }

  def persist(eventId: Long, key: String, value: String)(implicit connection: Connection): Option[Long] = {
    SQL("insert into event_data (key, value, event_id) values ({key}, {value}, {eventId})")
      .on('key->key, 'value->value, 'eventId->eventId).executeInsert()
  }

  def formMapping = mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "key" -> nonEmptyText,
      "value" -> nonEmptyText
    )(EventData.apply)(EventData.unapply)
}
