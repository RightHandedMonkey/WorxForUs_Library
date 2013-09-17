package com.worxforus.db;

import java.util.concurrent.Semaphore;

import com.worxforus.Result;

public class ConnectionLimitHelper {
	private final Semaphore semaphore;

	private int  maxRequests=1;
	
	ConnectionLimitHelper(int maxConcurrentRequests) {
		semaphore = new Semaphore(maxConcurrentRequests);
		maxRequests = maxConcurrentRequests;
	}

	public int getCurrentAllowedRequests() {
		return maxRequests;
	}
	
	public Result acquire(TableInterface table) {
		Result result = new Result();
		try {
			semaphore.acquire();
			result = table.openDb();
		} catch (InterruptedException e) {
			result.success = false;
			result.technical_error = e.getMessage() + ", Cause: "
				+ e.getCause();
		}

		return result;

	}

	public void release(TableInterface table) {
		try {
			table.closeDb();
		} finally {
			semaphore.release();
		}
	}
}
