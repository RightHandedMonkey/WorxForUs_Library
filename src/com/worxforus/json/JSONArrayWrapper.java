package com.worxforus.json;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONArrayWrapper {
	private JSONArray arr;

	//change this implementation when appropriate
	public JSONArrayWrapper(JSONArray array) {
		super();
		this.arr = array;
	}
	
    /**
     * Returns the number of values in this array.
     */
    public int length() {
        return arr.length();
    }

    public JSONObjectWrapper getJSONObject(int index) throws JSONExceptionWrapper {
    	try {
    		JSONObject object = arr.getJSONObject(index);
    		JSONObjectWrapper wrapper = new JSONObjectWrapper(object);
    		return wrapper;
		} catch (JSONException e) {
			throw new JSONExceptionWrapper(e.getMessage());
		}

    }
	
}
