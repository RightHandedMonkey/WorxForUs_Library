package com.worxforus;

public class SyncEntry {
	protected String tableName="";
	protected String downloadDate="";
	protected String uploadDate="";
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String table) {
		this.tableName = table;
	}
	public String getDownloadDate() {
		return downloadDate;
	}
	public void setDownloadDate(String downloadDate) {
		this.downloadDate = downloadDate;
	}
	public String getUploadDate() {
		return uploadDate;
	}
	public void setUploadDate(String uploadDate) {
		this.uploadDate = uploadDate;
	}
	public void invalidate() {
		this.uploadDate = "";
		this.downloadDate = "";
	}
}