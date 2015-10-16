package videosvc.controllers;

import videosvc.models.UserVideoRating;
import videosvc.models.Video;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import videosvc.utils.U;

import java.io.File;
import java.io.IOException;

import static play.libs.Json.mapper;
import static play.libs.Json.toJson;

// @With(LoggingAction.class) not used
// Logging is performed in videosvc.Global.java
public class WebService extends Controller {

    private final Logger.ALogger l = Logger.of(getClass());


    public Result ping() {

        l.debug("ping()");

        return ok(toJson(true));
    }

    public Result findAll() {

        l.debug("findAll()");

        // insertTestVideosIfRepoIsEmpty();
        return ok(toJson(U.videoFinder().all()));
    }

    public Result findById(Long id) {

        l.debug("findById(id = " + id + ")");

        Video v = U.videoById(id);
        boolean found = v != null;

        return found ? ok(toJson(v)) : notFound("Video with id " + id + " not found.");
    }

    public Result deleteById(Long id) {

        l.debug("deleteById(id = " + id + ")");

        Video v = U.videoById(id);
        boolean found = v != null;
        if (!found) {
            return notFound("Video with id " + id + " not found.");
        }

        deleteVideoData(U.videoById(id));     // delete video data
        U.videoFinder().deleteById(id);
        return ok(toJson(true));
    }

    private void deleteVideoData(Video video) {
        boolean deleted = false;
        if (U.fileManager().hasVideoData(video)) {
            deleted = U.fileManager().deleteVideoData(video);
        }
        l.debug("Video data deleted: " + deleted);
    }

//    public Result addVideoMetaData() {
//
//        info("addVideoMetaData()");
//
//        JsonNode json = request().body().asJson();
//        Video v = fromJson(json, Video.class);
//        return ok(toJson(U.insert(v)));
//    }
//
//    public Result addVideoData(Long id) {
//
//        String methodName = "addVideoData";
//        info(methodName + "(id = " + id + ")");
//
//        if (!U.videoExists(id)) {
//            return notFound("Video with id " + id + " doesn't exist.");
//        }
//
//        Http.MultipartFormData body = request().body().asMultipartFormData();
//        if (body == null) {
//            return badRequest("No multipart formdata sent");
//        }
//        Http.MultipartFormData.FilePart data = body.getFile("data");
//        if (data == null) {
//            return badRequest("No multipart formdata sent (missing filepart named \"data\")");
//        }
//        l.debug(methodName + "(): Video data has content-type: " + data.getContentType());
//        l.debug(methodName + "(): Video data saved to temp file: " + data.getFile().getAbsolutePath());
//
//        Video v = U.videoById(id);
//        v.contentType = data.getContentType();
//        v.url = U.urlFor(request(), id);
//        l.debug(methodName + "(): saving video with modified attributes: " + v);
//        v.save();
//
//        moveVideoDataFile(methodName, id, data.getFile());
//
//        return ok(toJson(true));
//    }

    public Result addVideo() {

        String methodName = "addVideo";
        l.debug(methodName + "()");

        l.debug(methodName + "(): Body:" + request().body().toString());

        Http.MultipartFormData multipartBody = request().body().asMultipartFormData();
        if (multipartBody == null) {
            return badRequest("No multipart formdata sent");
        }
        l.debug(methodName + "(): Got multipart formdata");

        String metaData = multipartBody.asFormUrlEncoded().get("meta-data")[0];
        if (metaData == null) {
            return badRequest("No video meta-data sent (missing datapart named \"meta-data\")");
        }
        l.debug(methodName + "(): meta-data: " + metaData);

        Http.MultipartFormData.FilePart data = multipartBody.getFile("data");
        if (data == null) {
            return badRequest("No multipart formdata sent (missing filepart named \"data\")");
        }
        l.debug(methodName + "(): Video data has content-type: " + data.getContentType());
        l.debug(methodName + "(): Video data saved to temp file: " + data.getFile().getAbsolutePath());

        Video video = videoFrom(metaData, data);
        video.save();                                               // store video in db
        l.debug(methodName + "(): new video stored in db: " + video);

        moveVideoDataFile(methodName, video.id, data.getFile());

        return ok(toJson(video));
    }

    private Video videoFrom(String metaData, Http.MultipartFormData.FilePart data) {

        Video v = parseJson(metaData); // resolves title and duration
        Long id = U.videoFinder().nextId();
        return new Video(id, v.owner, v.title, v.duration, data.getContentType(), U.urlFor(request(), id));
    }

    private Video parseJson(String jsonString) {

        assert jsonString != null;

        try {
            return mapper().readValue(jsonString, Video.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void moveVideoDataFile(String invoker, Long videoId, File tmpFile) {
        String dataPath = U.fileManager().moveVideoData(U.videoById(videoId), tmpFile);
        l.debug(invoker + "(): Video data moved to: " + dataPath);
    }

    public Result getVideoData(Long id) {

        l.debug("getVideoData(id = " + id + ")");

        if (!U.videoExists(id)) {
            return notFound("Video with id " + id + " doesn't exist.");
        }

        if (!U.fileManager().hasVideoData(U.videoById(id))) {
            return notFound("No data found for video with id " + id);
        }

        return ok(U.fileManager().getInputStream(U.videoById(id))).as("video/mp4");
    }

    public Result addVideoRating(Long id, int stars) {

        l.debug("addVideoRating(id = " + id + ", stars = " + stars + ")");

        if (!U.videoExists(id)) {
            return notFound("Video with id " + id + " doesn't exist.");
        }

        Video v = updateVideoRatingById(id, stars);
        return ok(toJson(v.getAverageVideoRating()));    }

    private Video updateVideoRatingById(Long id, int stars) {
        Video v = U.videoById(id);
        v.updateUserVideoRating(new UserVideoRating(stars, v.owner));
        v.save();
        return v;
    }

   public Result getVideoRating(Long id) {

       l.debug("getVideoRating(id = " + id + ")");

       Video v = U.videoById(id);
       return ok(toJson(v.getAverageVideoRating()));
    }
}
