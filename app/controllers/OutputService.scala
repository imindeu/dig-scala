package controllers

import anorm.Id
import models.{User, Stat}
import play.api._
import play.api.cache.Cache
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

/**
 * Created by pairg on 2014.12.12..
 */
object OutputService extends Controller {

  def sendData = Action {
    val user1 = User(Id(1), "test@example.org")
    val stat1 = Stat(Id(1), "test_key1", "-2", user1)
    sendMessage(stat1)
    val user2 = User(Id(2), "test@example.org")
    val stat2 = Stat(Id(2), "test_key2", "10", user2)
    sendMessage(stat2)
    Ok("OK")
  }

  def test = Action {
    Ok(views.html.websocket())
  }

  protected def sendMessage(stat: Stat) = {
    val msg = Json.stringify(stat.toJson)
    val channel = Cache.getAs[(Int, Enumerator[String], Channel[String])]("channel_"+stat.user.id.get)
    if(channel.isDefined) channel.get._3.push(msg)
  }

  def websocket(userId: Long) = WebSocket.using[String]{ request =>
    val cacheKey = "channel_"+userId
    val out = {
      val currentChannel = Cache.getAs[(Int, Enumerator[String], Channel[String])](cacheKey)
      if(currentChannel.isDefined) currentChannel.get._2
      else{
        val newChannel = Concurrent.broadcast[String]
        Cache.set(cacheKey, (1, newChannel._1, newChannel._2))
        newChannel._1
      }
    }
    val in = Iteratee.consume[String]().map { _ =>
      val oldChannel = Cache.getAs[(Int, Enumerator[String], Channel[String])](cacheKey)
      if(oldChannel.isDefined)
      {
        if(oldChannel.get._1 > 1)
        {
          Cache.set(cacheKey, (oldChannel.get._1 - 1, oldChannel.get._2, oldChannel.get._3))
        }else{
          oldChannel.get._3.eofAndEnd()
          Cache.remove(cacheKey)
        }
      }
    }
    (in, out)
  }

}
