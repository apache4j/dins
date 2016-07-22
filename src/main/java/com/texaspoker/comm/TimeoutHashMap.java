package com.texaspoker.comm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TimeoutHashMap<K,V> {
	ConcurrentHashMap<K,TimeNode> mMap = new ConcurrentHashMap<K,TimeNode>();
	long mTimeout;//超时的时间
	Log   mLogNormals   =   LogFactory.getLog("fornormal");
	
	public boolean setTimeout(long timeout){
		mTimeout = timeout;
		return true;
	}
	
	public boolean setCurrent(K key,V value){
		TimeNode stNode = mMap.get(key);
		if(stNode == null){
			stNode = new TimeNode();
		}
		
		stNode.updateTime = System.currentTimeMillis();
		stNode.data = value;
		mMap.put(key, stNode);
		return true;
	}
	
	public V get(K key){
		TimeNode stNode = mMap.get(key);
		if(stNode != null){
			return stNode.data;
		}
		return null;
	}
	
	//这个函数有bug，后面需要改一下
	public ArrayList<V> timeout(){
		ArrayList<V> list = new ArrayList<V>();
		
		long curTime = System.currentTimeMillis();
		//遍历map，返回所有超时的map 
		Iterator iter = mMap.entrySet().iterator(); 
		while (iter.hasNext()) { 
		    Map.Entry entry = (Map.Entry) iter.next(); 
		    Long  key = (Long) entry.getKey();
		    TimeNode  stTimeNode = (TimeNode) entry.getValue();	    
		    
		    if(curTime - stTimeNode.updateTime > mTimeout){
		    	list.add(stTimeNode.data);
		    }
		} 
		return list;
	}
	
	public class TimeNode {
		public long updateTime;
		public V data;
	}
	
	public int size(){
		return mMap.size();
	}
	
	public boolean containsKey(K key){
		return mMap.containsKey(key);
	}

	public void removeKey(K key){
		mMap.remove(key);
	}
	
	public void clear(){
		mMap.clear();
	}
}
