package models

import anorm.Pk
import play.api.data.Form
import models.AnormExtension._
import play.api.data.Forms._

/**
 * Created by vassdoki on 12/12/14.
 */
case class EventData(id: Pk[Long], event: Long, key: String, value: String) {

}

object EventData {
  def form = Form(
    mapping(
      "id" -> primaryKey(longNumber),
      "event" -> longNumber,
      "key" -> text,
      "value" -> text
    )(EventData.apply)(EventData.unapply)
  )


}
