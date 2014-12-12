package models

import java.sql.Connection

import anorm._
import anorm.SqlParser._
import org.joda.time.DateTime
import play.api.libs.json._

case class Stat(id: Pk[Long], key: String, value: String, user: User){
  def toJson = Json.obj("key" -> JsString(key), "value" -> JsString(value))
}

object Stat {

  private def simple(implicit connection: Connection) =
    get[Pk[Long]]("id")  ~
      get[String]("key") ~
      get[String]("value") ~
      get[Long]("user_id") map {
      case id ~ key ~ value ~ userId =>
        Stat(id, key, value, User.findById(userId).get)
    }

  def findByKeyAndUser(key: String, user: User)(implicit connection: Connection): Option[Stat] = {
    SQL("select * from stats where key = {key}") on("key" -> key) as simple.singleOpt
  }

  def findById(id: Long)(implicit connection: Connection): Option[Stat] = {
    SQL("select * from stats where id = {id}") on("id" -> id) as simple.singleOpt
  }

  def update(stat: Stat)(implicit connection: Connection): Option[Stat] = {
    stat.id match {
      case Id(id) => {
        SQL("update stats set key = {key}, value = {value}, user_id = {userId} where id = {id}").on(
          'key -> stat.key,
          'value -> stat.value,
          'userId -> stat.user.id,
          'id -> stat.id
        )
        Some(stat)
      }
      case NotAssigned => {
        val id:Option[Long] = SQL("insert into stats (user_id, key, value) values ({userId}, {key}, {value})").on(
          'userId -> stat.user.id,
          'key -> stat.key,
          'value -> stat.value
        ).executeInsert()
        id.map(id => Some(stat.copy(id = Id(id)))).getOrElse(None)
      }
    }
  }

}