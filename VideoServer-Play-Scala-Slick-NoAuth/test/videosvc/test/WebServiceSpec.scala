package videosvc.test

import java.io.FileInputStream

import org.apache.commons.io.IOUtils
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.BeforeAfterEach
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import play.api.{Logger, Play, mvc}
import slick.driver.JdbcProfile
import videosvc.controllers.WebService
import videosvc.models.Implicits._
import videosvc.models._
import videosvc.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WebServiceSpec extends Specification with BeforeAfter with BeforeAfterEach {

  import slick.driver.H2Driver.api._

  val l: Logger = Logger(this.getClass)

  val routing: Boolean = true


  override def before: Any = {
  }

  override def after: Any = {
  }


  "WebService" should {


    "(in Test 1) succeed to find all Videos available on the server" in new WithApplication {

      l.debug("---> Testing Action findAll()")

      // insert test videos into DB
      //
      val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
      Await.result(db.run(
        TableQuery[Videos] ++= Seq(
          new Video(-1L, "Bob", "Video of Bob 1", 10L, "", ""),
          new Video(-1L, "Lisa", "Video of Lisa 1", 15L, "", ""),
          new Video(-1L, "Lara", "Video of Lara 1", 20L, "", ""),
          new Video(-1L, "Bob", "Video of Bob 2", 30L, "", "")
        )
      ), Duration.Inf)

      // invoke App method and get result
      //
      val request = FakeRequest(GET, "/video")
      val fResult = if (routing) route(request).get else new WebService().findAll.apply(request)

      // check result
      //
      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "application/json")

      val json: JsValue = Json.parse(contentAsString(fResult))
      l.debug("Got JSON: \n" + Json.prettyPrint(json))
      json.isInstanceOf[JsArray] must beTrue
      var vSeq = json.as[Seq[Video]]
      l.debug("Video Sequence: " + vSeq)

      var v: Video = vSeq.head
      vSeq = vSeq.tail
      l.debug("Video 1: " + v)
      v.id must equalTo(1L)
      v.title must equalTo("Video of Bob 1")
      v.duration must equalTo(10L)

      v = vSeq.head
      vSeq = vSeq.tail
      l.debug("Video 2: " + v)
      v.id must equalTo(2L)
      v.title must equalTo("Video of Lisa 1")
      v.duration must equalTo(15L)

      v = vSeq.head
      vSeq = vSeq.tail
      l.debug("Video 3: " + v)
      v.id must equalTo(3L)
      v.title must equalTo("Video of Lara 1")
      v.duration must equalTo(20L)

      v = vSeq.head
      vSeq = vSeq.tail
      l.debug("Video 4: " + v)
      v.id must equalTo(4L)
      v.title must equalTo("Video of Bob 2")
      v.duration must equalTo(30L)

      vSeq.isEmpty must beTrue
    }


    "(in Test 2) succeed to find an existing video by it's id" in new WithApplication {

      l.debug("---> Testing Action findById() for existing video")

      // insert test videos into DB
      //
      val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
      Await.result(db.run(
        TableQuery[Videos] ++= Seq(
          new Video(-1L, "Bob", "Video of Bob 1", 10L, "", ""),
          new Video(-1L, "Lisa", "Video of Lisa 1", 15L, "", ""),
          new Video(-1L, "Lara", "Video of Lara 1", 20L, "", ""),
          new Video(-1L, "Bob", "Video of Bob 2", 30L, "", "")
        )
      ), Duration.Inf)

      // invoke App method and get result
      //
      val idOfLarasVideo = 3L
      val request = FakeRequest(GET, "/video/" + idOfLarasVideo)
      val fResult = if (routing) route(request).get else new WebService().findById(idOfLarasVideo).apply(request)

      // check result
      //
      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "application/json")

      val json: JsValue = Json.parse(contentAsString(fResult))
      l.debug("Got JSON: \n" + Json.prettyPrint(json))
      json.isInstanceOf[JsObject] must beTrue
      val vLara = json.as[Video]
      l.debug("Video: " + vLara)

      l.debug("Video 3: " + vLara)
      vLara.id must equalTo(3L)
      vLara.title must equalTo("Video of Lara 1")
      vLara.duration must equalTo(20L)
    }


    "(in Test 3) fail finding a non-existing video by it's id" in new WithApplication {

      l.debug("---> Testing Action findById() for non-existing video")

      val invalidVideoId = 1000L
      val request = FakeRequest(GET, "/video/" + invalidVideoId)
      val fResult = if (routing) route(request).get else new WebService().findById(invalidVideoId).apply(request)
      status(fResult) must equalTo(NOT_FOUND)
    }


    "(in Test 4) succeed to delete an existing video by it's id" in new WithApplication {

      l.debug("---> Testing Action deleteById() for existing video")

      // insert test videos into DB
      //
      val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
      Await.result(db.run(
        TableQuery[Videos] ++= Seq(
          new Video(-1L, "Bob", "Video of Bob 1", 10L, "", ""),
          new Video(-1L, "Lisa", "Video of Lisa 1", 15L, "", ""),
          new Video(-1L, "Lara", "Video of Lara 1", 20L, "", ""),
          new Video(-1L, "Bob", "Video of Bob 2", 30L, "", "")
        )
      ), Duration.Inf)

      // invoke App method and get result
      //
      val idOfLarasVideo = 3L
      val request = FakeRequest(DELETE, "/video/" + idOfLarasVideo)
      val fResult = if (routing) route(request).get else new WebService().deleteById(idOfLarasVideo).apply(request)

      // check result
      //
      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "application/json")

      val json: JsValue = Json.parse(contentAsString(fResult))
      l.debug("Got JSON: \n" + Json.prettyPrint(json))
      json.isInstanceOf[JsBoolean] must beTrue

      val request2 = FakeRequest(GET, "/video/" + idOfLarasVideo)
      val fResult2 = if (routing) route(request2).get else new WebService().findById(idOfLarasVideo).apply(request2)
      status(fResult2) must equalTo(NOT_FOUND)

      val request3 = FakeRequest(GET, "/video")
      val fResult3 = if (routing) route(request3).get else new WebService().findAll.apply(request3)
      status(fResult3) must equalTo(OK)
      contentType(fResult3) must beSome.which(_ == "application/json")

      val json3: JsValue = Json.parse(contentAsString(fResult3))
      json3.isInstanceOf[JsArray] must beTrue
      val vSeq = json3.as[Seq[Video]]
      l.debug("Video Sequence: " + vSeq)
      vSeq.length must beEqualTo(3)
      vSeq.tail.tail.tail.isEmpty must beTrue
    }


    "(in Test 5) fail deleting a non-existing video by it's id" in new WithApplication {

      l.debug("---> Testing Action deleteById() for non-existing video")

      val invalidVideoId = 1000L
      val request = FakeRequest(DELETE, "/video/" + invalidVideoId)
      val fResult = if (routing) route(request).get else new WebService().deleteById(invalidVideoId).apply(request)
      status(fResult) must equalTo(NOT_FOUND)
    }


