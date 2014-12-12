package controllers

import models.Event
import play.api.db.DB
import play.api.mvc._
import play.api.Play.current
import services.AggregatorService

object Aggregator  extends Controller {

  def index= Action(parse.json) { request =>
    (request.body \ "id").asOpt[Long].map { id =>
      DB.withConnection { implicit connection =>
        val eventOpt = Event.findById(id)
        eventOpt.fold(BadRequest("Error (event not found")){ event =>
          AggregatorService.start(event)
          Ok("OK")
        }
      }
    }.getOrElse {
      BadRequest("Missing parameter [id]")
    }
  }



}