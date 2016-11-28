package com.texaspoker.msg;

import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPoker.BuyinControlMsg;
import com.texaspoker.moment.TexasPoker.GameRoomPushMsg;
import com.texaspoker.moment.TexasPoker.PushMsg;
import com.texaspoker.moment.TexasPoker.QuickGameRoomPushMsg;
import com.texaspoker.moment.TexasPoker.SCGameRoomStateChange;
import com.texaspoker.moment.TexasPoker.UseItemPushMsg;
import com.texaspoker.moment.TexasPokerMsgService;
import com.texaspoker.moment.TexasPokerMsgService.BuyinControlMsgRequest;
import com.texaspoker.moment.TexasPokerMsgService.GameRoomPushMsgRequest;
import com.texaspoker.moment.TexasPokerMsgService.GameRoomStateChangePushRequest;
import com.texaspoker.moment.TexasPokerMsgService.MsgService;
import com.texaspoker.moment.TexasPokerMsgService.PushMsgRequest;
import com.texaspoker.moment.TexasPokerMsgService.QuickGameRoomPushMsgRequest;
import com.texaspoker.moment.TexasPokerMsgService.UseItemPushMsgRequest;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;


public class GameRoomMsgManager {
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
 
    MsgService.Stub msgService ;
	private static GameRoomMsgManager instance;

	
	//私有的构造函数
	private GameRoomMsgManager (){		
	    host = ConfigaturationManager.sMsgServiceHost;
	    port = ConfigaturationManager.iMsgServicePost;
	    
		client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	      channel.connect(host, port);
	    } catch (Exception e) {
	      System.out.println("pushFriendMsg client connect error:" + e.getMessage());
	      return;
	    }
	    
