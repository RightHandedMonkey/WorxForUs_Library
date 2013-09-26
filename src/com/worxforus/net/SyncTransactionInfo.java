package com.worxforus.net;

import com.worxforus.json.JSONArrayWrapper;
import com.worxforus.json.JSONExceptionWrapper;
import com.worxforus.json.JSONObjectWrapper;


public class SyncTransactionInfo {
	public int totalItems = 0;
	public int retrievedItems = 0;
	public int itemLimitPerPage = 0;
	public int numPages = 0;
	public JSONObjectWrapper json;
	public JSONArrayWrapper jsonItemArray;
	public String toDatetime;
	public String fromDatetime;
	public int selectedPage=0;

	/**
	 * Run this command on the first transaction with the server.  For remaining commands run loadTransactionDetail since 
	 * some values shouldn't need to be recalculated
	 * @param netResult
	 * @return
	 * @throws JSONException
	 */
	public NetResult loadFirstTransactionDetails(NetResult netResult) throws JSONExceptionWrapper {
		totalItems = ((JSONObjectWrapper) netResult.object).getInt(SyncTableManager.ITEMS_TOTAL);
//		retrievedItems = ((JSONObject) netResult.object).getInt(SyncManager.ITEMS_SENT);
		itemLimitPerPage = ((JSONObjectWrapper) netResult.object).getInt(SyncTableManager.ITEM_LIMIT_PER_PAGE);
		//this will tell us what time the first request hit the server - we keep sending this on subsequent requests so the pages don't get
		//mixed up in case someone changes the database while we are synchronizing
		toDatetime = ((JSONObjectWrapper) netResult.object).getString(SyncTableManager.TO_DATETIME);
		//From time is the last time the user reported being synched
		fromDatetime = ((JSONObjectWrapper) netResult.object).getString(SyncTableManager.FROM_DATETIME);
//		json = (JSONObject) netResult.object;
//		jsonItemArray = json.getJSONArray(SyncManager.ITEMS);
//		selectedPage = ((JSONObject) netResult.object).getInt(SyncManager.SELECTED_PAGE);

		loadTransactionDetails(netResult);
		if (itemLimitPerPage > 0)
			numPages = (int) Math.ceil((double) totalItems / (double) itemLimitPerPage);
		
		return netResult;
	}
	
	public NetResult loadTransactionDetails(NetResult netResult) throws JSONExceptionWrapper {
		retrievedItems = ((JSONObjectWrapper) netResult.object).getInt(SyncTableManager.ITEMS_SENT);
		json = (JSONObjectWrapper) netResult.object;
		jsonItemArray = json.getJSONArray(SyncTableManager.ITEMS);
		selectedPage = ((JSONObjectWrapper) netResult.object).getInt(SyncTableManager.SELECTED_PAGE);
		return netResult;
	}
}
