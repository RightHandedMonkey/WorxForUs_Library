package com.worxforus.db;

import com.worxforus.Result;

import junit.framework.Assert;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;


/*
 * - Database helper singleton style
 * table = TableManager.getTable(TableClass);
 * ConnectionManager.acquireConnection(table);
 */

public class TableManager {
	private static TableManager instance = new TableManager();
	//TODO: Would like to add a nice database table pool here of (TableInterface) objects to have a single place
	//to check if database tables have been created and verified.
	//for now: Create a separate TablePool class in your application
	private ConnectionLimitHelper connHelper;
	
	//This table is special because all database tables store there versions here - this is checked first
	//But we don't want to check itself against itself - so load it before all other dbs
	private TableVersionDb tableVersionDb; 
	
	private TableManager() {
	}

	private static TableManager self() {
		return instance;
	}

	private static ConnectionLimitHelper getConnectionHelper() {
		if (self().connHelper == null) {
			self().connHelper = ConnectionLimit.getConnectionHelper();
		}
		return self().connHelper;
	}
	
	public static TableVersionDb getTableVersionDB(Context appContext, String dbName) {
		Assert.assertNotNull(appContext);
		if (self().tableVersionDb == null) {
			self().tableVersionDb = new TableVersionDb(appContext, dbName);
		}
		return self().tableVersionDb;
	}

	public static Result acquireConnection(Context appContext, String dbName, TableInterface table) {
		Assert.assertNotNull(table);
		//if table has not been verified and is not TableVersionDbHelper, check it before connecting
		if (!table.isTableVerified()) {
			//Check against the table version table
			verifyTable(appContext, dbName, table);
		}
		return self().getConnectionHelper().acquire(table);
	}
	
	public static void releaseConnection(TableInterface table) {
		self().getConnectionHelper().release(table);
	}
	
	private static void verifyTable(Context appContext, String dbName, TableInterface table) {
		if (!checkIfTableExists(appContext, dbName, table.getTableName()) ) { //make sure table exists
			//bypass acquireConnection and use direct call in case table is the tableVersionDb b/c we'd be stuck in a loop
			self().getConnectionHelper().acquire(table);
			table.createTable(); //create table if not
			self().getConnectionHelper().release(table);
			//store table version in meta table
			modifyTableVersion(appContext, dbName, table.getTableName(), table.getTableCodeVersion());
		} else {
			int db_version = getTableVersion(appContext, dbName, table.getTableName());
			if (needTableUpgrade(table.getTableName(), db_version, table.getTableCodeVersion()) ) {
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
	/*
	public static TableInterface getTable() {
		//TODO: Need to replace table pool with a version here that persists one copy of each table class used and returns that
	}
	*/
	
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
		self().getTableVersionDB(appContext, dbName).insertOrUpdate(tableName, currentVersion);
		self().getConnectionHelper().release(self().getTableVersionDB(appContext, dbName));
	}

	private static int getTableVersion(Context appContext, String dbName, String tableName) {
		self().getConnectionHelper().acquire(self().getTableVersionDB(appContext, dbName));
		int version = self().getTableVersionDB(appContext, dbName).getTableVersion(tableName);
		self().getConnectionHelper().release(self().getTableVersionDB(appContext, dbName));
		return version;
	}
	
	/*
	protected static void verifyTable(Object db) {
		Assert.assertTrue(db instanceof BaseDBInterface);
		if (!((BaseDBInterface) db).isTableVerified()) { //check that table hasn't already been verified
			if (!((BaseDBInterface) db).check_if_table_exists()) { //make sure table exists
				((BaseDBInterface) db).create_database(); //create table if not
				//store table version in meta table
				modify_table_version(((BaseDBInterface) db).get_table_name(), ((BaseDBInterface) db).get_table_code_version());
			} else {
				int db_version = get_table_version(((BaseDBInterface) db).get_table_name());
				if (need_table_upgrade(((BaseDBInterface) db).get_table_name(), db_version, ((BaseDBInterface) db).get_table_code_version()) ) {
					//table needs update, so update it
					((BaseDBInterface) db).update_database(db_version);
					//store table version in meta table
					modify_table_version(((BaseDBInterface) db).get_table_name(), ((BaseDBInterface) db).get_table_code_version());
				}
			}
			//mark as updated
			((BaseDBInterface) db).setTableVerified();
		} 
	}
	*/
	//===========================--------------------> Connection Manager Helper <-----------------====================\\


	
}