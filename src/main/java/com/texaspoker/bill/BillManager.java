package com.texaspoker.bill;

import java.util.List;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPokerBillService;
import com.texaspoker.moment.TexasPokerBillService.BuyStackInRoomRequest;
import com.texaspoker.moment.TexasPokerBillService.CreateBillInfoRequest;
import com.texaspoker.moment.TexasPokerBillService.FinishGameRoomAndReturnRequest;
import com.texaspoker.moment.TexasPokerBillService.GetAllBillUsersRequest;
import com.texaspoker.moment.TexasPokerBillService.GetAllBillUsersResponse;
import com.texaspoker.moment.TexasPokerBillService.RemoveBillInfoRequest;
import com.texaspoker.moment.TexasPokerBillService.RemoveBillInfoResponse;
import com.texaspoker.moment.TexasPokerBillService.SetCurrentStacksInRoomRequest;
import com.texaspoker.moment.TexasPokerBillService.AddInsuranceBuyinRequest;
import com.texaspoker.moment.TexasPokerBillService.AddInsuranceGetStackRequest;
import com.texaspoker.moment.TexasPokerBillService.SetFinalInsuranceInfoRequest;
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
			stAddInsuranceGetStackRequestBuilder.setLGetStacks(stacks);
			
			billService.addInsuranceGetStack(controller, stAddInsuranceGetStackRequestBuilder.build(), null);
			
		}
	}
	
	public void setFinalInsuranceGetStacks(long uuid,long roomid,long stacks){
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
		
		if(stGameRoomDetailInfo!=null 
				&& stGameRoomDetailInfo.getStGameRoomInfo()!=null
				&& stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo()!=null){
			//设置用户
			SetFinalInsuranceInfoRequest.Builder stSetFinalInsuranceInfoRequestBuilder = SetFinalInsuranceInfoRequest.newBuilder();
			stSetFinalInsuranceInfoRequestBuilder.setUuid(uuid);
			stSetFinalInsuranceInfoRequestBuilder.setLBillID(roomid);
			stSetFinalInsuranceInfoRequestBuilder.setLTotalGetStacks(stacks);
			stSetFinalInsuranceInfoRequestBuilder.setLTotalInsuranceBuyin(0);
			
			billService.setFinalInsuranceInfo(controller, stSetFinalInsuranceInfoRequestBuilder.build(), null);
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
	
	public List<Long> getBillInfoUsers(long lBillID){
		GetAllBillUsersRequest.Builder stGetAllBillUsersRequestBuilder = GetAllBillUsersRequest.newBuilder();
		stGetAllBillUsersRequestBuilder.setLBillID(lBillID);
		
		GetAllBillUsersResponse stGetAllBillUsersResponse = null;
		try {
			stGetAllBillUsersResponse = blockingBillService.getAllBillUsers(controller, stGetAllBillUsersRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stGetAllBillUsersResponse!=null && stGetAllBillUsersResponse.getIErrorCode() == 0){
			return stGetAllBillUsersResponse.getVUuidsList();
		}else{
			return null;
		}
	}
	
	public void finishGameRoomAndReturn(long lBillID){
		FinishGameRoomAndReturnRequest.Builder stFinishGameRoomAndReturnRequestBuilder = FinishGameRoomAndReturnRequest.newBuilder();
		stFinishGameRoomAndReturnRequestBuilder.setLBillID(lBillID);
		
		try {
			blockingBillService.finishGameRoomAndReturn(controller, stFinishGameRoomAndReturnRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

