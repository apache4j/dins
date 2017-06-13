package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.texaspoker.jedis.JedisManager;

public class TranscationLogService {   
	Log   mLogTranscation;
	private static TranscationLogService instance; 
	
    public TranscationLogService()  
    {   
        //读取配置文件    
    	mLogTranscation  =   LogFactory.getLog("fortransaction");
    	
    }   
  	
  	//单例模式
  	public static TranscationLogService getInstance() {  
  		if (instance == null) {  
  			synchronized(TranscationLogService.class){
  				instance = new TranscationLogService();
  			}
  		}  
  		return instance;  
  	}
  	
  	public void error(String content){
  		Jedis mJedis = JedisManager.getJedis();
  		mLogTranscation.error(content);
  		mJedis.publish("transaction_log", System.currentTimeMillis() + " " + content);  
  		JedisManager.returnRes(mJedis);
  	}
  	
//  	public void errorExtra(String content){
//  		Jedis mJedis = JedisManager.getJedis();
//  		mLogTranscation.error(content);
//  		mJedis.publish("transaction_log_extra", System.currentTimeMillis() + " " + content);  
//  		JedisManager.returnRes(mJedis);
//  	}
 
}  
