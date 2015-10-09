package videosvc.util

import java.io.File
import java.nio.file.StandardCopyOption

import play.api.http.HeaderNames
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.{Logger, mvc}
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{MissingFilePart, BadPart, FilePart}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import videosvc.controllers.WebService
import videosvc.models.Video
import videosvc.models.Implicits._

import scala.concurrent.Future

object Utils_MultipartFormdataBody_WithoutRouting {

  val l: Logger = Logger(this.getClass)

  def sendAddVideoRequest(videoToAdd: Video, videoDataFile: String): Future[mvc.Result] = {

    val metaData = Json.toJson(videoToAdd).toString
    l.debug("videoAsJsonString = " + metaData)

    val tmpFile: String = Utils.copyToTmpFile(videoDataFile)

    val formData: MultipartFormData[TemporaryFile] =
      multiPartFormData("meta-data", metaData, "data", "video/mp4", tmpFile)

    val request = FakeRequest(POST,
      "/video",
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "multipart/form-data")),
      body = formData)

    new WebService().addVideo.apply(request)
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
