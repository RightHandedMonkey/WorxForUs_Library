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
    
    /**
     * Returns the value at {@code index} if it exists and is an int or
     * can be coerced to an int. Returns 0 otherwise.
     */
    public int optInt(int index) {
        return arr.optInt(index, 0);
    }

}
