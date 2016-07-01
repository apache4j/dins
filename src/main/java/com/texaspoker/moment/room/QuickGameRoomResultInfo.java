package com.texaspoker.moment.room;

import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;

public class QuickGameRoomResultInfo {
	
	private ERROR_CODE_TYPE eErrorCodeType;
	private QuickGameRoomInfo stQuickGameRoomInfo;
	private GameRoomInfo stGameRoomInfo;
	
	public ERROR_CODE_TYPE geteErrorCodeType() {
		return eErrorCodeType;
	}
	public void seteErrorCodeType(ERROR_CODE_TYPE eErrorCodeType) {
		this.eErrorCodeType = eErrorCodeType;
	}
	public QuickGameRoomInfo getStQuickGameRoomInfo() {
		return stQuickGameRoomInfo;
	}
	public void setStQuickGameRoomInfo(QuickGameRoomInfo stQuickGameRoomInfo) {
		this.stQuickGameRoomInfo = stQuickGameRoomInfo;
	}
	public GameRoomInfo getStGameRoomInfo() {
		return stGameRoomInfo;
	}
	public void setStGameRoomInfo(GameRoomInfo stGameRoomInfo) {
		this.stGameRoomInfo = stGameRoomInfo;
	}

}
