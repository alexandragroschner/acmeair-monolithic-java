package com.acmeair.mongo.services;

import com.acmeair.mongo.ConnectionManager;
import com.acmeair.mongo.MongoConstants;
import com.acmeair.service.CarService;
import com.acmeair.web.dto.Car;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.json.JSONObject;

import javax.print.Doc;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class CarServiceImpl implements MongoConstants, CarService {

    private MongoCollection<Document> carCollection;
    private static final Logger logger = Logger.getLogger(CarServiceImpl.class.getName());

    @PostConstruct
    public void initialization() {
        MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
        carCollection = database.getCollection("car");
        try {
            loadCarDb();
        } catch (Exception e) {
            logger.warning("Error during initialization of CarServiceImpl: " + e);
        }
    }
    @Override
    public Car getCar(String id) {
        try {
            /* REMOVED DB CALL
            try  {
                jsonObject = new JSONObject(carCollection.find(eq("_id", id)).first().toJson());
            } catch (Exception e) {
                logger.warning("Could not get car from db");
            }
             */
            // ADDED HARD-CODED CAR
            Document carDoc = new Document("_id", id)
                    .append("carName", "trabant")
                    .append("baseCost", "100")
                    .append("loyaltyPoints", "20");
            JSONObject jsonObject = new JSONObject(carDoc);
            return new Car(id, jsonObject.getString("carName"),
                    Integer.parseInt(jsonObject.getString("baseCost")),
                    Long.parseLong(jsonObject.getString("loyaltyPoints")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Car getCarByName(String name) {
        try {
            /* REMOVED DB CALL
            try  {
                jsonObject = new JSONObject(carCollection.find(eq("carName", name.toLowerCase())).first().toJson());
            } catch (Exception e) {
                logger.warning("Could not get car from db with name " + name);
            }
             */
            // ADDED HARD-CODED CAR
            Document carDoc = new Document("_id", "98e7c77c-53af-4a1f-9cb7-b6ae57ac7cd4")
                    .append("carName", "trabant")
                    .append("baseCost", "50")
                    .append("loyaltyPoints", "10");
            JSONObject jsonObject = new JSONObject(carDoc);
            return new Car(jsonObject.getString("_id"), name,
                    Integer.parseInt(jsonObject.getString("baseCost")),
                    Long.parseLong(jsonObject.getString("loyaltyPoints")));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String insertCar(String name, String baseCost, String loyaltyPoints) {
        String id = java.util.UUID.randomUUID().toString();
        try {
            Document carDoc = new Document("_id", id)
                    .append("carName", name)
                    .append("baseCost", baseCost)
                    .append("loyaltyPoints", loyaltyPoints);
            carCollection.insertOne(carDoc);

            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void purgeDb() {
        for (Document d : carCollection.find()) {
            carCollection.deleteOne(d);
        }
        logger.warning("Purged DB: Amount of documents left in reward collection: " + carCollection.countDocuments());
    }

    @Override
    public void loadCarDb() throws IOException {
        if (carCollection.countDocuments() != 0) {
            logger.warning("Loading reward db aborted. Database is not empty!");
            return;
        }

        InputStream csvInputStream = CarServiceImpl.class.getResourceAsStream("/cars.csv");

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

            String id = java.util.UUID.randomUUID().toString();
            logger.warning("Inserting car: " + lineAsStringArray.get(0));

            carCollection.insertOne(new Document("_id", id)
                    .append("carName", lineAsStringArray.get(0))
                    .append("baseCost", lineAsStringArray.get(1))
                    .append("loyaltyPoints", lineAsStringArray.get(2)));
        }
    }
}
