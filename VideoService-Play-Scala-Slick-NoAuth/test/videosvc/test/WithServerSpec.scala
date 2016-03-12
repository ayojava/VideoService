package videosvc.test

import java.io.{IOException, FileInputStream}
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.apache.commons.io.IOUtils
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.BeforeAfterEach
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.Writeable
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.{WSRequest, WSResponse, WS}
import play.api.Play.current
import play.api.mvc.{AnyContentAsMultipartFormData, MultipartFormData, Results}
import play.api.test.FutureAwaits
import play.api.test.Helpers._
import play.api.test._
import play.api.{Logger, Play, mvc}
import slick.driver.JdbcProfile
import videosvc.controllers.WebService
import videosvc.models.Implicits._
import videosvc.models._
import videosvc.util.{MultipartFormDataWritable, Utils, Utils_MultipartFormdataBody_WithRoutingAndWritable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WithServerSpec extends PlaySpecification with Results with BeforeAfter with BeforeAfterEach {

  import slick.driver.H2Driver.api._

  val l: Logger = Logger(this.getClass)

  val testPort: Int = 3333
  val baseUrl: String = "http://localhost:" + testPort
  val videoUrl: String = baseUrl + "/video"


  override def before: Any = {
  }

  override def after: Any = {
  }


  "WebService running at " + videoUrl should {


    "(in Test 1) satisfy all requirements within one test" in new WithServer(port = testPort) {

      l.debug("---> Testing the App by invoking the server's external WebService interface")

      var videoToAdd: Video = new Video(-1L, "Bob", "Video of Bob 1", 10L, "", "")
      var videoDataFile: String = "testVideos/video1.mp4"
      l.debug("videoToAdd = " + videoToAdd)
      val vBob: Video = testUploadVideo(videoToAdd, videoDataFile)

      testIsVideoInVideoList(videoToAdd)

      testDownloadedEqualsUploaded(vBob, videoDataFile)

      testDataOfNonExistingVideoNotFound()

      testAddRating(vBob.id, 2, 2.0, 1)

      testGetRating(vBob.id, 2.0, 1)

      testAddRating(vBob.id, 5, 5.0, 1)

      testAddRatingForNonExistingVideo(9999L, 4)

      testGetRatingForNonExistingVideo(9999L)

      videoToAdd = new Video(-1L, "Lisa", "Video of Lisa 1", 15L, "", "")
      videoDataFile = "testVideos/video2.mp4"
      l.debug("videoToAdd = " + videoToAdd)
      val vLisa: Video = testUploadVideo(videoToAdd, videoDataFile)
      testIsVideoInVideoList(videoToAdd)

      videoToAdd = new Video(-1L, "Lara", "Video of Lara 1", 20L, "", "")
      videoDataFile = "testVideos/video3.mp4"
      l.debug("videoToAdd = " + videoToAdd)
      val vLara: Video = testUploadVideo(videoToAdd, videoDataFile)
      testIsVideoInVideoList(videoToAdd)

      testGetAll(3)

      testGetById(vLisa)

      testGetByNonExistingId(9999L)

      testDeleteById(vLisa.id)

      testGetAll(2)

      testDeleteByNonExistingId(vLisa.id)
      testDeleteByNonExistingId(9999L)
    }


    "(in Test 2) ----- TERMINATE TEST SEQUENCE ----------" in new WithServer(port = testPort) {
    }
  }


  private def testUploadVideo(v: Video, videoDataFile: String): Video = {

    l.debug("-----> testUploadVideo(" + v + ", " + videoDataFile + ")")

    val response: WSResponse = sendAddVideoRequest(v, videoDataFile)

    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(response.body)
    json.isInstanceOf[JsObject] must beTrue
    val vUploaded: Video = json.as[Video]
    isCorrectVideo(vUploaded, v) must beTrue

    vUploaded
  }

  private def testIsVideoInVideoList(v: Video) {

    l.debug("-----> testIsVideoInVideoList(" + v + ")")

    val response: WSResponse = await(WS.url(baseUrl + "/video").execute(GET)) // (new Timeout(40, TimeUnit.SECONDS))
    response.status must equalTo(OK)
    videoIsInSeq(toVideoSeq(response.body), v) must beTrue
  }

  private def testDownloadedEqualsUploaded(v: Video, videoDataFile: String) {

    l.debug("-----> testDownloadedEqualsUploaded(" + v + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + v.id + "/data").execute(GET) )
    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_ == "video/mp4")

    val retrievedFile: Array[Byte] = response.bodyAsBytes
    val originalFile: Array[Byte] = IOUtils.toByteArray(new FileInputStream(videoDataFile))
    retrievedFile must beEqualTo(originalFile)
  }

  private def testDataOfNonExistingVideoNotFound(): Unit = {

    l.debug("-----> testDataOfNonExistingVideoNotFound()")

    val invalidVideoId = 1000L
    val response: WSResponse = await( WS.url(baseUrl + "/video/" + invalidVideoId + "/data").execute(GET) )
    response.status must equalTo(NOT_FOUND)
  }

  private def testAddRating(videoId: Long, newRating: Int, expectedRating: Double, expectedTotal: Int) {

    l.debug("-----> testAddRating(" + videoId + ", " + newRating + ", " + expectedRating + ", " + expectedTotal + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + videoId + "/rating/" + newRating).withBody("").execute(POST) )

    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(response.body)
    json.isInstanceOf[JsObject] must beTrue
    val avgRating: AverageVideoRating = json.as[AverageVideoRating]

    avgRating.videoId must beEqualTo(videoId)
    avgRating.rating must beEqualTo(expectedRating)
    avgRating.totalRatings must beEqualTo(expectedTotal)
  }

  private def testGetRating(videoId: Long, expectedRating: Double, expectedTotal: Int) {

    l.debug("-----> testGetRating(" + videoId + ", " + expectedRating + ", " + expectedTotal + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + videoId + "/rating").execute(GET) )

    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(response.body)
    json.isInstanceOf[JsObject] must beTrue
    val avgRating: AverageVideoRating = json.as[AverageVideoRating]

    avgRating.videoId must beEqualTo(videoId)
    avgRating.rating must beEqualTo(expectedRating)
    avgRating.totalRatings must beEqualTo(expectedTotal)
  }

  private def testAddRatingForNonExistingVideo(videoId: Long, newRating: Int) {

    l.debug("-----> testAddRatingForNonExistingVideo(" + videoId + ", " + newRating + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + videoId + "/rating/" + newRating).withBody("").execute(POST) )

    response.status must equalTo(NOT_FOUND)
  }

  private def testGetRatingForNonExistingVideo(videoId: Long) {

    l.debug("-----> testGetRatingForNonExistingVideo(" + videoId + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + videoId + "/rating").execute(GET) )

    response.status must equalTo(NOT_FOUND)
  }

  private def testGetAll(expectedCount: Int) {

    l.debug("-----> testGetAll(" + expectedCount + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video").execute(GET) )

    response.status must equalTo(OK)
    toVideoSeq(response.body).length must beEqualTo(expectedCount)
  }

  private def testGetById(compareVideo: Video) {

    l.debug("-----> testGetById(" + compareVideo + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + compareVideo.id).execute(GET) )

    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(response.body)
    json.isInstanceOf[JsObject] must beTrue
    val vRetrieved: Video = json.as[Video]
    isCorrectVideo(vRetrieved, compareVideo) must beTrue
  }

  private def testGetByNonExistingId(id: Long) {

    l.debug("-----> testGetByNonExistingId(" + id + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + id).execute(GET) )

    response.status must equalTo(NOT_FOUND)
  }

  private def testDeleteById(id: Long) {

    l.debug("-----> testDeleteById(" + id + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + id).execute(DELETE) )

    response.status must equalTo(OK)
    response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(response.body)
    json.isInstanceOf[JsBoolean] must beTrue
    val deleted: Boolean = json.as[Boolean]
    deleted must beTrue

    testGetByNonExistingId(id)
  }

  private def testDeleteByNonExistingId(id: Long) {

    l.debug("-----> testDeleteByNonExistingId(" + id + ")")

    val response: WSResponse = await( WS.url(baseUrl + "/video/" + id).execute(DELETE) )

    response.status must equalTo(NOT_FOUND)
  }

  //----- methods ----------

  def videoIsInSeq(videoSeq: Seq[Video], v: Video) = {
    videoSeq.exists(isCorrectVideo(_, v))
  }

  def toVideoSeq(jsonString: String) = {
    val json: JsValue = Json.parse(jsonString)
    json.isInstanceOf[JsArray] must beTrue
    json.as[Seq[Video]]
  }

  private def isCorrectVideo(video: Video, compareVideo: Video): Boolean = {
    l.debug("isCorrectVideo(): video = " + video)
    l.debug("isCorrectVideo(): compareVideo = " + compareVideo)
    video.id > 0 &&
            video.owner != null &&
            video.owner == compareVideo.owner &&
            video.title != null &&
            video.title == compareVideo.title &&
            video.duration == compareVideo.duration &&
            video.contentType != null &&
            video.contentType == "video/mp4" &&
            video.url != null &&
            video.url.matches("^http://.+/video/\\d+/data$")
  }

  def sendAddVideoRequest(videoToAdd: Video, videoDataFile: String) = {

    implicit val multipartFormdataWritable: Writeable[MultipartFormData[Files.TemporaryFile]] = MultipartFormDataWritable.singleton
    implicit val anyContentAsMultipartFormDataWritable: Writeable[AnyContentAsMultipartFormData] = MultipartFormDataWritable.singleton.map(_.mdf)

    val metaData = Json.toJson(videoToAdd).toString()
    l.debug("video metaData = " + metaData)

    val tmpFile: String = Utils.copyToTmpFile(videoDataFile)

    val formData: MultipartFormData[TemporaryFile] =
      Utils_MultipartFormdataBody_WithRoutingAndWritable.multiPartFormData("meta-data", metaData, "data", "video/mp4", tmpFile)
    val anyContent: AnyContentAsMultipartFormData = new AnyContentAsMultipartFormData(formData)

    val request: WSRequest = WS.url(baseUrl + "/video").withBody(anyContent)(anyContentAsMultipartFormDataWritable)
    val response: WSResponse = await(request.execute(POST)) // (new Timeout(40, TimeUnit.SECONDS))

    Utils.removeFileIfExists(tmpFile)

    response
  }
}
