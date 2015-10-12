package videosvc.controllers

import java.io.File
import javax.inject.Inject

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
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// class WebService @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {
  // val dbConfig = dbConfigProvider.get[JdbcProfile]                            // get db driver via DI

class WebService extends Controller {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)     // get db driver via global lookup

  val l: Logger = Logger(this.getClass())

  import slick.driver.H2Driver.api._

  val db = dbConfig.db

  val videos = TableQuery[Videos]
  val userVideoRatings = TableQuery[UserVideoRatings]

  implicit val videoDataDir: String = "videos"


  def ping = Action {
    l.debug("ping()")
    Ok(Json.toJson(true))
  }

  def findAll = Action.async {
    l.debug("findAll()")
    val dbAction = videos.result
    db.run(dbAction).map(videoSeq => Ok(Json.toJson(videoSeq.toList)))
  }

  def findById(id: Long) = Action.async {
    l.debug("findById(id = " + id + ")")
    fVideoOptById(id).map(toResult(_, id))
  }

  private def fVideoOptById(id: Long): Future[Option[Video]] = {
    val dbAction = videos.filter(_.id === id).result
    db.run(dbAction).map(videoSeq => videoSeq.toList.headOption)
  }

  private def videoOptById(id: Long): Option[Video] = Await.result(fVideoOptById(id), Duration.Inf)

  private def videoById(id: Long): Video = videoOptById(id).get

  private def toResult(vOpt: Option[Video], id: Long): Result = {
    vOpt match {
      case Some(video) => Ok(Json.toJson(video))
      case None => NotFound("Video with id " + id + " not found.")
    }
  }

  def deleteById(id: Long) = Action.async {
    l.debug("deleteById(id = " + id + ")")
    val dbAction = TableQuery[Videos].filter(_.id === id).delete
    db.run(dbAction).map { nRows =>
      if (nRows > 0)
        Ok(Json.toJson(true))
      else
        NotFound("Video with id " + id + " not found.")
    }
  }

  def addVideoMetaData = Action.async(BodyParsers.parse.json) { request =>

    l.debug("addVideo()")

    val validationResult: JsResult[Video] = request.body.validate[Video]

    validationResult.fold(
      errors => {
        Future {
          BadRequest(Json.obj("message" -> JsError.toJson(errors)))
        }
      },
      video => {
        insert(video).map { v => Ok(Json.toJson(v)) }
      }
    )
  }

  def addVideo = Action.async(BodyParsers.parse.multipartFormData) { request =>

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
            l.debug("addVideo(): Video data has content-type: " + contentType)
            l.debug("addVideo(): Video data saved to temp file: " + filePart.filename)

            val json: JsValue = Json.parse(metaData)
            val vNew: Video = json.as[Video]

            Future {
              val vStored: Video = Await.result(insert(vNew), Duration.Inf) // now the inserted video has a new generated id
              val vForUpdate: Video = newVideo(vStored, contentType, request) // add contentType and url
              createDirIfNotExists(videoDataDir)
              filePart.ref.moveTo(new File(vForUpdate.dataPath), true) // move temp file to permanent location
              val vUpdated = Await.result(update(vForUpdate), Duration.Inf) // update video with added properties in db
              l.debug("addVideo(): new video stored in db: " + vUpdated)
              Ok(Json.toJson(vUpdated))
            }
        }
    }
  }

  private def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }

  private def newVideo(v: Video, contentType: String, request: Request[MultipartFormData[TemporaryFile]]): Video =
    new Video(v.id, v.owner, v.title, v.duration, contentType, urlFor(request, v.id))

  private def urlFor(request: Request[MultipartFormData[TemporaryFile]], id: Long): String =
    "http://" + host(request) + "/video/" + id + "/data"

  private def host(request: Request[MultipartFormData[TemporaryFile]]): String =
    if (request.host == null || request.host.trim.length == 0) "localhost" else request.host

  private def update(v: Video): Future[Video] = {
    db.run(
      TableQuery[Videos].filter(_.id === v.id).update(v)
        andThen
        TableQuery[Videos].filter(_.id === v.id).result
    ) map (_.head)
  }

  private def insert(video: Video) = insert_4(video)

  private def insert_1(video: Video): Future[Video] = {

    val id: Long = Await.result(
      db.run(
        (videos returning videos.map(_.id)) += video),
      Duration.Inf
    )
    val dbAction = {
      videos.filter(_.id === id).result
    }
    db.run(dbAction).map(_.toList.head)
  }

  private def insert_2(video: Video): Future[Video] = {

    val fId: Future[Long] = db.run(
      (videos returning videos.map(_.id)) += video
    )
    val fVideo: Future[Video] = fId.flatMap { id =>
      val dbAction = videos.filter(_.id === id)
      db.run(dbAction.result).map(_.toList.head)
    }
    fVideo
  }

  private def insert_3(v: Video): Future[Video] = {
    db.run(
      (videos returning videos.map(_.id)) += v
    ).flatMap { id =>
      db.run(videos.filter(_.id === id).result).map(_.toList.head)
    }
  }

  private def insert_4(v: Video): Future[Video] = {
    for {
      id <- db.run((videos returning videos.map(_.id)) += v)
      vs <- db.run(videos.filter(_.id === id).result).map(_.toList)
    } yield vs.head
  }

  def getVideoData(id: Long) = Action {

    l.debug("getVideoData(id = " + id + ")")

    videoOptById(id) match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        NotFound("Video with id " + id + " doesn't exist.")

      case Some(video) =>

        if (!existsDataFile(video)) {
          l.debug("getVideoData(): No data found for video with id " + id)
          NotFound("No data found for video with id " + id)
        } else {
          //          Result(
          //            header = ResponseHeader(200),
          //            body = Enumerator.fromFile(new File(video.dataPath))
          //          ).withHeaders("Content-Type" -> "video/mp4")
          Ok.sendFile(new File(video.dataPath))
          // .withHeaders("Content-Type" -> "video/mp4")
        }
    }
  }

  private def existsDataFile(video: Video) = java.nio.file.Files.exists(new File(video.dataPath).toPath)

  def addVideoRating(id: Long, stars: Int) = Action {

    l.debug("addVideoRating(id = " + id + ", stars = " + stars + ")")

    videoOptById(id) match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        NotFound("Video with id " + id + " doesn't exist.")

      case Some(video) =>
        val avgRating: AverageVideoRating = updateVideoRatingById(video.id, video.owner, stars)
        Ok(Json.toJson(avgRating))
    }
  }

  private def updateVideoRatingById(videoId: Long, user: String, stars: Int): AverageVideoRating = {

    val uvr = new UserVideoRating(videoId, stars, user)

    // try to update existing row
    val nRows = Await.result(db.run(
      TableQuery[UserVideoRatings].filter(r => r.videoId === videoId && r.user === user).update(uvr)
    ), Duration.Inf)

    // if no row for update exists, insert new row
    if (nRows < 1) {
      Await.result(db.run(
        TableQuery[UserVideoRatings] += new UserVideoRating(videoId, stars, user)
      ), Duration.Inf)   }

    averageVideoRating(videoId)
  }

  def getVideoRating(id: Long) = Action {

    l.debug("getVideoRating(id = " + id + ")")

    videoOptById(id) match {

      case None =>
        l.debug("getVideoData(): Video with id " + id + " doesn't exist.")
        NotFound("Video with id " + id + " doesn't exist.")

      case Some(video) =>
        val avgRating: AverageVideoRating = averageVideoRating(video.id)
        Ok(Json.toJson(avgRating))
    }
  }

  private def averageVideoRating(videoId: Long): AverageVideoRating = {
    val avgRating = Await.result(db.run(
      TableQuery[UserVideoRatings].filter(_.videoId === videoId).map(r => r.rating).avg.result
    ), Duration.Inf).get

    val totalRatings = Await.result(db.run(
      TableQuery[UserVideoRatings].filter(_.videoId === videoId).length.result
    ), Duration.Inf)

    new AverageVideoRating(videoId, avgRating, totalRatings)
  }
}

