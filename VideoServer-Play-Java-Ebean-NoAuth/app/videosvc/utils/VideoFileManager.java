package videosvc.utils;

import videosvc.models.Video;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class VideoFileManager {

    private static VideoFileManager videoFileManager;

    public static VideoFileManager videoFileManager() {
        if (videoFileManager == null) {
            synchronized (VideoFileManager.class) {
                if (videoFileManager == null) {
                    videoFileManager = new VideoFileManager();
                }
            }
        }
        return videoFileManager;
    }

    private Path targetDir_ = Paths.get("videos");


    private VideoFileManager() {
        if(!Files.exists(targetDir_)){
            try {
                Files.createDirectories(targetDir_);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private Path getVideoPath(Video video){

        assert(video != null);
        assert(video.dataFile() != null);

        return targetDir_.resolve(video.dataFile());
    }

    public boolean hasVideoData(Video video){

        if (video.dataFile() == null) {
            return false;
        }
        return Files.exists(getVideoPath(video));
    }

    public InputStream getInputStream(Video video) {

        try {

            Path source = getVideoPath(video);
            if(!Files.exists(source)){
                throw new FileNotFoundException("Unable to find the referenced video file for videoId:"+video.id);
            }
            return new FileInputStream(source.toFile());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void copyVideoData(Video video, OutputStream out) {

        try {
            Path source = getVideoPath(video);
            if(!Files.exists(source)){
                throw new FileNotFoundException("Unable to find the referenced video file for videoId:"+video.id);
            }
            Files.copy(source, out);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String moveVideoData(Video video, File tmpFile) {

        Path source = tmpFile.toPath();
        Path dest = getVideoPath(video);
        try {
            if(!Files.exists(source)){
                throw new FileNotFoundException("Unable to find the referenced video file for videoId: " + video.id);
            }
            Files.move(tmpFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toFile().getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void saveVideoData(Video video, InputStream videoData){

        assert(video != null);
        assert(videoData != null);

        Path target = getVideoPath(video);

        try {
            Files.copy(videoData, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean deleteVideoData(Video video) {

        assert(video != null);

        try {
            Path target = getVideoPath(video);
            if (!Files.exists(target)) {
                return false;
            }
            Files.delete(target);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String readContent(File source) {

        try {
            List<String> lines = Files.readAllLines(source.toPath());
            return lines.stream().collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
