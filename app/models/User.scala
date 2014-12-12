package models

import java.sql.Connection
import anorm.{NotAssigned, Pk}
import dig.AnormExtension._
import play.api.data._
import Forms._

import anorm._
import anorm.SqlParser._

case class User (id: Pk[Long], email: String)

object User {
  private def simple =
    get[Pk[Long]]("id")  ~
      get[String]("email") map {
      case id ~ email =>
        User(id, email)
    }

  def findById(id: Long)(implicit connection: Connection): Option[User] = {
    SQL("select * from users where id = {id}") on("id" -> id) as simple.singleOpt
  }

  def findOrCreateByEmail(email: String)(implicit connection: Connection): Option[User] = {
    val user = SQL("select * from users where email = {email}") on("email" -> email) as simple.singleOpt
    if (user.isDefined) {
      user
    } else {
      findById(createUser(email).get)
    }
  }

  def createUser(email:String)(implicit connection: Connection):Option[Long] = {
    if (email.size > 0) {
      SQL("insert into users (email)values({email})").on('email -> email).executeInsert()
    } else {
      None
    }
  }

  def formMapping = mapping(
    "id"  -> ignored(NotAssigned: Pk[Long]),
    "email" -> nonEmptyText
  )(User.apply)(User.unapply)

}