package videosvc.wsapi;

public class AverageVideoRating {

	private final double rating;
	private final long videoId;
	private final int totalRatings;

	public AverageVideoRating(long videoId, double rating, int totalRatings) {

        this.videoId = videoId;
		this.rating = rating;
		this.totalRatings = totalRatings;
	}

	public double getRating() {
		return rating;
	}

	public long getVideoId() {
		return videoId;
	}

	public int getTotalRatings() {
		return totalRatings;
	}
}
