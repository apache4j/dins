//package com.texaspoker.moment.room;
//
//import com.alibaba.dubbo.common.utils.CollectionUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import java.util.ArrayList;
//
///**
// * @author zed
// */
//public class GameRoomLogService implements Runnable {
//
//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		//每次需要执行的代码放到这里面。
//		ArrayList<Long> roomList = GameRoomManager.getInstance().mGameRoomDetailInfos.getListKey();
//
//		if(!CollectionUtils.isEmpty(roomList)&&roomList.size()>0){
//			for(int i = 0 ; i < roomList.size() ; i++){
//				try{
//					GameRoomDetailInfo gameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomList.get(i));
//					long currentTime = System.currentTimeMillis();
//				if(gameRoomDetailInfo.getGameStarted()&&!gameRoomDetailInfo.bPause&&currentTime-gameRoomDetailInfo.getStageUpdateTime()>60 *1000){
//					gameRoomDetailInfo.printGameStatus();
//				}
//				}
//				catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//
//		}
//	}
//}
