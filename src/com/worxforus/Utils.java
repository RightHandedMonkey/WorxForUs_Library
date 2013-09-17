package com.worxforus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.http.StatusLine;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class Utils {

	public static final String MYSQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String FILE_DATETIME_FORMAT = "yyyyMMdd_HHmmss";
	public static String MYSQL_TIMEZONE = "America/New_York";

	/**
	 * Shared buffer used by {@link #getUrlContent(String)} when reading results
	 * from an API request.
	 */
	private static byte[] sBuffer = new byte[512];
	/**
	 * {@link StatusLine} HTTP status code when no server error has occurred.
	 */
	private static final int HTTP_STATUS_OK = 200;

	public static final String CHARSET = "UTF-8";

	public static void debug_toast(Activity app, boolean debug, String msg) {
		if (debug) {
			Toast toast = Toast.makeText(app.getApplicationContext(), msg,
					Toast.LENGTH_SHORT);
		}
	}

	/**
	 * Datetime object created on android device which may be in a diff time
	 * zone than the server. So convert to eastern time
	 * 
	 * @return
	 */
	public static String get_current_datetime_str() {
		// Date d = new java.util.Date();

		// DateFormat df = DateFormat.getTimeInstance();
		// df.setTimeZone(TimeZone.getTimeZone("gmt"));

		// return (new
		// SimpleDateFormat(com.cxworx.Utils.MYSQL_DATETIME_FORMAT)).format(new
		// java.util.Date());
		DateFormat dateFormat = new SimpleDateFormat(
				Utils.MYSQL_DATETIME_FORMAT);
		// set to eastern time zone
		dateFormat.setTimeZone(TimeZone.getTimeZone(MYSQL_TIMEZONE));
		java.util.Date d = new java.util.Date();
		return dateFormat.format(d);
	}

	public static String get_current_datetime_str_for_filename() {
		DateFormat dateFormat = new SimpleDateFormat(Utils.FILE_DATETIME_FORMAT);
		// set to eastern time zone
		dateFormat.setTimeZone(TimeZone.getTimeZone(MYSQL_TIMEZONE));
		java.util.Date d = new java.util.Date();
		return dateFormat.format(d);
	}

	/**
	 * Assumes datetime object created on android device which may be in a diff
	 * time zone than the server. So always convert to eastern time
	 * 
	 * @param d
	 * @return
	 */
	public static String convert_datetime_to_str(java.util.Date d) {
		if (d == null)
			return "";
		DateFormat dateFormat = new SimpleDateFormat(Utils.MYSQL_DATETIME_FORMAT);
		// set to eastern time zone
		dateFormat.setTimeZone(TimeZone.getTimeZone(MYSQL_TIMEZONE));
		// return (new
		// SimpleDateFormat(com.cxworx.Utils.MYSQL_DATETIME_FORMAT)).format(d);
		return dateFormat.format(d);
	}

    public static Result getJSONObject(String string) {
    	Result result = new Result();
    	
    	JSONObject response = new JSONObject();
        try {
            // Drill into the JSON response to find the content body
            response = new JSONObject(string);
            result.object = response;
        } catch (Exception e) {
            result.success = false;
            result.error = "Problem parsing website response: "+e;
            Log.d("com.cxworx.Utils", "JSON Parsing error: "+e);
        }
        return result;
    }

    
	public static boolean is_debug_mode(Context context) {

		boolean result = false;
		String DEBUGKEY = "308201e53082014ea00302010202044ef4cb7b300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3131313232333138343230335a170d3431313231353138343230335a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d00308189028181008660d4f740dfa9ccea051298c02629e83d78096ae2b8c21f2f601d1fcc3efe46a43e21a26b931b2aca6fd8789dccf5e7da3b98fdf5fde43bf324a1fb45a98a18d2f5296cce1d86d7ff10331e67cb84c7424701a8d3af18e780bf2d62d0886bce6e67273bf9466f9fcd2a4cb77ac06c11caab581a6a70d49482a26f32377b52690203010001300d06092a864886f70d0101050500038181001dcb40276ab571e75c8d4af3acc4c3b5f56112f690203cadf190f2d14581442dfe2ff446953590888b3dae41520d73c4875e854d4c6650116722e062ec245875710ffdabf2d0c85715667fd7f87fd5da515db30e64cc2bbc1f6fb3e5078a0ec53f170f491b22a528e182d1db8c4004a877500ac376cb2cbfc72bdf77e7939c32";
		String TAG = "com.worxforus.Utils";
		try {
			ComponentName comp = new ComponentName(context, context.getClass());
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(
					comp.getPackageName(), PackageManager.GET_SIGNATURES);
			android.content.pm.Signature sigs[] = pinfo.signatures;
			for (int i = 0; i < sigs.length; i++)
				Log.d(TAG, sigs[i].toCharsString());
			if (DEBUGKEY.equals(sigs[0].toCharsString())) {
				result = true;
				Log.d(TAG, "package has been signed with the debug key");
			} else {
				Log.d(TAG, "package signed with a key other than the debug key");
			}

		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return false;
		}
		return result;
	}

	public static void sleep(int msec) {
		try { // gradually increase timeout if not successful
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			Log.e(Utils.class.getName(), "Thread could not sleep");
		}
	}

}