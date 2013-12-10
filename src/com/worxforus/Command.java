package com.worxforus;

import android.content.Context;

/**
 * When subclasses, note that each method should be synchronized so that
 * execute, attach, and release can't interrupt each other.
 * @author sbossen
 *
 */
public interface Command {

	//synchronized methods
	public Result execute();
	public void attach(Context c);
	public void release();
	
	//non synchronized method
	public void abort();
}
