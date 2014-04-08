package com.worxforus;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.http.StatusLine;

import com.worxforus.json.JSONObjectWrapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

/**
 * This class contains many android specific utilities for frequently occurring tasks.
 * @author sbossen
 *
 */
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

	public static String isRunningOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) 
			return "Currently running on the main thread.";
		else
			return "Not running on the main thread.";
	}
	/**
	 * Only sends the data to the android log system if it detects we are a debug build
	 * Utils.LogD(tag, msg);
	 * @param tag
	 * @param msg
	 */
	public static void LogD(String tag, String msg) {
		if (BuildConfig.DEBUG)
			Log.d(tag, msg);
	}
	
	/**
	 * This activates the android strict mode for platforms that have access to it.  Does not activate
	 * Strict Mode unless it is running a debug build.
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static void activateStrictMode() {
		if (BuildConfig.DEBUG) { //only activate in the debug build
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//             .detectDiskReads()
//             .detectDiskWrites()
//             .detectNetwork()   // or .detectAll() for all detectable problems
             .penaltyLog()
             .build());
	         StrictMode.VmPolicy.Builder b = new StrictMode.VmPolicy.Builder()
             .detectLeakedSqlLiteObjects()
             .penaltyLog()
             .penaltyDeath();
	         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	        	 Utils.activateStrictModeHoneycombHelper(b);
	         }
	         StrictMode.setVmPolicy(b.build());
			}
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static StrictMode.VmPolicy.Builder activateStrictModeHoneycombHelper(StrictMode.VmPolicy.Builder b) {
		b.detectLeakedClosableObjects();
		return b;
	}
	
//	public static void debug_toast(Activity app, boolean debug, String msg) {
//		if (debug) {
//			Toast toast = Toast.makeText(app.getApplicationContext(), msg,
//					Toast.LENGTH_SHORT);
//		}
//	}
	
	/**
	 * Use this when you need to create a toast when you are not currently in the UI thread
	 * @param c -application context or activity context
	 * @param msg
	 * @param toastTime
	 */
	public static void toastHelper(final Context c, final String msg, final int toastTime) {
		Handler mainHandler = new Handler(c.getMainLooper());
		Runnable toaster = new Runnable() {
		   @Override
		    public void run() {
			   Toast.makeText(c, msg, toastTime).show();
		    }		
		};
		mainHandler.post(toaster);
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

	public static Date getDatetimeObject(String mysql_date_str) {
		DateFormat dateFormat = new SimpleDateFormat(
				Utils.MYSQL_DATETIME_FORMAT);
		// set to eastern time zone
		dateFormat.setTimeZone(TimeZone.getTimeZone(MYSQL_TIMEZONE));
		Date d;
		try {
			d = dateFormat.parse(mysql_date_str);
		} catch (ParseException e) {
			d = new java.util.Date(1980, 1, 1);
		}
		return d;
	}
	
	public static Date getCurrentDatetime() {
		// set to eastern time zone
		TimeZone tz = TimeZone.getTimeZone(MYSQL_TIMEZONE);
		GregorianCalendar gc = new GregorianCalendar(tz);
		Date now = gc.getTime();
		return now;
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
    	
    	JSONObjectWrapper response;
        try {
            // Drill into the JSON response to find the content body
            response = new JSONObjectWrapper(string);
            result.object = response;
        } catch (Exception e) {
            result.success = false;
            result.error = "Problem parsing website response: "+e;
            Log.d("com.cxworx.Utils", "JSON Parsing error: "+e);
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
	
	/**
	 * Get a diff between two dates
	 * Original Source: http://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances
	 * @param date1 the oldest date
	 * @param date2 the newest date
	 * @param timeUnit the unit in which you want the diff
	 * @return the diff value, in the provided unit
	 */
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
	    long diffInMillies = date2.getTime() - date1.getTime();
	    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Used to remove a UTF-8 byte-order-mark (BOM) from a string
	 * @param s
	 * @return
	 */
    public static String removeUTF8BOM(String s) {
        if (s.startsWith("\uFEFF")) {
            s = s.substring(1);
        }
        return s;
    }

}