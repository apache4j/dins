package com.texaspoker.moment.room;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;

public class GameRoomService  implements Runnable {

	Log   mLogNormals   =   LogFactory.getLog("fornormal");
	
	public void run() {
		// TODO Auto-generated method stub
		//每次需要执行的代码放到这里面。  
		ArrayList<GameRoomDetailInfo> timeoutArray = GameRoomManager.getInstance().getTimeOutGameRoom();
		
		mLogNormals.error("GameRoomService begin : " +timeoutArray.size() );
		for(int i = 0 ; i < timeoutArray.size() ; i++){
			try{	
				GameRoomDetailInfo stGameRoomDetailInfo = (GameRoomDetailInfo) timeoutArray.get(i);
				System.out.println("stGameRoomDetailInfo : " + stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
				System.out.println("stGameRoomDetailInfo : " + stGameRoomDetailInfo.getStGameRoomInfo().getEGameState());
				switch(stGameRoomDetailInfo.getStGameRoomInfo().getEGameState().getNumber()){
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:
				case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:
					stGameRoomDetailInfo.gameRoomTimeoutHandle(GameRoomManager.getInstance().mGameRoomDetailInfos);
					break;
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}	
		mLogNormals.error("GameRoomService end : " +timeoutArray.size() );
	}
}
