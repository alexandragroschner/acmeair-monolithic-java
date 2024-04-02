package com.acmeair.mongo.services;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.acmeair.util.MilesAndLoyaltyPoints;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.bson.Document;

import com.acmeair.mongo.MongoConstants;
import com.acmeair.service.CustomerService;
import com.acmeair.web.dto.CustomerInfo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.acmeair.mongo.ConnectionManager;
import org.json.JSONObject;
//import org.json.simple.JSONObject;

import java.util.Objects;
import java.util.logging.Logger;


@ApplicationScoped
public class CustomerServiceImpl extends CustomerService implements MongoConstants {	
		
//	private final static Logger logger = Logger.getLogger(CustomerService.class.getName()); 
	
	private MongoCollection<Document> customer;
	private MongoCollection<Document> customerRewardData;
	private static final Logger logger = Logger.getLogger(CustomerService.class.getName());
	
	@PostConstruct
	public void initialization() {	
		MongoDatabase database = ConnectionManager.getConnectionManager().getDB();
		customer = database.getCollection("customer");
		customerRewardData = database.getCollection("customerRewardData");
	}
	
	@Override
	public Long count() {
		return customer.count();
	}
		
	@Override
	public void createCustomer(String username, String password,
			String phoneNumber, String phoneNumberType,
			String addressJson) {

		new Document();
		Document customerDoc = new Document("_id", username)
        .append("password", password)
        .append("address", Document.parse(addressJson))
        .append("phoneNumber", phoneNumber)
        .append("phoneNumberType", phoneNumberType);
		
		customer.insertOne(customerDoc);
	}
	
	@Override 
	public String createAddress (String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode){
		Document addressDoc = new Document("streetAddress1", streetAddress1)
		   .append("city", city)
		   .append("stateProvince", stateProvince)
		   .append("country", country)
		   .append("postalCode", postalCode);
		if (streetAddress2 != null){
			addressDoc.append("streetAddress2", streetAddress2);
		}
		return addressDoc.toJson();
	}

	@Override
	public void updateCustomer(String username, CustomerInfo customerInfo) {
		
		Document address = new Document("streetAddress1", customerInfo.getAddress().getStreetAddress1())
		   .append("city", customerInfo.getAddress().getCity())
		   .append("stateProvince", customerInfo.getAddress().getStateProvince())
		   .append("country", customerInfo.getAddress().getCountry())
		   .append("postalCode", customerInfo.getAddress().getPostalCode());

		if (customerInfo.getAddress().getStreetAddress2() != null){
			address.append("streetAddress2", customerInfo.getAddress().getStreetAddress2());
		}
		customer.updateOne(eq("_id", customerInfo.get_id()), 
				combine(set("address", address),
						set("phoneNumber", customerInfo.getPhoneNumber()),
						set("phoneNumberType", customerInfo.getPhoneNumberType())));
	}

	@Override
	protected String getCustomer(String username) {					
		return customer.find(eq("_id", username)).first().toJson();
	}
	
	@Override
	public String getCustomerByUsername(String username) {
		Document customerDoc = customer.find(eq("_id", username)).first();
		MilesAndLoyaltyPoints milesAndLoyaltyPoints = getCustomerMilesAndLoyalty(username);
		if (customerDoc != null) {
			customerDoc.remove("password");
			customerDoc.append("password", null);
			customerDoc.append("total_miles", milesAndLoyaltyPoints.getMiles().toString());
			customerDoc.append("loyaltyPoints", milesAndLoyaltyPoints.getLoyaltyPoints().toString());
		}
		return customerDoc.toJson();
	}

	@Override
	public void dropCustomers() {
		customer.deleteMany(new Document());
		
	}

	// USER ADDED CODE
	@Override
	public MilesAndLoyaltyPoints getCustomerMilesAndLoyalty (String username) {

		MilesAndLoyaltyPoints customerMilesAndLoyaltyReturn = new MilesAndLoyaltyPoints();

		/* REMOVED DB CALL
		Document doc = customerRewardData.find(eq("_id", customerId)).first();
		*/
		// ADDED HARD-CODED DOC
		Document doc = new Document();

        if (Objects.isNull(null)) {
			logger.warning("Reward data for customer " + username + " does not exist - Inserting now...");

			Document customerRewardDataEntry = new Document("_id", username)
					.append("miles", "0")
					.append("loyaltyPoints", "0");
			/* REMOVED DB CALL
			customerRewardData.insertOne(customerRewardDataEntry);
			 */
			customerMilesAndLoyaltyReturn.setMiles(0L);
			customerMilesAndLoyaltyReturn.setLoyaltyPoints(0L);
			return customerMilesAndLoyaltyReturn;
		} else {
			logger.warning("Existing data for user found in customerRewardData");
			JSONObject jsonObject = new JSONObject(doc.toJson());

			customerMilesAndLoyaltyReturn.setMiles(Long.parseLong(jsonObject.getString("miles")));
			customerMilesAndLoyaltyReturn.setLoyaltyPoints(Long.parseLong(jsonObject.getString("loyaltyPoints")));
			return customerMilesAndLoyaltyReturn;
		}

	}

	@Override
	public MilesAndLoyaltyPoints updateCustomerMilesAndPoints(String customerId, Long miles, Long loyaltyPoints) {

		/* REMOVED DB CALL
		Document doc = customerRewardData.find(eq("_id", customerId)).first();
		*/
		// ADDED HARD-CODED DOC
		Document doc = new Document();

		MilesAndLoyaltyPoints updatedMilesAndPoints = new MilesAndLoyaltyPoints(0L, 0L);
		// if customer does not exist, create customer with 0
		if (Objects.isNull(null)) {
			logger.warning("Reward data for customer " + customerId + " does not exist - Inserting now...");

			Document customerRewardDataEntry = new Document("_id", customerId)
					.append("miles", miles.toString())
					.append("loyaltyPoints", loyaltyPoints.toString());

			updatedMilesAndPoints.setMiles(miles);
			updatedMilesAndPoints.setLoyaltyPoints(loyaltyPoints);

			/* REMOVED DB CALL
			customerRewardData.insertOne(customerRewardDataEntry);
			 */
		} else {
			logger.warning("Existing data for user found in customerRewardData");
			JSONObject jsonObject = new JSONObject(doc.toJson());

			updatedMilesAndPoints.setMiles(jsonObject.getLong("miles") + miles);
			updatedMilesAndPoints.setLoyaltyPoints(jsonObject.getLong("loyaltyPoints") + loyaltyPoints);

			/* REMOVED DB CALL
			customerRewardData.updateOne(eq("_id", customerId),
					combine(set("miles", updatedMilesAndPoints.getMiles().toString()),
							set("loyaltyPoints", updatedMilesAndPoints.getLoyaltyPoints().toString())));

			 */
		}
		return updatedMilesAndPoints;

	}
	
}
