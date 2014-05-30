package org.onepf.store;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.onepf.store.R;
import org.onepf.store.data.Database;
import org.onepf.store.data.Purchase;

import android.app.Activity;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

public class PurchaseActivity extends Activity {

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String KEY_ALGORITHM = "RSA";
	
	private static String APPSTORE_ID = "org.onepf.store";
	
	private static String AUTH_TOKEN = "NKmESljNib6B0g8OwNlMJKvDAWCfXcVzsqsZ3GFc";
	
	private static String REPOSITORY_URL = "http://ec2-54-85-44-11.compute-1.amazonaws.com/onepf.repository_war/openaep/signReceipt";
	
	private static String PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ6p+Swv5cMEbW7yzGdtEla7DBqE" +
								 "c/5iE4bRCU2IsLaVL9iS1mucY2fmPDlaJ9RNBrQcYVixzooCH9B/wcbGRkONfE+mRVh+SFmaepGG" +
								 "TqMOmyxpKZnC7u+7IK4bUpPAQb8FQ4jAbRHzEqv/TR6r2ypYvSWK9KAP6DGZ5sNwjbbVAgMBAAEC" +
								 "gYByO+yjrMSfGUP1GqiiWnxWp4s6WFzvLQvkCALLqPuaKWVuZ6Irjkhf2PtJS6jbiK2n5bsM0c/j" +
								 "u03Onv0UMVMKTELYsDW1GqXEdgvYTZoAc4MmYxsRwUHjdOf1uvJKowDyCfFr3HoUOylY/uS8AOEb" +
								 "WardsMZ907UNS+BarIglDQJBAP0SJ02uB7CoKS70bR2r0KHh7PALxEOV9CHpYDG7zLbtj9Hrz2PS" +
								 "Oydc7XyDylc9JPTq0K9t1YA/YghTHOrN1tMCQQCggBfNtUuEVBzjr22S78a8SN2bfsKsvom5i0ar" +
								 "hCjbEt37bOq9RTzim43+w9K5VxfFyExBNDZsa/hCi8v/JoK3AkAMWF9cdbngT0O7C6drBB+oVfoV" +
								 "96z6Uw1Wvii4JF4nKk2SwvsZ9n52PB1FrrQnL09nNzE47ZW1rOHeYBeQCLyLAkBPPC2ELnZjk6rX" +
								 "dKmhmqIAHfDZbRDqev4/DVUMI6iPzpasIy8X1XCjZqwJE9+aBDcGO0X6Aq7Dg+IB6EYDZp2tAkAG" +
								 "p/FxrbtpJKZHYLg/1jqmExc4NdOlVcP3Vc9qHeIMBRZjxrmXsaEdYy1hM4sifoE/A45bVTTgAAXm" +
								 "FZltStH2";
	
	private static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCeqfksL+XDBG1u8sxnbRJWuwwahHP+YhOG0QlN" +
								"iLC2lS/YktZrnGNn5jw5WifUTQa0HGFYsc6KAh/Qf8HGxkZDjXxPpkVYfkhZmnqRhk6jDpssaSmZ" +
								"wu7vuyCuG1KTwEG/BUOIwG0R8xKr/00eq9sqWL0livSgD+gxmebDcI221QIDAQAB";
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase);
    }

    public void onCancelClick(View view) {
        Intent intent = getIntent();
        intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_USER_CANCELED);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onOkClick(View view) {
        Intent intent = getIntent();
        Database db = ((StoreApplication) getApplication()).getDatabase();

        final String packageName = intent.getStringExtra("packageName");
        final String sku = intent.getStringExtra("sku");
        final String developerPayload = intent.getStringExtra("developerPayload");

        Purchase purchase = db.createPurchase(packageName, sku, developerPayload, APPSTORE_ID, "appland.se" );
        if (purchase == null) {
            intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_ERROR);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            new QueryRepositoryTask().execute(purchase);
        }

        
    }
    
    public static KeyPair loadKeyPair(String algorithm)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {


// Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.DEFAULT));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(PRIVATE_KEY, Base64.DEFAULT));
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
    
    private static String getSignature(String receiptData) {
        String signature = null;
        KeyPair keyPair = null;
        try {
            keyPair = loadKeyPair(KEY_ALGORITHM);
        } catch (Exception e) {

        }

        if (keyPair != null) {
            try {
                signature = makeSignature(receiptData, keyPair.getPrivate());
                //boolean verified = verifySignature(receiptData, signature, keyPair.getPublic());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return signature;
    }
    
    public static String makeSignature(String receiptData, PrivateKey key) throws Exception{
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initSign(key);
        sig.update(receiptData.getBytes());
        byte[] signatureBytes = sig.sign();
        return Base64.encodeToString(signatureBytes, Base64.DEFAULT);
    }
    
    public static String createBody(String receiptData, String signature, String appstoreId){
        String body = "<receipt version='1' receipt-data='%s' distributor-appstore='%s' distributor-signature='%s' />";
        return String.format(body, receiptData, appstoreId, signature);
    }
    
    private class QueryRepositoryTask extends AsyncTask<Purchase, Void, String> {
    	
        private String sendRequest(String request, String url) throws Exception{
        	System.out.println("request: " + request);
        	String result = null;
            AndroidHttpClient httpclient = AndroidHttpClient.newInstance("");
            try {
                HttpPost httppost = new HttpPost(url);
                httppost.setHeader("authToken", AUTH_TOKEN);
                httppost.setEntity(new StringEntity(request));

                    System.out.println("executing request " + httppost.getRequestLine());
                    long uploadtime = System.currentTimeMillis();
                    HttpResponse response = httpclient.execute(httppost);
                    uploadtime = System.currentTimeMillis() - uploadtime;
                    System.out.println("Upload time: " + uploadtime);
                    try {
                        System.out.println("----------------------------------------");
                        System.out.println(response.getStatusLine());
                        int resultCode = response.getStatusLine().getStatusCode();

                        if (resultCode == HttpStatus.SC_OK) {
                            HttpEntity resEntity = response.getEntity();
                            result = EntityUtils.toString(resEntity);
                            resEntity.consumeContent();
                        } else {
                        	System.out.println("Reason: " + response.getStatusLine().getReasonPhrase());
                            throw new Exception(response.getStatusLine().toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } 
            } catch(Exception e) {
            	e.printStackTrace();
            } finally {
                httpclient.close();
            }
            return result;
        }
    	

		@Override
		protected String doInBackground(Purchase... params) {
			String result = null;
			String receiptData = params[0].toJson();
			String signature = getSignature(receiptData);
			String body = createBody(receiptData, signature, APPSTORE_ID);
	        try {
	            result =  sendRequest(body, REPOSITORY_URL);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
		}

		@Override
		protected void onPostExecute(String result) {
			System.out.println("Result: " + result);
			
			Intent intent = getIntent();
	        Database db = ((StoreApplication) getApplication()).getDatabase();

	        final String packageName = intent.getStringExtra("packageName");
	        final String sku = intent.getStringExtra("sku");
	        final String developerPayload = intent.getStringExtra("developerPayload");

	        Purchase purchase = db.createPurchase(packageName, sku, developerPayload, APPSTORE_ID, "appland.se");
			db.storePurchase(purchase);
            intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_OK);
            intent.putExtra(BillingBinder.INAPP_PURCHASE_DATA, purchase.toJson());
            // TODO: create signature properly!
            intent.putExtra(BillingBinder.INAPP_DATA_SIGNATURE, "");
            setResult(RESULT_OK, intent);
            finish();
		}

    }
    
    
}