package videosvc.test

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Logger
import play.api.test.Helpers._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WebApplicationSpec extends PlaySpecification {

  val l: Logger = Logger(this.getClass)

  "WebApplication" should {

    "send 404 on a bad request" in new WithApplication {

      l.debug("(in Test 1) send 404 on a bad request")

      val ofResult = route(FakeRequest(GET, "/boum"))

      l.debug("headers = [[" + await(ofResult.get).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")


      ofResult must beSome.which (status(_) == NOT_FOUND)
    }

    "redirect to /videoApp" in new WithApplication {

      l.debug("(in Test 2) redirect to /videoApp")

      val fResult = route(FakeRequest(GET, "/")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(SEE_OTHER)
    }

    "render the TODO page" in new WithApplication {

      l.debug("(in Test 3) render the TODO page")

      val fResult = route(FakeRequest(GET, "/videoApp")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(NOT_IMPLEMENTED)
      contentType(fResult) must beSome.which(_ == "text/html")
      contentAsString(fResult) must contain ("TODO")
      contentAsString(fResult) must contain ("Action not implemented yet")
    }
  }
}
