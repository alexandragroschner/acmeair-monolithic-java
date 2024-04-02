/*******************************************************************************
* Copyright (c) 2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.mongo.services;

import static com.mongodb.client.model.Filters.eq;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.acmeair.util.CostAndMiles;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.acmeair.AirportCodeMapping;
import com.acmeair.mongo.MongoConstants;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
//import com.mongodb.async.client.*;

import com.acmeair.mongo.ConnectionManager;

import javax.print.Doc;

@ApplicationScoped
public class FlightServiceImpl extends FlightService implements  MongoConstants {

	private final static Logger logger = Logger.getLogger(FlightService.class.getName());
	private static final JsonReaderFactory factory = Json.createReaderFactory(null);
	
	private MongoCollection<Document> flight;
	private MongoCollection<Document> flightSegment;
	private MongoCollection<Document> airportCodeMapping;
	
	@Inject
	KeyGenerator keyGenerator;
	

	
	@PostConstruct
	public void initialization() {
		MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
		flight = database.getCollection("flight");
		flightSegment = database.getCollection("flightSegment");
		airportCodeMapping = database.getCollection("airportCodeMapping");
	}
	
	@Override
	public Long countFlights() {
		// REMOVED DB CALL
		// return flight.count();
		return 1L;
	}
	
	@Override
	public Long countFlightSegments() {
		// REMOVED DB CALL
		// return flightSegment.count();
		return 1L;
	}
	
	@Override
	public Long countAirports() {
		// REMOVED DB CALL
		//return airportCodeMapping.count();
		return 1L;
	}
	
	protected String getFlight(String flightId, String segmentId) {
		// REMOVED DB CALL
		// return flight.find(eq("_id", flightId)).first().toJson();

		// ADDED HARD-CODED FLIGHT
		return new Document("_id", "b7e3b028-7248-4763-b2a4-1b9c502664a1")
				.append("firstClassBaseCost", 500)
				.append("economyClassBaseCost", 200)
				.append("numFirstClassSeats", 10)
				.append("numEconomyClassSeats", 200)
				.append("airplaneTypeId", "B747")
				.append("flightSegmentId", "AA3")
				.append("scheduledDepartureTime","ISODate(\"2024-04-02T00:00:00.000Z\")")
				.append("scheduledArrivalTime", "ISODate(\"2024-04-02T00:00:00.000Z\")").toJson();
	}

	@Override
	protected  String getFlightSegment(String fromAirport, String toAirport){
		try {
			// REMOVED DB CALL
			// return flightSegment.find(new BasicDBObject("originPort", fromAirport).append("destPort", toAirport)).first().toJson();

			// ADDED HARD-CODED SEGMENT
			return new Document("_id", "AA19")
					.append("originPort", fromAirport)
					.append("destPort", toAirport)
					.append("miles", 14202).toJson();

		}catch (java.lang.NullPointerException e){
			if(logger.isLoggable(Level.FINE)){
				logger.fine("getFlghSegment returned no flightSegment available");
			}
			return "";
		}
	}
	
	@Override
	protected  List<String> getFlightBySegment(String segment, Date deptDate){
		try {
			JSONObject segmentJson = (JSONObject) new JSONParser().parse(segment);
			//MongoCursor<Document> cursor;
			List<String> flights =  new ArrayList<String>();

			if(deptDate != null) {
				if(logger.isLoggable(Level.FINE)){
					logger.fine("getFlghtBySegment Search String : " + new BasicDBObject("flightSegmentId", segmentJson.get("_id")).append("scheduledDepartureTime", deptDate).toJson());
				}
				// REMOVED DB CALL
				// cursor = flight.find(new BasicDBObject("flightSegmentId", segmentJson.get("_id")).append("scheduledDepartureTime", deptDate)).iterator();

				// ADDED HARD-CODED FLIGHT
				Document flightDoc = new Document("_id", "b7e3b028-7248-4763-b2a4-1b9c502664a1")
						.append("firstClassBaseCost", 500)
						.append("economyClassBaseCost", 200)
						.append("numFirstClassSeats", 10)
						.append("numEconomyClassSeats", 200)
						.append("airplaneTypeId", "B747")
						.append("flightSegmentId", "AA3")
						.append("scheduledDepartureTime", deptDate.toString())
						.append("scheduledArrivalTime", deptDate.toString())
						.append("flightSegment", segmentJson);

				flights.add(flightDoc.toJson());
				flights.add(flightDoc.toJson());

			} else {
				// REMOVED DB CALL
				// cursor = flight.find(eq("flightSegmentId", segmentJson.get("_id"))).iterator();

				// ADDED HARD-CODED FLIGHT
				Document flightDoc = new Document("_id", "b7e3b028-7248-4763-b2a4-1b9c502664a1")
						.append("firstClassBaseCost", 500)
						.append("economyClassBaseCost", 200)
						.append("numFirstClassSeats", 10)
						.append("numEconomyClassSeats", 200)
						.append("airplaneTypeId", "B747")
						.append("flightSegmentId", "AA3")
						.append("scheduledDepartureTime", "ISODate(\"2024-04-02T00:00:00.000Z\")")
						.append("scheduledArrivalTime", "ISODate(\"2024-04-02T14:15:00.000Z\")")
						.append("flightSegment", segmentJson);

				flights.add(flightDoc.toJson());
			}

			// REMOVED DB LOGIC
			/*
			try{
				while(cursor.hasNext()){
					Document tempDoc = cursor.next();

					if(logger.isLoggable(Level.FINE)){
						logger.fine("getFlghtBySegment Before : " + tempDoc.toJson());
					}
					
					Date deptTime = (Date)tempDoc.get("scheduledDepartureTime");
					Date arvTime = (Date)tempDoc.get("scheduledArrivalTime");
					tempDoc.remove("scheduledDepartureTime");
					tempDoc.append("scheduledDepartureTime", deptTime.toString());
					tempDoc.remove("scheduledArrivalTime");
					tempDoc.append("scheduledArrivalTime", arvTime.toString());					

					if(logger.isLoggable(Level.FINE)){
						logger.fine("getFlghtBySegment after : " + tempDoc.toJson());
					}

					flights.add(tempDoc.append("flightSegment", segmentJson).toJson());
				}
			}finally{
				cursor.close();
			}*/
			return flights;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	

	@Override
	public void storeAirportMapping(AirportCodeMapping mapping){
		Document airportDoc = new Document("_id", mapping.getAirportCode())
		        .append("airportName", mapping.getAirportName());
		airportCodeMapping.insertOne(airportDoc);
	}
	
	@Override
	public AirportCodeMapping createAirportCodeMapping(String airportCode, String airportName) {
		return new AirportCodeMapping(airportCode,airportName);
	}
	
	@Override
	public void createNewFlight(String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			int firstClassBaseCost, int economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		String id = keyGenerator.generate().toString();
		Document flightDoc = new Document("_id", id)
        .append("firstClassBaseCost", firstClassBaseCost)
        .append("economyClassBaseCost", economyClassBaseCost)
        .append("numFirstClassSeats", numFirstClassSeats)
        .append("numEconomyClassSeats", numEconomyClassSeats)
        .append("airplaneTypeId", airplaneTypeId)
        .append("flightSegmentId", flightSegmentId)
        .append("scheduledDepartureTime", scheduledDepartureTime)
        .append("scheduledArrivalTime", scheduledArrivalTime);
		
		flight.insertOne(flightDoc);
	}
	
	@Override 
	public void storeFlightSegment(String flightSeg){
		try {
			JSONObject flightSegJson = (JSONObject) new JSONParser().parse(flightSeg);
			storeFlightSegment ((String)flightSegJson.get("_id"), 
					(String)flightSegJson.get("originPort"), 
					(String)flightSegJson.get("destPort"), 
					(int)flightSegJson.get("miles"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override 
	public void storeFlightSegment(String flightName, String origPort, String destPort, int miles) {
		Document flightSegmentDoc = new Document("_id", flightName)
        .append("originPort", origPort)
        .append("destPort", destPort)
        .append("miles", miles);
		
		flightSegment.insertOne(flightSegmentDoc);
	}

	@Override
	public void dropFlights() {
		airportCodeMapping.deleteMany(new Document());
		flightSegment.deleteMany(new Document());
		flight.deleteMany(new Document());
	}

	// USER ADDED CODE

	@Override
	protected Long getRewardMilesFromSegment(String segmentId) {
		try {
			// REMOVED DB CALL
			// String segment = flightSegment.find(new BasicDBObject("_id", segmentId)).first().toJson();

			// ADDED HARD-CODED SEGMENT
			Document flightSegmentDoc = new Document("_id", "AA19")
					.append("originPort", "AKL")
					.append("destPort", "YUL")
					.append("miles", 10968);

			JsonReader jsonReader = factory.createReader(new StringReader(flightSegmentDoc.toJson()));
			JsonObject segmentJson = jsonReader.readObject();
			jsonReader.close();

			return segmentJson.getJsonNumber("miles").longValue();

		} catch (java.lang.NullPointerException e) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("getFlightSegment returned no flightSegment available");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Method to find base economy cost and bonus miles of a flight
	 * corresponding to the passed flight id.
	 * @param flightId unique key of flight in db
	 * @return base economy cost and bonus miles of the flight
	 */
	@Override
	public CostAndMiles getCostAndMilesById(String flightId) {
		//search flight by ID

		// ADDED HARD-CODED FLIGHT
		String id = "d83cf17e-336a-4336-ad03-5002537479a1";
		Document flightDoc = new Document("_id", id)
				.append("firstClassBaseCost", 500)
				.append("economyClassBaseCost", 200)
				.append("numFirstClassSeats", 10)
				.append("numEconomyClassSeats", 200)
				.append("airplaneTypeId", "B747")
				.append("flightSegmentId", "AA3")
				.append("scheduledDepartureTime", "ISODate(\"2024-04-02T00:00:00.000Z\")")
				.append("scheduledArrivalTime", "ISODate(\"2024-04-01T14:15:00.000Z\")");

		// REMOVED DB CALL
		// String flightById = flight.find(eq("_id", flightId)).first().toJson();

		JsonReader jsonReader = factory.createReader(new StringReader(flightDoc.toJson()));
		JsonObject flightByIdJson = jsonReader.readObject();
		jsonReader.close();

		CostAndMiles costAndMiles = new CostAndMiles();
		costAndMiles.setCost(flightByIdJson.getJsonNumber("economyClassBaseCost").longValue());
		costAndMiles.setMiles(getRewardMilesFromSegment(flightByIdJson.getJsonString("flightSegmentId").getString()));

		return costAndMiles;
	}
}
