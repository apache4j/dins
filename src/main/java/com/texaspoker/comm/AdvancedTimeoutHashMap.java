package com.texaspoker.comm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancedTimeoutHashMap<K,V> {
	ConcurrentHashMap<K,TimeNode> mMap = new ConcurrentHashMap<K,TimeNode>();
	
	public boolean setCurrent(K key){
		TimeNode stNode = mMap.get(key);
		if(stNode == null){
			stNode = new TimeNode();
		}
		
		stNode.updateTime = System.currentTimeMillis();
		mMap.put(key, stNode);
		return true;
	}
	
	public boolean setTimeout(K key,long value){
		TimeNode stNode = mMap.get(key);
		if(stNode == null){
			stNode = new TimeNode();
		}
		
		stNode.data = value;
		mMap.put(key, stNode);
		return true;
	}
	
	public boolean addTimeout(K key,long value){
		TimeNode stNode = mMap.get(key);
		if(stNode != null){
			stNode.data = stNode.data + value;
			mMap.put(key, stNode);
			return true;
		}else{
			return false;
		}
	}
	
	public long get(K key){
		TimeNode stNode = mMap.get(key);
		if(stNode != null){
			return stNode.data;
		}
		return 0;
	}
	
	
	//这个函数有bug，后面需要改一下
	public ArrayList<Long> timeout(){
		ArrayList<Long> list = new ArrayList<Long>();
		
		long curTime = System.currentTimeMillis();
		//遍历map，返回所有超时的map 
		Iterator iter = mMap.entrySet().iterator(); 
		while (iter.hasNext()) { 
		    Map.Entry entry = (Map.Entry) iter.next(); 
		    TimeNode  stTimeNode = (TimeNode) entry.getValue();
		    
		    if(curTime - stTimeNode.updateTime > (Long)stTimeNode.data*1000){
		    	Long  key = (Long) entry.getKey();
		    	list.add(key);
		    	iter.remove();
		    }
		} 
		return list;
	}
	
	public class TimeNode {
		public long updateTime;
		public long data;
	}
	
	public int size(){
		return mMap.size();
	}
	
	public boolean containsKey(K key){
		return mMap.containsKey(key);
	}

//	public long removeKey(K key){
//		return mMap.remove(key).data;
//	}
	
	public void removeAllKey(){
		mMap.clear();
	}
}
