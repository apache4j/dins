package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.texaspoker.dubbo.game.insurance.InsuranceApi;
import com.texaspoker.jedis.JedisManager;
import com.texaspoker.kafka.TablesLogKafkaProducer;

import redis.clients.jedis.Jedis;

public class TableLogService {   
	Log   mLogSocial;
	private static TableLogService instance;
	
    public TableLogService()  
    {   
        //读取配置文件    
    	mLogSocial  =   LogFactory.getLog("fortables");
    	
    }   
  	
  	//单例模式
  	public static TableLogService getInstance() {  
  		if (instance == null) {  
  			synchronized(TableLogService.class){
  				instance = new TableLogService();
  			}
  		}  
  		return instance;  
  	}
  	
  	public void error(String content){
  		long stamp = System.currentTimeMillis();
  		
  		mLogSocial.error(content);
  		Jedis mJedis = JedisManager.getJedis();
  		mJedis.publish("tables_log", System.currentTimeMillis() + " " + content);  
  		JedisManager.returnRes(mJedis);
  		
  		InsuranceApi.parseGameAction(content);
  		
  		 try {
             TablesLogKafkaProducer.send(stamp, "tables_log", content);
         } catch (Exception e) {
             e.printStackTrace();
         }
  	}
  
//  	public void error1(String content){
//  		mLogSocial.error(content);
//  	}
}  