package com.texaspoker.logger;

import com.texaspoker.kafka.ButtonLogKafkaProducer;
import com.texaspoker.kafka.TransactionLogKafkaProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ：piness
 * @date ：Created in 2020-11-23 17:48
 * @description：
 * @modified By：
 * @version: $
 */
public class ButtonLogService {
    Log mLogButtons;
    private static ButtonLogService instance;

    public ButtonLogService() {
        //读取配置文件
        mLogButtons = LogFactory.getLog("forbuttons");
    }

    /**
     * 单例模式
     * <br/>
     *
     * @param
     * @return
     * @author piness
     * @date 2020-11-23 17:51
     */
    public static ButtonLogService getInstance() {
        if (instance == null) {
            synchronized (ButtonLogService.class) {
                instance = new ButtonLogService();
            }
        }
        return instance;
    }

    public void error(String content) {
        System.out.println("-----content------");
        System.out.println(content);
        mLogButtons.error(content);
        try {
            ButtonLogKafkaProducer.send(content);
            System.out.println("-----kafka finish------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
