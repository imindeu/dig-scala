package controllers

import play.api.mvc._

object Aggregator  extends Controller {

  def index(id: Long) = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}