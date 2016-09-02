package com.texaspoker.moment.room;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerRoomHallService;
import com.texaspoker.moment.TexasPokerRoomHallService.GetQuickGameRoomCodeRequest;
import com.texaspoker.moment.TexasPokerRoomHallService.GetQuickGameRoomCodeResponse;
import com.texaspoker.moment.TexasPokerRoomHallService.RoomIDRequest;
import com.texaspoker.moment.TexasPokerRoomHallService.RoomIDResponse;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class RoomIDManager {
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerRoomHallService.RoomIDService.BlockingInterface blockingUserService ;
    
	private static RoomIDManager instance; 
	
	//私有的构造函数
	private RoomIDManager (){
	    host = ConfigaturationManager.sRoomHallServiceHost;
	    port = ConfigaturationManager.iRoomHallServicePost;
	    
		//rpc初始化流程
	    client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
	    	return;
	    }
	    
	    blockingUserService = TexasPokerRoomHallService.RoomIDService.newBlockingStub(channel);
	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
	}
	
	//单例模式
	public static RoomIDManager getInstance() {  
		if (instance == null) {  
			synchronized(RoomIDManager.class){
				instance = new RoomIDManager();
			}
		}  
		return instance;  
	}
	
	public long getNewRoomID(long uuid){
		RoomIDRequest.Builder stRoomIDRequestBuilder = RoomIDRequest.newBuilder();
		stRoomIDRequestBuilder.setUuid(uuid);
		RoomIDResponse stRoomIDResponse = null;
		try {
			stRoomIDResponse = blockingUserService.getNewRoomID(controller, stRoomIDRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stRoomIDResponse!=null){
			return stRoomIDResponse.getLNewRoomID();
		}else{
			return 0;
		}
	}
	
	
	public String getRandomCode(){
		GetQuickGameRoomCodeRequest.Builder stGetQuickGameRoomCodeBuilder = GetQuickGameRoomCodeRequest.newBuilder();
		stGetQuickGameRoomCodeBuilder.setUuid(0);
		GetQuickGameRoomCodeResponse stResponse= null;
		try {
			stResponse = blockingUserService.getQuickGameRoomCode(controller, stGetQuickGameRoomCodeBuilder.build());
//			System.out.println("getNewRoomIDgetNewRoomID : " + stResponse.getIErrorCode());
//			System.out.println("getNewRoomIDgetNewRoomID : " + stResponse.getSCode());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stResponse!=null){
			return stResponse.getSCode();
		}else{
			return "";
		}
	}
	
	
}
