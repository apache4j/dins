package com.texaspoker.apns;

import java.util.ArrayList;
import com.google.protobuf.RpcCallback;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.Log;
import com.texaspoker.logger.LoggerService;
import com.texaspoker.moment.TexasPokerAPNService.APNMsgResponse;
import com.texaspoker.moment.TexasPokerAPNService;
import com.texaspoker.moment.TexasPokerAPNService.APNMsgRequest;
import com.texaspoker.moment.TexasPokerAPNService.APNService;
import com.texaspoker.moment.TexasPokerAPNService.BuyinPushRequest;
import com.texaspoker.moment.TexasPokerAPNService.CreateGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNService.EmptyGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNService.EnterGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNService.GameOverPushRequest;
import com.texaspoker.moment.TexasPokerAPNService.StartGameRoomPushRequest;
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
    APNService.Stub apnService ;
    RpcCallback<APNMsgResponse> done;
    
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
	   	 apnService = TexasPokerAPNService.APNService.newStub(channel);
	   	 
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
	
	public void enterGameRoomPush(long uuid,String strToken,String strNick,String strGameRoomName,int iGameRoomUsers,int badge){
		try{
			
			EnterGameRoomPushRequest.Builder stEnterGameRoomPushRequestBuilder = EnterGameRoomPushRequest.newBuilder();
			stEnterGameRoomPushRequestBuilder.setUuid(uuid);
			stEnterGameRoomPushRequestBuilder.setSToken(strToken);
			stEnterGameRoomPushRequestBuilder.setStrNick(strNick);
			stEnterGameRoomPushRequestBuilder.setIBadge(""+badge);
			stEnterGameRoomPushRequestBuilder.setStrRoomName(strGameRoomName);
			stEnterGameRoomPushRequestBuilder.setIGameRoomUsers(iGameRoomUsers);

			apnService.enterGameRoomPush(controller, stEnterGameRoomPushRequestBuilder.build(), null);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void buyinPush(long uuid,String strToken,String strNick,String strGameRoomName,int badge){
		try{
			
			BuyinPushRequest.Builder stBuyinPushRequestBuilder = BuyinPushRequest.newBuilder();
			stBuyinPushRequestBuilder.setUuid(uuid);
			stBuyinPushRequestBuilder.setSToken(strToken);
			stBuyinPushRequestBuilder.setStrNick(strNick);
			stBuyinPushRequestBuilder.setIBadge(""+badge);
			stBuyinPushRequestBuilder.setStrRoomName(strGameRoomName);

			apnService.buyinPush(controller, stBuyinPushRequestBuilder.build(),null);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void StartGameRoomPush(long uuid,String strToken,String strGameRoomName,int badge){
		try{
			
			StartGameRoomPushRequest.Builder stStartGameRoomPushRequestBuilder = StartGameRoomPushRequest.newBuilder();
			stStartGameRoomPushRequestBuilder.setUuid(uuid);
			stStartGameRoomPushRequestBuilder.setSToken(strToken);
			stStartGameRoomPushRequestBuilder.setSGameRoomName(strGameRoomName);

			apnService.startGameRoomPush(controller, stStartGameRoomPushRequestBuilder.build(),null);

		}catch(Exception e){
			e.printStackTrace();
		}
	} 
	
	public void gameOverPush(long uuid,String strToken,String strGameRoomName,int badge){
		try{
			
			System.out.println("gameOverPushgameOverPushgameOverPush!!!!!!!!!!~~~~~~~~~~~~~~~~~~~~");
			GameOverPushRequest.Builder stGameOverPushRequestBuilder = GameOverPushRequest.newBuilder();
			stGameOverPushRequestBuilder.setUuid(uuid);
			stGameOverPushRequestBuilder.setSToken(strToken);
			stGameOverPushRequestBuilder.setIBadge(""+badge);
			stGameOverPushRequestBuilder.setStrRoomName(strGameRoomName);

			apnService.gameOverPush(controller, stGameOverPushRequestBuilder.build(),null);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void createGameRoomPush(long uuid,long lClubID,String strGameRoomName,String sHour,long lSmallBlinds,int iAnte,int badge){
		try{
			
			CreateGameRoomPushRequest.Builder stCreateGameRoomPushRequestBuilder = CreateGameRoomPushRequest.newBuilder();
			stCreateGameRoomPushRequestBuilder.setUuid(uuid);
			stCreateGameRoomPushRequestBuilder.setLClubID(lClubID);
			stCreateGameRoomPushRequestBuilder.setSHour(sHour);
			stCreateGameRoomPushRequestBuilder.setLSmallBlind(lSmallBlinds);
			stCreateGameRoomPushRequestBuilder.setIAnte(iAnte);
			stCreateGameRoomPushRequestBuilder.setSGameRoomName(strGameRoomName);
			stCreateGameRoomPushRequestBuilder.setSGameRoomType("普通保险");
			apnService.createGameRoomPush(controller, stCreateGameRoomPushRequestBuilder.build(),null);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void emptyGameRoomPush(long uuid,long lClubID,String strGameRoomName,String sHour,long lSmallBlinds,int iAnte,int badge){
		try{
			
			EmptyGameRoomPushRequest.Builder stEmptyGameRoomPushRequestBuilder = EmptyGameRoomPushRequest.newBuilder();
			stEmptyGameRoomPushRequestBuilder.setUuid(uuid);
			stEmptyGameRoomPushRequestBuilder.setLClubID(lClubID);
			stEmptyGameRoomPushRequestBuilder.setSHour(sHour);
			stEmptyGameRoomPushRequestBuilder.setLSmallBlind(lSmallBlinds);
			stEmptyGameRoomPushRequestBuilder.setIAnte(iAnte);

			apnService.emptyGameRoomPush(controller, stEmptyGameRoomPushRequestBuilder.build(),null);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
