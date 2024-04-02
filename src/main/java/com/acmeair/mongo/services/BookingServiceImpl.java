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

			// REMOVED DB CALL
			// booking.insertOne(bookingDoc);
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

				// REMOVED DB CALL
				// booking.insertOne(bookingDoc);
				return bookingId;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public String getBooking(String user, String bookingId) {
		try{
			// ADDED HARD-CODED BOOKING
			Document bookingDoc = new Document("_id", "1531fd5c-6b7d-43df-8b29-55b39edc53f1")
					.append("customerId", "uid0@email.com")
					.append("flightId", "fe47d42c-a85c-4917-afc2-a3a1452fe10b")
					.append("retFlightId", "2bb491fd-4580-47c1-923e-36a2ca8371f4")
					.append("carBooked", "NONE")
					.append("dateOfBooking", new Date())
					.append("flightPrice", "320")
					.append("carPrice", "0")
					.append("totalPrice", "320");
			JSONObject jsonObject = new JSONObject(bookingDoc.toJson());
			// REMOVED DB CALL
			// return booking.find(eq("_id", bookingId)).first().toJson();
			return new JSONObject(bookingDoc.toJson()).toString();
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
		// ADDED HARD-CODED BOOKING
		Document bookingDoc = new Document("_id", "1531fd5c-6b7d-43df-8b29-55b39edc53f1")
				.append("customerId", "uid0@email.com")
				.append("flightId", "fe47d42c-a85c-4917-afc2-a3a1452fe10b")
				.append("retFlightId", "2bb491fd-4580-47c1-923e-36a2ca8371f4")
				.append("carBooked", "NONE")
				.append("dateOfBooking", new Date())
				.append("flightPrice", "320")
				.append("carPrice", "0")
				.append("totalPrice", "320");

		bookings.add(bookingDoc.toJson());
		/* REMOVED DB CALL
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
		}*/
		return bookings;
	}

	@Override
	public void cancelBooking(String user, String bookingId) {
		if(logger.isLoggable(Level.FINE)){
			logger.fine("cancelBooking _id : " + bookingId);
		}
		/* REMOVED DB CALL
		try{
			booking.deleteMany(eq("_id", bookingId));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		 */
	}

	@Override
	public Long count() {
		// REMOVED DB CALL
		// return booking.count();
		return 1L;
	}

	@Override
	public void dropBookings() {
		// REMOVED DB CALL
		// booking.deleteMany(new Document());
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

			// REMOVED DB CALL
			// booking.insertOne(bookingDoc);
			return bookingId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
