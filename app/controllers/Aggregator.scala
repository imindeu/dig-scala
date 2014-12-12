package controllers

import controllers.Application._
import play.api.mvc._

/**
 * Created by szotyi on 12/12/14.
 */
object Aggregator  extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}