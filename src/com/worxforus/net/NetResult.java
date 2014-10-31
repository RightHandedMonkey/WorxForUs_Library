package com.worxforus.net;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import com.worxforus.Utils;

import android.util.Log;

/**
 * This class contains the information and related details when making a network request using the NetHandler class
 * @author sbossen
 *
 */
public class NetResult extends com.worxforus.Result {
	/**
	 * Tells you if the result of the network call was successful or not i.e. could the server be reached
	 */
	public boolean net_success = false; //starts as false to indicate we haven't attempted the connection yet
	/**
	 * If set: this contains the debug level information about the type of network error that occurred.
	 */
	public String net_error = "";
	/**
	 * If set: this contains the name of the exception that caused a network error
	 */
	public String net_error_type = "";
	/**
	 * This contains the HTTP status code returned by the server
	 */
	public int net_response_code = 0;
	/**
	 * This string is human readable text of the response code by the server
	 */
	public String net_response_message = "";
	/**
	 * This is a flag used by the NetHandler class to determine if the client connection should be reset on the next access attempt.
	 * This is useful to do on SocketTimeouts 
	 */
	public boolean net_reset_client_connection = false;
	/**
	 * This contains the actual response from the server.
	 * The most common use is to translate the webserver response into a string:
	 *     String consume_str = Utils.removeUTF8BOM(EntityUtils.toString(net_result.net_response_entity, Utils.CHARSET));
	 * or read JSON objects
	 *     NetResult.handleGenericJsonResponseHelper(net_result, this.getClass().getName()); //the string here is passed for logging purposes only
	 *     net_result.object will contain your JSON objects parsed for you
	 */
	public HttpEntity net_response_entity= null;
	
	/**
	 * 
	 * @return String - the resulting string from the host, with UTF BOM markers removed. 
	 * @throws ParseException
	 * @throws IOException
	 */
	public String getCleanedUTF8ServerResponse() throws ParseException, IOException  {
		return Utils.removeUTF8BOM(EntityUtils.toString(this.net_response_entity, Utils.CHARSET));
	}
	
	/**
	 * When set indicates how many times the system tried to retry the connection
	 */
	public int num_attemps=0;
	//NOTE: Need to call close_net_result() in calling program.
	
	public NetResult closeNetResult() {
		NetResult result = new NetResult();
		//Check when using Apache HttpClient
		if (this.net_response_entity != null && this.net_response_entity instanceof HttpEntity) {
			try {
				this.net_response_entity.consumeContent();
				this.net_response_entity = null;
				result.net_success = true;
			} catch (IOException e) {
				result.net_success = false;
				result.net_error = "Could not close network connection. "+e.getLocalizedMessage()+", cause: "+e.getCause();
				Log.e(this.getClass().getName(), result.net_error );
			}
		}
		return result;
	}
	
	public NetResult() {
		super();
	}

	/**
	 * Function is used to generate the data that should be seen in the log
	 * @return
	 */
	public String getLogEntry() {
		return ("Webserver Response: ["+net_response_message+"] , Error Type: ["+net_error_type+"], "+net_error);
	}
	
	public void clearNetResults() {
		net_success = false;
		net_error = "";
		net_error_type = "";
		net_response_code = 0;
		net_response_message = "";
		net_reset_client_connection = false;
		closeNetResult();
	}
	
	public String toString() {
		String msg = "net_success: "+net_success+", net_error: "+net_error+", net_error_type: "
				+net_error_type+", net_response_message: "+net_response_message;
		msg += "\nParent: "+super.toString();
		return msg;
	}
	//consider having an abstract class here if passing an entity object is not efficient
	//public abstract NetResult handle_net_response(HttpResponse response) {}
}
