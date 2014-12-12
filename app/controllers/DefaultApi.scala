package controllers

import play.api.Play.current
import play.api._
import play.api.mvc._
import models._
import play.api.db.DB

/**
 * Created by vassdoki on 12/12/14.
 */
object DefaultApi extends Controller {

  def insert(email:String, keys: List[String], values: List[String]) = Action{
    DB.withConnection { implicit connection =>
      User.findOrCreateByEmail(email).map {
        user => {
          val eventId = Event.persist(user.id.get)
          for (d <- keys zip values) {
            if (d._1.size > 0) {
              EventData.persist(eventId.get, d._1, d._2)
            }
          }
          Ok("Stored")
        }
      }.getOrElse(NotAcceptable)
    }
  }
}
