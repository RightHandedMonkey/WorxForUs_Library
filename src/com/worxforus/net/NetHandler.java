package com.worxforus.net;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.worxforus.Result;
import com.worxforus.Utils;

import android.os.SystemClock;
import android.util.Log;

/**
 * 
 * @author sbossen
 *
 */
//TODO: Rewrite class to use volley
public class NetHandler {
	//default is a short timeout
	private static NetHandler singleton;
	private static HttpClient client;

    public static final int NETWORK_DEFAULT_RETRY_ATTEMPTS=7;
    public static final int NETWORK_RETRY_TIMEOUT_MS=1500; //was 1300// was 1000
	public static final int NETWORK_SHORT_TIMEOUT_MS=1000; //was 700 //was 500
	public static final int TIMEOUT_CONNECTION_MS = 30000;//10000; //5000 too slow?
	public static final int TIMEOUT_SOCKET_MS = 30000; //10000; //5000 too slow, changed to 10000
	public static final long TIMEOUT_CONNECTION_MGR_MS = 13000;  //5000 too slow?
	public static final String TIMEOUT_SOCKET_MSG= "Network Timeout";

    private static final int HTTP_STATUS_OK = 200;

	public static NetHandler getInstance() {
		return singleton;
	}
	
	public static NetResult handle_post_with_retry(String url, List<NameValuePair> params, int num_retries) {
		NetResult result = new NetResult();
		int cur_try = 0;
		while(cur_try < num_retries && result.net_success==false) {
			cur_try++;
			Log.d(NetHandler.class.getName(), "handle_post_with_retry, attempt# "+cur_try);
			NetHandler.handle_post(url, params, result);
			//set num_attemps here
			result.num_attemps = cur_try;
			
			if (result.net_reset_client_connection == true) {
				NetHandler.setHttpClient(null); //reset connection
				Utils.sleep(cur_try*NETWORK_RETRY_TIMEOUT_MS);
			}
			if (result.net_success == false && cur_try < num_retries) {
				Utils.sleep(cur_try*NETWORK_SHORT_TIMEOUT_MS);
			}
		}
		return result;
	}
		
