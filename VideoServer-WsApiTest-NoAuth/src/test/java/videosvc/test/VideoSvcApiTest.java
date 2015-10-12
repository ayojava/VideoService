package videosvc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import videosvc.wsapi.Video;

import videosvc.wsapi.VideoSvcApi;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

public class VideoSvcApiTest {

	private static final String SERVER = "http://localhost:9000";

	private File testVideoData = new File("src/test/resources/video1.mp4");

    private Video video =
            Video.create()
                    .withOwner("Bob")
                    .withTitle(UUID.randomUUID().toString())
                    .withDuration(123)
                    .build();

	private static VideoSvcApi videoSvc = new RestAdapter.Builder()
			.setEndpoint(SERVER)
            .setLogLevel(RestAdapter.LogLevel.NONE)
            .build()
			.create(VideoSvcApi.class);

    @Before
    public void setUp() {
        System.out.println("----- setUp() ----------");
    }

    @After
    public void tearDown() {
        System.out.println("----- tearDown() ----------");
    }

    @BeforeClass
    public static void setUpClass() {
        System.out.println("----- setUpClass() ----------");
        try {
            Boolean ok = videoSvc.ping();
            if (!ok) {
                System.err.println("VideoServer not available on " + SERVER);
                System.exit(1);
            }
            System.out.println("VideoServer is running on " + SERVER);
        } catch (Exception e) {
            System.err.println("VideoServer not available on " + SERVER +
                    ", got Exception: (" + e.getClass().getName() + ") with message: " + e.getMessage());;
            System.exit(1);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        deleteVideos();
        System.out.println("----- tearDownClass() ---------\n");
    }

//    @Test
//	public void testAddVideoMetadata() throws Exception {
//		Video received = videoSvc.addVideoMetaData(video);
//        // System.out.println("video = " + video.toString());
//        // System.out.println("received = " + received.toString());
//		assert(isCorrectVideo(received, video));
//	}
	
//	@Test
//	public void testAddGetVideo() throws Exception {
//		Video received = videoSvc.addVideoMetaData(video);
//        assert(videoIsInList(videoSvc.getVideoList(), video));
//	}
	
//	@Test
//	public void testAddVideoData() throws Exception {
//		Video received = videoSvc.addVideoMetaData(video);
//		Boolean status = videoSvc1.uploadVideoData(received.getId(),
//                new TypedFile(received.getContentType(), testVideoData));
//		assertTrue(status);
//
//		Response response = videoSvc1.downloadVideoData(received.getId());
//		assertEquals(200, response.getStatus());
//
//		InputStream videoData = response.getBody().in();
//		byte[] originalFile = IOUtils.toByteArray(new FileInputStream(testVideoData));
//		byte[] retrievedFile = IOUtils.toByteArray(videoData);
//		assertTrue(Arrays.equals(originalFile, retrievedFile));
//	}

    @Test
    public void testAddVideo() throws Exception {

        System.out.println("----- testAddVideo() ----------");

        /*
        TypedString typedString = new TypedString("some string");
        TypedByteArray typedByteArray = new TypedByteArray("mime/type", "some bytes".getBytes());
        */

        Video addedVideo = videoSvc.addVideo(video, new TypedFile("video/mp4", testVideoData));

        assertTrue(addedVideo != null);
        assertTrue(addedVideo.getId() > 0);
        assertTrue(addedVideo.getTitle().equals(video.getTitle()));
        assertTrue(addedVideo.getDuration() == video.getDuration());

        assertTrue(videoIsInList(videoSvc.getVideoList(), video));

        Response response = videoSvc.downloadVideoData(addedVideo.getId());
        assertEquals(200, response.getStatus());

        InputStream videoData = response.getBody().in();
        byte[] originalFile = IOUtils.toByteArray(new FileInputStream(testVideoData));
        byte[] retrievedFile = IOUtils.toByteArray(videoData);
        assertTrue(Arrays.equals(originalFile, retrievedFile));
    }

	@Test
	public void testGetNonExistantVideosData() throws Exception {

        System.out.println("----- testGetNonExistantVideosData() ----------");

        Long nonExistantId = getInvalidVideoId();

        try {
            Response r = videoSvc.downloadVideoData(nonExistantId);
            assertEquals(404, r.getStatus());
        } catch (RetrofitError e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

//	@Test
//	public void testAddNonExistantVideosData() throws Exception {
//        Long nonExistantId = getInvalidVideoId();
//		try{
//			videoSvc1.uploadVideoData(nonExistantId, new TypedFile(video.getContentType(), testVideoData));
//			fail("The client should receive a 404 error code and throw an exception if an invalid"
//                    + " video ID is provided in uploadVideoData()");
//		}catch(RetrofitError e){
//			assertEquals(404, e.getResponse().getStatus());
//		}
//	}

    @Test
    public void testAddVideoRating() throws Exception {

        System.out.println("----- testAddVideoRating() ----------");

        Video addedVideo = videoSvc.addVideo(video, new TypedFile("video/mp4", testVideoData));
        assert(videoIsInList(videoSvc.getVideoList(), video));
        assert(videoSvc.rateVideo(addedVideo.getId(), 2).getRating() == 2.0);
        assert(videoSvc.rateVideo(addedVideo.getId(), 5).getRating() == 5.0);
    }

    @Test
    public void testDeleteVideos() throws Exception {

        System.out.println("----- testDeleteVideos() ----------");

        Video addedVideo = videoSvc.addVideo(video, new TypedFile("video/mp4", testVideoData));
        assert(videoIsInList(videoSvc.getVideoList(), video));
        addedVideo = videoSvc.addVideo(video, new TypedFile("video/mp4", testVideoData));
        assert(videoIsInList(videoSvc.getVideoList(), video));
        deleteVideos();
    }

    // ----- helper methods ----------

	private Long getInvalidVideoId() {

        Set<Long> ids = videoSvc.getVideoList()
                .stream()
                .map(v -> v.getId())
                .collect(Collectors.<Long>toSet());

        Long nonExistantId = Long.valueOf(Long.MIN_VALUE);
		while (ids.contains(nonExistantId)) {
			nonExistantId += 1L;
		}
		return nonExistantId;
    }

    private boolean videoIsInList(final Collection<Video> videoList, final Video video) {
        return videoList.stream().filter(v -> isCorrectVideo(v, video)).count() > 0;
    }

    private boolean isCorrectVideo(Video v, Video compareVideo) {
        System.out.println("v = " + v);
        System.out.println("compareVideo = " + compareVideo);
        return v.getId() != null
            && v.getId() > 0
            && v.getOwner() != null
            && v.getOwner().equals(compareVideo.getOwner())
            && v.getTitle() != null
            && v.getTitle().equals(compareVideo.getTitle())
            && v.getDuration() == compareVideo.getDuration();
    }

    private static void deleteVideos() {
        Collection<Video> videos = videoSvc.getVideoList();
        videos.stream().forEach(v -> {
            boolean deleted = videoSvc.deleteVideo(v.getId());
            assertTrue(deleted);
        });
        assertTrue(videoSvc.getVideoList().isEmpty());
    }

    private static void sleep(long secs) {
        try {
            Thread.sleep(secs * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
