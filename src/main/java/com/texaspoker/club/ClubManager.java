package com.texaspoker.club;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerClubService;
import com.texaspoker.moment.TexasPokerClubService.AddClubFundRequest;
import com.texaspoker.moment.TexasPokerClubService.AddClubFundResponse;
import com.texaspoker.moment.TexasPokerClubService.AddClubMaxRequest;
import com.texaspoker.moment.TexasPokerClubService.AddClubMaxResponse;
import com.texaspoker.moment.TexasPokerClubService.AddClubMemberRequest;
import com.texaspoker.moment.TexasPokerClubService.AddClubMemberResponse;
import com.texaspoker.moment.TexasPokerClubService.AddClubScoreRequest;
import com.texaspoker.moment.TexasPokerClubService.AddClubScoreResponse;
import com.texaspoker.moment.TexasPokerClubService.ClubGameRequest;
import com.texaspoker.moment.TexasPokerClubService.ClubGamesNet;
import com.texaspoker.moment.TexasPokerClubService.ClubInfoClubID;
import com.texaspoker.moment.TexasPokerClubService.DeleteClubCoverRequest;
import com.texaspoker.moment.TexasPokerClubService.DeleteClubCoverResponse;
import com.texaspoker.moment.TexasPokerClubService.DeleteClubMemberRequest;
import com.texaspoker.moment.TexasPokerClubService.DeleteClubMemberResponse;
import com.texaspoker.moment.TexasPokerClubService.GetClubBaseInfoRequest;
import com.texaspoker.moment.TexasPokerClubService.GetClubBaseInfoResponse;
import com.texaspoker.moment.TexasPokerClubService.GetClubInfoRequest;
import com.texaspoker.moment.TexasPokerClubService.GetClubInfoResponse;
import com.texaspoker.moment.TexasPokerClubService.GetClubManagerUuidsRequest;
import com.texaspoker.moment.TexasPokerClubService.GetClubManagerUuidsResponse;
import com.texaspoker.moment.TexasPokerClubService.GetClubSettingRequest;
import com.texaspoker.moment.TexasPokerClubService.GetClubSettingResponse;
import com.texaspoker.moment.TexasPokerClubService.SetClubBaseInfoRequest;
import com.texaspoker.moment.TexasPokerClubService.SetClubBaseInfoResponse;
import com.texaspoker.moment.TexasPokerClubService.SetClubSettingRequest;
import com.texaspoker.moment.TexasPokerClubService.SetClubSettingResponse;
import com.texaspoker.moment.TexasPokerClubService.SetClubUserLevelRequest;
import com.texaspoker.moment.TexasPokerClubService.SetClubUserLevelResponse;
import com.texaspoker.moment.TexasPokerClubService.UploadClubCoverRequest;
import com.texaspoker.moment.TexasPokerClubService.UploadClubCoverResponse;
import com.texaspoker.moment.TexasPokerClubService.UploadClubIconRequest;
import com.texaspoker.moment.TexasPokerClubService.UploadClubIconResponse;
import com.texaspoker.moment.TexasPokerCommon.ClubBaseInfo;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerHttpClub.CLUB_USER_LEVEL;
import com.texaspoker.moment.TexasPokerHttpClub.ClubInfo;
import com.texaspoker.moment.TexasPokerHttpClub.ClubUserInfo;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class ClubManager {	
	private static ClubManager instance; 
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    private TexasPokerClubService.ClubService.BlockingInterface blockingClubService ;
	
	
	private ClubManager (){		
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
	public static  ClubManager getInstance() {  
		if (instance == null) {  
			synchronized(ClubManager.class){
				instance = new ClubManager();
			}
		}  
		return instance;  
	}
	
	//获取俱乐部信息
	public ClubInfo.Builder getClubInfo(long lClubID){
		GetClubInfoRequest.Builder stGetClubInfoRequestBuilder = GetClubInfoRequest.newBuilder();
		stGetClubInfoRequestBuilder.setLClubID(lClubID);
	    
	    try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
			GetClubInfoResponse response = blockingClubService.getClubInfo(controller, stGetClubInfoRequestBuilder.build());
	    	
	    	if (response != null && response.getStClubInfo()!=null && response.getStClubInfo().getStClubBaseInfo().getLClubID() > 0) {
	    		return response.getStClubInfo().toBuilder();
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	//获取俱乐部信息
	public void setClubInfo(ClubInfo.Builder stClubInfoBuilder){
	    try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
	    	blockingClubService.setClubInfo(controller, stClubInfoBuilder.build());
	    	
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
	}
	
	//删除俱乐部信息
	public void removeClubInfo(long lClubID){
		ClubInfoClubID.Builder stClubInfoClubIDBuilder = ClubInfoClubID.newBuilder();
		stClubInfoClubIDBuilder.setLClubID(lClubID);
	    
	    try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
			blockingClubService.removeClubInfo(controller, stClubInfoClubIDBuilder.build());
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
	}
	
	//往俱乐部中添加牌局
	public void addGameRooms(long clubID,long gameroomid){
		ClubGameRequest.Builder stClubGameRequestBuilder = ClubGameRequest.newBuilder();
		stClubGameRequestBuilder.setLClubID(clubID);
		stClubGameRequestBuilder.setLGameRoomID(gameroomid);
	    
	    try {
//			System.out.println("addGameRoomsaddGameRoomsaddGameRooms     clubID." + clubID);
//			System.out.println("addGameRoomsaddGameRoomsaddGameRooms gameroomid." + gameroomid);
			blockingClubService.addClubGameRooms(controller, stClubGameRequestBuilder.build());
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
	}
	
	//获取俱乐部中的牌局
	public ArrayList<Long> getGameRooms(long clubID){
		ClubInfoClubID.Builder stClubInfoClubIDBuilder = ClubInfoClubID.newBuilder();
		stClubInfoClubIDBuilder.setLClubID(clubID);
		
	    try {
//			System.out.println("getGameRoomsgetGameRoomsgetGameRoomsgetGameRooms begin.");
			ClubGamesNet stClubGamesNet = blockingClubService.getClubGameRooms(controller, stClubInfoClubIDBuilder.build());
			
			if(stClubGamesNet!=null && stClubGamesNet.getVGameRoomIDsList()!=null){
				return new ArrayList<Long>(stClubGamesNet.getVGameRoomIDsList());
			}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
		
		return null;
	}
	
	//删除俱乐部中的牌局
	public void removeChatRoomGameRoom(long clubID,long gameroomid){
		ClubGameRequest.Builder stClubGameRequestBuilder = ClubGameRequest.newBuilder();
		stClubGameRequestBuilder.setLClubID(clubID);
		stClubGameRequestBuilder.setLGameRoomID(gameroomid);
	    
	    try {
			blockingClubService.removeClubGameRooms(controller, stClubGameRequestBuilder.build());
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
	}
	
	//检查俱乐部是否已经满员
	public static boolean checkFull(ClubInfo stClubInfo){
		boolean bFull = false;
		if(stClubInfo!=null){			
			if(stClubInfo.getVClubUserInfosCount()>=stClubInfo.getStClubBaseInfo().getIMaxMembers()){
				bFull = true;
			}
		}
		return bFull;
		
	}
	
	//获取俱乐部信息
	public ClubBaseInfo.Builder getClubBaseInfo(long lClubID){
		GetClubBaseInfoRequest.Builder stGetClubBaseInfoRequestBuilder = GetClubBaseInfoRequest.newBuilder();
		stGetClubBaseInfoRequestBuilder.setLClubID(lClubID);
	    
	    try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
	    	GetClubBaseInfoResponse response = blockingClubService.getClubBaseInfo(controller, stGetClubBaseInfoRequestBuilder.build());
	    	
	    	if (response != null && response.getStClubBaseInfo()!=null && response.getStClubBaseInfo().getLClubID() > 0) {
	    		return response.getStClubBaseInfo().toBuilder();
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}

	//该用户是否在俱乐部中
	public boolean inClub(long clubID, long uuid) {
		ClubInfo.Builder stClubInfoBuilder = getClubInfo(clubID);

		boolean bExist = false;
		for (int i = 0; i < stClubInfoBuilder.getVClubUserInfosCount(); i++) {
			ClubUserInfo.Builder stTempClubUserInfoBuilder = stClubInfoBuilder
					.getVClubUserInfosBuilder(i);
			if (stTempClubUserInfoBuilder.getStUserBaseInfo().getUuid() == uuid) {
				bExist = true;
				break;
			}
		}

		return bExist;
	}
	
	//添加俱乐部基金
	public ClubInfo.Builder addClubfund(long lClubid,long lFund){
		AddClubFundRequest.Builder stAddClubFundRequestBuilder = AddClubFundRequest.newBuilder();
		stAddClubFundRequestBuilder.setLClubID(lClubid);
	    stAddClubFundRequestBuilder.setLFund(lFund);
	    
	    try {
			AddClubFundResponse response = blockingClubService.addClubFund(controller, stAddClubFundRequestBuilder.build());
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	//添加俱乐部积分
	public ClubInfo.Builder addClubScore(long lClubid,long lScore){
		AddClubScoreRequest.Builder stAddClubScoreRequestBuilder = AddClubScoreRequest.newBuilder();
		stAddClubScoreRequestBuilder.setLClubID(lClubid);
		stAddClubScoreRequestBuilder.setLScore(lScore);
	    
	    try {
			AddClubScoreResponse response = blockingClubService.addClubScore(controller, stAddClubScoreRequestBuilder.build());

	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder setClubBaseInfo(long clubid , String sClubName , String sClubDesc){
		SetClubBaseInfoRequest.Builder stSetClubBaseInfoRequestBuilder = SetClubBaseInfoRequest.newBuilder();
		stSetClubBaseInfoRequestBuilder.setLClubID(clubid);
		stSetClubBaseInfoRequestBuilder.setSClubName(sClubName);
		stSetClubBaseInfoRequestBuilder.setSDesc(sClubDesc);
	    
	    try {
			SetClubBaseInfoResponse response = blockingClubService.setClubBaseInfo(controller, stSetClubBaseInfoRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder setClubUserLevel(long clubid , long uuid , CLUB_USER_LEVEL eClubUserLevel){
		SetClubUserLevelRequest.Builder stSetClubUserLevelRequestBuilder = SetClubUserLevelRequest.newBuilder();
		stSetClubUserLevelRequestBuilder.setLClubID(clubid);
		stSetClubUserLevelRequestBuilder.setUuid(uuid);
		stSetClubUserLevelRequestBuilder.setEClubUserLevel(eClubUserLevel);
	    
	    try {
			SetClubUserLevelResponse response = blockingClubService.setClubUserLevel(controller, stSetClubUserLevelRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder uploadClubIcon(long clubid,String sBigIcon,String sSmallIcon){
		UploadClubIconRequest.Builder stUploadClubIconRequestBuilder = UploadClubIconRequest.newBuilder();
		stUploadClubIconRequestBuilder.setLClubID(clubid);
		stUploadClubIconRequestBuilder.setSSmallClubIcon(sSmallIcon);
		stUploadClubIconRequestBuilder.setSBigClubIcon(sBigIcon);
	    
	    try {
	    	UploadClubIconResponse response = blockingClubService.uploadClubIcon(controller, stUploadClubIconRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder uploadClubCover(long clubid,String sBigCover,String sSmallCover,int iPos){
		UploadClubCoverRequest.Builder stUploadClubCoverRequestBuilder = UploadClubCoverRequest.newBuilder();
		stUploadClubCoverRequestBuilder.setLClubID(clubid);
		stUploadClubCoverRequestBuilder.setSBigAlbums(sBigCover);
		stUploadClubCoverRequestBuilder.setSSmallAlbums(sSmallCover);
		stUploadClubCoverRequestBuilder.setIPos(iPos);
	    try {
	    	UploadClubCoverResponse response = blockingClubService.uploadClubCover(controller, stUploadClubCoverRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder deleteClubCover(long clubid,int iPos){
		DeleteClubCoverRequest.Builder stDeleteClubCoverRequestBuilder = DeleteClubCoverRequest.newBuilder();
		stDeleteClubCoverRequestBuilder.setLClubID(clubid);
		stDeleteClubCoverRequestBuilder.setIPos(iPos);
	    
	    try {
	    	DeleteClubCoverResponse response = blockingClubService.deleteClubCover(controller, stDeleteClubCoverRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubSettingInfo setClubSetting(long uuid,long clubID,int mute,int vibrate){
		SetClubSettingRequest.Builder stSetClubSettingRequestBuilder = SetClubSettingRequest.newBuilder();
		stSetClubSettingRequestBuilder.setLClubID(clubID);
		stSetClubSettingRequestBuilder.setUuid(uuid);
		stSetClubSettingRequestBuilder.setIMute(mute);
		stSetClubSettingRequestBuilder.setIVibrate(vibrate);
	    
	    try {
	    	SetClubSettingResponse response = blockingClubService.setClubSetting(controller, stSetClubSettingRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		ClubSettingInfo stClubSettingInfo = new ClubSettingInfo();
	    		stClubSettingInfo.iMute = response.getIMute();
	    		stClubSettingInfo.iVibrate = response.getIVibrate();
	    		return stClubSettingInfo;
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubSettingInfo getClubSetting(long uuid,long clubID){
		GetClubSettingRequest.Builder  stGetClubSettingBuilder = GetClubSettingRequest.newBuilder();
		stGetClubSettingBuilder.setLClubID(clubID);
		stGetClubSettingBuilder.setUuid(uuid);
	    
	    try {
	    	GetClubSettingResponse response = blockingClubService.getClubSetting(controller, stGetClubSettingBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		ClubSettingInfo stClubSettingInfo = new ClubSettingInfo();
	    		stClubSettingInfo.iMute = response.getIMute();
	    		stClubSettingInfo.iVibrate = response.getIVibrate();
	    		return stClubSettingInfo;
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public ClubInfo.Builder addClubMember(long clubid,long uuid,CLUB_USER_LEVEL eClubUserLevel){
		AddClubMemberRequest.Builder stAddClubMemberRequestBuilder = AddClubMemberRequest.newBuilder();
		stAddClubMemberRequestBuilder.setLClubID(clubid);
		stAddClubMemberRequestBuilder.setUuid(uuid);
		stAddClubMemberRequestBuilder.setEClubUserLevel(eClubUserLevel);
		
	    try {
	    	AddClubMemberResponse response = blockingClubService.addClubMember(controller, stAddClubMemberRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	} 
	
	public ClubInfo.Builder deleteClubMember(long clubid,long uuid){
		DeleteClubMemberRequest.Builder stDeleteClubMemberRequestBuilder = DeleteClubMemberRequest.newBuilder();
		stDeleteClubMemberRequestBuilder.setLClubID(clubid);
		stDeleteClubMemberRequestBuilder.setUuid(uuid);
	    
	    try {
	    	DeleteClubMemberResponse response = blockingClubService.deleteClubMember(controller, stDeleteClubMemberRequestBuilder.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	} 

	public ClubInfo.Builder addClubMax(long clubid,int iNum){
		AddClubMaxRequest.Builder stAddClubMaxRequest = AddClubMaxRequest.newBuilder();
		stAddClubMaxRequest.setLClubID(clubid);
		stAddClubMaxRequest.setINum(iNum);
	    
	    try {
	    	AddClubMaxResponse response = blockingClubService.addClubMax(controller, stAddClubMaxRequest.build());
	    	
	    	if (response != null&&response.getIErrorCode() == 0) {
	    		if(response.getStClubInfo()!=null){
	    			return response.getStClubInfo().toBuilder();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	} 
	
	public List<Long> getClubManagerUuids(long clubid){
		GetClubManagerUuidsRequest.Builder stGetClubManagerUuidsRequestBuilder = GetClubManagerUuidsRequest.newBuilder();
		stGetClubManagerUuidsRequestBuilder.setLClubID(clubid);
		stGetClubManagerUuidsRequestBuilder.setUuid(0);
	    
	    try {
	    	GetClubManagerUuidsResponse response = blockingClubService.getClubManagerUuids(controller, stGetClubManagerUuidsRequestBuilder.build());
	    	
	    	if (response != null&&response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
	    		if(response.getVClubManagerUuidsList()!=null){
	    			return response.getVClubManagerUuidsList();
	    		}else{
	    			return null;
	    		}
	    	} else {
	    		return null;
	    	}
	    } catch (ServiceException e) {
	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	public boolean checkClubManager(long lClubID,long uuid){
		List<Long> vClubManagerUuids = ClubManager.getInstance().getClubManagerUuids(lClubID);
		if(vClubManagerUuids!=null){
			//检查房主是否是房间的管理员
			for(long lClubManagerUuid:vClubManagerUuids){
				//检查房主是否是俱乐部的管理员
				if(lClubManagerUuid==uuid){
					return true;
				}		
			}	
		}
		
		return false;
	}
}
