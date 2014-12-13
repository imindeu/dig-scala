package controllers

import java.sql.Connection

import anorm.Id
import models.{User, Stat}
import play.api._
import play.api.cache.Cache
import play.api.db.DB
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

import scala.concurrent.Future

/**
 * Created by pairg on 2014.12.12..
 */
object OutputService extends Controller {

  def sendData = Action.async { request: Request[AnyContent] =>
    DB.withConnection { implicit connection =>
      val statId: Option[Long] = {
        val postData = request.body.asJson
        if(postData.isDefined) postData.get.\("id").asOpt[Long]
        else None
      }
      println("statId: " + statId)
      val result = sendMessage(statId)
      Future.successful(Ok(Json.obj("error" -> JsBoolean(!result))))
    }
  }

  def test(email: String) = Action {
    Ok(views.html.websocket(email))
  }

  protected def sendMessage(statId: Option[Long])(implicit connection: Connection): Boolean = {
    if(statId.isDefined) {
      val stat = Stat.findById(statId.get)
      if (stat.isDefined) {
        val msg = Json.stringify(stat.get.toJson)
        val channel = Cache.getAs[(Int, Enumerator[String], Channel[String])]("channel_" + stat.get.user.email)
        if (channel.isDefined) channel.get._3.push(msg)
        true
      } else false
    }else false
  }

  def websocket(userEmail: String) = WebSocket.async[String]{ request =>
    Future {
      val cacheKey = "channel_" + userEmail
      val out = {
        val currentChannel = Cache.getAs[(Int, Enumerator[String], Channel[String])](cacheKey)
        if (currentChannel.isDefined) currentChannel.get._2
        else {
          val newChannel = Concurrent.broadcast[String]
          Cache.set(cacheKey, (1, newChannel._1, newChannel._2))
          newChannel._1
        }
      }
      val in = Iteratee.consume[String]().map { _ =>
        val oldChannel = Cache.getAs[(Int, Enumerator[String], Channel[String])](cacheKey)
        if (oldChannel.isDefined) {
          if (oldChannel.get._1 > 1) {
            Cache.set(cacheKey, (oldChannel.get._1 - 1, oldChannel.get._2, oldChannel.get._3))
          } else {
            oldChannel.get._3.eofAndEnd()
            Cache.remove(cacheKey)
          }
        }
      }
      (in, out)
    }
  }

}
