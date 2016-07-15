package com.texaspoker.bill;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPokerBillService;
import com.texaspoker.moment.TexasPokerBillService.BuyStackInRoomRequest;
import com.texaspoker.moment.TexasPokerBillService.CreateBillInfoRequest;
import com.texaspoker.moment.TexasPokerBillService.RemoveBillInfoRequest;
import com.texaspoker.moment.TexasPokerBillService.RemoveBillInfoResponse;
import com.texaspoker.moment.TexasPokerBillService.SetCurrentStacksInRoomRequest;
import com.texaspoker.moment.TexasPokerBillService.AddInsuranceBuyinRequest;
import com.texaspoker.moment.TexasPokerBillService.AddInsuranceGetStackRequest;
import com.texaspoker.moment.TexasPokerBillService.SetGameOverTagRequest;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class BillManager {
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerBillService.BillService.BlockingInterface blockingBillService ;

    TexasPokerBillService.BillService.Stub billService ;
    
	private static BillManager instance;
	
	//私有的构造函数
	private BillManager (){
	    host = ConfigaturationManager.sBillGameServiceHost;
	    port = ConfigaturationManager.iBillGameServicePost;
	    
		client = new RpcClient();
		channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
	    	return;
	    }
	    
	    blockingBillService = TexasPokerBillService.BillService.newBlockingStub(channel);
	    billService = TexasPokerBillService.BillService.newStub(channel);
	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
		
	}
	
	//单例模式
	public static  BillManager getInstance() {  
		if (instance == null) {  
			synchronized(BillManager.class){
				instance = new BillManager(); 
			}
		}  
		return instance;  
	}  
	
