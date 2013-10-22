package com.worxforus.net;

import com.worxforus.json.JSONObjectWrapper;
import com.worxforus.net.NetResult;

public class NetAuthentication {
	
	private String username="";
	private String password="";
	private String accessToken=""; //accessToken can be used instead of a password
	private String uuid=""; //when combined with accessToken it can be used instead of a password
	private volatile boolean isLoggedIn = false; //store if we have been authenticated
	private long lastLoginTime = 0; //stores last time we authenticated
	
	protected volatile int loginStatus = 0; //starts as false to indicate we haven't attempted the connection yet
	
	public static final int NO_ERRORS = 0;
	public static final int LOGIN_FAILURE = 1; //login supplied but not valid
	public static final int NETWORK_ERROR = 2; //could not communicate to server
	public static final int SERVER_ERROR = 3; //server response could not be parsed
	public static final int NOT_LOGGED_IN = 4; //server indicates user is not logged in - should only happen in context other than trying to log in.

	public static long LOGIN_REFRESH_TIME_SECONDS = 28800; //retries login after 8 hours if requested
    // Private constructor prevents instantiation from other classes
   private NetAuthentication() { }

   /**
   * SingletonHolder is loaded on the first execution of Singleton.getInstance()
   * or the first access to SingletonHolder.INSTANCE, not before.
   */

   private static class AuthenticationHolder {
           public static final NetAuthentication INSTANCE = new NetAuthentication();
   }

   public static NetAuthentication getInstance() {
           return AuthenticationHolder.INSTANCE;
   }
   
   public static void loadUsernamePassword(String user, String pass) {
	   NetAuthentication.getInstance().username = user;   
	   NetAuthentication.getInstance().password = pass;   
	   reset();
   }

   /**
    * Returns the results of whether the login was successful.
    * @return
    */
   public static int getLoginStatus() {
	   return NetAuthentication.getInstance().loginStatus;   
   }
   
   public static void loadAccessToken(String token, String uuid) {
	   NetAuthentication.getInstance().accessToken = token;   
	   NetAuthentication.getInstance().uuid = uuid;   
	   reset();
   }
   
   /**
    * Use this function after calling loadUsername and loadPassword
    * Run inside a separate thread using Runnable or ASyncTask since the network calls may block for a while
    * 
    * The NetHelper interface is used to attempt access multiple retries if needed 
    * @return NetResult - NetResult.success tells if the login worked or not
    */
   public static NetResult authenticate(String host, NetAuthenticationHelper authHelper) {
	   synchronized(NetAuthentication.getInstance()) { //so we can only try to authenticate one at a time
		   //check if current authentication is valid
		   NetResult result = new NetResult();
		   result.net_success = true;
	
		   if (NetAuthentication.isCurrentAuthenticationValid()) {
			   authHelper.markAsLoginSuccessFromCache(result);
			   return result; //don't sent to network - expect to be able to use the cookie.
		   }
		   
		   //if username/password available use that
		   if (NetAuthentication.getInstance().username.length() > 0 && NetAuthentication.getInstance().password.length() > 0) {
			   result = authHelper.handleUsernameLogin(host, NetAuthentication.getInstance().username, NetAuthentication.getInstance().password);
		   } else if (NetAuthentication.getInstance().accessToken.length() > 0 && NetAuthentication.getInstance().uuid.length() > 0) {
			   result = authHelper.handleUsernameLogin(host, NetAuthentication.getInstance().accessToken, NetAuthentication.getInstance().uuid);
		   } else {
			   //error - no login credentials specified.
			   result.success = false;
			   result.net_success = true; //since this is not a network problem
			   //set result to read as login failure
			   authHelper.markAsLoginFailure(result);
		   }
	
		   //check for login error - read response as a json object
		   //this function below should be called in NetAUthenticationHelper.handleXXXLogin(...);
		   //NetHandler.handleGenericJsonResponseHelper(result, NetAuthentication.class.getName());
		   int status = authHelper.validateLoginResponse(result);
		   NetAuthentication.getInstance().loginStatus = status;
		   if (status == NO_ERRORS) {
			   NetAuthentication.getInstance().lastLoginTime = System.nanoTime();
			   NetAuthentication.getInstance().isLoggedIn = true;
		   } else {
			   NetAuthentication.invalidate();
			   result.success = false;
		   }
		   
		   return result;
	   }
   }

   /**
    * Returns true if the cache value looks ok and we think we are still logged in.
    * Returns false if not.
    */
   public static boolean isCurrentAuthenticationValid() {
	   if (NetAuthentication.getInstance().isLoggedIn) {
		   long time_diff_nano = System.nanoTime() - NetAuthentication.getInstance().lastLoginTime;
		   long time_diff_sec = time_diff_nano/1000000000; //convert nanoseconds to seconds: nano/micro/milli
		   if (time_diff_sec < NetAuthentication.LOGIN_REFRESH_TIME_SECONDS)
			   return true;
		   else {
			   reset();
			   return false;
		   }
	   }
	   return false;
   }
   
   /**
    * Call this function whenever the login information changes or login can be requeried.
    */
   public static void reset() {
	   NetAuthentication.getInstance().loginStatus = 0;
	   NetAuthentication.invalidate();
   }

   /**
    * Mark logged in cache as invalid - so it will be rechecked next time authenticate is called
    */
   public static void invalidate() {
	   NetAuthentication.getInstance().isLoggedIn = false;
   }
   
   public interface NetAuthenticationHelper {
	   /**
	    * @return url of where to send login request
	    */
	   public String getLoginURL(String host);
	   
	   /**
	    * Use this function to force the result to appear as a login failure 
	    * This simulates a login failure so it can be reported back to the calling class
	    * @param result
	    */
	   public void markAsLoginFailure(NetResult result);
	   
	   /**
	    * Use this function to force the result to appear as a login success due to cache hit 
	    * This simulates a login success so it can be reported back to the calling class
	    * @param result
	    */
	   public void markAsLoginSuccessFromCache(NetResult result);

	   /**
	    * With this function, AuthNetHandler can use it to send an error message back up to the application
	    * @return
	    */
		public String getLoginErrorMessage();

		/**
		 * This function is called after a request for the login page to see if a login error was created
		 * 
		 * @param netResult - most useful for netResult.object to be a json object
		 * @return int containing loginStatus based on given netResult
		 */
		public int validateLoginResponse(NetResult netResult);
		
	   /**
	    * This function is to be used by external classes that check to see if a login error was created.
	    * Run after using a network command to any page other than the login interface page.
	    * This does not check for all login failures, but only:
	    * 	NO_ERRORS
	    *   NOT_LOGGED_IN
	    * 
	    * If this returns the error with the NOT_LOGGED_IN string, then NetAuthentication.invalidate() should be called
	    * 
	    * @param netResult - most useful for netResult.object to be a json object
	    * @return int containing loginStatus based on given netResult - either NO_ERRORS or NOT_LOGGED_IN
	    */
	   public int peekForNotLoggedInError(NetResult netResult);

	   /**
	    * Login to the getLoginURL() with the given username and password.  Expect a json success response or failure
	    * makes no determination of success or failure of login, just network connection
	    * run checkForLoginError(...) to determine type of login failure
	    * @param username
	    * @param password
	    * @return
	    */
	   public NetResult handleUsernameLogin(String host, String username, String password);
	   
	   public NetResult handleTokenLogin(String host, String accessToken, String uuid);
   }
}
