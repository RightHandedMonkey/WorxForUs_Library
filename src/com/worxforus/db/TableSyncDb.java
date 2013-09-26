package com.worxforus.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.worxforus.Result;
import com.worxforus.SyncEntry;
import com.worxforus.db.TableInterface;

public class TableSyncDb extends TableInterface<SyncEntry> {

	public static final String DATABASE_TABLE = "table_sync";
	public static final int TABLE_VERSION = 1; 
	// 1 - Initial version
	public String databaseName;

	// table fields
	static int i = 0; // counter for field index
	public static final String  SYNC_TABLE_NAME="table_name";
	public static final int  	SYNC_TABLE_NAME_COL=i++;
	public static final String  SYNC_TABLE_DOWNLOAD_DATE="download_date";
	public static final int 	SYNC_TABLE_DOWLOAD_DATE_COL=i++;
	public static final String  SYNC_TABLE_UPLOAD_DATE="upload_date";
	public static final int 	SYNC_TABLE_UPLOAD_DATE_COL=i++;
	
	private static final String DATABASE_CREATE = "CREATE TABLE "+ DATABASE_TABLE + " ( " 
		+ SYNC_TABLE_NAME 				+ " TEXT PRIMARY KEY,"
			+ SYNC_TABLE_DOWNLOAD_DATE	+ " TEXT DEFAULT ''," 
			+ SYNC_TABLE_UPLOAD_DATE	+ " TEXT DEFAULT ''" + 
			")";
	// NOTE: When adding to the table items added locally: i.e. no id field, the
	// client needs to put in the client_uuid and client_index
	// Something like: INSERT INTO Log (id, rev_no, description) VALUES ((SELECT
	// IFNULL(MAX(id), 0)) + 1 FROM Log), 'rev_Id', 'some description')

	private static final String INDEX_1_NAME = DATABASE_TABLE+"_index_1";//NOTE: Indexes must be unique across all tables in db
	private static final String INDEX_1 = "CREATE INDEX " + INDEX_1_NAME
			+ " ON " + DATABASE_TABLE + " (  `" + SYNC_TABLE_NAME + "`, `"+ SYNC_TABLE_DOWNLOAD_DATE + "`, `"
			+ SYNC_TABLE_UPLOAD_DATE + "` )";

	private SQLiteDatabase db;
	// holds the app using the db
	private CTGTagTableDbHelper dbHelper;

//	protected int last_version = 0;

	public TableSyncDb(Context _context, String dbName) {
		this.databaseName = dbName;

		dbHelper = new CTGTagTableDbHelper(_context, this.databaseName, null,
				DATABASE_VERSION); // DATABASE_VERSION);
	}

	/**
	 * This method should be called from the ConnectionManager which allows only
	 * a limited number of open database helper objects (ie. initially a single
	 * connection - all others wait)
	 * 
	 * @return result object with SQLiteDatabase in result.object if successful
	 */
	public synchronized Result openDb() {
		Result r = new Result();
		try {
			db = dbHelper.getWritableDatabase();
		} catch (SQLException e) {
			// SQLiteDatabaseLockedException is only available starting in
			// Honeycomb - here we fake it
			if (e.getClass().getName()
					.contains("SQLiteDatabaseLockedException")) {
				// design decision here. Db should not be locked if I wait for
				// the previous activity to close
				// fully before opening the next one.
				r.error = "Database is locked. " + e.getMessage();
				r.success = false;
			} else {
				r.error = "Could not open database. " + e.getMessage();
				r.success = false;
			}
			Log.e(this.getClass().getName(), r.error);
		}
		return r;
	}

	@Override
	public void closeDb() {
		if (db != null)
			db.close();
	}

	@Override
	public synchronized void createTable() {
		dbHelper.onCreate(db);
	}

	@Override
	public synchronized void updateTable(int last_version) {
		dbHelper.onUpgrade(db, last_version, TABLE_VERSION);
	}

	@Override
	public String getTableName() {
		return DATABASE_TABLE;
	}

	@Override
	public int getTableCodeVersion() {
		return TABLE_VERSION;
	}
	//======================----------------> Db Access Methods <----------------================\\
	public void wipeTable() {
		synchronized (DATABASE_TABLE) {
			db.delete(DATABASE_TABLE, null, null);
		}
	}

	public void dropTable() {
		db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
		//invalidate table
		invalidateTable();
	}

	public void beginTransaction() {
		db.beginTransaction();
	}
	
