package com.texaspoker.moment.room;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 * @author zed
 */
public class GameRoomLogService implements Runnable {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//每次需要执行的代码放到这里面。  
		ArrayList<GameRoomDetailInfo> timeoutArray = GameRoomManager.getInstance().getTimeOutGameRoom();
		
		for(int i = 0 ; i < timeoutArray.size() ; i++){
			try{	
				GameRoomDetailInfo stGameRoomDetailInfo = (GameRoomDetailInfo) timeoutArray.get(i);
				long currentTime = System.currentTimeMillis();
				if(stGameRoomDetailInfo.getGameStarted()&&!stGameRoomDetailInfo.bPause&&currentTime-stGameRoomDetailInfo.getStageUpdateTime()>60 *1000){
					stGameRoomDetailInfo.printGameStatus();
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}	
	}
}
