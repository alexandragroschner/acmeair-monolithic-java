package com.acmeair.web;

import com.acmeair.service.*;
import com.acmeair.util.CostAndMiles;
import com.acmeair.util.MilesAndLoyaltyPoints;
import com.acmeair.web.dto.Car;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@ApplicationScoped
public class RewardTracker {
    @Inject
    FlightService flightService;
    @Inject
    RewardService rewardService;
    @Inject
    CustomerService customerService;
    @Inject
    CarService carService;

    private static final Logger logger = Logger.getLogger(RewardTracker.class.getName());

    public List<Long> updateRewardMiles(String userid, String flightId, String retFlightId, boolean add,
                                        String carName, boolean isOneWay) {

        // this will be the response and contain the updated flight price and updated car price (if no car -> null)
        List<Long> updatedPrices = new ArrayList<>();


        CostAndMiles costAndMiles = flightService.getCostAndMilesById(flightId);
        CostAndMiles retCostAndMiles = new CostAndMiles();

        if (isOneWay) {
            // make an empty return miles response if no return flight
            logger.warning("Creating empty Return Flight CostAndMilesResponse due one way booking");
            retCostAndMiles.setMiles(0L);
            retCostAndMiles.setCost(0L);
        } else {
            retCostAndMiles = flightService.getCostAndMilesById(retFlightId);
        }

        Car carToBook = null;
        if (Objects.nonNull(carName)) {
            carToBook = carService.getCarByName(carName);
            logger.warning("Called car service for car: " + carName);
            if (carToBook == null) {
                logger.warning("carToBook is null - could not find car with name: " + carName);
                return null;
            }
        }

        MilesAndLoyaltyPoints customerMilesAndLoyalty = customerService.getCustomerMilesAndLoyalty(userid);
        logger.warning("current miles: " + customerMilesAndLoyalty.getMiles() + " and points: "
                + customerMilesAndLoyalty.getLoyaltyPoints());

        Long totalFlightMiles = costAndMiles.getMiles() + retCostAndMiles.getMiles();
        Long totalFlightPrice = costAndMiles.getCost() + retCostAndMiles.getCost();

        // add new flight price to return array
        updatedPrices.add(rewardService.getNewFlightPrice(totalFlightMiles, customerMilesAndLoyalty.getMiles(), totalFlightPrice));

        Long loyaltyPoints = 0L;
        if (Objects.nonNull(carToBook)) {
            Long newCarPrice = rewardService.getNewCarPrice(carToBook.getLoyaltyPoints(),
                    customerMilesAndLoyalty.getLoyaltyPoints(), (long) carToBook.getBaseCost());

            logger.warning("new car price is " + newCarPrice);
            loyaltyPoints = carToBook.getLoyaltyPoints();
            updatedPrices.add(newCarPrice);
        } else {
            // add 0 as car price if no car is booked
            logger.warning("adding 0 as car price (no car booked)");
            updatedPrices.add(0L);
        }


        // update customer miles and loyalty points
        MilesAndLoyaltyPoints updatedMilesAndLoyalty = customerService.updateCustomerMilesAndPoints(userid, totalFlightMiles, loyaltyPoints);
        logger.warning("Updated miles: " + updatedMilesAndLoyalty.getMiles());
        logger.warning("Updated loyalty: " + updatedMilesAndLoyalty.getLoyaltyPoints());

        return updatedPrices;
    }

}