		//机器类型和Update的对应表
	    msgService = TexasPokerMsgService.MsgService.newStub(channel);
	}
	
	//单例模式
	public static GameRoomMsgManager getInstance() {  
		if (instance == null) {  
			synchronized(GameRoomMsgManager.class){
				instance = new GameRoomMsgManager(); 
			}
		}  
		return instance;  
	}  
	
	public void sendGameRoomMsg(Long toUserId,GameRoomPushMsg stGameRoomPushMsg){
		// TODO Auto-generated method stub
		GameRoomPushMsgRequest.Builder stGameRoomPushMsgBuilder = GameRoomPushMsgRequest.newBuilder(); 
		stGameRoomPushMsgBuilder.setUuid(toUserId);
		stGameRoomPushMsgBuilder.addVPushMsgs(stGameRoomPushMsg);
		
		msgService.pushGameRoomPushMsg(controller, stGameRoomPushMsgBuilder.build(),null);
//		try {
//			GameRoomPushMsgResponse stGameRoomPushMsgResponse = blockingMsgService.pushGameRoomPushMsg(controller, stGameRoomPushMsgBuilder.build());			
//		    if (stGameRoomPushMsgResponse != null) {
////		    	System.out.println("sendGameRoomMsg syn game service response:" + stGameRoomPushMsgResponse);
//		    } else {
////		    	System.out.println("syn echo response null" + controller.errorText());
//		    }
//		} catch (ServiceException e) {
//		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
	}
	
	public boolean sendQuickGameRoomMsg(Long toUserId,QuickGameRoomPushMsg stQuickGameRoomPushMsg){		
		QuickGameRoomPushMsgRequest.Builder stQuickGameRoomPushMsgBuilder = QuickGameRoomPushMsgRequest.newBuilder(); 
		stQuickGameRoomPushMsgBuilder.setUuid(toUserId);
		stQuickGameRoomPushMsgBuilder.addVQuickGameRoomPushMsgs(stQuickGameRoomPushMsg);
		
		msgService.pushQuickRoomMsg(controller, stQuickGameRoomPushMsgBuilder.build(),null);
//		try {
////		    if (stQuickGameRoomPushMsgResponse != null) {
//////		    	System.out.println("sendQuickGameRoomMsg syn game service response:" + stQuickGameRoomPushMsgResponse);
////		    } else {
//////		    	System.out.println("syn echo response null" + controller.errorText());
////		    }
//		} catch (ServiceException e) {
//		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
		return true;
	}
	
	public void sendGameRoomStateChangeMsg(Long toUserId,SCGameRoomStateChange stSCGameRoomStateChange){
//		System.out.println("sendGameRoomStateChangeMsg.");
		GameRoomStateChangePushRequest.Builder stGameRoomStateChangePushRequestBuilder = GameRoomStateChangePushRequest.newBuilder(); 
		stGameRoomStateChangePushRequestBuilder.setUuid(toUserId);
		stGameRoomStateChangePushRequestBuilder.setStSCGameRoomStateChange(stSCGameRoomStateChange);
//		TexasPokerMsgService.MsgService.BlockingInterface blockingMsgService =   TexasPokerMsgService.MsgService.newBlockingStub(channel);
		
		msgService.pushGameRoomStateChange(controller, stGameRoomStateChangePushRequestBuilder.build(),null);
//		try {
//			GameRoomStateChangePushResponse stGameRoomStateChangePushResponse = blockingMsgService.pushGameRoomStateChange(controller, stGameRoomStateChangePushRequestBuilder.build());
//	
//		    if (stGameRoomStateChangePushResponse != null) {
////		    	System.out.println("sendGameRoomStateChangeMsg syn game service response:" + stGameRoomStateChangePushResponse);
//		    } else {
////		    	System.out.println("syn echo response null" + controller.errorText());
//		    }
//		} catch (ServiceException e) {
//		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
	}
	
	public boolean sendMsg(Long toUserId,PushMsg stPushMsg){		
		PushMsgRequest.Builder stPushMsgRequestBuilder = PushMsgRequest.newBuilder(); 
		stPushMsgRequestBuilder.setUuid(toUserId);
		stPushMsgRequestBuilder.addVPushMsgs(stPushMsg);
//		TexasPokerMsgService.MsgService.BlockingInterface blockingMsgService =   TexasPokerMsgService.MsgService.newBlockingStub(channel);
		
		msgService.pushPushMsg(controller, stPushMsgRequestBuilder.build(),null);
		
//		try {
//			PushMsgResponse stPushMsgResponse = blockingMsgService.pushPushMsg(controller, stPushMsgRequestBuilder.build());
//	
//		    if (stPushMsgResponse != null) {
////		    	System.out.println("sendMsg syn game service response:" + stPushMsgResponse);
//		    } else {
////		    	System.out.println("syn echo response null" + controller.errorText());
//		    }
//		} catch (ServiceException e) {
////		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
		
		return true;
	}
	
	public boolean sendUseItemMsg(Long toUserId,UseItemPushMsg stUseItemPushMsg){		
		UseItemPushMsgRequest.Builder stUseItemPushMsgRequestBuilder = UseItemPushMsgRequest.newBuilder(); 
		stUseItemPushMsgRequestBuilder.setUuid(toUserId);
		stUseItemPushMsgRequestBuilder.addVPushMsgs(stUseItemPushMsg);
		
		msgService.pushUseItemPushMsg(controller, stUseItemPushMsgRequestBuilder.build(),null);
		
//		TexasPokerMsgService.MsgService.BlockingInterface blockingMsgService =   TexasPokerMsgService.MsgService.newBlockingStub(channel);
//		try {
//			UseItemPushMsgResponse stPushMsgResponse = blockingMsgService.pushUseItemPushMsg(controller, stUseItemPushMsgRequestBuilder.build());
//	
//		    if (stPushMsgResponse != null) {
////		    	System.out.println("sendUseItemMsg syn game service response:" + stPushMsgResponse);
//		    } else {
////		    	System.out.println("syn echo response null" + controller.errorText());
//		    }
//		} catch (ServiceException e) {
////		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
		
		return true;
	}
	
	public boolean sendBuyinControlMsg(Long toUserId,BuyinControlMsg stMsgData){
		
		BuyinControlMsgRequest.Builder stBuyinControlMsgRequestBuilder = BuyinControlMsgRequest.newBuilder(); 
		stBuyinControlMsgRequestBuilder.setUuid(toUserId);
		System.out.println("sendBuyinControlMsg toUserId : " + toUserId);
		stBuyinControlMsgRequestBuilder.addVPushMsgs(stMsgData);
		
		msgService.pushBuyinControlMsg(controller, stBuyinControlMsgRequestBuilder.build(),null);
		
//		TexasPokerMsgService.MsgService.BlockingInterface blockingMsgService =   TexasPokerMsgService.MsgService.newBlockingStub(channel);
//		try {
//			BuyinControlMsgResponse stBuyinControlMsgResponse = blockingMsgService.pushBuyinControlMsg(controller, stBuyinControlMsgRequestBuilder.build());
//	
//		    if (stBuyinControlMsgResponse != null) {
////		    	System.out.println("sendBuyinControlMsg syn game service response:" + stBuyinControlMsgResponse);
//		    } else {
////		    	System.out.println("syn echo response null" + controller.errorText());
//		    }
//		} catch (ServiceException e) {
////		   	System.out.println("syn echo error:" + e.getMessage());
//		} 
		
		return true;
	}
}
