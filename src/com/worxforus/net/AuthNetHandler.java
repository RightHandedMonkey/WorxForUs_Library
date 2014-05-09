package com.worxforus.net;

import java.util.List;
import org.apache.http.NameValuePair;
import com.worxforus.net.NetAuthentication.NetAuthenticationHelper;

/**
 * This is the net handler to handle authenticated requests to/from the network with automatic login attempts.  
 * Includes allowing multiple network retries in case of transmission failure.
 * 
 * Usage:
 * Set the authentication object
 * AuthNetHelper.setAuthentication("host", subclassed NetAuthentication.NetAuthenticationHelper);
 * Then load the authentication 
 * NetAuthentication.loadUsernamePassword(username, passwd);
 * 
 * @author sbossen
 *
 */
public class AuthNetHandler {
	static String host;
	static NetAuthenticationHelper authHelper;
	
	/*
	 * Prepare the authentication system for usage.
	 * This must be setup prior to using the post functions
	 * Usage: AuthNetHandler.setAuthentication(url, new ConcreteAuthentication(context)); 
	 */
	public static void setAuthentication(String host, NetAuthenticationHelper authHelper) {
		AuthNetHandler.host = host;
		AuthNetHandler.authHelper = authHelper;
	}
	
	public static boolean isAuthenticationSet() {
		if (AuthNetHandler.host == null || AuthNetHandler.authHelper == null)
			return false;
		return true;
	}
	
	/**
	 * Use this function when posts requires a login of some sort prior to accessing data.
	 * First tries to login using cached values if possible,
	 * Then it makes the network request, finally it
	 * Checks to see if a not logged in error was generated and retries the login and network request if that error was found
	 * 
	 *  Usage:
	 *  AuthNetHandler.setAuthentication(host, new NetAuthenticationHelper());
	 *  AuthNetHandler.handleAuthPostWithRetry(url, params, num_retries);
	 * @param url
	 * @param params
	 * @param num_retries
	 * @return NetResult object
	 */
	public static NetResult handleAuthPostWithRetry(String url, List<NameValuePair> params, int num_retries) {
		if (!AuthNetHandler.isAuthenticationSet() ) {
			throw new RuntimeException("Using handleAuthPostWithRetry(..) requires that AuthNetHandler.setAuthentication(..) is called first.");
		}
		NetResult netResult = new NetResult();
		int loginStatus = handleLoginAttempt(); //AuthNetHandler.authHelper.checkForLoginError(netResult);

		//for certain values, we don't even try to complete the request.
		if (loginStatus == NetAuthentication.LOGIN_FAILURE) {
			netResult.net_success = false;
			netResult.net_error = AuthNetHandler.authHelper.getLoginErrorMessage();
		} else if (loginStatus == NetAuthentication.NO_ERRORS) {
			//continue on with request
			netResult = NetHandler.handlePostWithRetry(url, params, num_retries);
			//check for errors - use peek here instead of validate since we are not connecting to login interface page
			loginStatus = AuthNetHandler.authHelper.peekForNotLoggedInError(netResult);
			if (loginStatus == NetAuthentication.NOT_LOGGED_IN) {
				//this means that the authenticate(..) function used a saved value, but server indicates user not logged in, so reset it.
				NetAuthentication.invalidate();
				loginStatus = handleLoginAttempt(); 
				if (loginStatus == NetAuthentication.NO_ERRORS) { 
					//we could now login, so retry
					netResult = NetHandler.handlePostWithRetry(url, params, num_retries);
				} //else, we couldn't log in, so don't try to complete request - just pass previous netResult along
			}
		} else if (loginStatus == NetAuthentication.SERVER_ERROR) {
			netResult.success = false;
			netResult.message = "Network server error detected.";
			netResult.technical_error = "Server did not return the expected JSON response. ";
		}
		return netResult;
	}
	
	private static int handleLoginAttempt() {
		NetResult netResult = NetAuthentication.authenticate(AuthNetHandler.host, AuthNetHandler.authHelper);
		
		int loginStatus = AuthNetHandler.authHelper.validateLoginResponse(netResult);
		//for certain values, we don't even try to complete the request.
		return loginStatus;
	}
}
