package com.worxforus.net;

import java.util.List;

import org.apache.http.NameValuePair;

import com.worxforus.net.NetAuthentication.NetAuthenticationHelper;

public class AuthNetHandler {
	static String host;
	static NetAuthenticationHelper authHelper;
	
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
			throw new RuntimeException("Using handleAuthPostWithRetry(..) requires that setAuthentication(..) is called first.");
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
			//check for errors
			loginStatus = AuthNetHandler.authHelper.checkForLoginError(netResult);
			if (loginStatus == NetAuthentication.NOT_LOGGED_IN) {
				//this means that the authenticate(..) function used a saved value, but server indicates user not logged in, so reset it.
				NetAuthentication.invalidate();
				loginStatus = handleLoginAttempt(); //AuthNetHandler.authHelper.checkForLoginError(netResult);
				if (loginStatus == NetAuthentication.NO_ERRORS) { //we could now login, so retry
					netResult = NetHandler.handlePostWithRetry(url, params, num_retries);
				} //else, we couldn't log in, so don't try to complete request - just pass previous netResult along
			}
		}
		return netResult;
	}
	
	private static int handleLoginAttempt() {
		NetResult netResult = NetAuthentication.authenticate(AuthNetHandler.host, AuthNetHandler.authHelper);
		int loginStatus = AuthNetHandler.authHelper.checkForLoginError(netResult);
		//for certain values, we don't even try to complete the request.
		return loginStatus;
	}
}
