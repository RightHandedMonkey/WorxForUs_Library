package com.worxforus.observable;



import java.util.Observable;

import com.worxforus.Utils;

/**
 * This class is used to let the system know whenever data has been changed so that a network sync task can be scheduled.
 * @author sbossen
 *
 */

public class SyncRequestNotifier extends Observable {

	private static SyncRequestNotifier instance = new SyncRequestNotifier();

	public void update() {
		Utils.LogD(this.getClass().getName(), "A sync request has been received.");
		setChanged();
		notifyObservers();
	}
	
	public SyncRequestNotifier() {
		Utils.LogD(this.getClass().getName(), "Sync request notifier was created.");
	}

	public static SyncRequestNotifier getNotifier() {
		return instance;
	}

}