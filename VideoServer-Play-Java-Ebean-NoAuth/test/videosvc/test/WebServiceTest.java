package videosvc.test;

import java.io.*;
import java.util.*;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import videosvc.controllers.WebService;
import videosvc.models.AverageVideoRating;
import videosvc.models.Video;
import org.apache.commons.io.IOUtils;
import org.junit.*;

import play.Logger;
import play.api.mvc.*;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.libs.Scala;
import play.mvc.*;
import play.mvc.Result;
import play.test.*;
import scala.Tuple2;
import scala.collection.Seq;
import scala.collection.immutable.Map;
import scala.collection.immutable.HashMap;
import scala.collection.immutable.Nil$;
import videosvc.util.TestUtils;
import videosvc.utils.U;

import static play.test.Helpers.*;
import static org.junit.Assert.*;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class WebServiceTest extends WithApplication {

    private final Logger.ALogger l = Logger.of(getClass());


    @Test
    public void testFindAll() {

        l.debug("---> Testing Action findAll()");

        // insert test videos into DB
        //
        U.insert(new Video("Bob", "Video of Bob 1", 10L));
        U.insert(new Video("Lisa", "Video of Lisa 1", 15L));
        U.insert(new Video("Lara", "Video of Lara 1", 20L));
        U.insert(new Video("Bob", "Video of Bob 2", 30L));

        // invoke App method and get result
        //
        Result result = new WebService().findAll();

        // check result
        //
        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        JsonNode json = Json.parse(contentAsString(result));
        assertTrue(json.isArray());
        Iterator<JsonNode> nodes = json.elements();

        assertTrue(nodes.hasNext());
        Video vBob = Json.fromJson(nodes.next(), Video.class);
        l.debug(vBob.toString());
        assertTrue(vBob.id == 1L);
        assertTrue(vBob.title.equals("Video of Bob 1"));
        assertTrue(vBob.duration == 10L);

        assertTrue(nodes.hasNext());
        Video vLisa = Json.fromJson(nodes.next(), Video.class);
        l.debug(vLisa.toString());
        assertTrue(vLisa.id == 2L);
        assertTrue(vLisa.title.equals("Video of Lisa 1"));
        assertTrue(vLisa.duration == 15L);

        assertTrue(nodes.hasNext());
        Video vLara = Json.fromJson(nodes.next(), Video.class);
        l.debug(vLara.toString());
        assertTrue(vLara.id == 3L);
        assertTrue(vLara.title.equals("Video of Lara 1"));
        assertTrue(vLara.duration == 20L);

        assertTrue(nodes.hasNext());
        Video vBob2 = Json.fromJson(nodes.next(), Video.class);
        l.debug(vBob2.toString());
        assertTrue(vBob2.id == 4L);
        assertTrue(vBob2.title.equals("Video of Bob 2"));
        assertTrue(vBob2.duration == 30L);

        assertFalse(nodes.hasNext());
    }


    @Test
    public void testFindById() {

        l.debug("---> Testing Action findById()");

        // insert test videos into DB
        //
        U.insert(new Video("Bob", "Video of Bob 1", 10L));
        U.insert(new Video("Lisa", "Video of Lisa 1", 15L));
        U.insert(new Video("Lara", "Video of Lara 1", 20L));
        U.insert(new Video("Bob", "Video of Bob 2", 30L));

        // invoke App method and get result
        //
        Long idOfLarasVideo = 3L;
        Result result = new WebService().findById(idOfLarasVideo);

        // check result
        //
        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        Video vLara = TestUtils.parseVideoFromJson(contentAsString(result));

        assertTrue(vLara.id == 3L);
        assertTrue(vLara.title.equals("Video of Lara 1"));
        assertTrue(vLara.duration == 20L);

        Long idNotExisting = 1000L;
        result = new WebService().findById(idNotExisting);
        assertEquals(NOT_FOUND, result.status());
    }


    @Test
    public void testDeleteById() {

        l.debug("---> Testing Action deleteById()");

        // insert test videos into DB
        //
        U.insert(new Video("Bob", "Video of Bob 1", 10L));
        U.insert(new Video("Lisa", "Video of Lisa 1", 15L));
        U.insert(new Video("Lara", "Video of Lara 1", 20L));
        U.insert(new Video("Bob", "Video of Bob 2", 30L));

        // invoke App method and get result
        //
        Long idOfLarasVideo = 3L;
        Result result = new WebService().deleteById(idOfLarasVideo);

        // check result
        //
        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        JsonNode json = Json.parse(contentAsString(result));
        assertTrue(json.isBoolean());

        Boolean deleted = Json.fromJson(json, Boolean.class);
        assertTrue(deleted);

        result = new WebService().findById(idOfLarasVideo);
        assertEquals(NOT_FOUND, result.status());

        result = new WebService().findAll();
        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        json = Json.parse(contentAsString(result));
        assertTrue(json.isArray());
        Iterator<JsonNode> nodes = json.elements();

        assertTrue(nodes.hasNext());
        assertTrue(nodes.next() != null);
        assertTrue(nodes.hasNext());
        assertTrue(nodes.next() != null);
        assertTrue(nodes.hasNext());
        assertTrue(nodes.next() != null);
        assertFalse(nodes.hasNext());

        Long idNotExisting = 1000L;
        result = new WebService().deleteById(idNotExisting);
        assertEquals(NOT_FOUND, result.status());
    }

    /*
---> HTTP POST http://localhost:9000/video
Content-Type: multipart/form-data; boundary=70479a61-0587-47ec-b9fc-9378b74d6910
Content-Length: 358992
--70479a61-0587-47ec-b9fc-9378b74d6910
Content-Disposition: form-data; name="meta-data"
Content-Type: application/json; charset=UTF-8
Content-Length: 105
Content-Transfer-Encoding: binary

{"title":"8dced98a-94e3-4274-aeb1-504e148e8292","duration":123,"contentType":"video/mp4","avgRating":0.0}
--70479a61-0587-47ec-b9fc-9378b74d6910
Content-Disposition: form-data; name="data"; filename="test.mp4"
Content-Type: video/mp4
Content-Length: 358454
Content-Transfer-Encoding: binary

?????????? BINARY DATA ??????????
     */

    @Test
    public void testAddVideo() throws IOException {

        l.debug("---> Testing Actions addVideo() and getVideoData()");

        Video videoToAdd = new Video("Bob", "Video of Bob 1", 10L);
        String videoDataFile = "testVideos/video1.mp4";
        l.debug("videoToAdd = " + videoToAdd);

        // upload video
        //
        Result result = sendAddVideoRequest(videoToAdd, videoDataFile);

        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        Video vBob = TestUtils.parseVideoFromJson(contentAsString(result));
        assertTrue(vBob.id == 1L);
        assertTrue(vBob.title.equals("Video of Bob 1"));
        assertTrue(vBob.duration == 10L);
        assertTrue(vBob.contentType.equals("video/mp4"));

        // download video
        //
        result = new WebService().getVideoData(vBob.id);

        assertEquals(OK, result.status());
        assertEquals("video/mp4", result.contentType());

        byte[] retrievedFile = JavaResultExtractor.getBody(result, 10000L);
        byte[] originalFile = IOUtils.toByteArray(new FileInputStream(videoDataFile));
        assertTrue(Arrays.equals(originalFile, retrievedFile));

        // download non-existing video
        result = new WebService().getVideoData(TestUtils.getInvalidVideoId());

        assertEquals(NOT_FOUND, result.status());
    }

    private Result sendAddVideoRequest(Video videoToAdd, String videoDataFile) {

        String videoAsJsonString = Json.toJson(videoToAdd).toString();
        l.debug("videoAsJsonString = " + videoAsJsonString);

        String separator = TestUtils.separator();

        byte[] rawBody = TestUtils.multiPartRequestBody(separator,
                "meta-data", "application/json", "UTF-8", videoAsJsonString,
                "data", "video/mp4", videoDataFile);

        Http.RequestBuilder requestBuilder =
                fakeRequest(POST, "http://localhost:9000/video")
                .bodyRaw(rawBody)
                .header("Content-Type", "multipart/form-data; boundary=" + separator)
                .header("Content-Length", "" + rawBody.length)
                ;

        return route(requestBuilder);
    }


    /*
    DefaultRequestBody(
        None,
        None,
        None,
        None,
        None,
        Some(

            MultipartFormData(
                Map(meta-data ->
                    List(
                        {"title":"8dced98a-94e3-4274-aeb1-504e148e8292","duration":123,"contentType":"video/mp4","avgRating":0.0}
                    )
                ),
                List(
                    FilePart(
                        data,
                        test.mp4,
                        Some(video/mp4),
                        TemporaryFile(/var/folders/16/by7rpbcx5qg2pl4_zp27vlnw0000gn/T/playtemp5495459936415388044/multipartBody6877319857302893696asTemporaryFile)
                    )
                ),
                List(),
                List()
            )

        )
    )

     */


    @Test
    public void testAddAndGetVideoRating() {

        l.debug("---> Testing Actions addVideoRating() and getVideoRating()");

        Video videoToAdd = new Video("Bob", "Video of Bob 1", 10L);
        l.debug("videoToAdd = " + videoToAdd);

        // add a video
        Result result = sendAddVideoRequest(videoToAdd, "testVideos/video1.mp4");

        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        Video vBob = TestUtils.parseVideoFromJson(contentAsString(result));

        // add a rating for the video (returns the new average rating)
        result = new WebService().addVideoRating(vBob.id, 2);

        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        JsonNode json = Json.parse(contentAsString(result));
        assertTrue(json.isObject());

        AverageVideoRating rating = Json.fromJson(json, AverageVideoRating.class);
        l.debug(rating.toString());

        assertTrue(rating.videoId == vBob.id);
        assertTrue(rating.rating == 2.0);
        assertTrue(rating.totalRatings == 1);

        // get the average rating
        result = new WebService().getVideoRating(vBob.id);

        assertEquals(OK, result.status());
        assertEquals("application/json", result.contentType());

        json = Json.parse(contentAsString(result));
        assertTrue(json.isObject());

        rating = Json.fromJson(json, AverageVideoRating.class);
        l.debug(rating.toString());

        assertTrue(rating.videoId == vBob.id);
        assertTrue(rating.rating == 2.0);
        assertTrue(rating.totalRatings == 1);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    // does not work in Play 2.4
    // @Test
    public void testAddVideo_NOTWORKING() {

        l.debug("---> Testing Action addVideo()");

        Video videoToAdd = new Video("Bob", "Video of Bob 1", 10L);
        l.debug("videoToAdd = " + videoToAdd);

        // create video as Json object tree
        String videoAsJsonString = Json.toJson(videoToAdd).toString();
        l.debug("videoAsJsonString = " + videoAsJsonString);

        // data part map with on entry: key = "meta-data" and value = videoAsJsonString
        Map<String, Seq<String>> dataPartsMap = dataPartsMap("meta-data", videoAsJsonString);

        // file part list with one element: key = "data", content-type = "video/mp4", file = path/to/videoFile
        scala.collection.immutable.List<MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile>> filePartList = filePartListWith("data", "video/mp4", "testVideos/video1.mp4");

        // create MultipartFormData with data part map and file part list
        MultipartFormData<play.api.libs.Files.TemporaryFile> formData = new MultipartFormData<play.api.libs.Files.TemporaryFile>(dataPartsMap, filePartList, null, null);

        // create body content
        AnyContent anyContent = new AnyContentAsMultipartFormData(formData);
        l.debug(anyContent.toString());

        Tuple2 t = new Tuple2("content-type", "multipart/form-data");
        scala.collection.immutable.List l = Nil$.MODULE$.$colon$colon((Tuple2) t);
        Headers headers = new Headers(l);
        play.api.test.FakeRequest fakeRequest = new play.api.test.FakeRequest(POST, "http://localhost:9000/video", headers, anyContent, null, null, 666, null, false);

        Http.Request request = new Http.RequestImpl(fakeRequest);

        // Result result = route(request);

        // assertEquals(OK, result.status());
    }

    private Map<String, Seq<String>> dataPartsMap(String key, String value) {

        l.debug("value = " + value);
        // put the json string into a Scala Seq with one String element
        Seq<String> dataPart = toScalaList(value);
        l.debug(dataPart.toString());

        // create an empty Scala immutable Map
        Map<String, Seq<String>> dataPartsMap = new HashMap<>();

        // add the data part to the map using "meta-data" as key
        return addDataPart(dataPartsMap, "meta-data", dataPart);
    }

    private Map addDataPart(Map<String, Seq<String>> dataPartsMap, String key, Seq<String> dataPart) {
        return dataPartsMap.$plus(new Tuple2(key, dataPart));
    }

    private scala.collection.immutable.List<String> toScalaList(String... values) {
        return scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(values)).toList();
    }

    private scala.collection.immutable.List<MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile>> filePartListWith(String key, String contentType, String file) {

        MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile> part = filePart(key, contentType, file);

        return filePartList(part);
    }

    private scala.collection.immutable.List<MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile>> filePartList(
            MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile>... fileParts) {

        List<MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile>> filePartList = new ArrayList<>();

        for (MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile> filePart : fileParts) {
            filePartList.add(filePart);
        }
        // convert to Scala List
        return scala.collection.JavaConversions.asScalaBuffer(filePartList).toList();
    }

    private MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile> filePart(String key, String contentType, String filePath) {
        return new MultipartFormData.FilePart<>(key, filePath, Scala.Option(contentType), new play.api.libs.Files.TemporaryFile(new File(filePath)));
    }
}
