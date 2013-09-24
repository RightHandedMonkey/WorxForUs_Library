package com.worxforus.net;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;


public interface SyncInterface<T> {
	public static final int DEFAULT_PAGE_ITEM_LIMIT = 100; //Default number of items to retrieve per page if none specified

	public abstract String getDownloadURL(String host);

	public abstract List<NameValuePair> getDownloadParams(int selPage, int limitPerPage, String lastSync);
	
	public abstract String getUploadURL(String host);
	
	public abstract List<NameValuePair> getUploadParams(ArrayList<T> objects);

	public abstract List<NameValuePair> fillInObjectParams(ArrayList<T> list, List<NameValuePair> params);
	
	public abstract ArrayList<T> parseJSONtoArrayList(JSONArray jsonArr);
}
