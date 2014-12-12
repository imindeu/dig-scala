package models

import java.sql.Connection

import anorm.SqlParser._
import dig.AnormExtension._
import org.joda.time.DateTime
import anorm._

case class Event(id: Pk[Long], date: DateTime, user: User, eventDatas: Option[List[EventData]] = None) {
  def withEventDataListFromDB(implicit connection: Connection): Event = {
    if (eventDatas.isDefined) {
      this
    } else {
      copy(eventDatas = Some(EventData.findByEventId(id.get)))
    }
  }

}

object Event {
  val DateFormat = "yyyy.MM.dd."

  private def simple(implicit connection: Connection) =
    get[Pk[Long]]("id")  ~
      get[DateTime]("date") ~
      get[Long]("user") map {
      case id ~ date ~ userId =>
        Event(id, date, User.findById(userId).get)
    }

  def findById(id: Long)(implicit connection: Connection): Option[Event] = {
    SQL("select * from event where id {id}") on("id" -> id) as simple.singleOpt
  }

}