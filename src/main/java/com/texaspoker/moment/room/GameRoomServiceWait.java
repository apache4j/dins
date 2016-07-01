package com.texaspoker.moment.room;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;

public class GameRoomServiceWait  implements Runnable {

	Log   mLogNormals   =   LogFactory.getLog("fornormal");
	
	public void run() {
		// TODO Auto-generated method stub
		//每次需要执行的代码放到这里面。  
		ArrayList<GameRoomDetailInfo> timeoutArray = GameRoomManager.getInstance().getTimeOutGameRoom();
		
		mLogNormals.error("GameRoomServiceWait begin : " +timeoutArray.size() );
		for(int i = 0 ; i < timeoutArray.size() ; i++){
			try{	
				GameRoomDetailInfo stGameRoomDetailInfo = (GameRoomDetailInfo) timeoutArray.get(i);
				System.out.println("stGameRoomDetailInfo : " + stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
				System.out.println("stGameRoomDetailInfo : " + stGameRoomDetailInfo.getStGameRoomInfo().getEGameState());
				switch(stGameRoomDetailInfo.getStGameRoomInfo().getEGameState().getNumber()){
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE:
					stGameRoomDetailInfo.gameRoomTimeoutHandle(GameRoomManager.getInstance().mGameRoomDetailInfos);
					break;
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}	
		mLogNormals.error("GameRoomServiceWait end : " +timeoutArray.size() );
	}
}
