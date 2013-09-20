package com.worxforus.db;



import com.worxforus.Result;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TableVersionDb extends TableInterface {

	public static final int TABLE_VERSION = 1; 
	
	//DATABASE_VERSION is used by android SQLiteHelper, but we are not using it here because it is database wide
	//and we want to control version of individual tables.
	//User needs to call verify tables from the TableManager instead.

	public String database_name;
	
	public static final String DATABASE_TABLE = "table_meta_data";

	//table fields
	static int i=0;

	public static final String  KEY_TABLE_NAME="name";
	public static final int  	KEY_TABLE_NAME_COL=i++;
	public static final String  KEY_TABLE_VERSION="version";
	public static final int 	KEY_TABLE_VERSION_COL=i++;

	private static final String DATABASE_CREATE = "CREATE TABLE "+DATABASE_TABLE+" ( " +
	KEY_TABLE_NAME+"			TEXT PRIMARY KEY,"+
	KEY_TABLE_VERSION+"    		INTEGER NOT NULL"+
	")";

	
	private SQLiteDatabase db;
	//holds the app using the db
	private TableVersionDbHelper dbHelper;
	protected int last_version=0;
	
	/**
	 * 
	 * @param _context - use app context here
	 * @param db_name - specify database name to connect to
	 */
	public TableVersionDb(Context _context, String db_name) {
		this.database_name = db_name;
		dbHelper = new TableVersionDbHelper(_context, this.database_name, null, DATABASE_VERSION);
	}
	 
	/**
	 * This method should be called from the ConnectionManager which allows only a limited number of open 
	 * database helper objects (ie. initially a single connection - all others wait)
	 * @return result object with SQLiteDatabase in result.object if successful
	 */
	public synchronized Result openDb() {
		Result r = new Result();
		try {
			db=dbHelper.getWritableDatabase();
		} catch (SQLException e) {
			//SQLiteDatabaseLockedException is only available starting in Honeycomb - here we fake it
			if (e.getClass().getName().contains("SQLiteDatabaseLockedException")) {
				//design decision here.  Db should not be locked if I wait for the previous activity to close
				//fully before opening the next one.
					r.error = "Database is locked. "+e.getMessage();
					r.success = false;
			}	else {
				r.error = "Could not open database. "+e.getMessage();
				r.success = false;
			}
			Log.e(this.getClass().getName(), r.error);
		}
		return r;
	}
	
	public synchronized void closeDb() { db.close(); }
	

	public void wipeTable() {
		synchronized (DATABASE_TABLE) {
			db.delete(DATABASE_TABLE, null, null);
		}
	}
	
	protected boolean needTableUpgrade() {
		boolean needs_upgrade=false;
		last_version = getTableVersion(DATABASE_TABLE);
		if (TABLE_VERSION > last_version) {
			needs_upgrade = true;
		}
		return needs_upgrade;
	}
	
	/**
	 * When a table is updated, save the new version to the TableVersion Db
	 */
	protected void modifyTableVersion() {
		insertOrUpdate(DATABASE_TABLE, TABLE_VERSION);
	}
	
	/**
	 * Used to merge data from web to local database
	 * @param c
	 * @return
	 */
	public Result insertOrUpdate(String table_name, int table_ver) {
		Result r = new Result();
		synchronized (DATABASE_TABLE) {
			try {
				int index = (int)db.replaceOrThrow(DATABASE_TABLE, null, getContentValues(table_name, table_ver));
				r.last_insert_id = index;
			} catch (SQLException s) {
				r.error = s.getMessage();
				r.success = false;
			}
		}
		return r;
	}
	
	
	public Cursor getTableVersionsCursor() {
		return db.query(DATABASE_TABLE, 
				null, 
				null, null, null, null, KEY_TABLE_NAME);
	}
	
	public static int getVersionFromCursor(Cursor record) {
		int ver =-1;
		if (record.getColumnCount() > 1){ //make sure data is in the result.  Read only first entry
			ver = (record.getInt(KEY_TABLE_VERSION_COL));
		}
		return ver;
	}

	
	public boolean removeEntry(String name) {
		synchronized (DATABASE_TABLE) {
			return db.delete(DATABASE_TABLE, 
				KEY_TABLE_NAME+" = ? ", new String[] {name})> 0;
		}
	}

	public synchronized int getTableVersion(String name) {
		int ver = -1;
		Cursor result= db.query(DATABASE_TABLE, 
				null, 
				KEY_TABLE_NAME+" = ? ", new String [] {name}, null, null, null);
		
		if (result.moveToFirst() ) { //make sure data is in the result.  Read only first entry
			ver = getVersionFromCursor(result);
		}
		result.close();
		return ver;
	}
	

    /** returns a ContentValues object for database insertion
     * 
     * @return
     */
    public ContentValues getContentValues(String name, int version) {
    	ContentValues vals = new ContentValues();
    	//prepare info for db insert/update
    	vals.put(KEY_TABLE_NAME, name);
    	vals.put(KEY_TABLE_VERSION, version);
    	
		return vals;
    }
    
	
	public boolean checkIfTableExists(String tableName) {
//		String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+DATABASE_TABLE+"'";
    	String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+tableName+"'";
		Cursor result = db.rawQuery(query, null);
		int count = result.getCount();
		result.close();
		return count > 0;
    }
    
	
	public void createTable() {
		dbHelper.onCreate(db);
	}
	
	public void updateTable(int last_version) {
		dbHelper.onUpgrade(db, last_version, TABLE_VERSION);
	}
	
	
	public String getTableName() {
		return DATABASE_TABLE;
	}

	public int getTableCodeVersion() {
		return TABLE_VERSION;
	}


	//================------------> helper class <-----------==============\\
	private static class TableVersionDbHelper extends SQLiteOpenHelper {

		public TableVersionDbHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			
			//Add itself to the entry
	    	ContentValues vals = new ContentValues();
	    	vals.put(KEY_TABLE_NAME, TableVersionDb.DATABASE_TABLE);
	    	vals.put(KEY_TABLE_VERSION, TableVersionDb.TABLE_VERSION);

			db.insert(DATABASE_TABLE, null, vals);
		}

		/**
		 * Best usage is to use a rails-like migration.
		 * if (oldVersion < 2 ) { this.update_to_version2(db); //code to change version 1 to 2 }
		 * if (oldVersion < 3 ) { this.update_to_version3(db); //code to change version 2 to 3 } etc...
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			//called when the version of the existing db is less than the current
			Log.w(this.getClass().getName(), "Upgrading db from "+oldVersion+" to "+newVersion);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
			onCreate(db);
		}
		
	}

		
}
