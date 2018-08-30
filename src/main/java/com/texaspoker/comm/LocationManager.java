package com.texaspoker.comm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.texaspoker.database.ConnectDatabase;
public class LocationManager {
    private ConcurrentHashMap<Long, Long>   mLeagueids = new ConcurrentHashMap<>();;
    private boolean bOpen = false;
    /**
     * 本类实例对象
     */
    private static LocationManager instance;
    /**
     * 私有构造方法
     */
    private LocationManager() {
        LimitRefreshService stLimitRefreshService = new LimitRefreshService();
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(stLimitRefreshService, 100, 10*60*1000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 获取本类唯一实例对象
     *
     * @return 本类实例对象
     */
    public static synchronized LocationManager getInstance() {
        if (instance == null){
            synchronized(LocationManager.class){
                instance = new LocationManager();  
            }
        }
        return instance;
    }
    
    //重新载入内存
    public void reloadSetting() {
        Connection stConnection = null;
        try {
            //先清空内存，初始化数据
            mLeagueids.clear();
            bOpen = false;
            
            stConnection = ConnectDatabase.getInstance().mDatabasePool.getConnection();
            String insertChatRoomMemberSl = "select id from t_opencheck_league";
            PreparedStatement ps = stConnection.prepareStatement(insertChatRoomMemberSl);
            ResultSet leagueids =null;
            leagueids = ps.executeQuery();
            while(leagueids.next()){
                long leagueid = leagueids.getLong("id");
                System.out.println("LocationManager --  reloadSetting >>>"+ leagueid);
                if(leagueid == 100){
                    bOpen = true;
                    break;
                }else if(leagueid > 0){
                    mLeagueids.put(leagueid, leagueid);
                }
            }
            stConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                stConnection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    /*
     * 根据联盟ID判断是否进行每一手GPS和IP的检测
     */
    public boolean checkOpen(long leangueid){
        if(!bOpen){
            if(leangueid <= 0 || !mLeagueids.contains(leangueid)){
                return false;
            }
        }
        return true;
    }
}
