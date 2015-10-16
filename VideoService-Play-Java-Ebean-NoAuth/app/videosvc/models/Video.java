package videosvc.models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Video extends Model {

    // @GeneratedValue(strategy = GenerationType.AUTO)
    @Id public Long id;
    public String owner;
    public String title;
    public long duration;
    public String contentType;
    public String url;

    // @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "videoId")
    @JsonIgnore
    public Set<UserVideoRating> videoRatings = new HashSet<>();

    public Video(Long id, String owner, String title, long duration, String contentType, String url) {
        this.id = id;
        this.owner = owner;
        this.title = title;
        this.duration = duration;
        this.contentType = contentType;
        this.url = url;
    }

    public Video(String owner, String title, long duration) {
        this(-1l, owner, title, duration, null, null);
    }

    public Video() {
    }

    public String dataPath(String dir) {
        return contentType == null ? null : dir + "/" + dataFile();
    }

    public String dataFile() {
        return contentType == null ? null : "video" + id + "." + suffix();
    }

    private String suffix() {
        return contentType == null ? null : contentType.substring(contentType.indexOf('/') + 1);
    }

    public void updateUserVideoRating(UserVideoRating rating) {
        // see equals() in UserVideoRating
        videoRatings.remove(rating);
        videoRatings.add(rating);
    }

    @JsonProperty
    public AverageVideoRating getAverageVideoRating() {
        //double sum = videoRatings.stream().collect(Collectors.summingDouble(r -> r.getRating()));
        double average = videoRatings.stream().collect(Collectors.averagingDouble(r -> r.rating));
        return new AverageVideoRating(id, average, videoRatings.size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, title, duration);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Video)
                && Objects.equals(this.owner, ((Video) other).owner)
                && Objects.equals(this.title, ((Video) other).title)
                && this.duration == ((Video) other).duration;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": {" +
                "Id: "+ id + ", " +
                "Owner: "+ owner + ", " +
                "Title: "+ title + ", " +
                "Duration: "+ duration + ", " +
                "ContentType: "+ contentType + ", " +
                "Url: "+ url +
            "}";
    }
}
