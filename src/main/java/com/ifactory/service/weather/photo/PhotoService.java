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

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.CommandFailureException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.lang.Math;
// import com.mongodb.ServerAddress;
// db.photos.ensureIndex({"geo.coordinates":"2d"})

class PhotoService {
  private static String PHOTO_COLLECTION = "photos";
  private static double UNAVAILABLE_LATITUDE = 91;
  private static double UNAVAILABLE_LONGITUDE = 181;
  private static double DEFAULT_RADIUS = 0.001;
  private static int DEFAULT_LIMIT = 10;
  private int weatherId;  
  // To directly connect to a single MongoDB server (note that this will not 
  // auto-discover the primary even if it's a member of a replica set:
  /**
  MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
  // or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
  MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
                                        new ServerAddress("localhost", 27018),
                                        new ServerAddress("localhost", 27019)));
                                        */                                        
  private MongoClient mongoClient;
  private String dbName;  
  private double lat = UNAVAILABLE_LATITUDE;
  private double lng = UNAVAILABLE_LONGITUDE;
  private double radius = DEFAULT_RADIUS;
  private boolean growable = false;
  private int limit = DEFAULT_LIMIT;
  
  public PhotoService(String host, int port, String dbName) 
      throws UnknownHostException {
    this.weatherId = -1;
    this.mongoClient = new MongoClient(host , port);
    this.dbName = dbName;    
  }
    
  public PhotoService geoCoord(double lat, double lng, double radius) {
    this.lat = lat;
    this.lng = lng;
    this.radius = radius;
    return this;
  }
  
  public PhotoService weatherId(int weatherId) {
    this.weatherId = weatherId;
    return this;
  }
  
  public PhotoService growable(boolean growable) {
    this.growable = growable;
    return this;
  }  
  
  public PhotoService limit(int limit) {
    this.limit = limit;
    return this;
  }
  
  public void close() {
    this.mongoClient.close();
  }
  
  private BasicDBObject setGeoCoord(double lat, double lng, double radius) {
    BasicDBObject query = new BasicDBObject();
    BasicDBList geo = new BasicDBList();
    geo.add(lat);
    geo.add(lng);
    BasicDBList center = new BasicDBList();   
    center.add(geo);
    center.add(radius); 
    query.append("geo.coordinates", new BasicDBObject("$within", 
                 new BasicDBObject("$center", center)));
    return query;
  }
  
  public ArrayList<Photo> get() {
    DB db = mongoClient.getDB(this.dbName);
    DBCollection coll = db.getCollection(PHOTO_COLLECTION);    
    BasicDBObject query = null;
    DBCursor cursor = null;
    ArrayList<Photo> photoList = new ArrayList();  
    int weatherClassMin = -1;
    int weatherClassMax = -1;
    
    while (true) {      
      // If latitude and longitude were given, append geo search query
      if (this.lat != UNAVAILABLE_LATITUDE && 
          this.lng != UNAVAILABLE_LONGITUDE) {
        query = setGeoCoord(this.lat, this.lng, this.radius);
      } else {
        query = new BasicDBObject();
      }      
      
      // It the weather Id has given, append weather search query
      if (this.weatherId > 0) {
        if (weatherClassMin == -1 && weatherClassMax == -1) {
          query.append("weather", this.weatherId);
        } else {          
          System.out.println("query with weatherClassMin(" + weatherClassMin + 
            ") and weatherClassMax(" + weatherClassMax + ")");    
          query.append("weather", new BasicDBObject("$gte", weatherClassMin)
            .append("$lte", weatherClassMax));  
          // System.out.println(query.toString());                     
        }        
      }
      
      try {
        cursor = coll.find(query).limit(this.limit);
        if (cursor.count() > 0 || this.growable == false || 
            this.radius >= UNAVAILABLE_LATITUDE) {
          if (this.radius >= UNAVAILABLE_LATITUDE) {
            this.radius = 45;
            if (weatherClassMin == -1 && weatherClassMax == -1) {
              // In this case, there is no proper photos by the given weather.
              // Let's find any photos bound for same weather class.
              weatherClassMin = ((int)this.weatherId / 100) * 100;
              weatherClassMax = (((int)this.weatherId / 100) + 1) * 100;    
              System.out.println("weatherClassMin and weatherClassMax exist");          
              continue;
            } else if (this.weatherId > 0) {
              this.weatherId = 0;     
              System.out.println("weatherid goes to zero");     
              continue;    
            } else {
              break;
            }
          } else {
            break;  
          }          
        }  
      } catch (CommandFailureException e) {
        cursor = null;
        break;
      }
      
      this.radius = this.radius * 2; 
    }  
    
    try {
      while (cursor != null && cursor.hasNext()) {
        DBObject obj = cursor.next();
        Photo.Builder b = new Photo.Builder((String)obj.get("name"), 
            ((Number)obj.get("weather")).intValue());   

        ArrayList<Double> coord = ((ArrayList<Double>)((DBObject)obj
                                    .get("geo")).get("coordinates"));
        b.geoCoord(coord.get(0), coord.get(1))
          .day(((Boolean)obj.get("day")).booleanValue())
            .timestamp(((Number)obj.get("timestamp")).longValue());
              
        photoList.add(b.build());
      }
    } finally {
      if (cursor != null) {
        cursor.close(); 
      }       
    }
    return photoList;
  }
}