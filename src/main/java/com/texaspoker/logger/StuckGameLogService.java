package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by zhangmeng on 2019/4/11.
 */
public class StuckGameLogService {
    Log mLogStuckGame;
    private static StuckGameLogService instance;

    public StuckGameLogService()
    {
        //读取配置文件
        mLogStuckGame  =   LogFactory.getLog("forstuckgame");
    }

    //单例模式
    public static StuckGameLogService getInstance() {
        if (instance == null) {
            synchronized(StuckGameLogService.class){
                instance = new StuckGameLogService();
            }
        }
        return instance;
    }

    public void error(String content){
        mLogStuckGame.error(content);
    }
}
