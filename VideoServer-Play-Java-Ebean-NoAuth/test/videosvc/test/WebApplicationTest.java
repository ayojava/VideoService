package videosvc.test;

import videosvc.controllers.WebApplication;
import org.junit.Test;
import play.Logger;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.route;

public class WebApplicationTest extends WithApplication {

    private final Logger.ALogger l = Logger.of(getClass());


    @Test
    public void renderTemplate() {

        l.debug("---> Testing rendering template videosvc.views.html.index");

        Content html = videosvc.views.html.index.render("Your new application is ready.");
        assertEquals("text/html", contentType(html));
        assertTrue(contentAsString(html).contains("Your new application is ready."));
    }

    @Test
    public void testIndex() {

        l.debug("---> Testing Action index()");

        Result result = new WebApplication().index();
        assertEquals(OK, result.status());
        assertEquals("text/html", result.contentType());
        assertEquals("utf-8", result.charset());
        assertTrue(contentAsString(result).contains("Welcome"));
    }

    @Test
    public void testCallIndex() {

        l.debug("---> Testing reverse route of Action index()");

        Result result = route(videosvc.controllers.routes.WebApplication.index());
        assertEquals(OK, result.status());
    }

}
