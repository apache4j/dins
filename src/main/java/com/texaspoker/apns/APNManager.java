package com.texaspoker.apns;

import java.util.ArrayList;
import com.google.protobuf.RpcCallback;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.Log;
import com.texaspoker.logger.LoggerService;
import com.texaspoker.moment.TexasPokerAPNService.APNClubGameEmptyRequest;
import com.texaspoker.moment.TexasPokerAPNService.APNClubGameEmptyResponse;
import com.texaspoker.moment.TexasPokerAPNService.APNMsgResponse;
import com.texaspoker.moment.TexasPokerAPNService.APNMsgService;
import com.texaspoker.moment.TexasPokerAPNService;
import com.texaspoker.moment.TexasPokerAPNService.APNMsgRequest;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class APNManager  {

	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    APNMsgService.Stub apnService ;
    RpcCallback<APNMsgResponse> done;
    RpcCallback<APNClubGameEmptyResponse> stClubDone;
    
	private static APNManager instance;
    static LoggerService mLog = Log.getLoggerService(APNManager.class.getName()); 
    ArrayList<APNData> vAPNDatas = new ArrayList<APNData>();
    ArrayList<APNData> vTempAPNDatas = new ArrayList<APNData>();
    
	//私有的构造函数
	private APNManager (){
	    host = ConfigaturationManager.sApnServiceHost;
	    port = ConfigaturationManager.iApnServicePost;
		
		client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	      channel.connect(host, port);
	    } catch (Exception e) {
	      return;
	    }
	    
	   	 // Make the (asynchronous) RPC request
	   	 apnService = TexasPokerAPNService.APNMsgService.newStub(channel);
	   	 
		 // Create the response
	  	 done = new RpcCallback<APNMsgResponse>() {
	  	 public void run(APNMsgResponse response) {
	  		 if (response != null) {
//	  		 System.out.println("asyn echo response:" + response.getIErrorCode());
	  		 } else {
//	  		 System.out.println("asyn echo response null" + controller.errorText());
	  		 }
	  	 }
	  	 };
	  	 
	  	stClubDone = new RpcCallback<APNClubGameEmptyResponse>() {
		  	 public void run(APNClubGameEmptyResponse response) {
		  		 if (response != null) {
//		  		 System.out.println("asyn echo response:" + response.getIErrorCode());
		  		 } else {
//		  		 System.out.println("asyn echo response null" + controller.errorText());
		  		 }
		  	 }
		  	 };
	}
	
	//单例模式
	public static  APNManager getInstance() {          
		if (instance == null) {  
			synchronized(APNManager.class){
				instance = new APNManager();  
			}
		}  
		return instance;  
	}  

	public void pushAPNMsg(long uuid,String strToken,String msg,int badge){
		try{
			APNMsgRequest.Builder stAPNMsgRequestBuilder = APNMsgRequest.newBuilder();
			stAPNMsgRequestBuilder.setUuid(uuid);
			stAPNMsgRequestBuilder.setSMsg(msg);
			stAPNMsgRequestBuilder.setSToken(strToken);
			stAPNMsgRequestBuilder.setIBadge(""+badge);

			apnService.pushFriendMsg(controller, stAPNMsgRequestBuilder.build(), done);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void pushClubGameEmptyAPNMsg(long uuid,long lClubID,String msg,int badge){
		try{
			APNClubGameEmptyRequest.Builder stAPNClubGameEmptyRequestBuilder = APNClubGameEmptyRequest.newBuilder();
			stAPNClubGameEmptyRequestBuilder.setUuid(uuid);
			stAPNClubGameEmptyRequestBuilder.setSMsg(msg);
			stAPNClubGameEmptyRequestBuilder.setLClubID(lClubID);
			stAPNClubGameEmptyRequestBuilder.setIBadge(""+badge);

			apnService.pushAPNClubGameEmpty(controller, stAPNClubGameEmptyRequestBuilder.build(), stClubDone);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
