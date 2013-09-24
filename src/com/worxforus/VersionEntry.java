package com.worxforus;

public class VersionEntry {
	protected String tableName="";
	protected int tableVer=0;
	
	public VersionEntry(String databaseTable, int tableVersion) {
		setTableName(databaseTable);
		setTableVer(tableVersion);
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public int getTableVer() {
		return tableVer;
	}
	public void setTableVer(int tableVer) {
		this.tableVer = tableVer;
	}

}
