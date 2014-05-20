package com.worxforus.db;

import com.worxforus.Result;
import com.worxforus.SyncEntry;
import com.worxforus.Utils;
import com.worxforus.VersionEntry;

import junit.framework.Assert;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;


/*
 * Provides serialized access to the database & manages individual table versions
 * 
 * Usage:
 * CustomTable table = new CustomTable(context); //CustomTable extends TableInterface
 * TableManager.acquireConnection(context, db_name, table);
 * table.yourTableOperationHere(..);
 * TableManager.releaseConnection(table);
 * 
 * All table meta information is stored in tableVersionDb 'table_meta_data'.
 * Table sync information is stored in tableSyncDb 'table_sync'.
 */

public class TableManager {
	private static TableManager instance = new TableManager();
	private ConnectionLimitHelper connHelper;
	
	//This table is special because all database tables store there versions here - this is checked first
	//But we don't want to check itself against itself - so load it before all other dbs
	private TableVersionDb tableVersionDb; 
	private TableSyncDb tableSyncDb;
	
	private TableManager() {
	}

	private static TableManager self() {
		return instance;
	}
	
	/**
	 * Check if the database exist
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	public static boolean checkDatabaseExists(Context c, String dbName) {
		String path = c.getDatabasePath(dbName).getAbsolutePath();
	    SQLiteDatabase checkDB = null;
	    try {
	        checkDB = SQLiteDatabase.openDatabase(path, null,
	                SQLiteDatabase.OPEN_READONLY);
	        checkDB.close();
	    } catch (SQLiteException e) {
	        // database doesn't exist yet.
	    }
	    return checkDB != null ? true : false;
	}
	
	private static ConnectionLimitHelper getConnectionHelper() {
		if (self().connHelper == null) {
			self().connHelper = ConnectionLimit.getConnectionHelper();
		}
		return self().connHelper;
	}
	
	protected synchronized static TableVersionDb getTableVersionDB(Context appContext, String dbName) {
		Assert.assertNotNull(appContext);
		if (self().tableVersionDb == null) {
			self().tableVersionDb = new TableVersionDb(appContext, dbName);
		}
		return self().tableVersionDb;
	}
	
	protected synchronized static TableSyncDb getTableSyncDB(Context appContext, String dbName) {
		Assert.assertNotNull(appContext);
		if (self().tableSyncDb == null) {
			self().tableSyncDb = new TableSyncDb(appContext, dbName);
		}
		return self().tableSyncDb;
	}

	/**
	 * This function verifies that the table has been created and that the upgrade modifications have been performed on it
	 * Then it locks the table for use
	 * Use: releaseConnection(table) when finished performing operations on the table
	 * @param appContext
	 * @param dbName
	 * @param table
	 * @return
	 */
	public static Result acquireConnection(Context appContext, String dbName, TableInterface table) {
		Assert.assertNotNull(table);
		//if table has not been verified and is not TableVersionDbHelper, check it before connecting
		if (!table.isTableVerified()) {
			//Check against the table version table
			verifyTable(appContext, dbName, table);
		}
		return self().getConnectionHelper().acquire(table);
	}
	
	/**
	 * Call to declare you are done performing operations on a table
	 * @param table
	 */
	public static void releaseConnection(TableInterface table) {
		self().getConnectionHelper().release(table);
	}
	
