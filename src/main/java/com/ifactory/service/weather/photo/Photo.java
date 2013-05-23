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

import java.io.Serializable;

public class Photo implements Serializable {
  private String name;
	private boolean day;
	private int weatherId;
	private double lat;
	private double lng;
  private long timestamp;
	
	static final class Builder {
    private String name;
  	private boolean day;
  	private int weatherId;
  	private double lat;
  	private double lng;
    private long timestamp;
		
		public Builder(String name, int weatherId) {
			this.name = name;
			this.weatherId = weatherId;
		}

		public Builder day(boolean day) {
			this.day = day;
			return this;
		}
		
		public Builder geoCoord(double lat, double lng) {
			this.lat = lat;
			this.lng = lng;
			return this;
		}
		
		public Builder timestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}
				
		public Photo build() {
			return new Photo(this);
		}
	}
	
	private Photo(Builder builder) {
		name = builder.name;
		weatherId = builder.weatherId;
		day = builder.day;
		lat = builder.lat;			
		lng = builder.lng;			
		timestamp = builder.timestamp;			
	}	
	
	public boolean getDay() {
		return this.day;
	}
	
	public String getName() {
		return this.name;
	}

	public int getWeatherId() {
		return this.weatherId;
	}
	
	public double getLatitude() {
	  return this.lat;
	}
	
	public double getLongitude() {
	  return this.lng;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
}