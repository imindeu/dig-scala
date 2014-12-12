package controllers

import java.sql.Connection

import anorm.{Pk, NotAssigned}
import models.{User, Stat, Event}
import play.api.db.DB
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import scala.concurrent.Future

object Aggregator  extends Controller {
  val LocKey = "loc"
  val ProjectKey = "project"
  val LanguageKey = "language"

  def aggregator(key: String, value: String, user: User, f: String => String)(implicit connection: Connection): Unit = {
    val statOpt = Stat.findByKeyAndUser(key, user)
    val (id, newValue) = statOpt.fold((NotAssigned.asInstanceOf[Pk[Long]], value))(stat => (stat.id, f(stat.value)))
    Stat.update(Stat(id, key, newValue, user))
  }

  def locAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    aggregator(LocKey, value, user, v => (v.toLong + value.toLong).toString)
  }

  def projectAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    aggregator(ProjectKey + value, "1", user, v => (v.toLong + 1L).toString)
  }

  def languageAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    aggregator(LanguageKey + value, "1", user, v => (v.toLong + 1L).toString)
  }

  def start(event: Event)(implicit connection: Connection): Future[Unit] = Future {
    event.eventDatas.map { list =>
      list.foreach { eventData =>
        eventData.key match {
          case LocKey => locAggregator(eventData.value, event.user)
          case ProjectKey => projectAggregator(eventData.value, event.user)
          case LanguageKey => languageAggregator(eventData.value, event.user)
        }
      }
    }
  }

  def index= Action(parse.json) { request =>
    (request.body \ "id").asOpt[Long].map { id =>
      DB.withConnection { implicit connection =>
        val eventOpt = Event.findById(id)
        eventOpt.fold(BadRequest("Error (event not found")){ event =>
          start(event.withEventDataListFromDB)
          Ok("OK")
        }
      }
    }.getOrElse {
      BadRequest("Missing parameter [name]")
    }
  }



}