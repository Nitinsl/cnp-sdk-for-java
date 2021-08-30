package io.github.vantiv.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Calendar;
import java.util.Properties;

import io.github.vantiv.sdk.generate.AccountUpdateFileRequestData;
import io.github.vantiv.sdk.generate.RFRRequest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestRFRFile {

    private String preliveStatus = System.getenv("preliveStatus");

    @Before
    public void setup() {
        if (preliveStatus == null) {
            System.out.println("preliveStatus environment variable is not defined. Defaulting to down.");
            preliveStatus = "down";
        }
    }

    @Test
    public void testSendToCnpSFTP() throws Exception {

        Assume.assumeFalse(preliveStatus.equalsIgnoreCase("down"));
        
        String requestFileName = "cnpSdk-testRFRFile-fileConfigSFTP.xml";
        RFRRequest rfrRequest = new RFRRequest();

        CnpRFRFileRequest request = new CnpRFRFileRequest(requestFileName, rfrRequest);

        Properties configFromFile = request.getConfig();

        AccountUpdateFileRequestData data = new AccountUpdateFileRequestData();
        data.setMerchantId(configFromFile.getProperty("merchantId"));
        data.setPostDay(Calendar.getInstance());
        rfrRequest.setAccountUpdateFileRequestData(data);

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com", configFromFile.getProperty("batchHost"));

        String workingDirRequests = configFromFile.getProperty("batchRequestFolder");
        prepDir(workingDirRequests);

        String workingDirResponses = configFromFile.getProperty("batchResponseFolder");
        prepDir(workingDirResponses);

        /* call method under test */
        try {
            CnpRFRFileResponse response = request.sendToCnpSFTP();

            // assert request and response files were created properly
            assertGeneratedFiles(workingDirRequests, workingDirResponses, requestFileName, request, response);
        } catch (Exception e) {
            fail("Unexpected Exception!");
        }
    }

    private void assertGeneratedFiles(String workingDirRequests, String workingDirResponses, String requestFileName,
                                      CnpRFRFileRequest request, CnpRFRFileResponse response) throws Exception {
        File fRequest = request.getFile();
        assertEquals(workingDirRequests + File.separator + requestFileName, fRequest.getAbsolutePath());
        assertTrue(fRequest.exists());
        assertTrue(fRequest.length() > 0);

        File fResponse = response.getFile();
        assertEquals(workingDirResponses + File.separator + requestFileName, fResponse.getAbsolutePath());
        assertTrue(fResponse.exists());
        assertTrue(fResponse.length() > 0);

    }

    private void prepDir(String dirName) {
        File fRequestDir = new File(dirName);
        fRequestDir.mkdirs();
    }
}
