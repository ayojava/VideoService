package videosvc.models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


@Entity
public class UserVideoRating extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
	public Long id;
	public double rating;
	public String user;

	public UserVideoRating() {
	}

	public UserVideoRating(double rating, String user) {
		this.rating = rating;
		this.user = user;
	}

	@Override
	public int hashCode() {
		return Objects.hash(user);
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof UserVideoRating)
				&& Objects.equals(this.user, ((UserVideoRating) that).user);
	}

    @Override
    public String toString() {
        return "{" +
                "Id: "+ id + ", "+
                "User: "+ user + ", "+
                "Rating: "+ rating + ", "+
                "}";
    }
}
