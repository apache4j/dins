package com.texaspoker.logger;

public class PlayLogService {
    private LoggerService loggerServicePlay;
    private static PlayLogService instance;

    public PlayLogService() {
        loggerServicePlay = Log.getLoggerService("forplays");
    }

    //单例模式
    public static PlayLogService getInstance() {
        if (instance == null) {
            synchronized (PlayLogService.class) {
                instance = new PlayLogService();
            }
        }
        return instance;
    }

    public void error(String content) {
        loggerServicePlay.error(content);
    }

    public void debug(String content) {
        loggerServicePlay.debug(content);
    }
}  
