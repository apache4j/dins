package com.texaspoker.logger;

public class Log 
{
    public static LoggerService getLoggerService(String name) {
        return new LoggerService(name);
    }			
}