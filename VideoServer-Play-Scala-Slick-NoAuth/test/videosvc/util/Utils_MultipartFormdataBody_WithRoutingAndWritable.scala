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
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import videosvc.controllers.WebService
import videosvc.models.Video
import videosvc.models.Implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/*
  The code for the Writeable[AnyContentAsMultipartFormData] comes from:
  http://tech.fongmun.com/post/125479939452/test-multipartformdata-in-play
 */
object MultipartFormDataWritable {

  val boundary = "--------ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

  def formatDataParts(data: Map[String, Seq[String]]) = {
    val dataParts = data.flatMap { case (key, values) =>
      values.map { value =>
        val name = s""""$key""""
        s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name\r\n\r\n$value\r\n"
      }
    }.mkString("")
    Codec.utf_8.encode(dataParts)
  }

  def filePartHeader(file: FilePart[TemporaryFile]) = {
    val name = s""""${file.key}""""
    val filename = s""""${file.filename}""""
    val contentType = file.contentType.map { ct =>
      s"${HeaderNames.CONTENT_TYPE}: $ct\r\n"
    }.getOrElse("")
    Codec.utf_8.encode(s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name; filename=$filename\r\n$contentType\r\n")
  }

  val singleton = Writeable[MultipartFormData[TemporaryFile]](
    transform = { form: MultipartFormData[TemporaryFile] =>
      formatDataParts(form.dataParts) ++
        form.files.flatMap { file =>
          val fileBytes = java.nio.file.Files.readAllBytes(Paths.get(file.ref.file.getAbsolutePath))
          filePartHeader(file) ++ fileBytes ++ Codec.utf_8.encode("\r\n")
        } ++
        Codec.utf_8.encode(s"--$boundary--")
    },
    contentType = Some(s"multipart/form-data; boundary=$boundary")
  )
}

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
    optFutResult.get
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
