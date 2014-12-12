package services

import java.sql.Connection
import anorm.{Pk, NotAssigned}
import models.{Event, Stat, User}
import play.api.db.DB
import play.api.libs.json.Json
import play.api.libs.ws.WS
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

object AggregatorService {

  val LocKey = "loc"
  val ProjectKey = "project"
  val LanguageKey = "language"

  def aggregator(key: String, parentKey: Option[String], value: String, user: User, f: String => String)(implicit connection: Connection): Unit = {
      val statOpt = Stat.findByKeyAndUser(key, user)
      val (id, newValue) = statOpt.fold((NotAssigned.asInstanceOf[Pk[Long]], value))(stat => (stat.id, f(stat.value)))
      Stat.update(Stat(id, key, newValue, user)).map(stat => {
        WS.url("/out/sendData").post(Json.stringify(Json.obj("id" -> stat.id.get)))
      })
      if (id == NotAssigned && parentKey.isDefined) {
        aggregator(parentKey.get, None, "1", user, v => (v.toLong + 1).toString)
      }
  }

  def locAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    aggregator(LocKey, None, value, user, v => (v.toLong + value.toLong).toString)
  }

  def projectAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    val key = ProjectKey + "_" + value
    aggregator(key, Some(ProjectKey), "1", user, v => (v.toLong + 1L).toString)
  }

  def languageAggregator(value: String, user: User)(implicit connection: Connection): Unit = {
    val key = LanguageKey + "_" + value
    aggregator(key, Some(LanguageKey), "1", user, v => (v.toLong + 1L).toString)
  }

  def start(event: Event): Unit = {
    Future {
      DB.withConnection { implicit connection =>
          event.withEventDataListFromDB.eventDatas.map {
            list =>
              list.foreach {
                eventData =>
                  eventData.key match {
                    case LocKey => locAggregator(eventData.value, event.user)
                    case ProjectKey => projectAggregator(eventData.value, event.user)
                    case LanguageKey => languageAggregator(eventData.value, event.user)
                  }
              }
          }
      }
    }
  }

}
