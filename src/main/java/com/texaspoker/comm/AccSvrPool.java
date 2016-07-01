package com.texaspoker.comm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.channel.Channel;


public class AccSvrPool {
	private static AccSvrPool instance; 
	private Map<Long,Channel> mAccInfos;	//声音相关的数据
	
	//私有的构造函数
	private AccSvrPool (){
		mAccInfos = new ConcurrentHashMap<Long,Channel>()  ;
	}
	
	//单例模式
	public static  AccSvrPool getInstance() {  
		if (instance == null) {  
			synchronized(AccSvrPool.class){
				instance = new AccSvrPool();  
			}
		}  
		return instance;  
	} 
	
	public boolean setSession(Long id , Channel session){
		mAccInfos.put(id, session);
		return true;
	}
	
	public Channel getSession(Long id){
		return mAccInfos.get(id);
	}
	
	public void removeSession(Long id){
		mAccInfos.remove(id);
	}
	
//	public static String getInfo(IoSession session) {  
//		if (session == null) {  
//			return null;  
//	    }  
//	    
//		InetSocketAddress address = (InetSocketAddress) session  
//	                .getRemoteAddress(); 
//		
//		if(address == null){
//			return null;
//		}
//	    int port = address.getPort();  
//	    String ip = address.getAddress().getHostAddress();  
//	    return ip + ":" + port;  
//	} 
}
