package videosvc.utils;

import com.avaje.ebean.Model;
import videosvc.models.Video;
import play.mvc.Http;

import java.util.List;

public class U {


    public static Model.Finder<Long, Video> videoFinder() {
        return new Model.Finder<>(Long.class, Video.class);
    }

    public static Video videoById(Long id) {
        return videoFinder().byId(id);
    }

    public static VideoFileManager fileManager() {
        return VideoFileManager.videoFileManager();
    }

    public static boolean videoExists(Long id) {
        return videoById(id) != null;
    }

    public static String urlFor(Http.Request request, Long id) {
        return "http://" +  request.host() + "/video/" + id + "/data";
    }

    public static void insertTestVideosIfRepoIsEmpty() {
        List<Video> all = videoFinder().all();
        if (all.isEmpty()) {
            insertTestVideos();
        }
    }

    public static Video insert(Video v) {
        v.id = videoFinder().nextId();
        v.save();
        return videoById(v.id);
    }

    public static void insertTestVideos() {
        insert(new Video("Bob", "Video of Bob 1", 10L));
        insert(new Video("Lisa", "Video of Lisa 1", 15L));
        insert(new Video("Lara", "Video of Lara 1", 20L));
        insert(new Video("Bob", "Video of Bob 2", 30L));
    }
}
