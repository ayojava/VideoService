package videosvc.controllers

import play.api.mvc._

class WebApplication extends Controller {

  def index = Action {
    Redirect(routes.WebApplication.videoApp())
  }

  def videoApp = TODO

//  def app = Action {
//    Ok(videosvc.views.html.index("Your new application is ready."))
//  }

}
