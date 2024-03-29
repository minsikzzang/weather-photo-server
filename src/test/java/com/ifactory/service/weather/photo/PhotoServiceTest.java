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

import java.net.UnknownHostException;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for PhotoService
 */
public class PhotoServiceTest extends TestCase {
	
	final double lat = 51.550927;
  final double lng = -0.180676;
	final String dbHost = "localhost";
	final String dbName = "broken_clouds_development";
	final int dbPort = 27017;	
	final String photoHost = "http://api.ifactory-lab.com/";
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public PhotoServiceTest(String testName) {
    super(testName);                
  }
  
  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(PhotoServiceTest.class);
  }
  
  /**
   * Test to retrieve a photo by weather id
   */
  public void testGetPhotoByWeather() {
    PhotoService photo = null;
    int weatherId = 800;
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.weatherId(weatherId).get();        
      for (Photo p: photos) {
        assertEquals(p.getWeatherId(), weatherId);
      }        
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}  	
  }
  
  /**
   * Test to retrieve a photo by geo coordinates
   */
  public void testGetPhotoByGeo() {
    PhotoService photo = null;
    double lat = 51.549978;
    double lng = -0.180459;
    double rad = 0.01;
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad).get();
      for (Photo p: photos) {
        assertTrue((p.getLatitude() > lat - rad));
        assertTrue((p.getLatitude() < lat + rad));
        assertTrue((p.getLongitude() > lng - rad));
        assertTrue((p.getLongitude() < lng + rad));
      }
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
  
  /**
   * Test to retrieve a photo by geo coordinates
   */
  public void testGetPhotoByGeoAndWeather() {
    PhotoService photo = null;
    int weatherId = 601;
    double lat = 51.549978;
    double lng = -0.180459;
    double rad = 0.01;
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad)
                                  .weatherId(weatherId).get();
      for (Photo p: photos) {
        assertEquals(p.getWeatherId(), weatherId);
        assertTrue((p.getLatitude() > lat - rad));
        assertTrue((p.getLatitude() < lat + rad));
        assertTrue((p.getLongitude() > lng - rad));
        assertTrue((p.getLongitude() < lng + rad));
      }
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
  
  /**
   * Test to retrieve a photo by geo coordinates but no result
   */
  public void testGetPhotoByGeoNoResult() {
    PhotoService photo = null;
    double lat = 49.549978;
    double lng = -0.180459;
    double rad = 0.01;
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad).get();
      assertTrue(photos.size() == 0);
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}      
  }
  
  /**
   * Test to retrieve a photo by geo coordinates without result. But put
   * "grow" option on, and it will be growing until we find a result.
   */
  public void testGetPhotoByGeoWithGrow() {
    PhotoService photo = null;
    int weatherId = 601;
    double lat = 40.549978;
    double lng = 5.180459;
    double rad = 0.01;
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad)
                                  .weatherId(weatherId)
                                    .growable(true)
                                      .get();
      assertTrue(photos.size() > 0);
      for (Photo p: photos) {
        assertEquals(p.getWeatherId(), weatherId);
      }
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
  
  /**
   * Test to retrieve a photo by geo coordinates with limit count
   */
  public void testGetPhotoByGeoWithLimit() {
    PhotoService photo = null;
    int weatherId = 601;
    double lat = 40.549978;
    double lng = 5.180459;
    double rad = 0.1;
    int limit = 2;
    
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad)
                                  .weatherId(weatherId)
                                    .growable(true).limit(limit)
                                      .get();
      assertTrue(photos.size() == 2);
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
  
  /**
   * Test to retrieve a photo by geo coordinates but no result so return 
   * same class of weather
   */
  public void testGetPhotoByGeoWithSameClassWeather() {
    PhotoService photo = null;
    int weatherId = 600;
    double lat = 40.549978;
    double lng = 5.180459;
    double rad = 0.1;
    int limit = 2;
    System.out.println("testGetPhotoByGeoWithSameClassWeather");
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad)
                                  .weatherId(weatherId)
                                    .growable(true).limit(limit)
                                      .get();
      assertTrue(photos.size() == 2);
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
  
  /**
   * Test to retrieve a photo by geo coordinates but no result and no
   * same class weather. It should return any of photos.
   */
  public void testGetPhotoByGeoWithNoSameClassWeather() {
    PhotoService photo = null;
    int weatherId = 500;
    double lat = 40.549978;
    double lng = 5.180459;
    double rad = 0.1;
    int limit = 2;
    System.out.println("testGetPhotoByGeoWithNoSameClassWeather");
  	try {
  	  photo = new PhotoService(dbHost, dbPort, dbName, photoHost);
      ArrayList<Photo> photos = photo.geoCoord(lat, lng, rad)
                                  .weatherId(weatherId)
                                    .growable(true).limit(limit)
                                      .get();
      // System.out.println(photos.size());                                
      assertTrue(photos.size() == limit);
  	} catch (UnknownHostException e) {
  	} finally {
  	  if (photo != null) {
  	    photo.close();  
  	  }    	  
  	}
  }
}
