package com.texaspoker.club;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClubGameRooms {
	public ConcurrentMap<Long,Long>  mRoomMap =new ConcurrentHashMap<Long,Long>(); 
}
