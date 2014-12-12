package controllers

import java.sql.Connection

import models.Event
import play.api.db.DB
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

object Aggregator  extends Controller {

  def start(event: Event)(implicit connection: Connection): Future[Unit] = Future {
    event.eventDatas.map { list =>
      list.foreach { eventData =>
      }
    }
  }

  def index(id: Long) = Action {
    DB.withConnection { connection =>
      val eventOpt = Event.findById(id)
      eventOpt.fold(Ok("Error (event not found")){ event =>
        start(event.withEventDataListFromDB)
        Ok("OK")
      }
    }
  }



}