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

import java.net.UnknownHostException;
import java.util.ArrayList;
// import com.mongodb.ServerAddress;


class PhotoService {
  private static String PHOTO_COLLECTION = "photos";
  private static double UNAVAILABLE_LATITUDE = 91;
  private static double UNAVAILABLE_LONGITUDE = 181;
  private static double DEFAULT_RADIUS = 0.001;
  private int weatherId;  
  // To directly connect to a single MongoDB server (note that this will not auto-discover the primary even
  // if it's a member of a replica set:
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
  
  public PhotoService(String host, int port, String dbName) throws UnknownHostException {
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
  
  public void close() {
    this.mongoClient.close();
  }
  
  public ArrayList<Photo> get() {
    DB db = mongoClient.getDB(this.dbName);
    DBCollection coll = db.getCollection(PHOTO_COLLECTION);    
    BasicDBObject query = new BasicDBObject();
    
    // If latitude and longitude were given, append geo search query
    if (this.lat != UNAVAILABLE_LATITUDE && 
        this.lng != UNAVAILABLE_LONGITUDE) {
      BasicDBList geo = new BasicDBList();
      geo.add(this.lat);
      geo.add(this.lng);
      BasicDBList center = new BasicDBList();   
      center.add(geo);
      center.add(this.radius); 
      query.append("geo.coordinates", 
                   new BasicDBObject("$within", 
                                     new BasicDBObject("$center", center)));
    }
    
    // It the weather Id has given, append weather search query
    if (this.weatherId > 0) {
      query.append("weather", this.weatherId);
    }
    
    ArrayList<Photo> photoList = new ArrayList();    
    DBCursor cursor = coll.find(query);
    try {
      while (cursor.hasNext()) {
        DBObject obj = cursor.next();
        Photo.Builder b = new Photo.Builder((String)obj.get("name"), 
                                            ((Double)obj.get("weather"))
                                              .intValue());   

        ArrayList<Double> coord = ((ArrayList<Double>)((DBObject)obj
                                    .get("geo")).get("coordinates"));
        b.geoCoord(coord.get(0), coord.get(1))
          .day(((Boolean)obj.get("day")).booleanValue())
            .timestamp(((Number)obj.get("timestamp")).longValue());
              
        photoList.add(b.build());
      }
    } finally {
       cursor.close();
    }
    return photoList;
  }
}