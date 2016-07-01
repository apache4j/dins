package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NormalLogService {   
	Log   mLogNormal;
	private static NormalLogService instance; 
	
    public NormalLogService()  
    {   
        //读取配置文件    
    	mLogNormal  =   LogFactory.getLog("fornormal");
    }   
  	
  	//单例模式
  	public static NormalLogService getInstance() {  
  		if (instance == null) {  
  			synchronized(TranscationLogService.class){
  				instance = new NormalLogService();
  			}
  		}  
  		return instance;  
  	}
  	
  	public void error(String content){
  		mLogNormal.error(content);
  	}
}  
