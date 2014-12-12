package models

import dig.AnormExtension._
import org.joda.time.DateTime
import play.api.data._
import Forms._
import anorm.Pk

case class Event(id: Pk[Long], date: DateTime, user: User, eventData: List[EventData]) {

}

object Event {
  val DateFormat = "yyyy.MM.dd."

//  def form = Form(
//    mapping(
//      "id" -> primaryKey(longNumber),
//      "date" -> jodaDate(pattern = DateFormat),
//      "user" -> longNumber,
//      "eventData" -> List[EventData.form]
//    )(Event.apply)(Event.unapply)
//  )
}