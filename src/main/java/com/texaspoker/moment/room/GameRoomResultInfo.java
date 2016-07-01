package com.texaspoker.moment.room;

import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;

public class GameRoomResultInfo {
	private ERROR_CODE_TYPE eErrorCodeType;
	private GameRoomInfo stGameRoomInfo;
	
	public ERROR_CODE_TYPE geteErrorCodeType() {
		return eErrorCodeType;
	}
	public GameRoomInfo getStGameRoomInfo() {
		return stGameRoomInfo;
	}
	public void setStGameRoomInfo(GameRoomInfo stGameRoomInfo) {
		this.stGameRoomInfo = stGameRoomInfo;
	}
	public void seteErrorCodeType(ERROR_CODE_TYPE eErrorCodeType) {
		this.eErrorCodeType = eErrorCodeType;
	}
}
