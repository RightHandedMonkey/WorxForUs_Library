package com.worxforus.net;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.util.Log;

import com.worxforus.Pool;
import com.worxforus.Result;
import com.worxforus.SyncEntry;
import com.worxforus.Utils;
import com.worxforus.db.TableInterface;
import com.worxforus.db.TableManager;
import com.worxforus.db.TableSyncDb;
import com.worxforus.json.JSONExceptionWrapper;
import com.worxforus.net.NetAuthentication.NetAuthenticationHelper;


/**
 * This class manages the actual syncing with the network
 * @author sbossen
 *
 */
public class SyncTableManager {

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
        Log.i(this.getClass().getName(), "Adding retrieved network objects to:"+table.getTableName());
		TableManager.acquireConnection(c, dbName, table);
		Result r = table.insertOrUpdateArrayList(objects);
		TableManager.releaseConnection(table);
		return r;
	}

	public <T> Result handleParsedObjectsHelper(SyncTransactionInfo sync, Context c, String dbName, TableInterface<T> table, SyncInterface<T> syncObject, Pool<T> pool) {
        Log.i(this.getClass().getName(), "Parsing network "+syncObject.getClass().getName()+" objects");
		Result r = syncObject.parseJSONtoArrayList(sync.jsonItemArray, pool);
		
		ArrayList<T> parseObjects = (ArrayList<T>) r.object;
		//check that retrieve items equals what we found
		if (parseObjects.size() != sync.retrievedItems) {
			r.success = false;
			r.error = "Retrieve "+sync.getClass().getName()+" items "+parseObjects.size()+" did not match expected value: "+sync.retrievedItems;
			Log.e(this.getClass().getName(), r.error);
			return r;
		}
		//store data into database
		r.add_results_if_error(addToDatabaseHelper(c, parseObjects, dbName, table), "Could not add objects to database");
		//once objects have been added to the database, we can release all objects back to the pool
		pool.freeAll(parseObjects);
		return r;
	}
	
	public <T> Result handleSyncTableDownload(Context c, String host, String dbName, TableInterface<T> table, SyncInterface<T> syncObject, int limitPerPage, String lastSync) {
        Log.i(this.getClass().getName(), "Performing sync network operation for:"+table.getTableName());
        //create object pool to pass around
        Pool<T> pool = new Pool<T>(syncObject, limitPerPage);
		Result r = new Result();

		//make net call
			String url = syncObject.getDownloadURL(host);
			//For the first call we don't know what the toDate is because we get that from the server
			List<NameValuePair> params = syncObject.getDownloadParams(0, limitPerPage, lastSync, "");
			NetResult netResult;
			if (syncObject.requireAuthOnDownload()) {
				netResult = AuthNetHandler.handleAuthPostWithRetry(url, params , NetHandler.NETWORK_DEFAULT_RETRY_ATTEMPTS);
			} else {
				netResult = NetHandler.handlePostWithRetry(url, params , NetHandler.NETWORK_DEFAULT_RETRY_ATTEMPTS);
			}
			NetHandler.handleGenericJsonResponseHelper(netResult, this.getClass().getName());
			//get json array
			r.success = netResult.net_success;
			SyncTransactionInfo syncInfo = new SyncTransactionInfo();
			
			if (netResult.net_success) {
				
				try {
					syncInfo.loadFirstTransactionDetails(netResult);
					Result handleResult = handleParsedObjectsHelper(syncInfo, c, dbName, table, syncObject, pool);
					r.add_results_if_error(handleResult, handleResult.error);
				} catch (JSONExceptionWrapper e) {
					r.error += "Could not parse JSON "+syncObject.getClass().getName()+" info:"+e.getMessage();
					r.success = false;
					Log.e(this.getClass().getName(), e.getMessage());
				}

				//close first network connection
				netResult.closeNetResult();
				//process until all items are collected
				for (int cur_page = 1; cur_page < syncInfo.numPages; cur_page++) {
					if(netResult.net_success) {
						//now we know what the toDate should be so send that along
						params = syncObject.getDownloadParams(cur_page, syncInfo.itemLimitPerPage, lastSync, syncInfo.toDatetime);
						
						if (syncObject.requireAuthOnDownload()) {
							netResult = AuthNetHandler.handleAuthPostWithRetry(url, params , NetHandler.NETWORK_DEFAULT_RETRY_ATTEMPTS);
						} else {
							netResult = NetHandler.handlePostWithRetry(url, params , NetHandler.NETWORK_DEFAULT_RETRY_ATTEMPTS);
						}
						NetHandler.handleGenericJsonResponseHelper(netResult, this.getClass().getName());
						if (netResult.net_success) {
							try {
								syncInfo.loadTransactionDetails(netResult);
								Result handleResult = handleParsedObjectsHelper(syncInfo, c, dbName, table, syncObject, pool);
								r.add_results_if_error(handleResult, handleResult.error);
							} catch (JSONExceptionWrapper e) {
								r.error += "Could not parse JSON "+syncObject.getClass().getName()+" info:"+e.getMessage();
								r.success = false;
								Log.e(this.getClass().getName(), e.getMessage());
							}
						} else {
							//we got a network error, because r.success was true, but something went wrong
							r.success = false;
							r.add_error(netResult.getLogEntry(), false);
						}
					} else {
						//we got a network error, because r.success was true, but something went wrong
						r.success = false;
						r.add_error(netResult.getLogEntry(), false);
					}
					
				}
				//update synchronized date if no errors occurred
				if (r.success) {
					SyncEntry se= TableManager.getTableSyncInfo(c, dbName, table.getTableName());
					se.setDownloadDate(syncInfo.toDatetime);
					r.add_results_if_error(TableManager.setTableSyncInfo(c, dbName, se), "Could not update table ["+table.getTableName()+"] with latest sync date of: "+syncInfo.toDatetime);
				}

			} else {
				r.technical_error = netResult.getLogEntry();
				r.error = "Could not reach page for ["+table.getTableName()+"] because of a network error.";
				r.success = false;
			}
        Log.i(this.getClass().getName(), "Finished sync network operation, result:"+r.toString());

		return r;
	}
	
	/**
	 * This function needs to be updated because it checks against the system time when the download
	 * date is generated by the server
	 * @param c
	 * @param dbName
	 * @param tableName
	 * @param syncDb
	 * @return
	 */
	@Deprecated
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
