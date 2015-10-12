package videosvc.test;

import com.fasterxml.jackson.databind.JsonNode;
import videosvc.controllers.WebService;
import videosvc.models.AverageVideoRating;
import videosvc.models.Video;
import org.apache.commons.io.IOUtils;
import play.Logger;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.test.WithServer;
import play.mvc.*;
import play.mvc.Result;
import static play.mvc.Http.Status.*;

import org.junit.*;
import videosvc.util.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class ServerTest extends WithServer {

    private static final Logger.ALogger l = Logger.of(ServerTest.class);


    @BeforeClass
    public static void beforeClass() {

        l.info("=======> Running " + ServerTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() {
        l.info("<======= Terminated " + ServerTest.class.getName() + "\n");
    }

    @Before
    public void before() {
    }

    @After
    public void after() {
    }


    private static int TIMEOUT = 5000;

    private String baseUrl() {
        return "http://localhost:" + this.testServer.port();
    }

    private WSResponse GET(String path) {
        String url = baseUrl() + path;
        return WS.url(url).get().get(TIMEOUT);
    }

    private WSResponse POST(String path, String body) {
        String url = baseUrl() + path;
        return WS.url(url).post(body).get(TIMEOUT);
    }

    private WSResponse POST(String path, JsonNode body) {
        String url = baseUrl() + path;
        return WS.url(url).post(body).get(TIMEOUT);
    }

    private WSResponse POST(String path, InputStream body) {
        String url = baseUrl() + path;
        return WS.url(url).post(body).get(TIMEOUT);
    }

    private WSResponse DELETE(String path) {
        String url = baseUrl() + path;
        return WS.url(url).delete().get(TIMEOUT);
    }

    // method throws play.core.server.netty.PlayDefaultUpstreamHandler - Exception caught in Netty
    // java.lang.IllegalArgumentException: empty text
    // problem probably solved in Netty 4.x
    // @Test
    public void testAddVideo_NOTWORKING() {

        l.info("---> Testing Action addVideo()");

        Video videoToAdd = new Video("Bob", "Video of Bob 1", 10L);
        l.debug("videoToAdd = " + videoToAdd);

        String videoAsJsonString = Json.toJson(videoToAdd).toString();
        l.debug("videoAsJsonString = " + videoAsJsonString);

        String separator = TestUtils.separator();

        byte[] rawBody = TestUtils.multiPartRequestBody(separator,
                "meta-data", "application/json", "UTF-8", videoAsJsonString,
                "data", "video/mp4", "testVideos/video1.mp4");

        String url = baseUrl() + "/video";
        WSRequest request = WS.url(url)
                //.setBody(new ByteArrayInputStream(rawBody))
                .setHeader("Content-Type", "multipart/form-data; boundary=" + separator)
                .setHeader("Content-Length", "" + rawBody.length)
                ;

        WSResponse response = request.post(new ByteArrayInputStream(rawBody)).get(TIMEOUT);

        assertEquals(OK, response.getStatus());
    }

    @Test
    public void testAll() throws IOException {

        l.info("---> Testing all Actions ...");

        Video videoToAdd = new Video("Bob", "Video of Bob 1", 10L);
        String videoDataFile = "testVideos/video1.mp4";
        l.debug("videoToAdd = " + videoToAdd);

        Video vBob = testUploadVideo(videoToAdd, videoDataFile);
        testIsVideoInVideoList(videoToAdd);

        testDownloadedEqualsUploaded(vBob, videoDataFile);

        testNonExistingVideoNotFound();

        testAddRating(vBob.id, 2, 2.0, 1);

        testGetRating(vBob.id, 2.0, 1);

        testAddRating(vBob.id, 5, 5.0, 1);

        testAddRatingForNonExistingVideo(TestUtils.getInvalidVideoId(), 4);

        testGetRatingForNonExistingVideo(TestUtils.getInvalidVideoId());

        videoToAdd = new Video("Lisa", "Video of Lisa 1", 15L);
        videoDataFile = "testVideos/video2.mp4";
        l.debug("videoToAdd = " + videoToAdd);

        Video vLisa = testUploadVideo(videoToAdd, videoDataFile);
        testIsVideoInVideoList(videoToAdd);

        videoToAdd = new Video("Lara", "Video of Lara 1", 20L);
        videoDataFile = "testVideos/video3.mp4";
        l.debug("videoToAdd = " + videoToAdd);

        Video vLara = testUploadVideo(videoToAdd, videoDataFile);
        testIsVideoInVideoList(videoToAdd);

        testGetAll(3);

        testGetById(vLisa);

        testGetByNonExistingId(TestUtils.getInvalidVideoId());

        testDeleteById(vLisa.id);

        testGetAll(2);

        testDeleteByNonExistingId(vLisa.id);
        testDeleteByNonExistingId(TestUtils.getInvalidVideoId());
    }

    private Video testUploadVideo(Video v, String videoDataFile) {

        l.info("-----> testUploadVideo(" + v + ", " + videoDataFile + ")");

        Result result = sendAddVideoRequest(v, videoDataFile);

        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        Video vUploaded = TestUtils.parseVideoFromJson(contentAsString(result));

        assertTrue(isCorrectVideo(vUploaded, v));

        return vUploaded;
    }

    private void testDownloadedEqualsUploaded(Video v, String videoDataFile) throws IOException {

        l.info("-----> testDownloadedEqualsUploaded(" + v + ")");

        Result result = new WebService().getVideoData(v.id);

        assertEquals(OK, result.status());
        assertEquals("video/mp4", result.contentType());

        byte[] retrievedFile = JavaResultExtractor.getBody(result, 10000L);
        byte[] originalFile = IOUtils.toByteArray(new FileInputStream(videoDataFile));
        assertTrue(Arrays.equals(originalFile, retrievedFile));
    }

    private void testNonExistingVideoNotFound() {

        l.info("-----> testDataOfNonExistingVideoNotFound()");

        Result result = new WebService().getVideoData(TestUtils.getInvalidVideoId());

        assertEquals(NOT_FOUND, result.status());
    }

    private void testIsVideoInVideoList(Video v) {

        l.info("-----> testIsVideoInVideoList(" + v + ")");

        WSResponse response = GET("/video");
        assertEquals(OK, response.getStatus());

        assertTrue(videoIsInList(toVideoList(response.asJson()), v));
    }

    private void testAddRating(Long videoId, int newRating, double expectedRating, int expectedTotal) {

        l.info("-----> testAddRating(" + videoId + ", " + newRating + ", " + expectedRating + ", " + expectedTotal + ")");

        WSResponse response = POST("/video/" + videoId + "/rating/" + newRating, "");

        assertEquals(OK, response.getStatus());
        assertTrue(response.getHeader("Content-Type").startsWith("application/json"));

        JsonNode json = response.asJson();
        assertTrue(json.isObject());

        AverageVideoRating rating = Json.fromJson(json, AverageVideoRating.class);
        l.debug(rating.toString());

        assertTrue(rating.videoId == videoId);
        assertTrue(rating.rating == expectedRating);
        assertTrue(rating.totalRatings == expectedTotal);
    }

    private void testAddRatingForNonExistingVideo(Long videoId, int newRating) {

        l.info("-----> testAddRatingForNonExistingVideo(" + videoId + ", " + newRating + ")");

        WSResponse response = POST("/video/" + videoId + "/rating/" + newRating, "");

        assertEquals(NOT_FOUND, response.getStatus());
    }

    private void testGetRating(Long videoId, double expectedRating, int expectedTotal) {

        l.info("-----> testGetRating(" + videoId + ", " + expectedRating + ", " + expectedTotal + ")");

        WSResponse response = GET("/video/" + videoId + "/rating");

        assertEquals(OK, response.getStatus());
        assertTrue(response.getHeader("Content-Type").startsWith("application/json"));

        JsonNode json = response.asJson();
        assertTrue(json.isObject());

        AverageVideoRating rating = Json.fromJson(json, AverageVideoRating.class);
        l.debug(rating.toString());

        assertTrue(rating.videoId == videoId);
        assertTrue(rating.rating == expectedRating);
        assertTrue(rating.totalRatings == expectedTotal);
    }

    private void testGetRatingForNonExistingVideo(Long videoId) {

        l.info("-----> testGetRatingForNonExistingVideo(" + videoId + ")");

        WSResponse response = POST("/video/" + videoId + "/rating", "");

        assertEquals(NOT_FOUND, response.getStatus());
    }

    private void testGetAll(int expectedCount) {

        l.info("-----> testGetAll(" + expectedCount + ")");

        WSResponse response = GET("/video");
        assertEquals(OK, response.getStatus());

        assertEquals(expectedCount, toVideoList(response.asJson()).size());
    }

    private void testGetById(Video compareVideo) {

        l.info("-----> testGetById(" + compareVideo + ")");

        WSResponse response = GET("/video/" + compareVideo.id);
        assertEquals(OK, response.getStatus());
        assertTrue(response.getHeader("Content-Type").startsWith("application/json"));

        Video found = Json.fromJson(response.asJson(), Video.class);
        assertTrue(isCorrectVideo(found, compareVideo));
    }

    private void testGetByNonExistingId(Long id) {

        l.info("-----> testGetByNonExistingId(" + id + ")");

        WSResponse response = GET("/video/" + id);
        assertEquals(NOT_FOUND, response.getStatus());
    }

    private void testDeleteById(Long id) {

        l.info("-----> testDeleteById(" + id + ")");

        WSResponse response = DELETE("/video/" + id);
        assertEquals(OK, response.getStatus());
        assertTrue(response.getHeader("Content-Type").startsWith("application/json"));

        Boolean deleted = Json.fromJson(response.asJson(), Boolean.class);
        assertTrue(deleted);

        testGetByNonExistingId(id);
    }

    private void testDeleteByNonExistingId(Long id) {

        l.info("-----> testDeleteByNonExistingId(" + id + ")");

        WSResponse response = DELETE("/video/" + id);
        assertEquals(NOT_FOUND, response.getStatus());
    }



    private Result sendAddVideoRequest(Video videoToAdd, String videoDataFile) {

        String videoAsJsonString = Json.toJson(videoToAdd).toString();
        l.debug("videoAsJsonString = " + videoAsJsonString);

        String boundary = TestUtils.separator();

        byte[] rawBody = TestUtils.multiPartRequestBody(boundary,
                "meta-data", "application/json", "UTF-8", videoAsJsonString,
                "data", "video/mp4", videoDataFile);

        Http.RequestBuilder requestBuilder =
                fakeRequest(POST, "http://localhost:9000/video")
                        .bodyRaw(rawBody)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Content-Length", "" + rawBody.length)
                ;

        return route(requestBuilder);
    }

    private List<Video> toVideoList(JsonNode json) {

        List<Video> videos = new ArrayList<>();

        assertTrue(json.isArray());
        Iterator<JsonNode> nodes = json.elements();

        while (nodes.hasNext()) {
            videos.add(Json.fromJson(nodes.next(), Video.class));
        }

        return videos;
    }

    private boolean videoIsInList(final Collection<Video> videoList, final Video video) {
        return videoList.stream().filter(v -> isCorrectVideo(v, video)).count() > 0;
    }

    private boolean isCorrectVideo(Video video, Video compareVideo) {
        l.debug("isCorrectVideo(): video = " + video);
        l.debug("isCorrectVideo(): compareVideo = " + compareVideo);
        return video.id != null
                && video.id > 0
                && video.title.equals(compareVideo.title)
                && video.duration == compareVideo.duration
                && video.contentType.equals("video/mp4");
    }
}
