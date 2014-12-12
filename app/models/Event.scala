package models

import java.sql.Connection

import anorm.SqlParser._
import anorm.{NotAssigned, Pk}
import dig.AnormExtension._
import org.joda.time.DateTime
import anorm._
import play.api.data._
import Forms._

case class Event(id: Pk[Long], createdAt: DateTime, user: User, eventDatas: Option[List[EventData]] = None) {
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
      get[DateTime]("created_at") ~
      get[Long]("user_id") map {
      case id ~ createdAt ~ userId =>
        Event(id, createdAt, User.findById(userId).get)
    }

  def findById(id: Long)(implicit connection: Connection): Option[Event] = {
    SQL("select * from events where id = {id}") on("id" -> id) as simple.singleOpt
  }

  def persist(userId: Long)(implicit connection: Connection): Option[Long] = {
    SQL("insert into events (created_at, user_id) values (now(), {user})").on('user -> userId).executeInsert()
  }

  def form = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "createdAt" -> jodaDate(pattern = DateFormat),
      "user" -> User.formMapping,
      "eventData" -> optional(list(EventData.formMapping))
    )(Event.apply)(Event.unapply)
  )

}