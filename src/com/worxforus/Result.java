package com.worxforus;

public class Result {

	/***
	 * Wrapper class for error handling. Basically this class is used to capture
	 * errors that you may or may not want to handle at the calling level. It
	 * keeps you from having to use try/catch blocks everywhere since you can
	 * now push them down further into your implementation. When you get the
	 * result object back you can check for the error if you want - or just
	 * ignore it.
	 */
	public boolean success = true;
	public String error = "";
	public String technical_error = "";
	public String message = "";
	public String sql = "";
	public long last_insert_id = 0;
	public long last_insert_linked_id = 0; // used for two primary keys
	public long last_insert_index = 0; // used for two primary keys
	public String string = "";
	public Object object = null;

	public static final String WEB_SUCCESS = "success";
	public static final String WEB_RESULT = "result";
	public static final String WEB_MESSAGE = "message";
	public static final String WEB_ERROR = "error";
	public static final String WEB_STRING = "string";

	// Constructor
	public Result() {
	}

	@Override
	public String toString() {
		return "Success: " + this.success + ", Error: " + this.error
				+ ", Msg: " + this.message + "SQL: " + this.sql
				+ ", Insert Id: " + this.last_insert_id + ", Insert Index: "
				+ this.last_insert_index;
	}

	/**
	 * This function is used to chain results together.  For example: if you are performing multiple operations
	 * that could have failures, then create one result object and use this function to populate it with a summary
	 * of all failures that occur (all operations must pass to return success).
	 * @param result_to_include - New operation to include for success consideration
	 * @param fail_msg - message to add if failure detected
	 */
	public void add_results_if_error(Result result_to_include, String fail_msg) {
		if (result_to_include != null) {
			if (!(result_to_include.success) && fail_msg != null) {
				// add these to return results
				if (fail_msg.length() > 0)
					fail_msg += "\r\n";
				this.success = false;
				this.error += fail_msg + result_to_include.error;
				this.message += result_to_include.message;
				this.sql += result_to_include.sql;
			}
		}
	}

	public void add_error(String str, boolean html_breaks) {
		this.error = this.add_helper(this.error, str, html_breaks);
	}

	public void add_message(String str, boolean html_breaks) {
		this.message = this.add_helper(this.message, str, html_breaks);
	}

	public void add_technical_error(String str, boolean html_breaks) {
		this.technical_error = this.add_helper(this.technical_error, str,
				html_breaks);
	}

	private static String add_helper(String orig, String new_str,
			boolean html_breaks) {
		String line_brk = "\r\n";
		if (html_breaks)
			line_brk = "<br />";
		if (orig.length() > 0)
			orig += line_brk;
		return (orig + new_str);
	}

}