//    "add a new Video (only meta-data) to the server's store" in new WithApplication {
//
//      l.debug("---> Testing Actions addVideoMetaData()")
//
//      val videoToAdd = new Video(-1L, "Bob", "Video of Bob 1", 10L, "", "")
//      val videoDataFile = "testVideos/video1.mp4"
//      l.debug("videoToAdd = " + videoToAdd)
//
//      // upload video
//      //
//      val Some(fResult): Option[Future[mvc.Result]] = route(FakeRequest(POST, "/video").withJsonBody(Json.toJson(videoToAdd)))
//
//      // check result
//      //
//      status(fResult) must equalTo(OK)
//      contentType(fResult) must beSome.which(_ == "application/json")
//
//      val json: JsValue = Json.parse(contentAsString(fResult))
//      l.debug("Got JSON: \n" + Json.prettyPrint(json))
//      json.isInstanceOf[JsObject] must beTrue
//      val vBob = json.as[Video]
//      l.debug("Video: " + vBob)
//
//      l.debug("Video 3: " + vBob)
//      vBob.id must equalTo(1L)
//      vBob.title must equalTo("Video of Bob 1")
//      vBob.duration must equalTo(10L)
//    }


    "(in Test 6) succeed to add a new Video (meta-data and data) to the server's store" in new WithApplication {

      l.debug("---> Testing Actions addVideo() and getVideoData()")

      val videoToAdd = new Video(-1L, "Bob", "Video of Bob 1", 10L, "", "")
      val videoDataFile = "testVideos/video1.mp4"
      l.debug("videoToAdd = " + videoToAdd)

      // upload video
      //
      val fResult: Future[mvc.Result] = sendAddVideoRequest(videoToAdd, videoDataFile)

      // check result
      //
      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "application/json")

      val json: JsValue = Json.parse(contentAsString(fResult))
      l.debug("Got JSON: \n" + Json.prettyPrint(json))
      json.isInstanceOf[JsObject] must beTrue
      val vBob = json.as[Video]
      l.debug(vBob.toString)

      vBob.id must equalTo(1L)
      vBob.title must equalTo("Video of Bob 1")
      vBob.duration must equalTo(10L)

      // download video
      //
      val request2 = FakeRequest(GET, "/video/" + vBob.id + "/data")
      val fResult2 = if (routing) route(request2).get else new WebService().getVideoData(vBob.id).apply(request2)

      // check result
      //
      status(fResult2) must equalTo(OK)
      contentType(fResult2) must beSome.which(_ == "video/mp4")

      val receivedBodyAsBytes: Array[Byte] = bodyAsBytes(fResult2)
      val originalFile: Array[Byte] = IOUtils.toByteArray(new FileInputStream(videoDataFile))
      receivedBodyAsBytes must beEqualTo(originalFile)
    }
  }


  "(in Test 7) fail downloading a non-existing video by it's id" in new WithApplication {

    l.debug("---> Testing Action getVideoData() for non-existing video")

    val invalidVideoId = 1000L
    val request = FakeRequest(GET, "/video/" + invalidVideoId + "/data")
    val fResult = if (routing) route(request).get else new WebService().getVideoData(invalidVideoId).apply(request)
    status(fResult) must equalTo(NOT_FOUND)
  }


  "(in Test 8) succeed to add and retieve video ratings" in new WithApplication {

    l.debug("---> Testing Actions addVideoRating() and getVideoRating() for existing video")

    val videoToAdd = new Video(-1L, "Bob", "Video of Bob 1", 10L, "", "")
    val videoDataFile = "testVideos/video1.mp4"
    l.debug("videoToAdd = " + videoToAdd)

    // upload video
    val fResult: Future[mvc.Result] = sendAddVideoRequest(videoToAdd, videoDataFile)

    status(fResult) must equalTo(OK)
    contentType(fResult) must beSome.which(_ == "application/json")

    val json: JsValue = Json.parse(contentAsString(fResult))
    l.debug("Got JSON: \n" + Json.prettyPrint(json))
    json.isInstanceOf[JsObject] must beTrue
    val vBob = json.as[Video]
    l.debug(vBob.toString)

    // add video rating with 2 stars
    //
    val stars2: Int = 2
    val request2 = FakeRequest(POST, "/video/" + vBob.id + "/rating/" + stars2)
    val fResult2 = if (routing) route(request2).get else new WebService().addVideoRating(vBob.id, stars2).apply(request2)

    // check result
    //
    status(fResult2) must equalTo(OK)
    contentType(fResult2) must beSome.which(_ == "application/json")

    val json2: JsValue = Json.parse(contentAsString(fResult2))
    l.debug("Got JSON: \n" + Json.prettyPrint(json2))
    json2.isInstanceOf[JsObject] must beTrue
    val avgRating2 = json2.as[AverageVideoRating]
    l.debug(avgRating2.toString)

    avgRating2.videoId must equalTo(1L)
    avgRating2.rating must equalTo(stars2.toDouble)
    avgRating2.totalRatings must equalTo(1)

    // add video rating with 5 stars for the same video
    //
    val stars3: Int = 5
    val request3 = FakeRequest(POST, "/video/" + vBob.id + "/rating/" + stars3)
    val fResult3 = if (routing) route(request3).get else new WebService().addVideoRating(vBob.id, stars3).apply(request3)

    // check result
    //
    status(fResult3) must equalTo(OK)
    contentType(fResult3) must beSome.which(_ == "application/json")

    val json3: JsValue = Json.parse(contentAsString(fResult3))
    l.debug("Got JSON: \n" + Json.prettyPrint(json3))
    json3.isInstanceOf[JsObject] must beTrue
    val avgRating3 = json3.as[AverageVideoRating]
    l.debug(avgRating3.toString)

    avgRating3.videoId must equalTo(1L)
    avgRating3.rating must equalTo(stars3.toDouble)
    avgRating3.totalRatings must equalTo(1)

    // get video rating must yield a rating with 5 stars
    //
    val request4 = FakeRequest(GET, "/video/" + vBob.id + "/rating")
    val fResult4 = if (routing) route(request4).get else new WebService().getVideoRating(vBob.id).apply(request4)

    // check result
    //
    status(fResult4) must equalTo(OK)
    contentType(fResult4) must beSome.which(_ == "application/json")

    val json4: JsValue = Json.parse(contentAsString(fResult4))
    l.debug("Got JSON: \n" + Json.prettyPrint(json4))
    json4.isInstanceOf[JsObject] must beTrue
    val avgRating4 = json4.as[AverageVideoRating]
    l.debug(avgRating4.toString)

    avgRating4.videoId must equalTo(1L)
    avgRating4.rating must equalTo(stars3.toDouble)
    avgRating4.totalRatings must equalTo(1)
  }


  "(in Test 9) fail to add a rating for a non-existing video by it's id" in new WithApplication {

    l.debug("---> Testing Action addVideoRating() for non-existing video")

    val invalidVideoId = 1000L
    val stars = 2
    val request = FakeRequest(POST, "/video/" + invalidVideoId + "/rating/" + stars)
    val fResult = if (routing) route(request).get else new WebService().addVideoRating(invalidVideoId, stars).apply(request)
    status(fResult) must equalTo(NOT_FOUND)
  }


  "(in Test 10) fail to retrieve the rating for a non-existing video by it's id" in new WithApplication {

    l.debug("---> Testing Action getVideoRating() for non-existing video")

    val invalidVideoId = 1000L
    val request = FakeRequest(GET, "/video/" + invalidVideoId + "/rating")
    val fResult = if (routing) route(request).get else new WebService().getVideoRating(invalidVideoId).apply(request)
    status(fResult) must equalTo(NOT_FOUND)
  }


  "(in Test 11) ----- TERMINATE TEST SEQUENCE ----------" in new WithApplication {
  }


  //----- methods ----------

  def sendAddVideoRequest(videoToAdd: Video, videoDataFile: String) = {

    // 1. This version directly produces the body as a byte array
    // and passes the request with the raw body to the route() function.
    //
    // Utils_MultipartFormdataAsRawBody.sendAddVideoRequest(videoToAdd, videoDataFile)

    // 2. This version avoids serializing the form data to a byte array.
    // It directly passes the MultipartFormdata structure to the application and circumvents routing.
    //
    // Utils_MultipartFormdataBody_WithoutRouting.sendAddVideoRequest(videoToAdd, videoDataFile)

    // 3. This version constucts a Writeable[AnyContentAsMultipartFormData] which is capable to serialize
    // the MultipartFormdata structure to a byte array. The Writable is passed
    // as an implicit parameter to the route() function.
    //
    Utils_MultipartFormdataBody_WithRoutingAndWritable.sendAddVideoRequest(videoToAdd, videoDataFile)
  }

  def bodyAsBytes(fResult: Future[mvc.Result]): Array[Byte] = {
    val result: mvc.Result = Await.result(fResult, Duration.Inf)
    val bodyEnumerator: Enumerator[Array[Byte]] = result.body
    val futureOfBodyAsBytes: Future[Array[Byte]] = bodyEnumerator.run(Iteratee.fold(Array.empty[Byte]) {
      (memo, nextChunk) => memo ++ nextChunk
    })
    Await.result(futureOfBodyAsBytes, Duration.Inf)
  }
}
