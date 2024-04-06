package com.acmeair.mongo.services;

import com.acmeair.mongo.ConnectionManager;
import com.acmeair.service.RewardService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class RewardServiceImpl implements RewardService {
    private MongoCollection<Document> rewardFlightCollection;
    private MongoCollection<Document> rewardCarCollection;
    private final static Logger logger = Logger.getLogger(RewardService.class.getName());
    private static final long MINIMUM_CAR_PRICE = 25;

    @PostConstruct
    public void initialization() {
        MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
        rewardFlightCollection = database.getCollection("rewardFlight");
        rewardCarCollection = database.getCollection("rewardCar");

        try {
            loadRewardDbs();
        } catch (Exception e) {
            logger.warning("Error in initialization of RewardServiceImpl" + e);
        }
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

    private List<Integer> getFlightRewardMapping() {
        /* REMOVED DB CALL
        // from https://stackoverflow.com/a/42696322
        List<String> ids = StreamSupport.stream(rewardFlightCollection.distinct("_id", String.class).spliterator(),
                false).collect(Collectors.toList());
         */
        // ADDED HARD-CODED IDS
        List<String> ids = new ArrayList<>();
        ids.add("700");
        ids.add("1400");
        ids.add("3500");
        ids.add("8000");

        logger.warning("Got all ids of flight rewards");

        return getSortedIds(ids);
    }

    private List<Integer> getCarRewardMapping() {
        /* REMOVED DB CALL
        // from https://stackoverflow.com/a/42696322
        List<String> ids = StreamSupport.stream(rewardCarCollection.distinct("_id", String.class).spliterator(),
                false).collect(Collectors.toList());
         */
        // ADDED HARD-CODED IDS
        List<String> ids = new ArrayList<>();
        ids.add("150");
        ids.add("600");
        ids.add("900");
        ids.add("1200");

        logger.warning("Got all ids of flight rewards");

        return getSortedIds(ids);
    }

    private List<Integer> getSortedIds(List<String> ids) {
        List<Integer> intIds = new ArrayList<>();
        for (String id : ids) {
            intIds.add(Integer.valueOf(id));
        }

        // sort ids ascending
        Collections.sort(intIds);
        return intIds;
    }

    private JSONObject getFlightRewardLevel(Integer id) {
        /* REMOVED DB CALL
        try {
            return new JSONObject(rewardFlightCollection.find(eq("_id", id.toString())).first().toJson());
        } catch (NullPointerException e) {
            logger.warning("Did not find flightRewardMapping for " + id);
            throw new RuntimeException();
        }
         */

        // ADDED HARD-CODED REWARD MAPPING
        Document flightRewardMappingDoc = new Document("_id", id)
                .append("status", "peasant")
                .append("reduction", "0");
        return new JSONObject(flightRewardMappingDoc);
    }

    private JSONObject getCarRewardLevel(Integer id) {
        /* REMOVED DB CALL
        try {
            return new JSONObject(rewardCarCollection.find(eq("_id", id.toString())).first().toJson());
        } catch (NullPointerException e) {
            logger.warning("Did not find carRewardMapping for " + id);
            throw new RuntimeException();
        }
         */
        // ADDED HARD-CODED REWARD MAPPING
        Document carRewardMappingDoc = new Document("_id", id)
                .append("status", "walker")
                .append("reduction", "0");
        return new JSONObject(carRewardMappingDoc);
    }

    @Override
    public Long getNewCarPrice(Long carLoyalty, Long customerLoyalty, Long carBaseCost) {
        Long pointsToCheck = customerLoyalty + carLoyalty;
        logger.warning("pointsToCheck: " + pointsToCheck);

        List<Integer> rewardLevels = getCarRewardMapping();
        // for every id (miles) check if miles of customer are in that status level
        // if yes, get reductionPercentage for that status
        int lastId = 0;
        for (Integer id : rewardLevels) {
            if (pointsToCheck < id) {
                // return baseCost multiplied by reductionPercentage
                return adjustCarPrice(id, carBaseCost);
            }
            // this happens only when id is not smaller than the miles to check i.e. is the highest id
            lastId = id;
        }
        return adjustCarPrice(lastId, carBaseCost);
    }

    // takes id (loyalty points) and a base price to calculate new price based on status (its mapped priceReduction)
    private long adjustCarPrice(Integer loyaltyPoints, Long baseCost) {
        long priceReduction = 0;
        JSONObject jsonObject = getCarRewardLevel(loyaltyPoints);
        priceReduction = jsonObject.getLong("reduction");

        logger.warning("Found reduction for status " + jsonObject.getString("status") + " is " + priceReduction + "€.");

        long result = baseCost - priceReduction;
        return Math.max(result, MINIMUM_CAR_PRICE);
    }

    @Override
    public Long getNewFlightPrice(Long flightMiles, Long customerMiles, Long baseCost) {
        Long milesToCheck = customerMiles + flightMiles;
        logger.warning("milesToCheck: " + milesToCheck);

        List<Integer> intIds = getFlightRewardMapping();
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
        JSONObject jsonObject = getFlightRewardLevel(miles);
        reductionPercentage = jsonObject.getFloat("reduction");

        logger.warning("Found reduction for status " + jsonObject.getString("status") + " is " + reductionPercentage + "%.");

        float result = ((100 - reductionPercentage) / 100) * (float) baseCost;
        return (long) result;
    }
}
