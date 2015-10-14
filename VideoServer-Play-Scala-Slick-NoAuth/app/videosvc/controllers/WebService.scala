package videosvc.controllers

import java.io.File
import javax.inject.Inject

import play.api.libs.iteratee.Enumerator
import play.api.{Play, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import slick.driver.JdbcProfile
import videosvc.models.Implicits._
import videosvc.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future

// class WebService @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  // val dbConfig = dbConfigProvider.get[JdbcProfile]                            // get db driver via DI

class WebService extends Controller {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)     // get db driver via global lookup

  val l: Logger = Logger(this.getClass())

  import slick.driver.H2Driver.api._

  val db = dbConfig.db

  implicit val videoDataDir: String = "videos"


  // route:   GET     /ping
  def ping = Action {

    l.debug("ping()")

    Ok(Json.toJson(true))
  }


  // route:   GET     /video/
  def findAll = Action.async {

    l.debug("findAll()")

    db.run {
      TableQuery[Videos].result
    }.map(videoSeq => Ok(Json.toJson(videoSeq.toList)))
  }


  // route:   GET     /video/:id
  def findById(id: Long) = Action.async {

    l.debug("findById(id = " + id + ")")

    dbQueryVideoById(id).map { _ match {
        case None => NotFound("Video with id " + id + " not found.")
        case Some(video) => Ok(Json.toJson(video))
      }
    }
  }


  // route:   DELETE  /video/:id
  def deleteById(id: Long) = Action.async {

    l.debug("deleteById(id = " + id + ")")

    db.run{
      TableQuery[Videos].filter(_.id === id).delete
    }.map { nRows =>
      if (nRows < 1)
        NotFound("Video with id " + id + " not found.")
      else
        Ok(Json.toJson(true))
    }
  }

/*
  // route:   GET     /video/:id
  def addVideoMetaData() = Action.async(BodyParsers.parse.json) { request =>

    l.debug("addVideo()")

    val validationResult: JsResult[Video] = request.body.validate[Video]

    validationResult.fold(
      errors => {
        Future {
          BadRequest(Json.obj("message" -> JsError.toJson(errors)))
        }
      },
      video => {
        dbInsert(video).map { v => Ok(Json.toJson(v)) }
      }
    )
  }
*/


  // route:   POST    /video
  def addVideo() = Action.async(BodyParsers.parse.multipartFormData) { request =>

    l.debug("addVideo(): Got multipart formdata")

    val metaDataSeqOpt: Option[Seq[String]] = request.body.dataParts.get("meta-data")

    metaDataSeqOpt match {

      case None =>
        Future {
          BadRequest("No video meta-data sent (missing datapart named \"meta-data\")")
        }

      case Some(metaDataSeq) =>

        val metaData: String = metaDataSeq.head
        l.debug("addVideo(): meta-data: " + metaData)
        val filePartOpt: Option[FilePart[TemporaryFile]] = request.body.file("data")

        filePartOpt match {

          case None =>
            Future {
              BadRequest("No multipart formdata sent (missing filepart named \"data\")")
            }

          case Some(filePart) =>

            val contentType: String = filePart.contentType.get
            val filename: String = filePart.filename
            val ref: TemporaryFile = filePart.ref
            l.debug("addVideo(): Video data has content-type: " + contentType)
            l.debug("addVideo(): Video data saved to temp file: " + filename)

            val json: JsValue = Json.parse(metaData)
            val vNew: Video = json.as[Video]

/*
            // This solution works, but should be avoided
            // as it blocks the current thread with Await.result().
            Future {
              val vStored: Video = Await.result(insert(vNew), Duration.Inf) // now the inserted video has a new generated id
              val vForUpdate: Video = updatedVideo(vStored, contentType, request) // add contentType and url
              val vForUpdate2: Video = moveTmpFileToPermanentLocation(vForUpdate, ref) // update video with added properties in db
              val vUpdated = Await.result(update(vForUpdate2), Duration.Inf)
              Ok(Json.toJson(vUpdated))
            }
*/

            // This solution doesn't block the current thread.
            dbInsert(vNew)
              .map(updatedVideo(_, contentType, request))
              .map(moveTmpFileToPermanentLocation(_, ref))
              .flatMap(dbUpdate(_))
              .map { v =>
                l.debug("addVideo(): new video stored in db: " + v)
                Ok(Json.toJson(v))
              }
        }
    }
  }

  private def updatedVideo(v: Video, contentType: String, request: Request[MultipartFormData[TemporaryFile]]): Video =
    new Video(v.id, v.owner, v.title, v.duration, contentType, urlFor(request, v.id))

  private def urlFor(request: Request[MultipartFormData[TemporaryFile]], id: Long): String =
    "http://" + host(request) + "/video/" + id + "/data"

  private def host(request: Request[MultipartFormData[TemporaryFile]]): String =
    if (request.host == null || request.host.trim.length == 0) "localhost" else request.host

  private def moveTmpFileToPermanentLocation(video: Video, ref: TemporaryFile): Video = {
    createDirIfNotExists(videoDataDir)
    ref.moveTo(new File(video.dataPath), true) // move temp file to permanent location
    video
  }

  private def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }

  private def dbInsert(video: Video): Future[Video] = {
    val videos = TableQuery[Videos]
    for {
      id <- db.run((videos returning videos.map(_.id)) += video)
      vs <- db.run(videos.filter(_.id === id).result).map(_.toList)
    } yield vs.head
  }

  private def dbUpdate(v: Video): Future[Video] = {
    db.run(
      TableQuery[Videos].filter(_.id === v.id).update(v)
        andThen
        TableQuery[Videos].filter(_.id === v.id).result
    ) map { vSeq => vSeq.head }
  }


  // route:   GET     /video/:id/data
  def getVideoData(id: Long) = Action.async {

    l.debug("getVideoData(id = " + id + ")")

    dbQueryVideoById(id).map { videoOpt =>
      sendFileResult(id, videoOpt)
    }
  }

  def sendFileResult(id: Long, videoOption: Option[Video]): Result = {

    videoOption match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        NotFound("Video with id " + id + " doesn't exist.")

      case Some(video) =>

        if (!existsDataFile(video)) {
          l.debug("getVideoData(): No data found for video with id " + id)
          NotFound("No data found for video with id " + id)
        } else {
          /*
                      Result(
                        header = ResponseHeader(200),
                        body = Enumerator.fromFile(new File(video.dataPath))
                      ).withHeaders("Content-Type" -> "video/mp4")
            */
          Ok.sendFile(new File(video.dataPath))
        }
    }
  }

  private def existsDataFile(video: Video) = java.nio.file.Files.exists(new File(video.dataPath).toPath)


  // route:   POST    /video/:id/rating/:stars
  def addVideoRating(id: Long, stars: Int) = Action.async {

    l.debug("addVideoRating(id = " + id + ", stars = " + stars + ")")

    dbQueryVideoById(id).flatMap { videoOpt =>
      doAddVideoRating(id, stars, videoOpt)
    }
  }

  private def doAddVideoRating(id: Long, stars: Int, videoOption: Option[Video]): Future[Result] = {

    videoOption match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        Future {
          NotFound("Video with id " + id + " doesn't exist.")
        }

      case Some(video) =>
        updateVideoRatingById(video.id, video.owner, stars)
          .map { r => Ok(Json.toJson(r)) }
    }
  }

  private def updateVideoRatingById(videoId: Long, user: String, stars: Int): Future[AverageVideoRating] = {

    db.run {

      // try to update existing row
      TableQuery[UserVideoRatings]
        .filter(r => r.videoId === videoId && r.user === user)
        .update(UserVideoRating(videoId, stars, user))

    }.flatMap { nRows =>

        if (nRows > 0)    // no row was updated
          Future(nRows)     // return the number of updated rows
        else        // perform insert if no update was performed
          db.run( TableQuery[UserVideoRatings] += new UserVideoRating(videoId, stars, user) )     // return the number of inserted rows

    }.flatMap { whatever =>
      averageVideoRating(videoId)
    }
  }


  // route:   GET     /video/:id/rating
  def getVideoRating(id: Long) = Action.async {

    l.debug("getVideoRating(id = " + id + ")")

    dbQueryVideoById(id).flatMap { videoOpt =>
      doGetVideoRating(id, videoOpt)
    }
  }

  def dbQueryVideoById(id: Long): Future[Option[Video]] = {
    db.run {
      TableQuery[Videos].filter(_.id === id).result
    }.map {
      videoSeq => videoSeq.toList.headOption
    }
  }

  private def doGetVideoRating(id: Long, videoOpt: Option[Video]): Future[Result] = {

    videoOpt match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        Future {
          NotFound("Video with id " + id + " doesn't exist.")
        }

      case Some(video) =>
        averageVideoRating(video.id)
          .map(r => Ok(Json.toJson(r)))
    }
  }

  private def averageVideoRating(videoId: Long): Future[AverageVideoRating] = {

    for {
      avgRating <- db.run( TableQuery[UserVideoRatings].filter(_.videoId === videoId).map(r => r.rating).avg.result )
      totalRatings <- db.run( TableQuery[UserVideoRatings].filter(_.videoId === videoId).length.result )
    } yield AverageVideoRating(videoId, avgRating.getOrElse(-1.0), totalRatings)
  }
}

