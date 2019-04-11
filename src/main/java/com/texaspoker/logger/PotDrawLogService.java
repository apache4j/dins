package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by zhangmeng on 2019/4/11.
 */
public class PotDrawLogService {
    Log mLogPotDraw;
    private static PotDrawLogService instance;

    public PotDrawLogService()
    {
        //读取配置文件
        mLogPotDraw  =   LogFactory.getLog("forpotdraw");
    }

    //单例模式
    public static PotDrawLogService getInstance() {
        if (instance == null) {
            synchronized(PotDrawLogService.class){
                instance = new PotDrawLogService();
            }
        }
        return instance;
    }

    public void error(String content){
        mLogPotDraw.error(content);
    }
}
