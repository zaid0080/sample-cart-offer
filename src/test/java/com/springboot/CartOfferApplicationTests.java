package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.ApplyOfferResponse;
import com.springboot.controller.OfferRequest;
import com.springboot.utils.ApiClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CartOfferApplicationTests extends ApiClient {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String OFFER_BASE = "http://localhost:9001/api/v1/offer";
	private static final String CART_BASE  = "http://localhost:9001/api/v1/cart/apply_offer";
	private static final int RESTAURANT_1_ID = 1;
	private static final int RESTAURANT_2_ID = 2;
	private static final int USER_1_ID = 1;
	private static final int USER_2_ID = 2;
	private static final int USER_3_ID = 3;
	private static final int USER_4_ID = 4;

	// Adding all the possible offers to the restaurant before starting executing the test cases
	@BeforeClass
	public static void setupOffers() throws Exception {
		ApiClient.post(OFFER_BASE, new OfferRequest(RESTAURANT_1_ID, "FLATX", 10, Collections.singletonList("p1")));
		ApiClient.post(OFFER_BASE, new OfferRequest(RESTAURANT_1_ID, "FLATX", 20, Collections.singletonList("p2")));
		ApiClient.post(OFFER_BASE, new OfferRequest(RESTAURANT_1_ID, "FLAT%", 10, Collections.singletonList("p3")));
		ApiClient.post(OFFER_BASE, new OfferRequest(RESTAURANT_2_ID, "FLAT%", 22, Collections.singletonList("p3")));
		ApiClient.post(OFFER_BASE, new OfferRequest(RESTAURANT_2_ID, "FLATX", 10, Collections.singletonList("p3")));
	}

	// Test case to add offer
	@Test
	public void checkFlatXForOneSegment() throws Exception {
		OfferRequest offerRequest = new OfferRequest(1, "FLATX", 10, Collections.singletonList("p1"));
		String response = ApiClient.post(OFFER_BASE, offerRequest);
		Assert.assertTrue(response.contains("success"));
	}

	// Test case to verify cart value after deducting flat discount from segment P1
	@Test
	public void validateFlatAmountDiscountForP1() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(200, RESTAURANT_1_ID, USER_1_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After Discount: " +  finalCartValue);
		Assert.assertEquals("Final cart value is not matching",190, finalCartValue);
	}

	// Test case to verify cart value after deducting flat discount from segment P2
	@Test
	public void validateFlatAmountDiscountForP2() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(300, RESTAURANT_1_ID, USER_2_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After Discount: " +  finalCartValue);
		Assert.assertEquals("Final cart value is not matching",280, finalCartValue);
	}

	// Test case to verify cart value after deducting flat percentage discount from segment P3
	@Test
	public void validatePercentDiscountForP3() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(400, RESTAURANT_1_ID, USER_3_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", 360, finalCartValue);
	}

	// Test case to check backend is applying the offer which has added first
	@Test
	public void checkFirstOfferAddedDiscount() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(100, RESTAURANT_2_ID, USER_3_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After First Offer Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", 78, finalCartValue);
	}

	// Test case to check backend is able to handle large vart value
	@Test
	public void validateVeryLargeCartValue() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(1000000, RESTAURANT_1_ID, USER_3_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After First Offer Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", 900000, finalCartValue);
	}

	/*
	Test case to check when cart value is zero
	This is false positive, because in real world scenario cart can't be negative
	 */
	@Test
	public void validateDiscountWithCartValueZero() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(0, RESTAURANT_1_ID, USER_1_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value remains unchanged Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value should match", -10, finalCartValue);
	}

	// Test case to verify when cart value is equivalent to discount value, final cart value should be zero
	@Test
	public void validateCartValueEqualToDiscountValue() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(10, RESTAURANT_1_ID, USER_1_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After First Offer Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", 0, finalCartValue);
	}

	/*
	Test case to check the backend behaviour when user is not in any segment
	Backend is throwing fileNotFoundException as user id 4 is not there in the test data.
	 */
	@Test
	public void validateUserIdWithNoSegment() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(500, RESTAURANT_1_ID, USER_4_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After No Discount: " + finalCartValue);
		Assert.assertEquals("Final cart value should be unchanged", 500, finalCartValue);
	}

	/*
	Test case to check when cart value is negative
	This is also false positive, in real world scenario because should throw 400 (Bad Request)
	when cart value is negative
	*/
	@Test
	public void applyOfferWithNegativeCartValue() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(-100, RESTAURANT_1_ID, USER_2_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Cart Value After Discount " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", -120, finalCartValue);
	}

	// Test case to verify the scenario when no offer is set for a restaurant
	@Test
	public void validateNoOfferAtRestaurant() throws Exception {
		ApplyOfferRequest applyOfferRequest = new ApplyOfferRequest(100, 99, USER_2_ID);
		String resp = ApiClient.post(CART_BASE, applyOfferRequest);
		ApplyOfferResponse applyOfferResponse = MAPPER.readValue(resp, ApplyOfferResponse.class);
		int finalCartValue = applyOfferResponse.getCart_value();
		System.out.println("Unknown restaurant should not discount " + finalCartValue);
		Assert.assertEquals("Final cart value is not matching", 100, finalCartValue);
	}

	// Test case to check api is throwing correct exception in case invalid http method
	@Test
	public void invalidHttpMethod() throws Exception {
		try {
			ApiClient.get(CART_BASE);
		} catch (RuntimeException e) {
			Assert.assertTrue(e.toString().contains("invalidHttpMethod") || e.toString().contains("405"));
		}
	}
}
