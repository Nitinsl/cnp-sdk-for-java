package io.github.vantiv.sdk;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.vantiv.sdk.generate.AdvancedFraudChecksType;
import io.github.vantiv.sdk.generate.AdvancedFraudResultsType;
import io.github.vantiv.sdk.generate.FraudCheck;
import io.github.vantiv.sdk.generate.FraudCheckResponse;

import java.util.Random;

public class TestFraudCheck {

    private static CnpOnline cnp;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cnp = new CnpOnline();
    }
    
    @Test
    public void testFraudCheck() throws Exception {
        FraudCheck fraudCheck = new FraudCheck();
        fraudCheck.setId("1");
        AdvancedFraudChecksType advancedFraudChecks = new AdvancedFraudChecksType();
        advancedFraudChecks.setThreatMetrixSessionId("123");
        advancedFraudChecks.setCustomAttribute1("pass");
        advancedFraudChecks.setCustomAttribute2("42");
        advancedFraudChecks.setCustomAttribute3("5");
//        advancedFraudChecks.setWebSessionId("abcd1234");
        fraudCheck.setAdvancedFraudChecks(advancedFraudChecks);
//        fraudCheck.setEventType("payment");
        fraudCheck.setAccountLogin("Java");
        String hash = "";
        Random rand = new Random();
        for (int i = 0; i < 56; i++){
            hash += rand.nextInt(10);
        }
        fraudCheck.setAccountPasshash(hash);
        FraudCheckResponse fraudCheckResponse = cnp.fraudCheck(fraudCheck);
        
        //System.out.println(fraudCheckResponse.getMessage());
        
        AdvancedFraudResultsType advancedFraudResultsType = fraudCheckResponse.getAdvancedFraudResults();
        assertEquals("pass", advancedFraudResultsType.getDeviceReviewStatus());
        assertEquals(Integer.valueOf(42), advancedFraudResultsType.getDeviceReputationScore());
        assertEquals(5, advancedFraudResultsType.getTriggeredRules().size());
    }
}
