package videosvc.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}
import slick.driver.H2Driver.api._
import slick.lifted.{MappedProjection, ForeignKeyQuery, ProvenShape}


case class Video(id: Long, owner: String, title: String, duration: Long, contentType: String, url: String) {

  override def toString = "Video(" + id + ", \"" + owner + "\", \"" + title + "\", " + duration + ", \"" + contentType + "\", \"" + url + "\")"

  def suffix: String = if (contentType == null) null else contentType.substring(contentType.indexOf('/') + 1)
  def dataFile: String = if (contentType == null) null else "video" + id + "." + suffix
  def dataPath(implicit dir: String): String = if (contentType == null) null else dir + "/" + dataFile
}

case class UserVideoRating(videoId: Long, rating: Double, user: String) {
  override def toString = "UserVideoRating(" + videoId + ", " + rating + ", \"" + user + "\")"
}

case class AverageVideoRating(videoId: Long, rating: Double, totalRatings: Int) {
  override def toString = "AverageVideoRating(" + videoId + "," + rating + ", " + totalRatings + ")"
}


object Implicits {

  implicit val videoWrites: Writes[Video] = (
        (JsPath \ "id").write[Long] and
        (JsPath \ "owner").write[String] and
        (JsPath \ "title").write[String] and
        (JsPath \ "duration").write[Long] and
        (JsPath \ "contentType").write[String] and
        (JsPath \ "url").write[String]
    )(unlift(Video.unapply))

  implicit val videoReads: Reads[Video] = (
        ((JsPath \ "id").read[Long] orElse Reads.pure(-1)) and
        ((JsPath \ "owner").read[String] orElse Reads.pure("")) and
        (JsPath \ "title").read[String] and
        (JsPath \ "duration").read[Long] and
        ((JsPath \ "contentType").read[String] orElse Reads.pure("")) and
        ((JsPath \ "url").read[String]orElse Reads.pure(""))
  )(Video.apply _)

  implicit val avgRatingWrites: Writes[AverageVideoRating] = (
        (JsPath \ "videoId").write[Long] and
        (JsPath \ "rating").write[Double] and
        (JsPath \ "totalRatings").write[Int]
    )(unlift(AverageVideoRating.unapply))

  implicit val avgRatingReads: Reads[AverageVideoRating] = (
        (JsPath \ "videoId").read[Long] and
        (JsPath \ "rating").read[Double] and
        (JsPath \ "totalRatings").read[Int]
    )(AverageVideoRating.apply _)
}


// A Videos table with 6 columns: id, owner, title, duration, contentType, url
//
class Videos(tag: Tag)
  extends Table[Video](tag, "VIDEOS") {

  // This is the primary key column:
  def id: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def owner: Rep[String] = column[String]("OWNER")
  def title: Rep[String] = column[String]("TITLE")
  def duration: Rep[Long] = column[Long]("DURATION")
  def contentType: Rep[String] = column[String]("CONTENT_TYPE")
  def url: Rep[String] = column[String]("URL")

  def * = (id, owner, title, duration, contentType, url) <> (Video.tupled, Video.unapply)
}

//object videos extends TableQuery(new Videos(_)) {
//  val findById = this.findBy(_.id)
//  val findByTitle = this.findBy(_.title)
//}

// A UserVideoRatings table with 4 columns: id, videoId, rating, user
//
class UserVideoRatings(tag: Tag)
  extends Table[UserVideoRating](tag, "USERVIDEORATINGS") {

//  def id: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def videoId: Rep[Long] = column[Long]("VIDEO_ID")
  def rating: Rep[Double] = column[Double]("RATING")
  def user: Rep[String] = column[String]("USER")

  def * = (videoId, rating, user) <> (UserVideoRating.tupled, UserVideoRating.unapply)
  def pk = primaryKey("PK_USERVIDEORATING", (videoId, user))

  def video: ForeignKeyQuery[Videos, Video] = foreignKey(
    "FK_VIDEO", videoId, TableQuery[Videos])(
    _.id, onDelete=ForeignKeyAction.Cascade, onUpdate=ForeignKeyAction.Restrict)
}

//object userVideoRatings extends TableQuery(new UserVideoRatings(_)) {
//  val findByVideoId = this.findBy(_.videoId)
//  def findByVideo(v: Video) = findByVideoId(v.id)
//}
