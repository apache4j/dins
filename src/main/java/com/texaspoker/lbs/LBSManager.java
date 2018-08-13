package com.texaspoker.lbs;

import java.util.ArrayList;
import java.util.List;
import com.google.protobuf.ServiceException;
import com.texaspoker.moment.TexasPokerLocationService;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerLocationService.VerifyIPInfoToOthersReq;
import com.texaspoker.moment.TexasPokerLocationService.VerifyIPInfoToOthersRsp;
import com.texaspoker.moment.TexasPokerLocationService.VerifyLocationToOthersReq;
import com.texaspoker.moment.TexasPokerLocationService.VerifyLocationToOthersRsp;
import com.texaspoker.moment.TexasPokerLocationService.VerifyTablesLocationReq;
import com.texaspoker.moment.TexasPokerLocationService.VerifyTablesLocationRsp;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;
import com.texaspoker.configuration.ConfigaturationManager;

public class LBSManager {
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerLocationService.LocationService.BlockingInterface blockingLBSService ;
    	
	private static LBSManager instance;

	//私有的构造函数
	private LBSManager (){		
	    host = ConfigaturationManager.sLBSServiceHost;
	    port = ConfigaturationManager.iLBSServicePost;
	    System.out.println("LBSManager hosthost : " + host);
	    System.out.println("LBSManager portport : " + port);
	    
	    client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
	    	return;
	    }
	    
	    blockingLBSService = TexasPokerLocationService.LocationService.newBlockingStub(channel);
	    System.out.println("client connect " + host + ":" + port + " ok");
	}
	
	//单例模式
	public static  LBSManager getInstance() {  
		if (instance == null) {  
			synchronized(LBSManager.class){
				instance = new LBSManager();  
			}
		}  
		return instance;  
	}  
	
	public boolean verifyIPInfoToOthers(long uuid,ArrayList<Long> verifiedUuids,long lGameRomID){
		VerifyIPInfoToOthersReq.Builder stVerifyIPInfoToOthersReqBuilder = VerifyIPInfoToOthersReq.newBuilder();
		stVerifyIPInfoToOthersReqBuilder.setMainUuid(uuid);
		stVerifyIPInfoToOthersReqBuilder.addAllCompUuids(verifiedUuids);
		stVerifyIPInfoToOthersReqBuilder.setLRoomid(lGameRomID);
		
		try {
			VerifyIPInfoToOthersRsp stVerifyIPInfoToOthersRsp =  blockingLBSService.verifyIPInfoToOthers(controller, stVerifyIPInfoToOthersReqBuilder.build());
			if(stVerifyIPInfoToOthersRsp!=null){
				if(stVerifyIPInfoToOthersRsp.getEErrCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					return true;
				}else{
					return false;
				}
			}else{
				return true;
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean verifyLocationToOthers(long uuid,ArrayList<Long> verifiedUuids,long lGameRomID){
		VerifyLocationToOthersReq.Builder stVerifyLocationToOthersReqBuilder = VerifyLocationToOthersReq.newBuilder();
		stVerifyLocationToOthersReqBuilder.setMainUuid(uuid);
		stVerifyLocationToOthersReqBuilder.addAllCompUuids(verifiedUuids);
		stVerifyLocationToOthersReqBuilder.setLRoomid(lGameRomID);
		
		try {
			VerifyLocationToOthersRsp stVerifyLocationToOthersRsp = blockingLBSService.verifyLocationToOthers(controller, stVerifyLocationToOthersReqBuilder.build());
			if(stVerifyLocationToOthersRsp!=null){
				if(stVerifyLocationToOthersRsp.getEErrCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					return true;
				}else{
					return false;
				}
			}else{
				return true;
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	public List<Long> verifyTablesUuids(List<Long> tableUuids, int bIpLimit, int bGpsLimit,long roomid,int handid){
	    List<Long> standupList = new ArrayList<Long>();
	    try{
	        VerifyTablesLocationReq.Builder  request = VerifyTablesLocationReq.newBuilder();
	        request.addAllTableUuids(tableUuids)
	               .setLRoomid(roomid)
	               .setBIpLimit(bIpLimit)
	               .setBGpsLimit(bGpsLimit)
	               .setIHands(handid);
	        VerifyTablesLocationRsp response = blockingLBSService.verifyTablesLocation(controller, request.build());
	        if(response!= null && response.getEErrCode().equals(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN)){
	            standupList = response.getDoubtUuidsList();
	        }
	    }catch (Exception e) {
            e.printStackTrace();
        }
	    return standupList;
	}

}