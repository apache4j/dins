package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by grunt on 2020/1/10.
 */
public class RecordLogService {

    Log mLogRecord;
    private static RecordLogService instance;

    public RecordLogService()
    {
        //读取配置文件
        mLogRecord  =   LogFactory.getLog("forrecord");
    }

    //单例模式
    public static RecordLogService getInstance() {
        if (instance == null) {
            synchronized(RecordLogService.class){
                instance = new RecordLogService();
            }
        }
        return instance;
    }

    public void error(String content){
        mLogRecord.error(content);
    }
}
