package com.worxforus.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is used to wrap any JSON calls - this way when a  faster implementation is to be used,
 * we can just connect it here without refactoring the entire project in these three files:
 * JSONArrayWrapper.java
 * JSONExceptionWrapper.java
 * JSONObjectWrapper.java
 * 
 * @author sbossen
 *
 */
public class JSONObjectWrapper {

	//replace implementation here when needed
	private JSONObject obj;
	
	public JSONObjectWrapper(JSONObject object) {
		this.obj = object;
	}
	
    public JSONObjectWrapper(String json) throws JSONExceptionWrapper {
    	//replace implementation here when needed
    	try {
			obj = new JSONObject(json);
		} catch (JSONException e) {
			throw new JSONExceptionWrapper(e.getMessage());
		}
    }
    
    public Object get(String name) throws JSONExceptionWrapper {
    	try {
    		return obj.get(name);
    	} catch (JSONException e) {
    		throw new JSONExceptionWrapper(e.getMessage());
    	}
    }
    
    public boolean has(String name) {
    	return this.obj.has(name);
    }
    
    public JSONObjectWrapper getJSONObject(String name) throws JSONExceptionWrapper {
    	try {
    		return new JSONObjectWrapper(obj.getJSONObject(name));
    	} catch (JSONException e) {
    		throw new JSONExceptionWrapper(e.getMessage());
    	}
    }
    
    public int getInt(String name) throws JSONExceptionWrapper {
    	try {
    		return obj.getInt(name);
    	} catch (JSONException e) {
    		throw new JSONExceptionWrapper(e.getMessage());
    	}
    }
    
    public boolean getBoolean(String name) throws JSONExceptionWrapper {
    	try {
    		return obj.getBoolean(name);
    	} catch (JSONException e) {
    		throw new JSONExceptionWrapper(e.getMessage());
    	}
    }
    
    public String getString(String name) throws JSONExceptionWrapper {
    	try {
			return obj.getString(name);
		} catch (JSONException e) {
			throw new JSONExceptionWrapper(e.getMessage());
		}
    }

    public JSONArrayWrapper getJSONArray(String name) throws JSONExceptionWrapper {
    	try {
    		JSONArray arr =obj.getJSONArray(name);
    		JSONArrayWrapper wrapper = new JSONArrayWrapper(arr);
			return wrapper; 
		} catch (JSONException e) {
			throw new JSONExceptionWrapper(e.getMessage());
		}
    }
}
