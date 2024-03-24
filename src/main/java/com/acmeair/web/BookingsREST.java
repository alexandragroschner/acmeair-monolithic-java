/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
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
package com.acmeair.web;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.acmeair.service.BookingService;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Path("/api/bookings")
public class BookingsREST {

	@Inject
	private BookingService bs;

	@Inject
	RewardTracker rewardTracker;

	private static final Logger logger = Logger.getLogger(BookingsREST.class.getName());

	private static final JsonReaderFactory factory = Json.createReaderFactory(null);
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("/bookflights")
	@Produces("text/plain")
	public /*BookingInfo*/ Response bookFlights(
			@FormParam("userid") String userid,
			@FormParam("toFlightId") String toFlightId,
			@FormParam("toFlightSegId") String toFlightSegId,
			@FormParam("retFlightId") String retFlightId,
			@FormParam("retFlightSegId") String retFlightSegId,
			@FormParam("oneWayFlight") boolean oneWay) {
		return bookFlightsAndCar(userid, toFlightId, toFlightSegId, retFlightId, retFlightSegId, oneWay, null);
	}
		
	@GET
	@Path("/byuser/{user}")
	@Produces("text/plain")
	public Response getBookingsByUser(@PathParam("user") String user) {
		try {
			return  Response.ok(bs.getBookingsByUser(user).toString()).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}


	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("/cancelbooking")
	@Produces("text/plain")
	public Response cancelBookingsByNumber(
			@FormParam("number") String number,
			@FormParam("userid") String userid) {
		try {
			JsonObject booking;

			try {
				JsonReader jsonReader = factory.createReader(new StringReader(bs
						.getBooking(userid, number)));
				booking = jsonReader.readObject();
				jsonReader.close();

				bs.cancelBooking(userid, number);
			}
			catch (RuntimeException e) {
				// Booking has already been deleted...
				return Response.ok("booking " + number + " deleted.").build();
			}

			boolean isOneWay = booking.getString("retFlightId").equals("NONE - ONE WAY FLIGHT");

			if (booking.getString("carBooked").equalsIgnoreCase("none")) {
				rewardTracker.updateRewardMiles(userid, booking.getString("flightId"),
						booking.getString("retFlightId"), false, null, isOneWay);
			} else {
				rewardTracker.updateRewardMiles(userid, booking.getString("flightId"),
						booking.getString("retFlightId"), false, booking.getString("carBooked"), isOneWay);
			}

			return Response.ok("booking " + number + " deleted.").build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
  	public Response status() {
    	return Response.ok("OK").build();
  	}

	  // USER ADDED CODE

	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("/bookflightsandcar")
	@Produces("text/plain")
	public Response bookFlightsAndCar(@QueryParam("userid") String userid,
									  @QueryParam("toFlightId") String toFlightId,
									  @QueryParam("toFlightSegId") String toFlightSegId,
									  @QueryParam("retFlightId") String retFlightId,
									  @QueryParam("retFlightSegId") String retFlightSegId,
									  @QueryParam("oneWayFlight") boolean oneWay,
									  @QueryParam("carname") String carName) {

		try {
			List<Long> newPrices;
			String bookingId;
			Long totalPrice;

			boolean carBooked = Objects.nonNull(carName) && !carName.equals("null") && !carName.trim().isEmpty();

			if (!oneWay) {
				logger.warning("Booking is not one way.");

				newPrices = rewardTracker.updateRewardMiles(userid, toFlightId, retFlightId, true, carName, oneWay);

				logger.warning("new flight price is: " + newPrices.get(0));
				logger.warning("new car price is: " + newPrices.get(1));

				if (carBooked) {
					logger.warning("Booking includes car.");
					totalPrice = newPrices.get(0) + newPrices.get(1);
					bookingId = bs.bookFlightWithCar(userid, toFlightSegId, toFlightId, retFlightId, carName,
							// totalPrice
							totalPrice.toString(),
							// newFlightPrice
							newPrices.get(0).toString(),
							// newCarPrice (0 if no car)
							newPrices.get(1).toString());
				} else {
					logger.warning("Booking includes no car.");
					bookingId = bs.bookFlight(userid, toFlightSegId, toFlightId, retFlightId, newPrices.get(0).toString());
				}
			} else {
				logger.warning("Booking is one way.");

				newPrices = rewardTracker.updateRewardMiles(userid, toFlightId,null, true, carName, true);

				if (carBooked) {
					logger.warning("Booking includes car.");
					totalPrice = newPrices.get(0) + newPrices.get(1);
					bookingId = bs.bookFlightWithCar(userid, toFlightSegId, toFlightId, "NONE - ONE WAY FLIGHT", carName,
							// totalPrice
							totalPrice.toString(),
							// newFlightPrice
							newPrices.get(0).toString(),
							// newCarPrice (0 if no car)
							newPrices.get(1).toString());
				} else {
					logger.warning("Booking includes no car.");
					bookingId = bs.bookFlight(userid, toFlightSegId, toFlightId, "NONE - ONE WAY FLIGHT", newPrices.get(0).toString());
				}
			}

			String bookingInfo = "{\"oneWay\":\"" + oneWay
					+ "\",\"price\":\"" + (newPrices.get(0) + newPrices.get(1))
					+ "\",\"flightPrice\":\"" + newPrices.get(0)
					+ "\",\"carPrice\":\"" + newPrices.get(1)
					+ "\",\"bookingId\":\"" + bookingId
					+ "\",\"carBooked\":\"" + (Objects.nonNull(carName) ? carName : "NONE")
					+ "\"}";

			return Response.ok(bookingInfo).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

	}

}