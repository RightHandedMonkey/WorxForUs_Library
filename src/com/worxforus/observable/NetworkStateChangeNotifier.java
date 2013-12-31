package com.worxforus.observable;



import java.util.Observable;

import com.worxforus.Utils;

/**
 * This class is used so that updates to the Network connectivity state get passed to the Network Task Schedule
 * and wherever else it is needed.  The purpose is to pause the Network Task Scheduler when inactive and to resume it
 * when active.
 * 
 * Usage:
 * NetworkStateChangeNotifier.getNotifier().notifyNetworkDisconnected(); //when network found to be disconnected
 * NetworkStateChangeNotifier.getNotifier().notifyNetworkConnected(); //when network found to be connected
 * @author sbossen
 *
 */

public class NetworkStateChangeNotifier extends Observable {

	private static NetworkStateChangeNotifier instance = new NetworkStateChangeNotifier();

	public static final int NETWORK_CONNECTED = 1;
	public static final int NETWORK_DISCONNECTED = 0;
	private volatile int connection = NETWORK_DISCONNECTED;
	
	private void update() {
		Utils.LogD(this.getClass().getName(), "Network connection change has been detected and set to: "+connection);
		setChanged();
		notifyObservers();
	}
	
	public void notifyNetworkConnected() {
		connection = NETWORK_CONNECTED;
		update();
	}
	
	public void notifyNetworkDisconnected() {
		connection = NETWORK_DISCONNECTED;
		update();
	}
	
	/**
	 * This function should only be used on receiving an update from the listening class.
	 * It will be in an invalid state until the first time notifyNetworkConnected() [or diconnected] is received.
	 * @return
	 */
	public int getNetworkConnected() {
		return connection;
	}
	
	public NetworkStateChangeNotifier() {
		Utils.LogD(this.getClass().getName(), "Network notifier was created.");
	}

	public static NetworkStateChangeNotifier getNotifier() {
		return instance;
	}

}