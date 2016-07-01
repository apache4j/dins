package com.texaspoker.club;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClubsSettingInfo {
	//房间ID同房间设置的关系
	public Map<Long,ClubSettingInfo> mClubssSettingInfo = new ConcurrentHashMap<Long,ClubSettingInfo>();
	
	public void setClubSettingInfo(long clubID,ClubSettingInfo stClubSettingInfo){
		mClubssSettingInfo.put(clubID, stClubSettingInfo);
	}
	
	ClubSettingInfo getClubSettingInfo(long clubID){
		return mClubssSettingInfo.get(clubID);
	}
}
