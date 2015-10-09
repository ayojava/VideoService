package videosvc.controllers

import play.api.mvc._

class WebApplication extends Controller {

  def index = Action {
    Ok(videosvc.views.html.index("Your new application is ready."))
  }

}
