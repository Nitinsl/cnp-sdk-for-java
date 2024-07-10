package io.github.vantiv.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.vantiv.sdk.generate.ApplepayHeaderType;
import io.github.vantiv.sdk.generate.ApplepayType;
import io.github.vantiv.sdk.generate.EcheckForTokenType;
import io.github.vantiv.sdk.generate.RegisterTokenRequestType;
import io.github.vantiv.sdk.generate.RegisterTokenResponse;

public class TestToken {

	private static CnpOnline cnp;

	@BeforeClass
	public static void beforeClass() throws Exception {
		cnp = new CnpOnline();
	}
	
	@Test
	public void simpleToken() throws Exception{
		RegisterTokenRequestType token = new RegisterTokenRequestType();
		token.setOrderId("12344");
		token.setAccountNumber("1233456789103801");
	    token.setId("id");
		RegisterTokenResponse response = cnp.registerToken(token);
		assertEquals("Account number was successfully registered", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}
	
	@Test
	public void simpleTokenWithPaypage() throws Exception{
		RegisterTokenRequestType token = new RegisterTokenRequestType();
		token.setOrderId("12344");
		token.setPaypageRegistrationId("1233456789101112");
	    token.setId("id");
		RegisterTokenResponse response = cnp.registerToken(token);
		assertEquals("Account number was successfully registered", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}
	
	@Test
	public void simpleTokenWithEcheck() throws Exception{
		RegisterTokenRequestType token = new RegisterTokenRequestType();
		token.setOrderId("12344");
		EcheckForTokenType echeck = new EcheckForTokenType();
		echeck.setAccNum("12344565");
		echeck.setRoutingNum("123476545");
		token.setEcheckForToken(echeck);
	    token.setId("id");
		RegisterTokenResponse response = cnp.registerToken(token);
		System.out.println(response.getCnpToken());
		assertEquals("Account number was successfully registered", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}
	
	@Test
	public void tokenEcheckMissingRequiredField() throws Exception{
		RegisterTokenRequestType token = new RegisterTokenRequestType();
		token.setOrderId("12344");
		EcheckForTokenType echeck = new EcheckForTokenType();
		echeck.setRoutingNum("123476545");
		token.setEcheckForToken(echeck);
		try {
			cnp.registerToken(token);
			fail("expected exception");
		} catch(CnpOnlineException e) {
			//assertTrue(e.getMessage(),e.getMessage().startsWith("Error validating xml data against the schema"));
		}
	}
	
	@Test
    public void simpleTokenWithApplepay() throws Exception{
        RegisterTokenRequestType token = new RegisterTokenRequestType();
        token.setOrderId("12344");
        ApplepayType applepayType = new ApplepayType();
        ApplepayHeaderType applepayHeaderType = new ApplepayHeaderType();
        applepayHeaderType.setApplicationData("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        applepayHeaderType.setEphemeralPublicKey("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        applepayHeaderType.setPublicKeyHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        applepayHeaderType.setTransactionId("1234");
        applepayType.setHeader(applepayHeaderType);
        applepayType.setData("user");
        applepayType.setSignature("sign");
        applepayType.setVersion("12345");
        token.setApplepay(applepayType);
        token.setId("id");
        RegisterTokenResponse response = cnp.registerToken(token);
        assertEquals("Account number was successfully registered", response.getMessage());
        assertEquals(Long.valueOf(0),response.getApplepayResponse().getTransactionAmount());
		assertEquals("sandbox", response.getLocation());
    }
	
	@Test
	public void convertPaypageRegistrationIdIntoToken() throws Exception {
		RegisterTokenRequestType tokenRequest = new RegisterTokenRequestType();
		tokenRequest.setOrderId("12345");
		tokenRequest.setPaypageRegistrationId("123456789012345678901324567890abcdefghi");
		tokenRequest.setId("id");
		RegisterTokenResponse tokenResponse = cnp.registerToken(tokenRequest);
		assertEquals("1111222233334444", tokenResponse.getCnpToken()); //all paypage registration ids return the same token
		assertEquals("sandbox", tokenResponse.getLocation());
	}
}