	/**
	 * This functions checks to see if the table has been created and creates it if needed.
	 * It performs the upgrade to tables as declared in the class file.
	 * @param appContext
	 * @param dbName
	 * @param table
	 */
	private static synchronized void verifyTable(Context appContext, String dbName, TableInterface table) {
		if (!checkIfTableExists(appContext, dbName, table.getTableName()) ) { //make sure table exists
			//bypass acquireConnection and use direct call in case table is the tableVersionDb b/c we'd be stuck in a loop
			Log.d(self().getClass().getName(), "Table: "+table.getTableName()+" was not found in db: "+dbName+"... creating table.");
			self().getConnectionHelper().acquire(table);
			table.createTable(); //create table for the first time
			self().getConnectionHelper().release(table);
			//store table version in meta table
			modifyTableVersion(appContext, dbName, table.getTableName(), table.getTableCodeVersion());
		} else {
			int db_version = getTableVersion(appContext, dbName, table.getTableName());
			int version = table.getTableCodeVersion();
			if (needTableUpgrade(table.getTableName(), db_version, version) ) {
				Log.d(self().getClass().getName(), "Table: "+table.getTableName()+", old version: "+db_version+", new version: "+version+" needs an update in db: "+dbName+"... creating table.");
				//table needs update, so update it
				self().getConnectionHelper().acquire(table);
				table.updateTable(db_version);
				self().getConnectionHelper().release(table);
				//store table version in meta table
				modifyTableVersion(appContext, dbName, table.getTableName(), table.getTableCodeVersion());
			}
		}
		//mark as updated
		table.setTableVerified();		
	}
	
	//===========================--------------------> Verification Methods <-----------------====================\\
	private static boolean checkIfTableExists(Context appContext, String dbName, String tableName) {
		self().getConnectionHelper().acquire(self().getTableVersionDB(appContext, dbName));
		boolean result = self().getTableVersionDB(appContext, dbName).checkIfTableExists(tableName);
		self().getConnectionHelper().release(self().getTableVersionDB(appContext, dbName));
		return result;
	}

	/**
	 * Check TableVersionDb to ensure we have the correct version
	 * @return boolean
	 */
	private static boolean needTableUpgrade(String table_name, int db_version, int latest_version) {
		if (latest_version > db_version) {
			return true;
		}
		return false;
	}
	
	/**
	 * When a table is updated, save the new version to the TableVersion Db
	 */
	private static void modifyTableVersion(Context appContext, String dbName, String tableName, int currentVersion) {
		self().getConnectionHelper().acquire(self().getTableVersionDB(appContext, dbName));
		self().getTableVersionDB(appContext, dbName).insertOrUpdate(new VersionEntry(tableName, currentVersion));
		self().getConnectionHelper().release(self().getTableVersionDB(appContext, dbName));
	}

	private static int getTableVersion(Context appContext, String dbName, String tableName) {
		self().getConnectionHelper().acquire(self().getTableVersionDB(appContext, dbName));
		int version = self().getTableVersionDB(appContext, dbName).getTableVersion(tableName);
		self().getConnectionHelper().release(self().getTableVersionDB(appContext, dbName));
		return version;
	}
	

	//===========================--------------------> Check Table Sync Dates <-----------------====================\\
	public static SyncEntry getTableSyncInfo(Context appContext, String dbName, String table) {
		TableSyncDb syncTable = self().getTableSyncDB(appContext, dbName);
		self().acquireConnection(appContext, dbName, syncTable);
		SyncEntry info = syncTable.getTableSyncData(table);
		//check if entry exists, if not then create it
		if (!info.getTableName().equalsIgnoreCase(table)) {
			//save it since it doesn't exist
			info.setTableName(table);
			syncTable.insertOrUpdate(info);
		}
		self().releaseConnection(syncTable);
		return info;
	}

	public static Result setTableSyncInfo(Context appContext, String dbName, SyncEntry info) {
		Result r = new Result();
		TableSyncDb syncTable = self().getTableSyncDB(appContext, dbName);
		self().acquireConnection(appContext, dbName, syncTable);
		r = syncTable.insertOrUpdate(info);
		//check if entry exists, if not then create it
		self().releaseConnection(syncTable);
		return r;
	}
	
	public static Result resetSyncData(Context appContext, String dbName) {
		Result r = new Result();
		TableSyncDb syncTable = self().getTableSyncDB(appContext, dbName);
		self().acquireConnection(appContext, dbName, syncTable);
		r = syncTable.resetSyncData();
		self().releaseConnection(syncTable);
		return r;
	}
	
	/**
	 * Reset all the database connections
	 */
	public synchronized static void invalidate() {
		Utils.LogD(TableManager.class.getName(), "Invalidating database connections");
		self().tableVersionDb = null;
		self().tableSyncDb = null;
	}

	
}