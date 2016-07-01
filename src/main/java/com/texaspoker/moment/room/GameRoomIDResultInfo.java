package com.texaspoker.moment.room;

import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;

public class GameRoomIDResultInfo {
	private ERROR_CODE_TYPE eErrorCodeType;
	private long lGameRoomID;
	public ERROR_CODE_TYPE geteErrorCodeType() {
		return eErrorCodeType;
	}
	public void seteErrorCodeType(ERROR_CODE_TYPE eErrorCodeType) {
		this.eErrorCodeType = eErrorCodeType;
	}
	public long getlGameRoomID() {
		return lGameRoomID;
	}
	public void setlGameRoomID(long lGameRoomID) {
		this.lGameRoomID = lGameRoomID;
	}
}
