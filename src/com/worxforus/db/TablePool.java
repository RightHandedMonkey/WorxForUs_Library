package com.worxforus.db;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.worxforus.Utils;

import android.content.Context;

/**
 * Database table holder - singleton style
 * 
 * Usage:
 * MyTable myTable = TablePool.getTable(MyTable.class, getContext());
 * @author sbossen
 *
 */
public class TablePool {
	//private Application application;
	private static TablePool instance = new TablePool();
	private HashMap tableMap;

	// databases - these don't need to be static because they will be called from the static instance
	private TableVersionDb tableVersionTable;

	public static TableVersionDb getTableVersions(Context appContext, String dbName) {
		if (self().tableVersionTable == null) {
			self().tableVersionTable = new TableVersionDb(appContext.getApplicationContext(), dbName);
		}
		return self().tableVersionTable;
	}

	private TablePool() {
		tableMap = new HashMap();
	}

	public static TablePool self() {
		return instance;
	}
	
	public static <T> T getTable(Class<T> tableClass, Context c) {
		if (self().tableMap.get(tableClass) == null) {
			Utils.LogD(TablePool.class.getName(), "Creating table connection");

			Constructor<T> construct;
			T tableInstance = null;
			try {
				construct = tableClass.getConstructor(Context.class);
				tableInstance = (T) construct.newInstance(c.getApplicationContext());
				self().tableMap.put(tableClass, tableInstance);
			} catch (Exception e) {
				throw new RuntimeException("Could not create the table, "+e.getMessage());
			}
			return tableInstance;
		} else {
			Utils.LogD(TablePool.class.getName(), "Reusing existing table connection");
			return (T) self().tableMap.get(tableClass);
		}
	}

}
