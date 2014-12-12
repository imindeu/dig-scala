package models

import java.sql.Connection

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
    SQL("select * from user where id {id}") on("id" -> id) as simple.singleOpt
  }

}