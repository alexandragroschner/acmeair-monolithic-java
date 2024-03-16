package com.acmeair.mongo.services;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.acmeair.util.MilesAndLoyaltyPoints;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.Document;

import com.acmeair.mongo.MongoConstants;
import com.acmeair.service.BookingService;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
//import com.mongodb.async.client.*;

import com.acmeair.mongo.ConnectionManager;
import org.json.JSONObject;

@ApplicationScoped
public class BookingServiceImpl implements BookingService, MongoConstants {

	private final static Logger logger = Logger.getLogger(BookingService.class.getName());
	private MongoCollection<Document> booking;
	private MongoCollection<Document> rewardFlightCollection;
	private MongoCollection<Document> rewardCarCollection;

	@Inject
	KeyGenerator keyGenerator;

	@PostConstruct
	public void initialization() {
		MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
		booking = database.getCollection("booking");
		rewardFlightCollection = database.getCollection("rewardFlight");
		rewardCarCollection = database.getCollection("rewardCar");

		try {
			loadRewardDbs();
		} catch (Exception e) {
			logger.warning("Error in initialization of BookingServiceImpl" + e);
		}
	}

	public String bookFlight(String customerId, String flightId, String retFlightId, String price) {
		try {

			String bookingId = keyGenerator.generate().toString();

			Document bookingDoc = new Document("_id", bookingId)
					.append("customerId", customerId)
					.append("flightId", flightId)
					.append("retFlightId", retFlightId)
					.append("carBooked", "NONE")
					.append("dateOfBooking",  new Date())
					.append("flightPrice", price)
					.append("carPrice", 0)
					.append("totalPrice", price);

			booking.insertOne(bookingDoc);
			return bookingId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String bookFlight(String customerId, String flightSegmentId, String flightId,
							 String retFlightId, String price) {

		if (flightSegmentId == null) {
			return bookFlight(customerId, flightId, retFlightId, price);
		} else {
			try {
				String bookingId = keyGenerator.generate().toString();

				Document bookingDoc = new Document("_id", bookingId)
						.append("customerId", customerId)
						.append("flightId", flightId)
						.append("retFlightId", retFlightId)
						.append("carBooked", "NONE")
						.append("dateOfBooking", new Date())
						.append("flightPrice", price)
						.append("carPrice", "0")
						.append("totalPrice", price);
				booking.insertOne(bookingDoc);
				return bookingId;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public String getBooking(String user, String bookingId) {
		try{
			return booking.find(eq("_id", bookingId)).first().toJson();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getBookingsByUser(String user) {
		List<String> bookings = new ArrayList<String>();
		if(logger.isLoggable(Level.FINE)){
			logger.fine("getBookingsByUser : " + user);
		}
		try (MongoCursor<Document> cursor = booking.find(eq("customerId", user)).iterator()){

			while (cursor.hasNext()){
				Document tempBookings = cursor.next();
				Date dateOfBooking = (Date)tempBookings.get("dateOfBooking");
				tempBookings.remove("dateOfBooking");
				tempBookings.append("dateOfBooking", dateOfBooking.toString());

				if(logger.isLoggable(Level.FINE)){
					logger.fine("getBookingsByUser cursor data : " + tempBookings.toJson());
				}
				bookings.add(tempBookings.toJson());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return bookings;
	}

	@Override
	public void cancelBooking(String user, String bookingId) {
		if(logger.isLoggable(Level.FINE)){
			logger.fine("cancelBooking _id : " + bookingId);
		}
		try{
			booking.deleteMany(eq("_id", bookingId));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Long count() {
		return booking.count();
	}

	@Override
	public void dropBookings() {
		booking.deleteMany(new Document());
	}

	@Override
    public String getServiceType() {
      return "mongo";
    }

	// USER ADDED CODE
	@Override
	public String bookFlightWithCar(String customerId, String flightSegmentId, String flightId, String retFlightId,
									String carName, String totalPrice, String flightPrice, String carPrice) {
		try {
			String bookingId = keyGenerator.generate().toString();

			Document bookingDoc = new Document("_id", bookingId)
					.append("customerId", customerId)
					.append("flightId", flightId)
					.append("retFlightId", retFlightId)
					.append("carBooked", carName)
					.append("dateOfBooking", new Date())
					.append("flightPrice", flightPrice)
					.append("carPrice", carPrice)
					.append("totalPrice", totalPrice);

			booking.insertOne(bookingDoc);
			return bookingId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Integer> getFlightRewardMapping() {
		// from https://stackoverflow.com/a/42696322
		List<String> ids = StreamSupport.stream(rewardFlightCollection.distinct("_id", String.class).spliterator(),
				false).collect(Collectors.toList());

		logger.warning("Got all ids of flight rewards");

		return getSortedIds(ids);
	}
	private static List<Integer> getSortedIds(List<String> ids) {
		List<Integer> intIds = new ArrayList<>();
		for (String id : ids) {
			intIds.add(Integer.valueOf(id));
		}

		// sort ids ascending
		Collections.sort(intIds);
		return intIds;
	}

	@Override
	public void loadRewardDbs() throws IOException {
		loadDb(rewardFlightCollection, "/milesstatusmapping.csv");
		loadDb(rewardCarCollection, "/loyaltypointsstatusmapping.csv");
	}

	@Override
	public void loadDb(MongoCollection<Document> collection, String resource) throws IOException {

		if (collection.countDocuments() != 0) {
			logger.warning("Loading booking db aborted. Database is not empty!");
			return;
		}

		InputStream csvInputStream = BookingServiceImpl.class.getResourceAsStream(resource);

		assert csvInputStream != null;
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(csvInputStream));

		while (true) {
			String line = lnr.readLine();
			// end reading lines when EOF
			if (line == null || line.trim().isEmpty()) {
				break;
			}
			StringTokenizer st = new StringTokenizer(line, ",");
			ArrayList<String> lineAsStringArray = new ArrayList<>();

			// adds value of every column of current line to array as string
			while (st.hasMoreTokens()) {
				lineAsStringArray.add(st.nextToken());
			}
			logger.warning("Inserting values for status: " + lineAsStringArray.get(1));

			collection.insertOne(new Document("_id", lineAsStringArray.get(0))
					.append("status", lineAsStringArray.get(1))
					.append("reduction", lineAsStringArray.get(2)));
		}
	}

	@Override
	public JSONObject getFlightRewardLevel(Integer id) {
		try {
			return new JSONObject(rewardFlightCollection.find(eq("_id", id.toString())).first().toJson());
		} catch (NullPointerException e) {
			logger.warning("Did not find flightRewardMapping for " + id);
			throw new RuntimeException();
		}
	}
}
