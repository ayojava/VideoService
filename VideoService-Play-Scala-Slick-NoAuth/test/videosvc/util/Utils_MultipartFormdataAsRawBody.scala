package videosvc.util

import java.io._
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.UUID

import play.api.libs.json.Json
import play.api.{Logger, mvc}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import videosvc.models.Video
import videosvc.models.Implicits._

import scala.concurrent.Future

private class HttpBodyStream {

  private var bodyStream: ByteArrayOutputStream = new ByteArrayOutputStream

  def writeBinary(data: Array[Byte]): HttpBodyStream = {
    try {
      bodyStream.write(data)
      bodyStream.flush
      this
    }
    catch {
      case e: IOException => {
        e.printStackTrace
        throw new RuntimeException(e)
      }
    }
  }

  def getBytes: Array[Byte] = {
    try {
      bodyStream.close
      bodyStream.toByteArray
    }
    catch {
      case e: IOException => {
        e.printStackTrace
        throw new RuntimeException(e)
      }
    }
  }
}

object Utils_MultipartFormdataAsRawBody {

  val l: Logger = Logger(this.getClass)

  private val LINEBREAK: String = "\r\n"

  def separator(): String = UUID.randomUUID().toString()

  def sendAddVideoRequest(videoToAdd: Video, videoDataFile: String): Future[mvc.Result] = {

    val metaData = Json.toJson(videoToAdd).toString()
    l.debug("videoAsJsonString = " + metaData)

    val boundary = separator()

    val rawBody: Array[Byte] = multiPartRequestBody(boundary,
                                "meta-data", "application/json", "UTF-8", metaData,
                                "data", "video/mp4", videoDataFile)

    val request = FakeRequest(POST, "/video")
      .withRawBody(rawBody)
      .withHeaders(
        ("Content-Type", "multipart/form-data; boundary=" + boundary),
        ("Content-Length", "" + rawBody.length)
      )

    // if (routing) route(request).get else new WebService().addVideo.apply(request)
    route(request).get
  }

  def multiPartRequestBody(separator: String,
                           dataPartKey: String, dataPartContentType: String, charSet: String, dataPartString: String,
                           filePartKey: String, filePartContentType: String, dataFilename: String): Array[Byte] = {

    val dataPartBody: Array[Byte] = createDataPartBody(dataPartString, charSet)

    val dataPartHeader: String = createDataPartHeader(separator, dataPartKey, dataPartContentType, charSet, dataPartBody.length)

    val filePartBody: Array[Byte] = createFilePartBody(dataFilename)

    val filePartHeader: String = createFilePartHeader(separator, filePartKey, filePartContentType, dataFilename, filePartBody.length)

    new HttpBodyStream()
      .writeBinary(toByteArray(dataPartHeader, charSet))
      .writeBinary(dataPartBody)
      .writeBinary(toByteArray(filePartHeader, charSet))
      .writeBinary(filePartBody)
      .getBytes
  }

  private def createDataPartBody(content: String, charSet: String): Array[Byte] = {

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream
    val writer: Writer = new OutputStreamWriter(baos, charSet)
    try {
      writer.append(content)
      writer.close
      baos.toByteArray
    }
    catch {
      case e: IOException => {
        e.printStackTrace
        throw new RuntimeException(e)
      }
    } finally {
      if (writer != null) writer.close
    }
  }

  private def createDataPartHeader(separator: String, key: String, contentType: String, charSet: String, contentLength: Long): String = {

    val header: String =
      new StringBuilder("--")
        .append(separator + LINEBREAK)
        .append("Content-Disposition: form-data; name=\"" + key + "\"" + LINEBREAK)
        .append("Content-Type: " + contentType + "; charset=" + charSet + LINEBREAK)
        .append("Content-Length: " + contentLength + LINEBREAK)
        .append("Content-Transfer-Encoding: binary" + LINEBREAK)
        .append(LINEBREAK)
        .toString

    // l.debug("dataPartHeader():\n----->>\n" + header + "<<-----")
    header
  }

  private def createFilePartBody(src: String): Array[Byte] = {

    try {
      val out: ByteArrayOutputStream = new ByteArrayOutputStream
      Files.copy(new File(src).toPath, out)
      out.toByteArray
    }
    catch {
      case e: IOException => {
        e.printStackTrace
        throw new RuntimeException(e)
      }
    }
  }

  private def createFilePartHeader(separator: String, key: String, contentType: String, filename: String, contentLength: Long): String = {

    val header: String =
      new StringBuilder(LINEBREAK + "--")
        .append(separator + LINEBREAK)
        .append("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filename + "\"" + LINEBREAK)
        .append("Content-Type: " + contentType + LINEBREAK)
        .append("Content-Length: " + contentLength + LINEBREAK)
        .append("Content-Transfer-Encoding: binary" + LINEBREAK)
        .append(LINEBREAK)
        .toString

    // l.debug("filePartHeader():\n----->>\n" + header + "<<-----")
    header
  }

  private def toByteArray(text: String, charSet: String): Array[Byte] = text.getBytes(Charset.forName(charSet))
}
