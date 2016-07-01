package com.texaspoker.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PlayLogService {   
	Log   mLogPlay;
	private static PlayLogService instance; 
	
    public PlayLogService()  
    {   
        //读取配置文件    
    	mLogPlay  =   LogFactory.getLog("forplays");
    }   
  	
  	//单例模式
  	public static PlayLogService getInstance() {  
  		if (instance == null) {  
  			synchronized(PlayLogService.class){
  				instance = new PlayLogService();
  			}
  		}  
  		return instance;  
  	}
  	
  	public void error(String content){
  		mLogPlay.error(content);
  	}
}  
