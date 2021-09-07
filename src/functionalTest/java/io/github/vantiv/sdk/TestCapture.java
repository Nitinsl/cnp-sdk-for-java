package io.github.vantiv.sdk;

import static org.junit.Assert.assertEquals;

import io.github.vantiv.sdk.generate.Capture;
import io.github.vantiv.sdk.generate.CaptureResponse;
import io.github.vantiv.sdk.generate.EnhancedData;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCapture {

	private static CnpOnline cnp;

	@BeforeClass
	public static void beforeClass() throws Exception {
		cnp = new CnpOnline();
	}

	@Test
	public void simpleCapture() throws Exception{
		Capture capture = new Capture();
		capture.setCnpTxnId(123456000L);
		capture.setAmount(106L);
		capture.setPayPalNotes("Notes");
		capture.setId("id");

		CaptureResponse response = cnp.capture(capture);
		assertEquals("Approved", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}
	
	@Test
	public void testCaptureWithPin() throws Exception{
		Capture capture = new Capture();
		capture.setCnpTxnId(123456000L);
		capture.setAmount(106L);
		capture.setPayPalNotes("Notes");
		capture.setId("id");
		capture.setPin("1234");

		CaptureResponse response = cnp.capture(capture);
		assertEquals("Approved", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}

	@Test
	public void simpleCaptureWithPartial() throws Exception{
		Capture capture = new Capture();
		capture.setCnpTxnId(123456000L);
		capture.setAmount(106L);
		capture.setPartial(true);
		capture.setPayPalNotes("Notes");
		capture.setId("id");

		CaptureResponse response = cnp.capture(capture);
		assertEquals("Approved", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}

	@Test
	public void complexCapture() throws Exception{
		Capture capture = new Capture();
		capture.setCnpTxnId(123456000L);
		capture.setAmount(106L);
		capture.setPayPalNotes("Notes");
		EnhancedData enhancedData = new EnhancedData();
		enhancedData.setCustomerReference("Cnp");
		enhancedData.setSalesTax(50L);
		enhancedData.setDeliveryType(EnhancedData.DeliveryType.TBD);
		capture.setEnhancedData(enhancedData);
		capture.setPayPalOrderComplete(true);
		capture.setId("id");

		CaptureResponse response = cnp.capture(capture);
		assertEquals("Approved", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}

	@Test
	public void simpleCaptureWithOrderId() throws Exception{
		Capture capture = new Capture();
		capture.setCnpTxnId(123456000L);
		capture.setAmount(106L);
		capture.setPayPalNotes("Notes");
		capture.setId("id");
		capture.setOrderId("1234567567");

		CaptureResponse response = cnp.capture(capture);
		assertEquals("Approved", response.getMessage());
		assertEquals("sandbox", response.getLocation());
	}

}


