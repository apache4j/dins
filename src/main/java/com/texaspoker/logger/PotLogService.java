package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by zhangmeng on 2019/9/10.
 */
public class PotLogService {

    Log mLogPotDraw;
    private static PotLogService instance;

    public PotLogService()
    {
        //读取配置文件
        mLogPotDraw  =   LogFactory.getLog("forpot");
    }

    //单例模式
    public static PotLogService getInstance() {
        if (instance == null) {
            synchronized(PotLogService.class){
                instance = new PotLogService();
            }
        }
        return instance;
    }

    public void error(String content){
        mLogPotDraw.error(content);
    }

}
