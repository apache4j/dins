package com.texaspoker.moment.room;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 * Created by zhangmeng on 2018/12/7.
 */
public class GameRoomDrawService implements Runnable {

    Log mLogNormals   =   LogFactory.getLog("fornormal");

    @Override
    public void run() {

        ArrayList<Long> timeoutArray = GameRoomManager.getInstance().getTimeOutGameRoomDraw();

        for(int i = 0 ; i < timeoutArray.size() ; i++){
            try{
                // 01.在抽水map内移除该牌局,即本次抽水时间已到
                GameRoomManager.getInstance().mGameRoomDraw.removeKey(timeoutArray.get(i));

                // 02.将该牌局再次添加到抽水map内,倒计时
                GameRoomManager.getInstance().setmGameRoomDraw(timeoutArray.get(i));

                // 03.设置该牌局在一手结束后进行抽水操作
                GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(timeoutArray.get(i));
                stGameRoomDetailInfo.bDraw = true;

            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
