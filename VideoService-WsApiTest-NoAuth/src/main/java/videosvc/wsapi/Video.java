package videosvc.wsapi;

import java.util.Objects;

import com.fluentinterface.ReflectionBuilder;
import com.fluentinterface.builder.Builder;
import com.google.gson.annotations.Expose;

public class Video {

	public static TestVideoBuilder create() {
		return ReflectionBuilder.implementationFor(TestVideoBuilder.class).create();
	}

	public interface TestVideoBuilder extends Builder<Video> {
        public TestVideoBuilder withOwner(String owner);
		public TestVideoBuilder withTitle(String title);
		public TestVideoBuilder withDuration(long duration);
		public TestVideoBuilder withContentType(String contentType);
	}

	@Expose(serialize = false, deserialize = true)
	private Long id;
	@Expose(serialize = true, deserialize = true)
	private String owner;
	@Expose(serialize = true, deserialize = true)
	private String title;
	@Expose(serialize = true, deserialize = true)
	private long duration;
	@Expose(serialize = true, deserialize = true)
	private String contentType;
	@Expose(serialize = false, deserialize = true)
	private String dataUrl;
	@Expose(serialize = false, deserialize = true)
	private double avgRating;

	public Video() {
	}

	public Video(String owner,
				 String title,
				 long duration,
				 String contentType) {

		this(null, owner, title, duration, contentType, null, -1.0);
	}

	public Video(Long id,
				 String owner,
				 String title,
				 long duration,
				 String contentType,
				 String dataUrl,
				 double avgRating) {

		this.id = id;
		this.owner = owner;
		this.title = title;
		this.duration = duration;
		this.contentType = contentType;
		this.dataUrl = dataUrl;
		this.avgRating = avgRating;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDataUrl() {
		return dataUrl;
	}

	public void setDataUrl(String dataUrl) {
		this.dataUrl = dataUrl;
	}

	public void setAvgRating(double avgRating) {
		this.avgRating = avgRating;
	}

	public double getAvgRating() {
		return avgRating;
	}

	@Override
	public String toString() {
		return "{" +
				"Id: " + id + ", " +
				"Owner: " + owner + ", " +
				"Title: " + title + ", " +
				"Duration: " + duration + ", " +
				"ContentType: " + contentType + ", " +
				"Data URL: " + dataUrl + ", " +
				"AvgRating: " + avgRating + ", " +
				"}";
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(),
				getOwner(),
				getTitle(),
				getDuration(),
				getContentType());
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Video)
				&& getId() == ((Video) obj).getId()
				&& Objects.equals(getOwner(), ((Video) obj).getOwner())
				&& Objects.equals(getTitle(), ((Video) obj).getTitle())
				&& getDuration() == ((Video) obj).getDuration()
				&& Objects.equals(getContentType(), ((Video) obj).getContentType());
	}
}
