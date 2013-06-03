/**
 * Copyright 2013 iFactory Lab Ltd.
 * 
 * Min Kim (minsikzzang@gmail.com)
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ifactory.service.weather.photo;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.InputStream;
import java.lang.NumberFormatException;
import java.lang.NullPointerException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;

//@WebServlet(urlPatterns = {"/photo"}, asyncSupported = true)
/*@MultipartConfig(location="/tmp",
                 fileSizeThreshold=1024*1024*2, // 2MB 
                 maxFileSize=1024*1024*10,      // 10MB
                 maxRequestSize=1024*1024*50)   // 50MB
                 */
public class PhotoServlet extends HttpServlet {
  static final String ATTRIBUTE_LAT = "lat";
	static final String ATTRIBUTE_LNG = "lng";
	static final String ATTRIBUTE_WEATHER = "weather";
	static final String ATTRIBUTE_LIMIT = "limit";
	static final String ATTRIBUTE_TIMESTAMP = "timestamp";
	static final double DEFAULT_RADIUS = 0.001;
	static final int PHOTO_FILE_LENGTH = 32;
	
	private String dbHost = "localhost";
	private int dbPort = 27017;
	private String dbName = "broken_clouds_development";
  private PhotoService photo = null;
  private String photoHost = "";
  
  @Override
  public void init(ServletConfig conf) throws ServletException {
    super.init(conf);    
    
    try {
      InputStream is = conf.getServletContext()
                          .getResourceAsStream("/WEB-INF/photo.properties"); 
      Properties props = new Properties(); 
      props.load(is);       
      dbHost = props.getProperty("dbhost");
      dbPort = Integer.parseInt(props.getProperty("dbport"));
      dbName = props.getProperty("database");    
      photoHost = props.getProperty("photo_origin");       
    } catch (IOException e) {
      // Log exception
    }
    
    try {
      this.photo = new PhotoService(dbHost, dbPort, dbName, photoHost);            
    } catch (UnknownHostException e) {
      // Log this exception....
    } finally {
    }    	          
  }
  
  @Override
  public void destroy() {
    super.destroy();
    
    if (this.photo != null) {
      this.photo.close();
    }
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    resp.setContentType("application/json");
    final AsyncContext aCtx = req.startAsync(req, resp);
    aCtx.start(new PostPhotoService(aCtx, this.photo));
  }
  
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	    throws IOException {
	  resp.setContentType("application/json");
    final AsyncContext aCtx = req.startAsync(req, resp);
    aCtx.start(new GetPhotoService(aCtx, this.photo));
	}
	
	private class PostPhotoService implements Runnable {
	  AsyncContext aCtx;
    PhotoService photo;

    public PostPhotoService(AsyncContext aCtx, PhotoService photo) {
      this.aCtx = aCtx;
      this.photo = photo;
    }
    
    public String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }
    
	  @Override
    public void run() {
      HttpServletRequest req = (HttpServletRequest)aCtx.getRequest();
      HttpServletResponse resp = (HttpServletResponse)aCtx.getResponse();
            
      try {
        Collection<Part> parts = req.getParts();
        if (parts.size() < 5) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                         "All Fields are mandatory.");
        } else {
          long timestamp = Long.parseLong(
              fromStream(req.getPart(ATTRIBUTE_TIMESTAMP).getInputStream()));
          double lat = Double.parseDouble(
              fromStream(req.getPart(ATTRIBUTE_LAT).getInputStream()));
          double lng = Double.parseDouble(
              fromStream(req.getPart(ATTRIBUTE_LNG).getInputStream()));
          int weatherId = Integer.parseInt(
              fromStream(req.getPart(ATTRIBUTE_WEATHER).getInputStream()));
          
          // Generate file name based on location and timestamp
          Part filePart = req.getPart("photo");
          String fileName = 
              RandomStringUtils.randomAlphanumeric(PHOTO_FILE_LENGTH);
          filePart.write(fileName); 
          
          System.out.println(fileName + ": " + lat + ", " + lng + ": " + 
                             weatherId + " - " + timestamp);
                        
          Photo photo = new Photo.Builder(fileName, weatherId)   
              .geoCoord(lat, lng)
                .day(true)
                  .timestamp(timestamp)
                    .origin(this.photo.getOrigin())
                      .build();
          
          if (this.photo.add(photo)) {
            resp.getWriter().print(new PhotoFormatter().json(photo));  	    
            resp.setStatus(HttpServletResponse.SC_OK);
          } else {
            filePart.delete();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);            
          }
        }
        
      } catch (IOException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (ServletException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
                          
  		aCtx.complete();	    
    }
  }
  
	private class GetPhotoService implements Runnable {
	  AsyncContext aCtx;
    PhotoService photo;

    public GetPhotoService(AsyncContext aCtx, PhotoService photo) {
      this.aCtx = aCtx;
      this.photo = photo;
    }

    private void handleGetPhoto(double lat, double lng, int weatherId, 
        int limit, HttpServletResponse resp) {     
      String response = null;
      
      if (this.photo == null) {
        try {
          this.photo = new PhotoService(dbHost, dbPort, dbName, photoHost);        
        } catch (UnknownHostException e) {
          // Log this exception....
        }        
      }
      
      ArrayList<Photo> photos = this.photo.geoCoord(lat, lng, DEFAULT_RADIUS)
                                  .weatherId(weatherId)
                                    .growable(true).limit(limit)                                      
                                      .get();       
      response = new PhotoFormatter().json(photos);            
      if (response != null) {
        try {
          resp.getWriter().print(response);  	    
          resp.setStatus(HttpServletResponse.SC_OK);
        }	catch (IOException e) {
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);	     
        }	  
      } else {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);	     
      }
    }
    
    @Override
    public void run() {
      HttpServletRequest req = (HttpServletRequest)aCtx.getRequest();
      HttpServletResponse resp = (HttpServletResponse)aCtx.getResponse();
      double lat = 0.0;
      double lng = 0.0;
      int weatherId = 0;
      int limit = 0;
      
      try {
        lat = Double.parseDouble(req.getParameter(ATTRIBUTE_LAT));
        lng = Double.parseDouble(req.getParameter(ATTRIBUTE_LNG));
        weatherId = Integer.parseInt(req.getParameter(ATTRIBUTE_WEATHER));
        limit = Integer.parseInt(req.getParameter(ATTRIBUTE_LIMIT));         
        handleGetPhoto(lat, lng, weatherId, limit, resp); 
      } catch (NumberFormatException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (NullPointerException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }     
      
  		aCtx.complete();	    
    }	  
	}
}