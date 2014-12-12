package controllers

import play.api.Play.current
import play.api._
import play.api.mvc._
import models._
import play.api.db.DB
import play.api.libs.ws.WS
import play.api.libs.json.Json
import services.AggregatorService

/**
 * Created by vassdoki on 12/12/14.
 */
object DefaultApi extends Controller {

  def insert(email:String, keys: List[String], values: List[String]) = Action{
    DB.withConnection { implicit connection =>
      User.findOrCreateByEmail(email).map {
        user => {
          Event.persist(user.id.get).map {
            eventId =>
              for (d <- keys zip values) {
                if (d._1.size > 0) {
                  EventData.persist(eventId, d._1, d._2)
                }
              }
              WS.url("http://127.0.0.1:9000/aggregator").post(Json.stringify(Json.obj("id" -> eventId)))
              Ok("Stored")
          }.getOrElse(BadRequest("Event not stored"))
        }
      }.getOrElse(NotAcceptable("User auth error"))
    }
  }

  def indexHypercounter = Action(parse.json) { request =>
    DB.withConnection { implicit connection =>
      User.findOrCreateByEmail("hypercounter.herokuapp.com").map {
        user => {
          Event.persist(user.id.get).map {
            eventId => {
              EventData.persist(eventId, AggregatorService.HyperCounter, (request.body \ "name").toString())
              WS.url("http://127.0.0.1:9000/aggregator").post(Json.stringify(Json.obj("id" -> eventId)))
              Ok("Stored")
            }
          }.getOrElse(BadRequest("Event not stored"))
        }
      }.getOrElse(BadRequest)
    }
  }


}
