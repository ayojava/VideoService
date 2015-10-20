package videosvc.test;

import org.junit.*;
import videosvc.controllers.WebApplication;
import play.Logger;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.NOT_IMPLEMENTED;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.route;

public class WebApplicationTest extends WithApplication {

    private static final Logger.ALogger l = Logger.of(WebApplicationTest.class);


    @BeforeClass
    public static void beforeClass() {

        l.info("=======> Running " + WebApplicationTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() {
        l.info("<======= Terminated " + WebApplicationTest.class.getName() + "\n");
    }

    @Before
    public void before() {
    }

    @After
    public void after() {
    }


    @Test
    public void renderTemplate() {

        l.info("---> Testing rendering template videosvc.views.html.index");

        Content html = videosvc.views.html.index.render("Your new application is ready.");
        assertEquals("text/html", contentType(html));
        assertTrue(contentAsString(html).contains("Your new application is ready."));
    }

    @Test
    public void testIndex() {

        l.info("---> Testing Action index()");

        Result result = new WebApplication().index();
        assertEquals(SEE_OTHER, result.status());
    }

    @Test
    public void testRouteToIndex() {

        l.info("---> Testing reverse route of Action index()");

        Result result = route(videosvc.controllers.routes.WebApplication.index());
        assertEquals(SEE_OTHER, result.status());
    }

    @Test
    public void testVideoApp() {

        l.info("---> Testing Action videoApp()");

        Result result = new WebApplication().videoApp();
        assertEquals(NOT_IMPLEMENTED, result.status());
        assertEquals("text/html", result.contentType());
        assertEquals("utf-8", result.charset());
        assertTrue(contentAsString(result).contains("TODO"));
        assertTrue(contentAsString(result).contains("Action not implemented yet"));
    }
}
