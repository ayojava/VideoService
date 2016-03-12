package videosvc.test

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Logger
import play.api.test.Helpers._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class WebApplicationSpec extends PlaySpecification {

  val l: Logger = Logger(this.getClass)

  "WebApplication" should {


    "(in Test 1) send 404 on a bad request" in new WithApplication {

      l.debug("send 404 on a bad request")

      val ofResult = route(FakeRequest(GET, "/boum"))

      l.debug("headers = [[" + await(ofResult.get).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      ofResult must beSome.which (status(_) == NOT_FOUND)
    }


    "(in Test 2) redirect to /videoApp" in new WithApplication {

      l.debug("redirect to /videoApp")

      val fResult = route(FakeRequest(GET, "/")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(SEE_OTHER)
    }

    "(in Test 3) render the TODO page" in new WithApplication {

      l.debug("render the TODO page")

      val fResult = route(FakeRequest(GET, "/videoApp")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(NOT_IMPLEMENTED)
      contentType(fResult) must beSome.which(_ == "text/html")
      charset(fResult) must beSome.which(_ == "utf-8")
      contentAsString(fResult) must contain ("TODO")
      contentAsString(fResult) must contain ("Action not implemented yet")
    }
  }
}
