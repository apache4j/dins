package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PotInfo {
	public long potSize;			//彩池的大小
	public long allinSize;			//allin的人数
	public ArrayList<Long> playerList = new ArrayList<Long>();			//该彩池对应的玩家
	public ArrayList<Integer> playerListpos = new ArrayList<Integer>();		//该玩家所在的位置
	public ArrayList<Long> playerNotFoldList = new ArrayList<Long>();			//该彩池对应的玩家
    public Map<Long,Long> potPlayerStacks = new ConcurrentHashMap<>();      //该彩池每个玩家对应的下注值
}
