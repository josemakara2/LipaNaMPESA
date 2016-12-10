/*
 * LNMOCheckoutAsyncClient.java
 *
 * 11/12/2016 Joseph Makara -  Created File to test Lina Na M-PESA Online checkout
 *
 */
package ke.co.makara.integration.mpesa.lnmocheckout;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import javax.xml.ws.*;
import org.apache.commons.codec.binary.*;

public class LNMOCheckoutAsyncClient {

	private static final String PASSKEY = "safcom-assigned-passkey";
	private static final String MERCHANT_ID = "a-registered-merchant-id";
	private static final String REFERENCE_ID = "";
	private static final String ENDPOINT_URL = "https://safaricom.co.ke/mpesa_online/";
	private static final String CALLBACK_URL = "https://makara.co.ke:8443/odt/checkout";
	private static final String CALLBACK_METHOD = "POST";

	static {
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				if (hostname.equals("safaricom.co.ke"))
					return true;
				return false;
			}
		});
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		LNMOPortType soapPort = outBoundLNMOCheckout(ENDPOINT_URL);

		ObjectFactory objFactory = new ObjectFactory();

		CheckOutHeader requestHeader = objFactory.createCheckOutHeader();
		requestHeader.setMERCHANTID(MERCHANT_ID);
		Date timestamp = new Date();
		String encryptedPassword = getEncryptedPassword(MERCHANT_ID, PASSKEY,
				timestamp);
		requestHeader.setPASSWORD(encryptedPassword);
		requestHeader.setTIMESTAMP(String.valueOf(timestamp.getTime()));

		ProcessCheckOutRequest checkOutRequest = objFactory
				.createProcessCheckOutRequest();
		checkOutRequest = processCheckOut(timestamp);

		try {
			LNMOCheckoutAsyncClient asyncCLient = new LNMOCheckoutAsyncClient();
			ProcessCheckOutRequestCallbackHandler callbackHandler = asyncCLient.new ProcessCheckOutRequestCallbackHandler();
			Future<?> resp = soapPort.processCheckOutAsync(checkOutRequest,
					requestHeader, callbackHandler);
			resp.get(5L, TimeUnit.MINUTES);
			ProcessCheckOutResponse result = callbackHandler.getResponse();

			System.out.println(" Return Code : " + result.getRETURNCODE());
			System.out.println(" Customer Msg : " + result.getCUSTMSG());
			System.out.println(" Description : " + result.getDESCRIPTION());
			System.out.println(" TRX ID : " + result.getTRXID());
			System.out.println(" ENC PARAMS : " + result.getENCPARAMS());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static ProcessCheckOutRequest processCheckOut(Date date) {

		ProcessCheckOutRequest checkOutRequest = new ProcessCheckOutRequest();
		checkOutRequest.setMERCHANTTRANSACTIONID(UUID.randomUUID().toString());
		checkOutRequest.setREFERENCEID("TD346534GH");
		checkOutRequest.setAMOUNT(345);
		checkOutRequest.setMSISDN("0721826284");
		checkOutRequest.setENCPARAMS("");
		checkOutRequest.setCALLBACKURL(CALLBACK_URL);
		checkOutRequest.setCALLBACKMETHOD(CALLBACK_METHOD);
		checkOutRequest.setTIMESTAMP(String.valueOf(date.getTime()));

		return checkOutRequest;
	}

	/**
	 * Convert the concatenated string to bytes Hash the bytes to get arbitary
	 * binary data Convert the binary data to string use base64
	 *
	 * @param merchantId
	 * @param passkey
	 * @param date
	 * @return
	 */
	private static String getEncryptedPassword(String merchantId,
			String passkey, Date date) {
		String encodedPassword = null;
		StringBuilder builder = new StringBuilder(merchantId).append(passkey)
				.append(date.getTime());

		System.out.println(" raw : " + builder.toString());
		System.out.println();

		try {
			String sha256 = getSHA256Hash(builder.toString());
			String base64 = new String(Base64.encodeBase64(sha256
					.getBytes("UTF-8")));
			return base64;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}

		return encodedPassword;
	}

	private static LNMOPortType outBoundLNMOCheckout(String url) {
		System.setProperty(
				"com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump",
				"true");
		LnmoCheckoutService lnmoCheckoutService = new LnmoCheckoutService();
		LNMOPortType soap = lnmoCheckoutService.getLnmoCheckout();

		((BindingProvider) soap).getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				url + "lnmo_checkout_server.php");

		return soap;
	}

	private static String getSHA256Hash(String input)
			throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return sb.toString();
	}

	private void doProcessCheckOutRequest(
			ProcessCheckOutRequest checkOutRequest, CheckOutHeader requestHeader) {
		try {
			ProcessCheckOutRequestCallbackHandler callbackHandler = new ProcessCheckOutRequestCallbackHandler();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ProcessCheckOutRequestCallbackHandler implements
			AsyncHandler<ProcessCheckOutResponse> {
		private ProcessCheckOutResponse response;

		@Override
		public void handleResponse(Response<ProcessCheckOutResponse> aResponse) {
			try {
				response = aResponse.get();
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		public ProcessCheckOutResponse getResponse() {
			return response;
		}
	}

	private void doTransactionConfirmRequest(
			TransactionConfirmRequest transactionConfirmRequest,
			CheckOutHeader header) {

		LNMOPortType soapPort = outBoundLNMOCheckout(ENDPOINT_URL);

		try {
			LNMOCheckoutAsyncClient asyncCLient = new LNMOCheckoutAsyncClient();
			TransactionConfirmRequestCallbackHandler callbackHandler = asyncCLient.new TransactionConfirmRequestCallbackHandler();
			Future<?> resp = soapPort.confirmTransactionAsync(
					transactionConfirmRequest, header, callbackHandler);
			resp.get(5L, TimeUnit.MINUTES);
			TransactionConfirmResponse result = callbackHandler.getResponse();

			System.out.println(" Return Code : " + result.getRETURNCODE());
			System.out.println(" MERCHANTTRANSACTIONID : "
					+ result.getMERCHANTTRANSACTIONID());
			System.out.println(" Description : " + result.getDESCRIPTION());
			System.out.println(" TRX ID : " + result.getTRXID());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class TransactionConfirmRequestCallbackHandler implements
			AsyncHandler<TransactionConfirmResponse> {
		private TransactionConfirmResponse response;

		@Override
		public void handleResponse(
				Response<TransactionConfirmResponse> aResponse) {
			try {
				response = aResponse.get();
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		public TransactionConfirmResponse getResponse() {
			return response;
		}
	}

	class XMLResultMessageCallbackHandler implements AsyncHandler<ResultMsg> {
		private ResultMsg response;

		@Override
		public void handleResponse(Response<ResultMsg> aResponse) {
			try {
				response = aResponse.get();
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		public ResultMsg getResponse() {
			return response;
		}
	}

}
