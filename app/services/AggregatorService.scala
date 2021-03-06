package services

import java.sql.Connection
import anorm._
import models._
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
  val HyperCounter = "hyperCounterName"
  val HyperTaleCounter = "hyperTailCounter"
  val HyperTaleConstant = "tales-from-the-cloud/upload_tale"
  val AddedFiles = "added_files"
  val RemovedFiles = "removed_files"
  val ModifiedFiles = "modified_files"



  def aggregator(key: String, parentKey: Option[String], value: String, user: User, f: String => String)(implicit connection: Connection): Unit = {
    println("key: " + key + "; parentKey: " + parentKey + "value: " + value + "; user: " + user)
    val statOpt = Stat.findByKeyAndUser(key, user)
    val (id, newValue) = statOpt.fold((NotAssigned.asInstanceOf[Pk[Long]], value))(stat => (stat.id, f(stat.value)))
    Stat.update(Stat(id, key, newValue, user)).map(stat => {
      WS.url("https://dig-scala.herokuapp.com/out/sendData").withHeaders(("Content-Type", "text/json")).post(Json.stringify(Json.obj("id" -> stat.id.get)))
    })
    if (id == NotAssigned && parentKey.isDefined) {
      incrementAggregator(parentKey.get, None, user)
    } else if (parentKey.isDefined) {
      Stat.findByKeyAndUser(parentKey.get, user).map(parentStat => WS.url("https://dig-scala.herokuapp.com/out/sendData").withHeaders(("Content-Type", "text/json")).post(Json.stringify(Json.obj("id" -> parentStat.id.get))))
    }
  }

  def addAggregator(key: String, value: String, user: User)(implicit connection: Connection): Unit = {
    aggregator(key, None, value, user, v => (v.toLong + value.toLong).toString)
  }

  def incrementAggregator(key: String, parentKey: Option[String], user: User)(implicit connection: Connection): Unit = {
    aggregator(key, parentKey, "1", user, v => (v.toLong + 1L).toString)
  }

  def start(event: Event): Unit = {
    Future {
      DB.withConnection { implicit connection =>
          event.withEventDataListFromDB.eventDatas.map {
            list =>
              list.foreach {
                eventData =>
                  eventData.key match {
                    case LocKey => addAggregator(LocKey, eventData.value, event.user)
                    case ProjectKey => incrementAggregator(ProjectKey + "_" + eventData.value, Some(ProjectKey), event.user)
                    case LanguageKey => incrementAggregator(LanguageKey + "_" + eventData.value, Some(LanguageKey), event.user)
                    case HyperCounter =>
                      if (eventData.value == HyperTaleConstant) {
                        incrementAggregator(HyperTaleCounter, None, event.user)
                      } else {
                        incrementAggregator(HyperCounter, None, event.user)
                        incrementAggregator(HyperCounter + "_" + eventData.value, None, event.user)
                      }
                    case AddedFiles => addAggregator(AddedFiles, eventData.value, event.user)
                    case RemovedFiles => addAggregator(RemovedFiles, eventData.value, event.user)
                    case ModifiedFiles => addAggregator(ModifiedFiles, eventData.value, event.user)
                  }
              }
          }
      }
    }
  }

}
