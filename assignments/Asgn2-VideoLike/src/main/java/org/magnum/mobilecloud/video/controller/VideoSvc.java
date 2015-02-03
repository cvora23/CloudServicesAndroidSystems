package org.magnum.mobilecloud.video.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * This simple VideoSvc allows clients to send HTTP POST requests with
 * videos that are stored in memory using a list. Clients can send HTTP GET
 * requests to receive a JSON listing of the videos that have been sent to
 * the controller so far. Stopping the controller will cause it to lose the history of
 * videos that have been sent to it because they are stored in memory.
 * 
 * Notice how much simpler this VideoSvc is than the original VideoServlet?
 * Spring allows us to dramatically simplify our service. Another important
 * aspect of this version is that we have defined a VideoSvcApi that provides
 * strong typing on both the client and service interface to ensure that we
 * don't send the wrong paraemters, etc.
 * 
 * @author jules
 *
 */

// Tell Spring that this class is a Controller that should 
// handle certain HTTP requests for the DispatcherServlet
@Controller
public class VideoSvc{
	
	static Logger log = Logger.getLogger(VideoSvc.class.getName());
	
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;

	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to conver the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	//
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi
	// interface. We use this constant to ensure that the 
	// client and service paths for the VideoSvc are always
	// in synch.
	//
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		log.info("POST REQUEST FOR VIDEO META DATA :::::::: HTTP REQUEST = POST/video");
		return  videos.save(v);
	}
	
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		log.info("GET REQUEST FOR VIDEO META DATA :::::::: HTTP REQUEST = GET/video");
		return Lists.newArrayList(videos.findAll());
	}
	
	// Receives GET requests to /video/{id} and returns the current
	// video corresponding to the id. Spring automatically converts
	// video to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			HttpServletResponse Response){
		
		log.info("GET REQUEST FOR VIDEO :::::::: HTTP REQUEST = GET/video/{id}");
		
		if(videos.exists(id)){
			log.info("Video found: ");
			Response.setStatus(HttpServletResponse.SC_OK);
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		long likes = videos.findOne(id).getLikes();
		log.info("Like count for video: "+id+" after getting is: "+likes);
		return videos.findOne(id);
	}

	// Receives POST requests to /video/{id}/like
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public @ResponseBody void likeVideo(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			Principal p,
			HttpServletResponse Response){
		
		log.info("POST REQUEST FOR VIDEO LIKE :::::::: HTTP REQUEST = POST/video/{id}/like");
		
		if(videos.exists(id)){
			log.info("Video found: ");
			String username = p.getName();
			log.info("Username who has requested to like the video : " + id + "is :" + username );
			if(videos.findOne(id).hasUserLiked(username)){
				log.info("Video found but user has already liked the video: ");
				Response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}else{
				Video v = videos.findOne(id);
				v.addUser(username);
				long likes = v.getLikes();
				log.info("Like count for video: "+id+" after getting is: "+likes);
				v.setLikes(likes+1);
				likes = v.getLikes();
				log.info("Like count for video: "+id+" after setting is: "+likes);
				Response.setStatus(HttpServletResponse.SC_OK);
				videos.save(v);
			}
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	// Receives POST requests to /video/{id}/unlike
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public @ResponseBody void unlikeVideo(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			Principal p,
			HttpServletResponse Response){
		
		log.info("POST REQUEST FOR VIDEO LIKE :::::::: HTTP REQUEST = POST/video/{id}/unlike");
		
		if(videos.exists(id)){
			log.info("Video found: ");
			String username = p.getName();
			log.info("Username who has requested to like the video : " + id + "is :" + username );
			if(!videos.findOne(id).hasUserLiked(username)){
				log.info("Video found but user has never already liked the video: ");
				Response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}else{
				Video v = videos.findOne(id);
				v.removeUser(username);
				long likes = v.getLikes();
				log.info("Like count for video: "+id+" after getting is: "+likes);
				v.setLikes(likes-1);
				likes = v.getLikes();
				log.info("Like count for video: "+id+" after setting is: "+likes);
				Response.setStatus(HttpServletResponse.SC_OK);
				videos.save(v);
			}
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	// Receives GET requests to /video/{id}/likedby
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			HttpServletResponse Response){
		
		log.info("POST REQUEST FOR VIDEO LIKE :::::::: HTTP REQUEST = GET/video/{id}/likedby");
		
		if(videos.exists(id)){
			log.info("Video found: ");
			Response.setStatus(HttpServletResponse.SC_OK);
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		return Lists.newArrayList(videos.findOne(id).getUsers());
	}

	
	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title
	){
		return videos.findByName(title);
	}

	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(VideoSvcApi.DURATION_PARAMETER) long maxDuration
	){
		return videos.findByDurationLessThan(maxDuration);
	}

}
