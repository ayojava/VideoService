package videosvc.util

import java.io.File
import java.nio.file.{Paths, StandardCopyOption}

import play.api.http.{Writeable, HeaderNames}
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.{Logger, mvc}
import play.api.mvc.{Result, AnyContentAsMultipartFormData, Codec, MultipartFormData}
import play.api.mvc.MultipartFormData.{MissingFilePart, BadPart, FilePart}
import play.api.test.{FakeHeaders, FakeRequest, FutureAwaits}
import play.api.test.Helpers._
import videosvc.controllers.WebService
import videosvc.models.Video
import videosvc.models.Implicits._

import scala.concurrent.{Await, Future}

object Utils_MultipartFormdataBody_WithRoutingAndWritable {

  val l: Logger = Logger(this.getClass)

  implicit val anyContentAsMultipartFormWritable: Writeable[AnyContentAsMultipartFormData] = MultipartFormDataWritable.singleton.map(_.mdf)

  def sendAddVideoRequest(videoToAdd: Video, videoDataFile: String): Future[mvc.Result] = {

    val metaData = Json.toJson(videoToAdd).toString
    l.debug("videoAsJsonString = " + metaData)

    val tmpFile: String = Utils.copyToTmpFile(videoDataFile)

    val formData: MultipartFormData[TemporaryFile] =
      multiPartFormData("meta-data", metaData, "data", "video/mp4", tmpFile)

    val request: FakeRequest[AnyContentAsMultipartFormData] = FakeRequest(POST, "/video").withMultipartFormDataBody(formData)

    // route(request)(anyContentAsMultipartFormWritable).get

    val optFutResult: Option[Future[Result]] = route(request) // (anyContentAsMultipartFormWritable) <-- implicit
    val futureResult: Future[Result] = optFutResult.get

    val result: Result = await(futureResult)
    Utils.removeFileIfExists(tmpFile)

    futureResult
  }

  def multiPartFormData(dataPartKey: String, dataPartString: String,
                        filePartKey: String, filePartContentType: String, tmpFilename: String): MultipartFormData[TemporaryFile] = {

    val dataParts: Map[String, Seq[String]] = Map(dataPartKey -> Seq(dataPartString))
    val files: Seq[FilePart[TemporaryFile]] = Seq(
      MultipartFormData.FilePart(filePartKey, tmpFilename, Some("Content-Type: " + filePartContentType), Files.TemporaryFile(new File(tmpFilename)))
    )
    val badParts: Seq[BadPart] = Seq()
    val missingFileParts: Seq[MissingFilePart] = Seq()

    new MultipartFormData(dataParts, files, badParts, missingFileParts)
  }
}
