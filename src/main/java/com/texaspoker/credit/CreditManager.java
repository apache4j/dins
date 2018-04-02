package com.texaspoker.credit;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerClubCreditService;
import com.texaspoker.moment.TexasPokerClubCreditService.BuyInToCreditRequest;
import com.texaspoker.moment.TexasPokerClubCreditService.BuyInToCreditResponse;
import com.texaspoker.moment.TexasPokerClubCreditService.CheckBuyInToCreditRequest;
import com.texaspoker.moment.TexasPokerClubCreditService.CheckBuyInToCreditResponse;
import com.texaspoker.moment.TexasPokerClubCreditService.CreateGameToCreditRequest;
import com.texaspoker.moment.TexasPokerClubCreditService.FinishToCreditRequest;
import com.texaspoker.moment.TexasPokerClubCreditService.SitDownToCreditRequest;
import com.texaspoker.moment.TexasPokerClubCreditService.SitDownToCreditResponse;
import com.texaspoker.moment.TexasPokerClubCreditService.StandUpToCreditRequest;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class CreditManager {
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerClubCreditService.ClubCreditService.BlockingInterface blockingService ;

    
	private static CreditManager instance;
	
	//私有的构造函数
	private CreditManager (){
	    host = ConfigaturationManager.sCreditServiceHost;
	    port = ConfigaturationManager.iCreditServicePost;
	    
		client = new RpcClient();
		channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
	    	return;
	    }
	    
	    blockingService = TexasPokerClubCreditService.ClubCreditService.newBlockingStub(channel);
	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
		
	}
	
	//单例模式
	public static  CreditManager getInstance() {  
		if (instance == null) {  
			synchronized(CreditManager.class){
				instance = new CreditManager(); 
			}
		}  
		return instance;  
	}
	
	
	public int checkBuyInToCredit(long uuid,long lGameRoomId,long lClubId){
        int result = 0;
        CheckBuyInToCreditRequest.Builder request = CheckBuyInToCreditRequest.newBuilder();
        request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId);
        CheckBuyInToCreditResponse response = null;
        try {
            response = blockingService.checkBuyInToCredit(controller, request.build());
            if(response != null){
                result = response.getIState();
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return result;
    }
	
	public int buyInToCredit(long uuid,long lGameRoomId,long lClubId,long lBuyinStacks,long lHasBuyinStacks,int iOnTable,String sGameRoomName,long lRemainStacks){
	    int result = 3;
	    BuyInToCreditRequest.Builder request = BuyInToCreditRequest.newBuilder();
	    request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId).setLBuyinStacks(lBuyinStacks)
	           .setLHasBuyinStacks(lHasBuyinStacks).setIOnTable(iOnTable).setSGameRoomName(sGameRoomName).setLRemainStacks(lRemainStacks);
	    BuyInToCreditResponse response = null;
        try {
            response = blockingService.buyInToCredit(controller, request.build());
            if(response != null){
                result = response.getIState();
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return result;
    }
	
	public void createGameToCredit(long uuid,long lGameRoomId,long lClubId,long lLeagueId,int iBigBlind,int iGameRoomType,String sGameRoomName){
        CreateGameToCreditRequest.Builder request = CreateGameToCreditRequest.newBuilder();
        request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId).setLLeagueId(lLeagueId)
               .setIBigBlind(iBigBlind).setIGameRoomType(iGameRoomType).setSGameRoomName(sGameRoomName);
        try {
            blockingService.createGameToCredit(controller, request.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

	public void finishToCredit(long uuid,long lGameRoomId,long lClubId,long lRemainStacks,String sGameRoomName,long lHasBuyinStacks){
	    FinishToCreditRequest.Builder request = FinishToCreditRequest.newBuilder();
        request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId).setLRemainStacks(lRemainStacks)
               .setSGameRoomName(sGameRoomName).setLHasBuyinStacks(lHasBuyinStacks);
        try {
            blockingService.finishToCredit(controller, request.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
	
	public void standUpToCredit(long uuid,long lGameRoomId,long lClubId,long lRemainStacks,String sGameRoomName,long lHasBuyinStacks){
	    StandUpToCreditRequest.Builder request = StandUpToCreditRequest.newBuilder();
        request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId).setLRemainStacks(lRemainStacks)
               .setSGameRoomName(sGameRoomName).setLHasBuyinStacks(lHasBuyinStacks);
        try {
            blockingService.standUpToCredit(controller, request.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
	
	public int sitDownToCredit(long uuid,long lGameRoomId,long lClubId,long lHasBuyinStacks,String sGameRoomName,long lRemainStacks){
        int result = 3;
        SitDownToCreditRequest.Builder request = SitDownToCreditRequest.newBuilder();
        request.setUuid(uuid).setLGameRoomId(lGameRoomId).setLClubId(lClubId)
               .setLHasBuyinStacks(lHasBuyinStacks).setSGameRoomName(sGameRoomName).setLRemainStacks(lRemainStacks);
        SitDownToCreditResponse response = null;
        try {
            response = blockingService.sitDownToCredit(controller, request.build());
            if(response != null){
                result = response.getIState();
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return result;
    }
	
}

