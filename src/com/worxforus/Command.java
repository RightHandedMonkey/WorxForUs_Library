package com.worxforus;

import android.content.Context;

/**
 * When subclassing, note that each method should be synchronized so that
 * execute, attach, and release can't interrupt each other.
 * @author sbossen
 *
 */
public interface Command {
	
	public static final int STATE_RUNNING = -1;
	public static final int STATE_WAITING = 0;
	public static final int STATE_FINISHED = 1;

	//If the task.getState() > STATE_RUNNING then it can be safely removed, otherwise call abort and wait.
	
	//synchronized methods
	public Result execute();
	public void attach(Context c);
	public void release();
	
	//non synchronized method
	public void abort();
	public int getState(); //create a private volatile int state=STATE_WAITING, in the derived class and set appropriately during execution
}
