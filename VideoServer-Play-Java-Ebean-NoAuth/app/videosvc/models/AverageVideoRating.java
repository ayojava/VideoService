package videosvc.models;

public class AverageVideoRating {

	public final long videoId;
	public final double rating;
	public final int totalRatings;

	public AverageVideoRating() {
		this(-1L, 0.0, 0);
	}

	public AverageVideoRating(long videoId, double rating, int totalRatings) {
        this.videoId = videoId;
		this.rating = rating;
		this.totalRatings = totalRatings;
	}
}
