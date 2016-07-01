package com.texaspoker.moment.room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserChatRooms {
	public ConcurrentMap<Long,Long>  mRoomMap=new ConcurrentHashMap<Long,Long>(); 
}
