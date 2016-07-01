package com.texaspoker.logger;

import org.apache.log4j.BasicConfigurator;   
import org.apache.log4j.Level;   
import org.apache.log4j.Logger;   
import org.apache.log4j.Priority;   
import org.apache.log4j.PropertyConfigurator;  
  
public class LoggerService   
{   
	private static Logger logger; 
  
    public LoggerService(String  location)  
    {   
        //读取配置文件   
        getLog4jConfig(); 
        logger = (Logger) Logger.getLogger(location);   
    }   
	
    public void debug(Object msg)   
    {   
        log("DEBUG", msg, null);   
    }   
  
    public void debugPush(Object msg)   
    {   
        log("DEBUG", msg, null);   
    }   
    
    public void debug(Object msg, Throwable t)   
    {   
        log("DEBUG", msg, t);   
    }   
  
    public void info(Object msg)   
    {   
        log("INFO", msg, null);   
    }   
  
    public void info(Object msg, Throwable t)   
    {   
        log("INFO", msg, t);   
    }   
    
    public void login(Object msg)   
    {   
        log("LOGIN", msg, null);   
    }   
  
    public void login(Object msg, Throwable t)   
    {   
        log("LOGIN", msg, t);   
    }   
  
  
    public void warn(Object msg)   
    {   
        log("WARN", msg, null);   
    }   
  
    public void warn(Object msg, Throwable t)   
    {   
        log("WARN", msg, t);   
    }   
  
    public void error(Object msg)   
    {   
        log("ERROR", msg, null);   
    }   
  
    public void error(Object msg, Throwable t)   
    {   
        log("ERROR", msg, t);   
    }   
  
    private void log(String level, Object msg, Throwable e)   
    {   
        if (null != logger)   
        {   
            logger.log((Priority) Level.toLevel(level), msg, e);   
        }   
    }   
       
    /**  
     * 读取配置文件  
     */  
    private void getLog4jConfig()   
    {   
        if (System.getProperty("log4j.properties") == null)   
        {   
        	PropertyConfigurator.configure( "log4j.properties" );
        	BasicConfigurator.configure();
        }
    }   
  
}  