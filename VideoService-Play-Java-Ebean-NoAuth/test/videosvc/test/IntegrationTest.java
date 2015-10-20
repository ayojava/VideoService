package videosvc.test;

import org.junit.*;

import play.Logger;
import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class IntegrationTest {

    private static final Logger.ALogger l = Logger.of(IntegrationTest.class);

    private static int serverPort = 3332;


    @BeforeClass
    public static void beforeClass() {

        l.info("=======> Running " + IntegrationTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() {
        l.info("<======= Terminated " + IntegrationTest.class.getName() + "\n");
    }

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    /**
     * add your integration test here
     * in this example we just check if the welcome page is being shown
     */
    @Test
    public void test() {

        l.info("---> test()");

        running(testServer(serverPort, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {

            public void invoke(TestBrowser browser) {

                browser.goTo("http://localhost:" + serverPort);

                assertTrue(browser.pageSource().contains("TODO"));
                assertTrue(browser.pageSource().contains("Action not implemented yet"));
            }
        });
    }

}
