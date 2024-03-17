package com.acmeair.service;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface RewardService {

    void loadRewardDbs() throws IOException;

    void loadDb(MongoCollection<Document> collection, String resource) throws IOException;

    Long getNewCarPrice(Long carLoyalty, Long customerLoyalty, Long carBaseCost);

    Long getNewFlightPrice(Long flightMiles, Long customerMiles, Long baseCost);
}