	public void endTransaction() {
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Used to insert data into to local database
	 * @param c
	 * @return
	 */
	public synchronized Result insertOrUpdate(SyncEntry t) {
		synchronized (DATABASE_TABLE) {
			int index = -1;
			Result r = new Result();
			try {
				ContentValues cv = getContentValues(t);
				index = (int) db.replace(DATABASE_TABLE, null, cv);
				r.last_insert_id = index;
			} catch( Exception e ) {
				Log.e(this.getClass().getName(), e.getMessage());
				r.error = e.getMessage();
				r.success = false;
			}
			return r;
		}
	}
		
	public synchronized Result insertOrUpdateArrayList(ArrayList<SyncEntry> t) {
		Result r = new Result();
		beginTransaction();
		for (SyncEntry item : t) {
			r.add_results_if_error(insertOrUpdate(item), "Could not add SyncEntry "+t+" to database." );
		}
		endTransaction();
		return r;
	}

	public SyncEntry getTableSyncData(String table) {
		//String where = KEY_NUM+" = "+user_num;
		SyncEntry c= new SyncEntry();
		Cursor result= db.query(DATABASE_TABLE, 
				null, 
				SYNC_TABLE_NAME+" = ? ", new String[] {table}, null, null, null);
		if (result.moveToFirst() ) { //make sure data is in the result.  Read only first entry
			c = getFromCursor(result);
		}
		result.close();
		return c;
	}
	
	/**
	 * Retrieve all entries for testing purposes
	 * @return ArrayList<SyncEntry>
	 */
	public ArrayList<SyncEntry> getAllEntries() {
		ArrayList<SyncEntry> al = new ArrayList<SyncEntry>();
		Cursor list = getAllEntriesCursor();
		if (list.moveToFirst()){
			do {
				al.add(getFromCursor(list));
			} while(list.moveToNext());
		}
		list.close();
		return al;
	}
	/**
	 * Used for testing
	 * @return
	 */
	protected Cursor getAllEntriesCursor() {
		return db.query(DATABASE_TABLE, null, null, null, null, null, SYNC_TABLE_NAME);
	}
	
	
	// ================------------> helpers <-----------==============\\
    /** returns a ContentValues object for database insertion
     * 
     * @return
     */
    public ContentValues getContentValues(SyncEntry c) {
    	ContentValues vals = new ContentValues();
    	//prepare info for db insert/update
    	vals.put(SYNC_TABLE_NAME, c.getTableName());
    	vals.put(SYNC_TABLE_DOWNLOAD_DATE, c.getDownloadDate());
    	vals.put(SYNC_TABLE_UPLOAD_DATE, c.getUploadDate());
		return vals;
    }

	/**
	 * Get the data for the item currently pointed at by the database
	 * @param record
	 * @return
	 */
	public SyncEntry getFromCursor(Cursor record) {
		SyncEntry c= new SyncEntry();
		
		if (record.getColumnCount() > 2){ //make sure data is in the result.  Read only first entry
			c.setTableName(record.getString(SYNC_TABLE_NAME_COL));
			c.setDownloadDate(record.getString(SYNC_TABLE_DOWLOAD_DATE_COL));
			c.setUploadDate(record.getString(SYNC_TABLE_UPLOAD_DATE_COL));
		}
		return c;
	}
	
    // ================------------> helper class <-----------==============\\

	
	private static class CTGTagTableDbHelper extends SQLiteOpenHelper {

		public CTGTagTableDbHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(getClass().getName(), "OnCreate called for table: "+DATABASE_TABLE+" was not found in db path: "+db.getPath()+"... creating table.");
			db.execSQL(DATABASE_CREATE);
			db.execSQL(INDEX_1);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// called when the version of the existing db is less than the
			// current
			Log.w(this.getClass().getName(), "Upgrading table from " + oldVersion + " to " + newVersion);
//			if (oldVersion < 2) {
//				//add locally changed field
//				db.execSQL("ALTER TABLE "+DATABASE_TABLE+" ADD COLUMN "+CTG_TAG_LOCALLY_CHANGED+" "+CTG_TAG_LOCALLY_CHANGED_TYPE);
//				db.execSQL("DROP INDEX IF EXISTS "+INDEX_1_NAME);
//				db.execSQL(INDEX_1);
//				Log.d(this.getClass().getName(), "Adding new field and new index to "	+ DATABASE_TABLE + " Table");
//			}
		}

	}


}
