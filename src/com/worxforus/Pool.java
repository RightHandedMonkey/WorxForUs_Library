package com.worxforus;

import java.util.ArrayList;
import java.util.List;

public class Pool<T> {

	public interface PoolObjectFactory<T> {
		public T createObject();
	}
	
	private final List<T> freeObjects;
	private final PoolObjectFactory<T> factory;
	private final int maxSize;
	
	public Pool(PoolObjectFactory<T> factory, int maxSize) {
		this.factory = factory;
		this.maxSize = maxSize;
		this.freeObjects = new ArrayList<T>(maxSize);
	}
	
	public T newObject() {
		T object=null;
		
		if(freeObjects.isEmpty())
			object=factory.createObject();
		else
			object=freeObjects.remove(freeObjects.size()-1);
		return object;
	}
	
	public void free(T object) {
		if(freeObjects.size() < maxSize) {
			freeObjects.add(object);
		}
	}
	public void freeAll(ArrayList<T> array) {
		for(int i=0; i < array.size(); i++ ) {
			free(array.get(i));
		}
	}
}
