package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.magnum.dataup.VideoSvcApi;

import retrofit.http.Part;
import retrofit.mime.TypedFile;

import javax.servlet.http.HttpServletResponse;

@Controller
public class VideoSvcController {
	
	private VideoFileManager mVideoFileManager;
	static Logger log = Logger.getLogger(VideoSvcController.class.getName());
	
	
	public VideoSvcController(){
		
		init();
		
	}
	
	// Initialize all my internal variables
	public void init(){
		
		try {
			mVideoFileManager = VideoFileManager.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to convert the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	//
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi
	// interface. We use this constant to ensure that the 
	// client and service paths for the VideoSvc are always
	// in synch.
	//
	// For some ways to improve the validation of the data
	// in the Video object, please see this Spring guide:
	// http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/validation.html#validation-beanvalidation
	//
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		
		log.info("POST REQUEST FOR VIDEO META DATA :::::::: HTTP REQUEST = POST/video");
		
		return mVideoFileManager.saveVideoMetaData(v);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			@RequestParam(VideoSvcApi.DATA_PARAMETER)MultipartFile videoData,
			HttpServletResponse Response){
		
		VideoStatus videoStatus = new VideoStatus(VideoStatus.VideoState.PROCESSING);
		
		if(mVideoFileManager.hasVideo(id)){
			try {
				mVideoFileManager.saveVideoData(mVideoFileManager.getVideo(id), videoData.getInputStream());
				videoStatus.setState(VideoStatus.VideoState.READY);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		
		return videoStatus;

	}
	
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		
		log.info("GET REQUEST FOR LIST OF VIDEOS :::::::::::: HTTP REQUEST = GET/video");
		return mVideoFileManager.getVideoList();
		
	}
	
	// Receives GET requests to /video/{id}/data and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getData(
			@PathVariable(VideoSvcApi.ID_PARAMETER)long id,
			HttpServletResponse Response){
		
		log.info("GET REQUEST FOR VIDEO :::::::::::: HTTP REQUEST = GET/video/{id}/data");
		log.info("Video requested for video id: "+id);
		if(mVideoFileManager.hasVideo(id)){
			try {
				mVideoFileManager.copyVideoData(mVideoFileManager.getVideo(id),Response.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			log.info("Video not found: ");
			Response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		
	}



}


