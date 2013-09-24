package com.worxforus.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SyncTransactionInfo {
	int totalItems = 0;
	int retrievedItems = 0;
	int itemLimitPerPage = 0;
	int numPages = 0;
	JSONObject json;
	JSONArray jsonItemArray;
	String toDatetime;
	String fromDatetime;
	int selectedPage=0;

	/**
	 * Run this command on the first transaction with the server.  For remaining commands run loadTransactionDetail since 
	 * some values shouldn't need to be recalculated
	 * @param netResult
	 * @return
	 * @throws JSONException
	 */
	public NetResult loadAllTransactionDetails(NetResult netResult) throws JSONException {
		totalItems = ((JSONObject) netResult.object).getInt(SyncManager.ITEMS_TOTAL);
		retrievedItems = ((JSONObject) netResult.object).getInt(SyncManager.ITEMS_SENT);
		itemLimitPerPage = ((JSONObject) netResult.object).getInt(SyncManager.ITEM_LIMIT_PER_PAGE);
		//this will tell us what time the first request hit the server - we keep sending this on subsequent requests so the pages don't get
		//mixed up in case someone changes the database while we are synchronizing
		toDatetime = ((JSONObject) netResult.object).getString(SyncManager.TO_DATETIME);
		//From time is the last time the user reported being synched
		fromDatetime = ((JSONObject) netResult.object).getString(SyncManager.FROM_DATETIME);
		json = (JSONObject) netResult.object;
		jsonItemArray = json.getJSONArray(SyncManager.ITEMS);
		selectedPage = ((JSONObject) netResult.object).getInt(SyncManager.SELECTED_PAGE);

		if (itemLimitPerPage > 0)
			numPages = (int) Math.ceil((double) totalItems / (double) itemLimitPerPage);
		
		return netResult;
	}
	
	public NetResult loadTransactionDetails(NetResult netResult) throws JSONException {
		retrievedItems = ((JSONObject) netResult.object).getInt(SyncManager.ITEMS_SENT);
		json = (JSONObject) netResult.object;
		jsonItemArray = json.getJSONArray(SyncManager.ITEMS);
		selectedPage = ((JSONObject) netResult.object).getInt(SyncManager.SELECTED_PAGE);
		return netResult;
	}
}
