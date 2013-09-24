package com.worxforus.db;

import java.util.ArrayList;

import com.worxforus.Result;

public abstract class TableInterface<T> {

	//DO NOT INCREMENT THIS VALUE - Use versions for individual tables and call
	//TableManager.verifyTable(TableClass);
	public static final int DATABASE_VERSION = 1; //to use in SQLiteOpenHelper - this is not used for migrations

	protected volatile boolean table_checked=false; //Used as a flag by the TableManager so table is only checked once per app instace
	public abstract Result openDb();
	public abstract void closeDb();

	/**
	 * Check TableVersionDb to ensure we have the correct version
	 * @return boolean
	 */
	//protected abstract boolean need_table_upgrade();
	
	/**
	 * When a table is updated, save the new version to the TableVersion Db
	 */
	//protected abstract void modify_table_version();
	
	//public abstract boolean checkIfTableExists();
	
	public abstract void createTable();
	
	public abstract void updateTable(int last_version);

	public abstract String getTableName();
	
	/**
	 * Returns the value of the current version of the table structure used by the code
	 * - query BaseTableVersionDBAdapter for the version of table currently running in the database
	 * @return int
	 */
	public abstract int getTableCodeVersion();
		
	public boolean isTableVerified() {
		return table_checked;
	}

	public void setTableVerified() {
		table_checked=true;
	}
	public abstract Result insertOrUpdate(T t);

	public abstract Result insertOrUpdateArrayList(ArrayList<T> t);

}
