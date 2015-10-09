/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package videosvc.wsapi;

import java.util.Collection;

import retrofit.client.Response;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Streaming;
import retrofit.mime.TypedFile;

public interface VideoSvcApi {

    @GET("/ping")
    public Boolean ping();

	@GET("/video")
	public Collection<Video> getVideoList();

    @GET("/video/{id}")
    public Video getVideo(@Path("id") Long id);

    @DELETE("/video/{id}")
    public Boolean deleteVideo(@Path("id") Long id);

//  @POST("/videoMetaData")
//  public Video addVideoMetaData(@Body Video v);
//
//	@Multipart
//	@POST("/video/{id}/data")
//	public Boolean uploadVideoData(@Path(ID_PARAMETER) Long id, @Part(DATA_PARAMETER) TypedFile videoData);

    @Multipart
    @POST("/video")
    public Video addVideo(@Part("meta-data") Video video, @Part("data") TypedFile videoData);

    @Streaming
    @GET("/video/{id}/data")
    Response downloadVideoData(@Path("id") Long id);

    @POST("/video/{id}/rating/{stars}")
	public AverageVideoRating rateVideo(@Path("id") Long id, @Path("stars") int stars);

    @POST("/video/{id}/rating")
    public AverageVideoRating getAverageRating(@Path("id") Long id);
}