	public static NetResult handle_post(String url, List<NameValuePair> params, NetResult result) {
		//first make sure we haven't marked as passed
		result.clear_net_results();
		try {
			HttpPost post = new HttpPost(url);
			if (params != null) {
				UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params, HTTP.UTF_8);
				post.setEntity(ent);
			}
//			long startTm = SystemClock.elapsedRealtime();
			Log.d(NetHandler.class.getName(), "handle_page called for url: "+url);
			HttpResponse responsePOST = NetHandler.getHttpClient().execute(post);  
			result.net_response_entity = responsePOST.getEntity();
			result.net_response_message = responsePOST.getStatusLine().toString();
			result.net_response_code = responsePOST.getStatusLine().getStatusCode();
			if (result.net_response_code == HTTP_STATUS_OK) {
				result.net_success = true;
			}
		} catch (HttpResponseException e) {
			result.net_error = "HttpResponseException Error: "+e.getMessage()+", Cause: "+e.getCause();
			result.net_error_type = e.getClass().getName();
			Log.e(NetResult.class.getName(), result.get_log_entry() );
		} catch (SocketTimeoutException e) {
			result.net_error = "SocketTimeoutException Error: "+e.getMessage()+", Cause: "+e.getCause();
			result.net_error_type = e.getClass().getName();
			//Reset HTTPClient connection in calling method
			result.net_reset_client_connection = true;
			Log.e(NetResult.class.getName(), result.get_log_entry() );
		} catch (SocketException e) {
			result.net_error = "SocketException Error: "+e.getMessage()+", Cause: "+e.getCause();
			result.net_error_type = e.getClass().getName();
			Log.e(NetResult.class.getName(), result.get_log_entry() );
		} catch (IOException e) {
			result.net_error = "IOException Error: "+e.getMessage()+", Cause: "+e.getCause();
			result.net_error_type = e.getClass().getName();
			Log.e(NetResult.class.getName(), result.get_log_entry() );
			if (e.getMessage() == null) //different entry here for debugging purposes
				Log.e(NetResult.class.getName(), result.get_log_entry() );
			else 
				Log.e(NetResult.class.getName(), result.get_log_entry() );
		}
		return result;
	}

	
	/**
	 * Creates an HttpClient object if one does not exist or reuse it if already created
	 * @return HttpClient 
	 */
    public static HttpClient getHttpClient() {
    	if (client == null) {
	        HttpParams params = new BasicHttpParams();
	
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
	        HttpProtocolParams.setUseExpectContinue(params, true);
	
	        HttpConnectionParams.setStaleCheckingEnabled(params, false);
	        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_CONNECTION_MS);
	        HttpConnectionParams.setSoTimeout(params, TIMEOUT_SOCKET_MS);
	        HttpConnectionParams.setSocketBufferSize(params, 8192);
	
	        SchemeRegistry schReg = new SchemeRegistry();
	        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
	        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
	
	        client = new DefaultHttpClient(conMgr, params);
    	}
        return client;
    }
    
	public static HttpClient getHttpClientOld() {
		if (client == null) {
			HttpParams httpParameters = new BasicHttpParams();
			//set HTTP protocol to 1.1 to avoid possible delays on posting
			HttpProtocolParams.setVersion(httpParameters, HttpVersion.HTTP_1_1);
			// Set the timeout in milliseconds until a connection is established.
			// The default value is zero, that means the timeout is not used. 
			HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_MS);
			// Set the default socket timeout (SO_TIMEOUT) 
			// in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET_MS);
			
			httpParameters.setLongParameter(ConnManagerPNames.TIMEOUT, TIMEOUT_CONNECTION_MGR_MS);

			//trying to disable expect-continue for better response when sending large files via tablet
			//That did not seem to help, still getting IOException - Network is unreachable which is a java.net.ConnectException
			/*
			 * 03-21 16:31:16.576: E/com.cxworx.FileResource.file_upload_helper(12213): com.cxworx.FileResource.file_upload_helper::IOException Error: Connection to http://www.cxworx-engr.com refused, Cause: java.net.ConnectException: /71.123.45.75:80 - Network is unreachable
			 */
			//httpParameters.setBooleanParameter("http.protocol.expect-continue", false);
 
			
			client = new DefaultHttpClient(httpParameters);
		}
		
		return client;
	}

	/**
	 * If a socket timeout is detected while connecting, reset the client by setting it to null here
	 * to regenerate the connection.  Otherwise a bad connection could be maintained for a while
	 * Set to null to close the connection
	 * @param new_http_client
	 */
	public static void setHttpClient(HttpClient new_http_client) {
		if (new_http_client == null) {
			NetHandler.closeConnection();
		}
		NetHandler.client = new_http_client;
	}
	
	public static void closeConnection()	{
	    if(NetHandler.client != null && NetHandler.client.getConnectionManager() != null) {
	        NetHandler.client.getConnectionManager().shutdown();
	    }
	}
	
	/**
	 * Used to parse JSON response from webserver in result.object
	 * Handles exceptions by logging them
	 * @param result - object containing net Entity object
	 * @param calling_class - String to use to name calling function in log
	 * @return
	 */
	public static NetResult handle_generic_json_response_helper(NetResult result, String calling_class) {
		if (result != null && result.net_success) {
			try {
	
				 String consume_str = Utils.removeUTF8BOM(EntityUtils.toString(result.net_response_entity, Utils.CHARSET));
//			     String consume_str = EntityUtils.toString(result.net_response_entity, Utils.CHARSET);
			     Result response;
		         response = Utils.getJSONObject(consume_str); //get main json
		         result.add_results_if_error(response, "Could not read JSON object.");
			     if (response.success) {
	        		 result.object = (JSONObject)response.object;
			     }
			} catch (IOException e) {
				result.error = "EntityUtils.toString threw - IOException Error: "+e.getMessage()+", Cause: "+e.getCause();
				if (e.getMessage() == null) //different entry here for debugging purposes
		    		Log.e(calling_class, result.error );
		    	else 
		    		Log.e(calling_class, result.error );
			}
		 } else { //failure could not get communications to server
			result.success = false;
			result.error = "Could not communicate with Web Server.";
			Log.i(calling_class, "Attemps to create test: "+result.num_attemps+" times, and connection was unsuccessful");
		 }
		result.close_net_result();
		return result;
		
	}

}
