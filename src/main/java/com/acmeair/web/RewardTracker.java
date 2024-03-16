package com.acmeair.web;

import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.FlightService;
import com.acmeair.util.CostAndMiles;
import com.acmeair.util.MilesAndLoyaltyPoints;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class RewardTracker {

    @Inject
    FlightService flightService;

    @Inject
    CustomerService customerService;

    @Inject
    BookingService bookingService;

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

        // todo add car stuff (get car name and check if found)
        MilesAndLoyaltyPoints customerMilesAndLoyalty = customerService.getCustomerMilesAndLoyalty(userid);
        logger.warning("current miles: " + customerMilesAndLoyalty.getMiles() + " and points: "
                + customerMilesAndLoyalty.getLoyaltyPoints());

        Long totalFlightMiles = costAndMiles.getMiles() + retCostAndMiles.getMiles();
        Long totalFlightPrice = costAndMiles.getCost() + retCostAndMiles.getCost();

        // add new flight price to return array
        updatedPrices.add(getNewFlightPrice(totalFlightMiles, customerMilesAndLoyalty.getMiles(), totalFlightPrice));

        // todo add getNewCarPrice - adding 0 as new carPrice for now
        Long loyaltyPoints = 0L;
        updatedPrices.add(0L);

        // update customer miles and loyalty points
        MilesAndLoyaltyPoints updatedMilesAndLoyalty = customerService.updateCustomerMilesAndPoints(userid, totalFlightMiles, loyaltyPoints);
        logger.warning("Updated miles: " + updatedMilesAndLoyalty.getMiles());
        logger.warning("Updated loyalty: " + updatedMilesAndLoyalty.getLoyaltyPoints());

        return updatedPrices;
    }

    public Long getNewFlightPrice(Long flightMiles, Long customerMiles, Long baseCost) {
        Long milesToCheck = customerMiles + flightMiles;
        logger.warning("milesToCheck: " + milesToCheck);

        List<Integer> intIds = bookingService.getFlightRewardMapping();
        // for every id (miles) check if miles of customer are in that status level
        // if yes, get reductionPercentage for that status
        int lastId = 0;
        for (Integer id : intIds) {
            if (milesToCheck < id) {
                // return baseCost multiplied by reductionPercentage
                return adjustFlightPrice(id, baseCost);
            }
            // this happens only when id is not smaller than the miles to check i.e. is the highest id
            lastId = id;
        }
        return adjustFlightPrice(lastId, baseCost);
    }

    // takes id (miles) and a base price to calculate new price based on status (its mapped priceReduction)
    private Long adjustFlightPrice(Integer miles, Long baseCost) {
        float reductionPercentage = 0;
        JSONObject jsonObject = bookingService.getFlightRewardLevel(miles);
        reductionPercentage = jsonObject.getFloat("reduction");

        logger.warning("Found reduction for status " + jsonObject.getString("status") + " is " + reductionPercentage + "%.");

        float result = ((100 - reductionPercentage) / 100) * (float) baseCost;
        return (long) result;
    }
}
