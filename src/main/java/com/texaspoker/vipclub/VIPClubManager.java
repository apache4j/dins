package com.texaspoker.vipclub;

import java.util.HashMap;
import java.util.Map;

public class VIPClubManager {	
	private static VIPClubManager instance; 
		
	Map<Long,Integer> mVIPClubPercent = new HashMap<Long,Integer>();
	
	
	private VIPClubManager (){		

	}
	
	//初始化
	public static  VIPClubManager getInstance() {  
		if (instance == null) {  
			synchronized(VIPClubManager.class){
				instance = new VIPClubManager();
			}
		}  
		return instance;  
	}
	
	//设置VIP俱乐部的比例
	public void setVIPClubPercent(long lClubID,int percent){
		mVIPClubPercent.put(lClubID, percent);
	}
	
	//获取VIP俱乐部的比例
	public Integer getVIPClubPercent(long lClubID){
		return mVIPClubPercent.get(lClubID);
	}
}