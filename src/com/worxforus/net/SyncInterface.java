package com.worxforus.net;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;

import com.worxforus.Pool;
import com.worxforus.Pool.PoolObjectFactory;
import com.worxforus.Result;
import com.worxforus.json.JSONArrayWrapper;


public interface SyncInterface<T> extends PoolObjectFactory<T> {
	public static final int DEFAULT_PAGE_ITEM_LIMIT = 100; //Default number of items to retrieve per page if none specified

	public abstract String getDownloadURL(String host);

	public abstract List<NameValuePair> getDownloadParams(int selPage, int limitPerPage, String lastSync, String toDate);
	
	public abstract String getUploadURL(String host);
	
	public abstract List<NameValuePair> getUploadParams(ArrayList<T> objects);

	public abstract List<NameValuePair> fillInObjectParams(ArrayList<T> list, List<NameValuePair> params);
	
	/**
	 * Takes a json array and converts to an ArrayList of the object type
	 * @param jsonArr
	 * @return Result object - with result.object = ArrayList<T> of the given type
	 */
	public abstract Result parseJSONtoArrayList(JSONArrayWrapper jsonArr, Pool<T> pool);
	
	public abstract boolean requireAuthOnDownload();
	
	public abstract boolean requireAuthOnUpload();
}
