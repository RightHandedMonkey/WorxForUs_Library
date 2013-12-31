package com.worxforus.observable;



import java.util.Observable;

import com.worxforus.Utils;

/**
 * This class is used so that updates to user credentials get populated to the proper components of the system.
 * For example:
 *  NetAuthentication needs to be updated and invalidated when network tasks have completed.
 *  Sync times need to be reset.
 * @author sbossen
 *
 */

public class LoginCredentialsChangeNotifier extends Observable {

	private static LoginCredentialsChangeNotifier instance = new LoginCredentialsChangeNotifier();

	public void update() {
		Utils.LogD(this.getClass().getName(), "Login credentials have changed.");
		setChanged();
		notifyObservers();
	}
	
	public LoginCredentialsChangeNotifier() {
		Utils.LogD(this.getClass().getName(), "Login credential change notifier was created.");
	}

	public static LoginCredentialsChangeNotifier getNotifier() {
		return instance;
	}

}