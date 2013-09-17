package com.worxforus.net;

import java.io.IOException;
import org.apache.http.HttpEntity;
import android.util.Log;

public class NetResult extends com.worxforus.Result {
	public boolean net_success = false; //starts as false to indicate we haven't attempted the connection yet
	public String net_error = "";
	public String net_error_type = "";
	public int net_response_code = 0;
	public String net_response_message = "";
	public boolean net_reset_client_connection = false;
	public HttpEntity net_response_entity= null;
	public int num_attemps=0;
	//NOTE: Need to call close_net_result() in calling program.

	public NetResult close_net_result() {
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
	public String get_log_entry() {
		return ("Webserver Response: ["+net_response_message+"] , Error Type: ["+net_error_type+"], "+net_error);
	}
	
	public void clear_net_results() {
		net_success = false;
		net_error = "";
		net_error_type = "";
		net_response_code = 0;
		net_response_message = "";
		net_reset_client_connection = false;
		close_net_result();
	}
	//consider having an abstract class here if passing an entity object is not efficient
	//public abstract NetResult handle_net_response(HttpResponse response) {}
}
