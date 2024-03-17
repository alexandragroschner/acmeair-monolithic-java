package com.acmeair.mongo.services;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.Document;

import com.acmeair.mongo.MongoConstants;
import com.acmeair.service.BookingService;
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
	@Inject
	KeyGenerator keyGenerator;

	@PostConstruct
	public void initialization() {
		MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
		booking = database.getCollection("booking");
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

}
