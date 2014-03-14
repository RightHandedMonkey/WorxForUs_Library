package com.worxforus.observable;



import java.util.Observable;

import com.worxforus.Utils;

/**
 * This class is used so that changes to the power level get passed to the synchronization scheduler
 * and wherever else it is needed.  The purpose is to change the refresh time and even stop updates based on
 * battery level
 * 
 * This class doubles as a maintainer of the current battery state
 * 
 * Usage:
 * PowerStateChangeNotifier.getNotifier().notifyChargerDisconnected(); //when Charger found to be disconnected
 * PowerStateChangeNotifier.getNotifier().notifyChargerConnected(); //when Charger found to be connected
 * 
 * PowerStateChangeNotifier.getNotifier().hasGoodPower(); //to check if the power is good for network usage or not
 * 
 * @author sbossen
 *
 */

public class PowerStateChangeNotifier extends Observable {

	private static PowerStateChangeNotifier instance = new PowerStateChangeNotifier();

	public static final int CHARGER_CONNECTED = 1;
	public static final int CHARGER_DISCONNECTED = 0;
	public static final int BATTERY_LOW_PCT = 40;
	private volatile int connection = CHARGER_DISCONNECTED;
	private volatile int batteryPct = 50;
	
	
	private void update() {
		Utils.LogD(this.getClass().getName(), "Charger connection change has been detected and set to: "+connection+", battery is: "+batteryPct);
		setChanged();
		notifyObservers();
	}
	
	public int getBatteryPct() {
		return batteryPct;
	}

	public void setBatteryPct(int pct) {
		this.batteryPct = pct;
	}
	
	public void notifyChargerConnected() {
		connection = CHARGER_CONNECTED;
		update();
	}
	
	public void notifyChargerDisconnected() {
		connection = CHARGER_DISCONNECTED;
		update();
	}
	
	public boolean hasGoodPower() {
		if (connection == CHARGER_CONNECTED || batteryPct > BATTERY_LOW_PCT) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * This function should only be used on receiving an update from the listening class.
	 * It will be in an invalid state until the first time notifyChargerConnected() [or diconnected] is received.
	 * @return
	 */
	public int getChargerConnected() {
		return connection;
	}
	
	public PowerStateChangeNotifier() {
		Utils.LogD(this.getClass().getName(), "Charger notifier was created.");
	}

	public static PowerStateChangeNotifier getNotifier() {
		return instance;
	}

}