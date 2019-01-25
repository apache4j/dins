package com.texaspoker.moment.room;

import com.texaspoker.moment.TexasPoker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

public class GameRoomServiceWait  implements Runnable {

	Log   mLogNormals   =   LogFactory.getLog("fornormal");

	int mNo = 0;
	int mTotal = 0;

	public GameRoomServiceWait(int iNo,int iTotal){
		mNo = iNo;
		mTotal = iTotal;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		//每次需要执行的代码放到这里面。  
		ArrayList<GameRoomDetailInfo> timeoutArray = GameRoomManager.getInstance().getTimeOutGameRoom();
		
		for(int i = 0 ; i < timeoutArray.size() ; i++){
			try{	
				GameRoomDetailInfo stGameRoomDetailInfo = (GameRoomDetailInfo) timeoutArray.get(i);
				if (stGameRoomDetailInfo.stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()%mTotal == mNo){
					switch(stGameRoomDetailInfo.getStGameRoomInfo().getEGameState().getNumber()){
						case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart_VALUE:
						case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE:
							stGameRoomDetailInfo.gameRoomTimeoutHandle(GameRoomManager.getInstance().mGameRoomDetailInfos);
							break;
					}
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}	
	}
}
