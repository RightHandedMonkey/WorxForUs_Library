package com.worxforus.db;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import android.content.Context;

public class TablePool {
	//private Application application;
	private static TablePool instance = new TablePool();
	private HashMap tableMap;

	// databases - these don't need to be static because they will be called from the static instance
	private TableVersionDb tableVersionTable;

	public static TableVersionDb getTableVersions(Context appContext, String dbName) {
		if (self().tableVersionTable == null) {
			self().tableVersionTable = new TableVersionDb(appContext, dbName);
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
			Constructor<T> construct;
			T tableInstance = null;
			try {
				construct = tableClass.getConstructor(Context.class);
				tableInstance = (T) construct.newInstance(c);
				self().tableMap.put(tableInstance, tableClass);
			} catch (Exception e) {
				throw new RuntimeException("Could not create the table, "+e.getMessage());
			}
			return tableInstance;
		} else {
			return (T) self().tableMap.get(tableClass);
		}
	}

}
