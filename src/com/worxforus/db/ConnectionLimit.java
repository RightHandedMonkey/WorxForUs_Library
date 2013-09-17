package com.worxforus.db;

/**
 * 
 * @author sbossen
 * Singleton Pattern from: http://en.wikipedia.org/wiki/Singleton_pattern (Bill Pugh)
 * Limiter Pattern from: http://www.google.com/url?q=http%3A%2F%2Fwww.javacodegeeks.com%2F2011%2F09%2Fjava-concurrency-tutorial-semaphores.html&sa=D&sntz=1&usg=AFQjCNE86ggZmFbal8yX4Nhq1hI6kNf7uA
 * Usage: 
 * ConnectionLimitHelper conn = ConnectionLimit.getConnectionHelper();
 */
public class ConnectionLimit {
	// Private constructor prevents instantiation from other classes
	public static int NUM_SIMULATANEOUS_CONNECTIONS = 1;

	private ConnectionLimit() { }

	/**
	 * 
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance()
	 * 
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */

	private static class SingletonHolder {
		public static final ConnectionLimitHelper INSTANCE = new ConnectionLimitHelper(NUM_SIMULATANEOUS_CONNECTIONS);
	}

	public static ConnectionLimitHelper getConnectionHelper() {
		return SingletonHolder.INSTANCE;
	}
	
	
}