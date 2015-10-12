package videosvc

import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play, Application, GlobalSettings}
import play.api.mvc.{Handler, Result, RequestHeader}
import videosvc.models._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import slick.driver.JdbcProfile

case object Global extends GlobalSettings {

  import slick.driver.H2Driver.api._

  val l: Logger = Logger(this.getClass())

  override def beforeStart(app: Application): Unit = {
    l.debug("===> WebService starting up ...")
  }

  override def onStart(app: Application): Unit = {
    l.debug("===> WebService has started")
    l.debug("===> Creating Database Schema ...")
    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
    Await.result(db.run(
      (TableQuery[Videos].schema ++ TableQuery[UserVideoRatings].schema).create
    ), Duration.Inf)
    l.debug("===> Database Schema created")
  }

  override def onStop(app: Application): Unit = {
    l.debug("===> Dropping Database Schema ...")
    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
    Await.result(db.run(
      (TableQuery[Videos].schema ++ TableQuery[UserVideoRatings].schema).drop
    ), Duration.Inf)
    l.debug("<=== Database Schema dropped")
    l.debug("<=== WebService shutting down ...\n")
  }

  override def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {
    super.onRequestReceived(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    super.onError(request, ex)
  }
}

