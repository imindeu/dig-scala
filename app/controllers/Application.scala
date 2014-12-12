package controllers

import play.api._
import play.api.mvc._
import models._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def apiTest = Action {
    Ok(views.html.apiTestPage())
  }


}