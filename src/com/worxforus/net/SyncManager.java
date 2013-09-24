package com.worxforus.net;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.worxforus.Result;
import com.worxforus.SyncEntry;
import com.worxforus.Utils;
import com.worxforus.db.TableInterface;
import com.worxforus.db.TableManager;
import com.worxforus.db.TableSyncDb;


/**
 * This class performs the actual synching with the network
 * @author sbossen
 *
 */
public class SyncManager {
	
	//Sync Identifiers
	public static final String ITEM_LIMIT_PER_PAGE = "item_limit_per_page"; //number of items to send for a given page
	public static final String ITEMS_TOTAL = "items_total"; //number of items found for given request (not including paging)
	public static final String SELECTED_PAGE = "selected_page"; //the current page of items to retrieve based on the number of items to send on a given page
	public static final String ITEMS_SENT = "items_sent"; //The number of items actually sent for this request - can be equal to or less that the page item limit
	public static final String ITEMS = "items" ; //The items requested will be stored under this JSON header
	public static final String TO_DATETIME = "to_datetime"; //Calculated on server, the datetime that the start of request was received - sent back to the device for later synchronization requests
	public static final String FROM_DATETIME = "from_datetime"; //The datetime that the device previously has data for. Will be blank if never synchronized.

	public static final int SYNC_TTL_SECS = 86400; //86400 sec=24 hours - This is how long we need to have from the last sync for recommending a sync

	public <T> Result addToDatabaseHelper(Context c, ArrayList<T> objects, String dbName, TableInterface<T> table) {
		TableManager.acquireConnection(c, dbName, table);
		Result r = table.insertOrUpdateArrayList(objects);
		TableManager.releaseConnection(table);
		return r;
	}
	
	public <T> Result handleSyncTable(Context c, String host, String dbName, TableInterface<T> table, SyncInterface<T> syncObject, int limitPerPage, String lastSync) {
		Result r = new Result();
		synchronized(table) {
			//make net call
			String url = syncObject.getDownloadURL(host);
			List<NameValuePair> params = syncObject.getDownloadParams(0, limitPerPage, lastSync);
			NetResult netResult = NetHandler.handle_post_with_retry(url, params , NetHandler.NETWORK_DEFAULT_RETRY_ATTEMPTS);
			//get json array
			r.success = netResult.net_success;
			SyncTransactionInfo syncInfo = new SyncTransactionInfo();
			
			if (netResult.net_success) {
				
				int total_items = 0;
				int retrieved_items = 0;
				int item_limit_per_page = 0;
				int num_pages = 0;
				try {
					total_items = ((JSONObject) netResult.object).getInt(ITEMS_TOTAL);
					retrieved_items = ((JSONObject) netResult.object).getInt(ITEMS_SENT);
					item_limit_per_page = ((JSONObject) netResult.object).getInt(ITEM_LIMIT_PER_PAGE);
					JSONObject json_tmp = (JSONObject) netResult.object;
					JSONArray json_tmp_arr = json_tmp.getJSONArray(ITEMS);
					ArrayList<T> parseObjects = syncObject.parseJSONtoArrayList(json_tmp_arr);
					//check that retrieve items equals what we found
					if (parseObjects.size() != retrieved_items) {
						r.success = false;
						r.error = "Retrieve "+syncObject.getClass().getName()+" items "+parseObjects.size()+" did not match expected value: "+retrieved_items;
						Log.e("com.worxforus.net", r.error);
						return r;
					}
					//store data into database
					r.add_results_if_error(addToDatabaseHelper(c, parseObjects, dbName, table), "Could not add objects to database");
					
				} catch (JSONException e) {
					r.error += "Could not parse JSON "+syncObject.getClass().getName()+" info:"+e.getMessage();
					r.success = false;
					Log.e("com.worxforus.net", e.getMessage());
				}
				if (item_limit_per_page > 0)
					num_pages = (int) Math.ceil((double) total_items / (double) item_limit_per_page);
				//process current page of data

				for (int cur_page = 1; cur_page < num_pages; cur_page++) {
				}

			}
			//calc size
			//process until all items are collected
			//update synchronized date
		}
		return r;
	}
	
	public boolean checkIfSyncNeeded(Context c, String dbName, String tableName, TableSyncDb syncDb) {
		Result r = new Result();
        Date curDate = Utils.getCurrentDatetime();
        TableManager.acquireConnection(c, dbName, syncDb);
        SyncEntry info = syncDb.getTableSyncData(tableName);
        Date syncDate = Utils.getDatetimeObject(info.getDownloadDate());
        TableManager.releaseConnection(syncDb);

		if (Utils.getDateDiff(syncDate, curDate, TimeUnit.SECONDS) > SYNC_TTL_SECS) {
			return true;
		} else
			return false;
	}
}
