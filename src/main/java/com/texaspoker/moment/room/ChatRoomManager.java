package com.texaspoker.moment.room;

import java.util.ArrayList;
import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPokerMsgService;
import com.texaspoker.moment.TexasPokerMsgService.AddChatGameRoomsRequest;
import com.texaspoker.moment.TexasPokerMsgService.GetChatGameRoomsRequest;
import com.texaspoker.moment.TexasPokerMsgService.GetChatGameRoomsResponse;
import com.texaspoker.moment.TexasPokerMsgService.GetChatRoomInfoRequest;
import com.texaspoker.moment.TexasPokerMsgService.GetChatRoomInfoResponse;
import com.texaspoker.moment.TexasPokerMsgService.RemoveChatGameRoomsRequest;
import com.texaspoker.moment.TexasPokerMsgService.RemoveChatRoomInfoRequest;
import com.texaspoker.moment.TexasPoker.ChatRoomInfo;
import com.texaspoker.moment.TexasPokerMsgService.SetChatRoomInfoRequest;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class ChatRoomManager {
	private static ChatRoomManager instance; 
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerMsgService.MsgService.BlockingInterface blockingMsgService ;
	
	//私有的构造函数
	private ChatRoomManager (){
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
//		System.out.println("pushFriendMsg client connect " + host + ":" + port + " ok");
		blockingMsgService = TexasPokerMsgService.MsgService.newBlockingStub(channel);
	}
	
	public ChatRoomInfo getChatRoomInfo(long lRoomID) {
//		GetChatRoomInfoRequest.Builder stGetChatRoomInfoRequestBuilder = GetChatRoomInfoRequest.newBuilder();
//		stGetChatRoomInfoRequestBuilder.setLChatRoomID(lRoomID);
//
//		try {
//			GetChatRoomInfoResponse stGetChatRoomInfoResponse = blockingMsgService.getChatRoomInfo(controller, stGetChatRoomInfoRequestBuilder.build());
//			if(stGetChatRoomInfoResponse!=null && stGetChatRoomInfoResponse.getIErrorCode()==0
//					&&stGetChatRoomInfoResponse.getStChatRoomInfo()!=null){
//				return stGetChatRoomInfoResponse.getStChatRoomInfo();
//			}
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return null;
	}

	public void setChatRoomInfo(long lRoomID,ChatRoomInfo stRoomInfo) {
//		SetChatRoomInfoRequest.Builder stSetChatRoomInfoRequestBuilder = SetChatRoomInfoRequest.newBuilder();
//		stSetChatRoomInfoRequestBuilder.setLChatRoomID(lRoomID);
//		stSetChatRoomInfoRequestBuilder.setStChatRoomInfo(stRoomInfo);
//
//		try {
//			blockingMsgService.setChatRoomInfo(controller, stSetChatRoomInfoRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public void removeChatRoomInfo(long lRoomID) {
		RemoveChatRoomInfoRequest.Builder stRemoveChatRoomInfoRequestBuilder = RemoveChatRoomInfoRequest.newBuilder();
		stRemoveChatRoomInfoRequestBuilder.setLChatRoomID(lRoomID);
		
		try {
			blockingMsgService.removeChatRoomInfo(controller, stRemoveChatRoomInfoRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//单例模式
	public static ChatRoomManager getInstance() {  
		if (instance == null) {  
			synchronized(ChatRoomManager.class){
				instance = new ChatRoomManager();
			}
		}  
		return instance;  
	}
	
	//添加聊天室相关的牌局数据
	public void addGameRooms(long chatroomid,long gameroomid){
//		AddChatGameRoomsRequest.Builder stAddChatGameRoomsRequestBuilder = AddChatGameRoomsRequest.newBuilder();
//		stAddChatGameRoomsRequestBuilder.setLChatRoomID(chatroomid);
//		stAddChatGameRoomsRequestBuilder.setLGameRoomID(gameroomid);
//
//		try {
//			blockingMsgService.addChatGameRooms(controller, stAddChatGameRoomsRequestBuilder.build());
//
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	//获取聊天室相关的牌局数据
	public ArrayList<Long> getGameRooms(long chatroomid){
		ArrayList<Long> vGameRooms = new ArrayList<Long>();
		GetChatGameRoomsRequest.Builder stGetChatGameRoomsRequestBuilder = GetChatGameRoomsRequest.newBuilder();
		stGetChatGameRoomsRequestBuilder.setLChatRoomID(chatroomid);
		try {
			GetChatGameRoomsResponse stGetChatGameRoomsResponse =  blockingMsgService.getChatGameRooms(controller, stGetChatGameRoomsRequestBuilder.build());
			if(stGetChatGameRoomsResponse!=null 
					&& stGetChatGameRoomsResponse.getIErrorCode()==0
					&& stGetChatGameRoomsResponse.getVGameRoomIDsCount() > 0){
				for(Long lGameRoomID:stGetChatGameRoomsResponse.getVGameRoomIDsList()){
					vGameRooms.add(lGameRoomID);
				}
			}
			return vGameRooms;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	//删除聊天室相关牌局数据
	public void removeChatRoomGameRoom(long chatroomid,long gameroomid){
//		RemoveChatGameRoomsRequest.Builder stRemoveChatGameRoomsRequestBuilder = RemoveChatGameRoomsRequest.newBuilder();
//		stRemoveChatGameRoomsRequestBuilder.setLChatRoomID(chatroomid);
//		stRemoveChatGameRoomsRequestBuilder.setLGameRoomID(gameroomid);
//		try {
//			blockingMsgService.removeChatGameRooms(controller, stRemoveChatGameRoomsRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}
