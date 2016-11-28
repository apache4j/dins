package com.texaspoker.league;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerClubService;
import com.texaspoker.moment.TexasPokerClubService.GetLeagueBaseInfoRequest;
import com.texaspoker.moment.TexasPokerClubService.GetLeagueBaseInfoResponse;
import com.texaspoker.moment.TexasPokerClubService.GetLeagueInfoRequest;
import com.texaspoker.moment.TexasPokerClubService.GetLeagueInfoResponse;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueBaseInfo;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueInfo;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class LeagueManager {	
	private static LeagueManager instance; 
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    private TexasPokerClubService.ClubService.BlockingInterface blockingClubService ;
	
	
	private LeagueManager (){		
	    host = ConfigaturationManager.sClubServiceHost;
	    port = ConfigaturationManager.iClubServicePost;
	    
		//RPC初始化流程
		client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
	    	return;
	    }
	    
	    blockingClubService = TexasPokerClubService.ClubService.newBlockingStub(channel);
	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
	}
	
	//初始化
	public static  LeagueManager getInstance() {  
		if (instance == null) {  
			synchronized(LeagueManager.class){
				instance = new LeagueManager();
			}
		}  
		return instance;  
	}
	
	public LeagueBaseInfo  getLeagueBaseInfo(long lLeagueID){
		GetLeagueBaseInfoRequest.Builder stGetLeagueBaseInfoRequestBuilder = GetLeagueBaseInfoRequest.newBuilder();
		stGetLeagueBaseInfoRequestBuilder.setLLeagueID(lLeagueID);
		
	    try {
	    	GetLeagueBaseInfoResponse response = blockingClubService.getLeagueBaseInfo(controller, stGetLeagueBaseInfoRequestBuilder.build());
	    	
			if (response != null && response.getIErrorCode()==0 && response.getStLeagueBaseInfo()!=null) {
	    		return response.getStLeagueBaseInfo();
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	System.out.println("syn echo error:" + e.getMessage());
	    	return null;
	    }
	}
	
	public LeagueInfo  getLeagueInfo(long lLeagueIDl){
		GetLeagueInfoRequest.Builder stGetLeagueInfoRequestBuilder = GetLeagueInfoRequest.newBuilder();
		stGetLeagueInfoRequestBuilder.setLLeagueID(lLeagueIDl);
		
	    try {
	    	GetLeagueInfoResponse response = blockingClubService.getLeagueInfo(controller, stGetLeagueInfoRequestBuilder.build());
	    	
			if (response != null && response.getIErrorCode()==0 && response.getStLeagueInfo()!=null) {
	    		return response.getStLeagueInfo();
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	System.out.println("syn echo error:" + e.getMessage());
	    	return null;
	    }
	}

}