//	//获取用户所对应的房间
//	public ArrayList<Long> getUserBillInfo(long uuid){
//		GetUserBillInfoRequest.Builder stGetUserBillInfoRequestBuilder = GetUserBillInfoRequest.newBuilder();
//		stGetUserBillInfoRequestBuilder.setUuid(uuid);
//		GetUserBillInfoResponse stGetUserBillInfoResponse = null;
//		try {
//			stGetUserBillInfoResponse = blockingBillService.getUserBillInfo(controller, stGetUserBillInfoRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if(stGetUserBillInfoResponse!=null && stGetUserBillInfoResponse.getIErrorCode() == 0){
//			return new ArrayList<Long>(stGetUserBillInfoResponse.getVBillIDsList());
//		}else{
//			return null;
//		}
//	}
//	//获取用户所对应的房间
//	public ArrayList<Long> getUserBillInfoOver(long uuid){
//		GetUserBillInfoOverRequest.Builder stGetUserBillInfoOverRequestBuilder = GetUserBillInfoOverRequest.newBuilder();
//		stGetUserBillInfoOverRequestBuilder.setUuid(uuid);
//		GetUserBillInfoOverResponse stGetUserBillInfoOverResponse = null;
//		try {
//			stGetUserBillInfoOverResponse = blockingBillService.getUserBillInfoOver(controller, stGetUserBillInfoOverRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if(stGetUserBillInfoOverResponse!=null && stGetUserBillInfoOverResponse.getIErrorCode() == 0){
//			return new ArrayList<Long>(stGetUserBillInfoOverResponse.getVBillIDsList());
//		}else{
//			return null;
//		}
//	}
	
	public boolean removeBill(long uuid , long roomid){
		//设置用户
		RemoveBillInfoRequest.Builder stRemoveBillInfoRequestBuilder = RemoveBillInfoRequest.newBuilder();
		stRemoveBillInfoRequestBuilder.setUuid(uuid);
		stRemoveBillInfoRequestBuilder.setLBillID(roomid);
		RemoveBillInfoResponse stRemoveBillInfoResponse = null;
		try {
			stRemoveBillInfoResponse = blockingBillService.removeBillInfo(controller, stRemoveBillInfoRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stRemoveBillInfoResponse!=null && stRemoveBillInfoResponse.getIErrorCode() == 0){
			return true;
		}else{
			return false;
		}
	}
	
//	//获取某个房间的账单信息
//	public BillInfoNet getRoomBills(long roomid){
//		//设置用户
//		GetRoomBillInfoRequest.Builder stGetRoomBillInfoRequestBuilder = GetRoomBillInfoRequest.newBuilder();
//		stGetRoomBillInfoRequestBuilder.setLBillID(roomid);
//		
//		GetRoomBillInfoResponse stGetRoomBillInfoResponse = null;
//		try {
//			stGetRoomBillInfoResponse = blockingBillService.getRoomBillInfo(controller, stGetRoomBillInfoRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if(stGetRoomBillInfoResponse!=null && stGetRoomBillInfoResponse.getIErrorCode() == 0){
//			
//			return stGetRoomBillInfoResponse.getStBillInfoNet();
//		}else{
//			return null;
//		}
//	}

	//改变某个房间的某个用户的购买筹码的值
	public void buyStackInRoom(long uuid , long roomid , long changeStacks){
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
		
		if(stGameRoomDetailInfo!=null 
				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
			//设置用户
			BuyStackInRoomRequest.Builder stBuyStackInRoomRequestBuilder = BuyStackInRoomRequest.newBuilder();
			stBuyStackInRoomRequestBuilder.setUuid(uuid);
			stBuyStackInRoomRequestBuilder.setLBillID(roomid);
			stBuyStackInRoomRequestBuilder.setLChangeStacks(changeStacks);
			
			try {
				blockingBillService.buyStackInRoom(controller, stBuyStackInRoomRequestBuilder.build());
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
//	//改变某个房间的某个用户的总筹码至
//	public void setFinalStacksInRoom(long uuid,long roomid,long stacks,int iSNGRank){
//		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
//		
//		if(stGameRoomDetailInfo!=null 
//				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
//				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
//			//设置用户
//			SetFinalStacksInRoomRequest.Builder stSetFinalStacksInRoomRequestBuilder = SetFinalStacksInRoomRequest.newBuilder();
//			stSetFinalStacksInRoomRequestBuilder.setUuid(uuid);
//			stSetFinalStacksInRoomRequestBuilder.setLBillID(roomid);
//			stSetFinalStacksInRoomRequestBuilder.setLStacks(stacks);
//			stSetFinalStacksInRoomRequestBuilder.setISNGRank(0);
//			try {
//				blockingBillService.setFinalStacksInRoom(controller, stSetFinalStacksInRoomRequestBuilder.build());
//			} catch (ServiceException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}	
//	}	
	
	public void setCurrentStacksInRoom(long uuid,long roomid,long stacks){
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
		
		if(stGameRoomDetailInfo!=null 
				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
			//设置用户
			SetCurrentStacksInRoomRequest.Builder stSetCurrentStacksInRoomRequestBuilder = SetCurrentStacksInRoomRequest.newBuilder();
			stSetCurrentStacksInRoomRequestBuilder.setUuid(uuid);
			stSetCurrentStacksInRoomRequestBuilder.setLBillID(roomid);
			stSetCurrentStacksInRoomRequestBuilder.setLStacks(stacks);
			stSetCurrentStacksInRoomRequestBuilder.setISNGRank(0);
			
			billService.setCurrentStacksInRoom(controller, stSetCurrentStacksInRoomRequestBuilder.build(), null);
//			try {
//				blockingBillService.setCurrentStacksInRoom(controller, stSetCurrentStacksInRoomRequestBuilder.build());
//			} catch (ServiceException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	public void addInsuranceBuyin(long uuid,long roomid,long stacks){
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
		
		if(stGameRoomDetailInfo!=null 
				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
			//设置用户
			AddInsuranceBuyinRequest.Builder stAddInsuranceBuyinRequestBuilder = AddInsuranceBuyinRequest.newBuilder();
			stAddInsuranceBuyinRequestBuilder.setUuid(uuid);
			stAddInsuranceBuyinRequestBuilder.setLBillID(roomid);
			stAddInsuranceBuyinRequestBuilder.setLInsuranceBuyin(stacks);
			
			billService.addInsuranceBuyin(controller, stAddInsuranceBuyinRequestBuilder.build(), null);
			
//			try {
//				blockingBillService.setCurrentStacksInRoom(controller, stSetCurrentStacksInRoomRequestBuilder.build());
//			} catch (ServiceException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	public void addInsuranceGetStacks(long uuid,long roomid,long stacks){
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
		
		if(stGameRoomDetailInfo!=null 
				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
			//设置用户
			AddInsuranceGetStackRequest.Builder stAddInsuranceGetStackRequestBuilder = AddInsuranceGetStackRequest.newBuilder();
			stAddInsuranceGetStackRequestBuilder.setUuid(uuid);
			stAddInsuranceGetStackRequestBuilder.setLBillID(roomid);
			stAddInsuranceGetStackRequestBuilder.setLBillID(stacks);
			
			billService.addInsuranceGetStack(controller, stAddInsuranceGetStackRequestBuilder.build(), null);
			
//			try {
//				blockingBillService.setCurrentStacksInRoom(controller, stSetCurrentStacksInRoomRequestBuilder.build());
//			} catch (ServiceException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}

	public void setGameOverTag(long roomid,long maxpot , long totalgamenum){
		//设置用户
		SetGameOverTagRequest.Builder stSetGameOverTagRequestBuilder = SetGameOverTagRequest.newBuilder();
		stSetGameOverTagRequestBuilder.setLBillID(roomid);
		stSetGameOverTagRequestBuilder.setLMaxpot(maxpot);
		stSetGameOverTagRequestBuilder.setLTotalgamenum(totalgamenum);
			
		try {
			blockingBillService.setGameOverTag(controller, stSetGameOverTagRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createBillInfo(long uuid,GameRoomBaseInfo stGameRoomBaseInfo){
		//设置用户
		CreateBillInfoRequest.Builder stCreateBillInfoRequestBuilder = CreateBillInfoRequest.newBuilder();
		stCreateBillInfoRequestBuilder.setLBillID(stGameRoomBaseInfo.getLGameRoomId());
		stCreateBillInfoRequestBuilder.setUuid(uuid);
		stCreateBillInfoRequestBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL);
		stCreateBillInfoRequestBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfo);
			
		try {
			blockingBillService.createBillInfo(controller, stCreateBillInfoRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

