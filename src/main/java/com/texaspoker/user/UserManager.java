package com.texaspoker.user;

import java.util.ArrayList;
import java.util.Timer;
import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPokerCommon.UserTokenInfoNet;
import com.texaspoker.moment.TexasPokerCommon.VIP_TYPE;
import com.texaspoker.moment.TexasPokerUserService;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerUserService.AddNewUserBaseInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.AddNewUserBaseInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.AddUserChatRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.AddUserCreateGameRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.ClearAPNTokenInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.ClearAPNTokenInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.DeleteUserCoverRequest;
import com.texaspoker.moment.TexasPokerUserService.DeleteUserCoverResponse;
import com.texaspoker.moment.TexasPokerUserService.GetUserChatRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.GetUserChatRoomsResponse;
import com.texaspoker.moment.TexasPokerUserService.GetUserCreateGameRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.GetUserCreateGameRoomsResponse;
import com.texaspoker.moment.TexasPokerUserService.GetUserQuickRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.GetUserQuickRoomsResponse;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterDataReq;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterDataRsp;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterPWDReq;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterPWDRsp;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterVerifyCodeReq;
import com.texaspoker.moment.TexasPokerUserService.GetUserRegisterVerifyCodeRsp;
import com.texaspoker.moment.TexasPokerUserService.GetUserStandByRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.GetUserStandByRoomsResponse;
import com.texaspoker.moment.TexasPokerUserService.GetUserTokenInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.GetUserTokenInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.RemoveUserChatRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.RemoveUserCreateGameRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.RemoveUserInTableRequest;
import com.texaspoker.moment.TexasPokerUserService.RemoveUserQuickRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.RemoveUserStandByRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserAPNTokenInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserAPNTokenInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoCoverRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoCoverResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoLoginTimeRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoLoginTimeResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoVIPRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserBaseInfoVIPResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserInTableRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserInfoSettingRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserInfoSettingResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserQuickRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserRegisterDataReq;
import com.texaspoker.moment.TexasPokerUserService.SetUserRegisterPWDReq;
import com.texaspoker.moment.TexasPokerUserService.SetUserRegisterVerifyCodeReq;
import com.texaspoker.moment.TexasPokerUserService.SetUserSSOTokenInfoRequest;
import com.texaspoker.moment.TexasPokerUserService.SetUserSSOTokenInfoResponse;
import com.texaspoker.moment.TexasPokerUserService.SetUserStandByRoomsRequest;
import com.texaspoker.moment.TexasPokerUserService.UploadUserCoverRequest;
import com.texaspoker.moment.TexasPokerUserService.UploadUserCoverResponse;
import com.texaspoker.moment.TexasPokerUserService.UserAddCoinRequest;
import com.texaspoker.moment.TexasPokerUserService.UserAddCoinResponse;
import com.texaspoker.moment.TexasPokerUserService.UserAddPopularityRequest;
import com.texaspoker.moment.TexasPokerUserService.UserAddPopularityResponse;
import com.texaspoker.moment.TexasPokerUserService.UserBaseInfoUuid;
import com.texaspoker.moment.TexasPokerUserService.UserClubRequest;
import com.texaspoker.moment.TexasPokerUserService.UserClubsNet;
import com.texaspoker.moment.room.UserChatRooms;
import com.texaspoker.moment.room.UserGameRooms;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

public class UserManager {
	
	//RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerUserService.UserService.BlockingInterface blockingUserService ;
    
	//设置timer方法
	Timer timer = new Timer(true);   
	
	private static UserManager instance;

	//私有的构造函数
	private UserManager (){		
	    host = ConfigaturationManager.sUserServiceHost;
	    port = ConfigaturationManager.iUserServicePost;
	    System.out.println("UserManager hosthost : " + host);
	    System.out.println("UserManager portport : " + port);
	    
	    client = new RpcClient();
	    channel = new RpcChannel(client);
	    try {
	    	channel.connect(host, port);
	    } catch (Exception e) {
//	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
	    	return;
	    }
	    
	    blockingUserService = TexasPokerUserService.UserService.newBlockingStub(channel);
	    System.out.println("client connect " + host + ":" + port + " ok");
//	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
	}
	
	//单例模式
	public static  UserManager getInstance() {  
		if (instance == null) {  
			synchronized(UserManager.class){
				instance = new UserManager();  
			}
		}  
		return instance;  
	}  
	
	public void setRegisterUserVerifyCode(String userid,String verifyCode){
		SetUserRegisterVerifyCodeReq.Builder stSetUserRegisterVerifyCodeReqBuilder = SetUserRegisterVerifyCodeReq.newBuilder();
		stSetUserRegisterVerifyCodeReqBuilder.setStrID(userid);
		stSetUserRegisterVerifyCodeReqBuilder.setSVerifyCode(verifyCode);
		
		try {
			blockingUserService.setUserRegisterVerifyCode(controller, stSetUserRegisterVerifyCodeReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getRegisterUserVerifyCode(String userid){
		GetUserRegisterVerifyCodeReq.Builder stGetUserRegisterVerifyCodeReqBuilder = GetUserRegisterVerifyCodeReq.newBuilder();
		stGetUserRegisterVerifyCodeReqBuilder.setStrID(userid);
		GetUserRegisterVerifyCodeRsp stGetUserRegisterVerifyCodeRsp = null;
		try {
			stGetUserRegisterVerifyCodeRsp = blockingUserService.getUserRegisterVerifyCode(controller, stGetUserRegisterVerifyCodeReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stGetUserRegisterVerifyCodeRsp!=null){
			return stGetUserRegisterVerifyCodeRsp.getSVerifyCode();
		}else{
			return null;
		}
	}
	
	public void setRegisterUserPWD(String userid,String pwd){
		SetUserRegisterPWDReq.Builder stSetUserRegisterPWDReqBuilder = SetUserRegisterPWDReq.newBuilder();
		stSetUserRegisterPWDReqBuilder.setStrID(userid);
		stSetUserRegisterPWDReqBuilder.setStrPWD(pwd);
		
		try {
			blockingUserService.setUserRegisterPWD(controller, stSetUserRegisterPWDReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getRegisterUserPWD(String userid){
		GetUserRegisterPWDReq.Builder stGetUserRegisterPWDReqBuilder = GetUserRegisterPWDReq.newBuilder();
		stGetUserRegisterPWDReqBuilder.setStrID(userid);
		GetUserRegisterPWDRsp stGetUserRegisterPWDRsp = null;
		try {
			stGetUserRegisterPWDRsp = blockingUserService.getUserRegisterPWD(controller, stGetUserRegisterPWDReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stGetUserRegisterPWDRsp!=null){
			return stGetUserRegisterPWDRsp.getStrPWD();
		}else{
			return null;
		}
	}
	
	public long getNewUuid(){
		UserBaseInfoUuid.Builder stUserBaseInfoUuidReqBuilder = UserBaseInfoUuid.newBuilder();
		stUserBaseInfoUuidReqBuilder.setUuid(0);
		UserBaseInfoUuid stUserBaseInfoUuidRsp  = null;
		try {
			stUserBaseInfoUuidRsp = blockingUserService.getNewUuid(controller, stUserBaseInfoUuidReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stUserBaseInfoUuidRsp!=null){
//			NormalLogService.getInstance().error("getNewUuid : " + stUserBaseInfoUuidRsp.getUuid());
			return stUserBaseInfoUuidRsp.getUuid();
		}else{
//			NormalLogService.getInstance().error("getNewUuid : " + 10000);
			return 10000;
		}
	}
	
	public UserBaseInfoNet getRegisterUserData(String userid){
		GetUserRegisterDataReq.Builder stGetUserRegisterDataReqBuilder = GetUserRegisterDataReq.newBuilder();
		stGetUserRegisterDataReqBuilder.setStrID(userid);
		GetUserRegisterDataRsp stGetUserRegisterDataRsp = null;
		try {
			stGetUserRegisterDataRsp = blockingUserService.getUserRegisterData(controller, stGetUserRegisterDataReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(stGetUserRegisterDataRsp!=null){
			return stGetUserRegisterDataRsp.getStUserBaseInfoNet();
		}else{
			return null;
		}
	}
	
	public void setRegisterUserData(String userid,UserBaseInfoNet stUserBaseInfo){
		SetUserRegisterDataReq.Builder stSetUserRegisterDataReqBuilder = SetUserRegisterDataReq.newBuilder();
		stSetUserRegisterDataReqBuilder.setStrID(userid);
		stSetUserRegisterDataReqBuilder.setStUserBaseInfoNet(stUserBaseInfo);
		
		try {
			blockingUserService.setUserRegisterData(controller, stSetUserRegisterDataReqBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//用户和俱乐部的关联
	public boolean addUserClubs(long uuid , long clubid){
		UserClubRequest.Builder stUserClubRequestBuilder = UserClubRequest.newBuilder();
		stUserClubRequestBuilder.setLUuid(uuid);
		stUserClubRequestBuilder.setLClubs(clubid);
		
		try {
			blockingUserService.addUserClubs(controller, stUserClubRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean removeUserClubs(long uuid , long clubid){
		UserClubRequest.Builder stUserClubRequestBuilder = UserClubRequest.newBuilder();
		stUserClubRequestBuilder.setLUuid(uuid);
		stUserClubRequestBuilder.setLClubs(clubid);
		try {
			blockingUserService.removeUserClubs(controller, stUserClubRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public ArrayList<Long> getUserClubs(long uuid){
		UserBaseInfoUuid.Builder stUserBaseInfoUuidBuilder = UserBaseInfoUuid.newBuilder(); 
		stUserBaseInfoUuidBuilder.setUuid(uuid);
		
		try {
			UserClubsNet stUserClubsNet = blockingUserService.getUserClubs(controller, stUserBaseInfoUuidBuilder.build());
			if(stUserClubsNet!=null && stUserClubsNet.getVClubIDsList()!=null){
				return new ArrayList<Long>(stUserClubsNet.getVClubIDsList());
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//用户加入的牌局
	public boolean setUserInTable(long uuid , long roomid){
		SetUserInTableRequest.Builder stSetUserInTableRequestBuilder = SetUserInTableRequest.newBuilder();
		stSetUserInTableRequestBuilder.setUuid(uuid);
		stSetUserInTableRequestBuilder.setLGameRoomID(roomid);
		
		try {
			blockingUserService.setUserInTable(controller, stSetUserInTableRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean removeUserInTable(long uuid){
		RemoveUserInTableRequest.Builder stRemoveUserInTableRequestBuilder = RemoveUserInTableRequest.newBuilder();
		stRemoveUserInTableRequestBuilder.setUuid(uuid);
		
		try {
			blockingUserService.removeUserInTable(controller, stRemoveUserInTableRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
//	public Long getUserInTable(long uuid){
//		GetUserInTableRequest.Builder stGetUserInTableRequestBuilder = GetUserInTableRequest.newBuilder();
//		stGetUserInTableRequestBuilder.setUuid(uuid);
//		
//		try {
//			GetUserInTableResponse stGetUserInTableResponse =  blockingUserService.getUserInTable(controller, stGetUserInTableRequestBuilder.build());
//			if(stGetUserInTableResponse!=null && stGetUserInTableResponse.getIErrorCode()==0){
//				return stGetUserInTableResponse.getLGameRoomID();
//			}
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return (long) 0;
//	}
	
	//用户和聊天室关系
	public boolean addUserChatRooms(long uuid , long roomid){
		AddUserChatRoomsRequest.Builder stAddUserChatRoomsRequestBuilder = AddUserChatRoomsRequest.newBuilder();
		stAddUserChatRoomsRequestBuilder.setUuid(uuid);
		stAddUserChatRoomsRequestBuilder.setLChatRoomID(roomid);
		try {
			blockingUserService.addUserChatRooms(controller, stAddUserChatRoomsRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean removeUserChatRooms(long uuid , long roomid){
		RemoveUserChatRoomsRequest.Builder stRemoveUserChatRoomsRequestBuilder = RemoveUserChatRoomsRequest.newBuilder();
		stRemoveUserChatRoomsRequestBuilder.setUuid(uuid);
		stRemoveUserChatRoomsRequestBuilder.setLChatRoomID(roomid);
		try {
			blockingUserService.removeUserChatRooms(controller, stRemoveUserChatRoomsRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public UserChatRooms getUserChatRooms(long uuid){
		UserChatRooms stUserChatRooms = new UserChatRooms();
		GetUserChatRoomsRequest.Builder stGetUserChatRoomsRequestBuilder = GetUserChatRoomsRequest.newBuilder();
		stGetUserChatRoomsRequestBuilder.setUuid(uuid);
		try {
			GetUserChatRoomsResponse stGetUserChatRoomsResponse =  blockingUserService.getUserChatRooms(controller, stGetUserChatRoomsRequestBuilder.build());
			if(stGetUserChatRoomsResponse!=null 
					&& stGetUserChatRoomsResponse.getIErrorCode()==0
					&& stGetUserChatRoomsResponse.getVChatRoomIDsCount() > 0){
				for(Long lChatRoomID:stGetUserChatRoomsResponse.getVChatRoomIDsList()){
					stUserChatRooms.mRoomMap.put(lChatRoomID, lChatRoomID);
				}
			}
			return stUserChatRooms;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	//用户和牌局关系
	public boolean addUserCreateGameRooms(long uuid , long roomid){
		AddUserCreateGameRoomsRequest.Builder stAddUserCreateGameRoomsRequestBuilder = AddUserCreateGameRoomsRequest.newBuilder();
		stAddUserCreateGameRoomsRequestBuilder.setUuid(uuid);
		stAddUserCreateGameRoomsRequestBuilder.setLGameRoomID(roomid);
		try {
			blockingUserService.addUserCreateGameRooms(controller, stAddUserCreateGameRoomsRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean removeUserCreateGameRooms(long uuid , long roomid){
		RemoveUserCreateGameRoomsRequest.Builder stRemoveUserCreateGameRoomsRequestBuilder = RemoveUserCreateGameRoomsRequest.newBuilder();
		stRemoveUserCreateGameRoomsRequestBuilder.setUuid(uuid);
		stRemoveUserCreateGameRoomsRequestBuilder.setLGameRoomID(roomid);
		try {
			blockingUserService.removeUserCreateGameRooms(controller, stRemoveUserCreateGameRoomsRequestBuilder.build());
			return true;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	public UserGameRooms getUserCreateGameRooms(long uuid){
		UserGameRooms stUserGameRooms = new UserGameRooms();
		GetUserCreateGameRoomsRequest.Builder stGetUserCreateGameRoomsRequestBuilder = GetUserCreateGameRoomsRequest.newBuilder();
		stGetUserCreateGameRoomsRequestBuilder.setUuid(uuid);
		try {
			GetUserCreateGameRoomsResponse stGetUserCreateGameRoomsResponse =  blockingUserService.getUserCreateGameRooms(controller, stGetUserCreateGameRoomsRequestBuilder.build());
			if(stGetUserCreateGameRoomsResponse!=null 
					&& stGetUserCreateGameRoomsResponse.getIErrorCode()==0
					&& stGetUserCreateGameRoomsResponse.getVGameRoomIDsCount() > 0){
				for(Long lGameRoomID:stGetUserCreateGameRoomsResponse.getVGameRoomIDsList()){
					stUserGameRooms.mRoomMap.put(lGameRoomID, lGameRoomID);
				}
			}
			return stUserGameRooms;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/////////////////////////////////////////////////////////////
	//用户数据相关
	/////////////////////////////////////////////////////////////
	public UserTokenInfoNet getUserTokenInfo(long uuid){
		GetUserTokenInfoRequest.Builder stGetUserTokenInfoRequestBuilder = GetUserTokenInfoRequest.newBuilder();
		stGetUserTokenInfoRequestBuilder.setUuid(uuid);
		try {
			GetUserTokenInfoResponse stGetUserTokenInfoResponse =  blockingUserService.getUserTokenInfo(controller, stGetUserTokenInfoRequestBuilder.build());
			if(stGetUserTokenInfoResponse!=null 
					&& stGetUserTokenInfoResponse.getIErrorCode()==0){
				return stGetUserTokenInfoResponse.getStUserTokenInfoNet();
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean clearAllAPNToken(String token){
		ClearAPNTokenInfoRequest.Builder stClearAPNTokenInfoRequestBuilder = ClearAPNTokenInfoRequest.newBuilder();
		stClearAPNTokenInfoRequestBuilder.setStrAPNToken(token);
		
		try {
			ClearAPNTokenInfoResponse stClearAPNTokenInfoResponse =  blockingUserService.clearAPNTokenInfo(controller, stClearAPNTokenInfoRequestBuilder.build());
			if(stClearAPNTokenInfoResponse!=null 
					&& stClearAPNTokenInfoResponse.getIErrorCode()==0){
				return true;
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean setUserSSOToken(long uuid,String sStrSSOToken){
		SetUserSSOTokenInfoRequest.Builder stSetUserSSOTokenInfoRequestBuilder = SetUserSSOTokenInfoRequest.newBuilder();
		stSetUserSSOTokenInfoRequestBuilder.setUuid(uuid);
		stSetUserSSOTokenInfoRequestBuilder.setStrSSOToken(sStrSSOToken);
		
		try {
			SetUserSSOTokenInfoResponse stSetUserSSOTokenInfoResponse =  blockingUserService.setUserSSOTokenInfo(controller, stSetUserSSOTokenInfoRequestBuilder.build());
			if(stSetUserSSOTokenInfoResponse!=null 
					&& stSetUserSSOTokenInfoResponse.getIErrorCode()==0){
				return true;
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean setUserAPNToken(long uuid,String sStrAPNToken){
		SetUserAPNTokenInfoRequest.Builder stSetUserAPNTokenInfoRequestBuilder = SetUserAPNTokenInfoRequest.newBuilder();
		stSetUserAPNTokenInfoRequestBuilder.setUuid(uuid);
		stSetUserAPNTokenInfoRequestBuilder.setStrAPNToken(sStrAPNToken);
		
		try {
			SetUserAPNTokenInfoResponse ststSetUserAPNTokenInfoRequestBuilder =  blockingUserService.setUserAPNTokenInfo(controller, stSetUserAPNTokenInfoRequestBuilder.build());
			if(ststSetUserAPNTokenInfoRequestBuilder!=null 
					&& ststSetUserAPNTokenInfoRequestBuilder.getIErrorCode()==0){
				return true;
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public UserBaseInfoNet.Builder addUserCoin(long uuid,long lCoin){
		try {
			UserAddCoinRequest.Builder stUserAddCoinRequestBuilder = UserAddCoinRequest.newBuilder();
			stUserAddCoinRequestBuilder.setUuid(uuid);
			stUserAddCoinRequestBuilder.setLCoin(lCoin);
			UserAddCoinResponse stUserAddCoinResponse = blockingUserService.addUserCoin(controller, stUserAddCoinRequestBuilder.build());
			if(stUserAddCoinResponse!=null && stUserAddCoinResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stUserAddCoinResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			}
		} catch (ServiceException e) {
	    }
		return null;
	}
	
	public UserBaseInfoNet.Builder addUserPopularity(long uuid,long lPopularity){
		try {
			UserAddPopularityRequest.Builder stUserAddPopularityRequest = UserAddPopularityRequest.newBuilder();
			stUserAddPopularityRequest.setUuid(uuid);
			stUserAddPopularityRequest.setLPopularity(lPopularity);
	    	UserAddPopularityResponse stUserAddPopularityResponse = blockingUserService.addUserPopularity(controller, stUserAddPopularityRequest.build());
	    	if(stUserAddPopularityResponse!=null && stUserAddPopularityResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stUserAddPopularityResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			}
		} catch (ServiceException e) {
//	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	    }
		return null;
	}
	
	public UserBaseInfoNet.Builder getUserBaseInfo(Long uuid){
	    
	    UserBaseInfoUuid.Builder stUserBaseInfoUuidBuilder = UserBaseInfoUuid.newBuilder();
	    stUserBaseInfoUuidBuilder.setUuid(uuid);
	    
	    try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin. : " + stUserBaseInfoUuidBuilder.build());
	    	UserBaseInfoNet response = blockingUserService.getUserBaseInfo(controller, stUserBaseInfoUuidBuilder.build());
	    	
	    	if (response != null) {
//	    		System.out.println("getUserBaseInfogetUserBaseInforpc end. : " + response.getLPopularity());
	    		return response.toBuilder();
	    	} else {
//	    		NormalLogService.getInstance().error("syn echo response null" + controller.errorText());
	    		return null;
	    	}
	    } catch (ServiceException e) {
//	    	NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
	      return null;
	    }
	}
	
	//设置用户vip数据
	public UserBaseInfoNet.Builder setUserVIP(long uuid,VIP_TYPE eVIPType,long lVIPLimitTime){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserBaseInfoVIPRequest.Builder stSetUserBaseInfoVIPRequestBuilder = SetUserBaseInfoVIPRequest.newBuilder();
		stSetUserBaseInfoVIPRequestBuilder.setUuid(uuid);
		stSetUserBaseInfoVIPRequestBuilder.setLVIPLimitTime(lVIPLimitTime);
		stSetUserBaseInfoVIPRequestBuilder.setEVIPType(eVIPType);
		
		try {
			SetUserBaseInfoVIPResponse stSetUserBaseInfoVIPResponse = blockingUserService.setUserBaseInfoVIP(controller, stSetUserBaseInfoVIPRequestBuilder.build());
			if(stSetUserBaseInfoVIPResponse!=null && stSetUserBaseInfoVIPResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stSetUserBaseInfoVIPResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置用户的登陆时间
	public UserBaseInfoNet.Builder setUserLoginTime(long uuid,long lLoginTime){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserBaseInfoLoginTimeRequest.Builder stSetUserBaseInfoLoginTimeRequestBuilder = SetUserBaseInfoLoginTimeRequest.newBuilder();
		stSetUserBaseInfoLoginTimeRequestBuilder.setUuid(uuid);
		stSetUserBaseInfoLoginTimeRequestBuilder.setLLoginTime(lLoginTime);
		
		try {
			SetUserBaseInfoLoginTimeResponse stSetUserBaseInfoLoginTimeResponse = blockingUserService.setUserBaseInfoLoginTime(controller, stSetUserBaseInfoLoginTimeRequestBuilder.build());
			if(stSetUserBaseInfoLoginTimeResponse!=null && stSetUserBaseInfoLoginTimeResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stSetUserBaseInfoLoginTimeResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置用户头像
	public UserBaseInfoNet.Builder setUserCover(long uuid,String strCover,String strSmallCover){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserBaseInfoCoverRequest.Builder stSetUserBaseInfoCoverRequestBuilder = SetUserBaseInfoCoverRequest.newBuilder();
		stSetUserBaseInfoCoverRequestBuilder.setUuid(uuid);
		stSetUserBaseInfoCoverRequestBuilder.setStrCover(strCover);
		stSetUserBaseInfoCoverRequestBuilder.setStrSmallCover(strSmallCover);
		try {
			SetUserBaseInfoCoverResponse stSetUserBaseInfoCoverResponse = blockingUserService.setUserBaseInfoCover(controller, stSetUserBaseInfoCoverRequestBuilder.build());
			if(stSetUserBaseInfoCoverResponse!=null && stSetUserBaseInfoCoverResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stSetUserBaseInfoCoverResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置用户信息
	public UserBaseInfoNet.Builder setUserInfoSetting(UserBaseInfoNet userBaseInfoNet){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserInfoRequest.Builder stSetUserInfoRequestBuilder = SetUserInfoRequest.newBuilder();
		stSetUserInfoRequestBuilder.setUuid(userBaseInfoNet.getUuid());
		stSetUserInfoRequestBuilder.setIGender(userBaseInfoNet.getIGender());
		stSetUserInfoRequestBuilder.setIMute(userBaseInfoNet.getIMute());
		stSetUserInfoRequestBuilder.setIVibrate(userBaseInfoNet.getIMute());
		stSetUserInfoRequestBuilder.setStrDesc(userBaseInfoNet.getStrDesc());
		stSetUserInfoRequestBuilder.setStrNick(userBaseInfoNet.getStrNick());
		stSetUserInfoRequestBuilder.setStrPhoneNumber(userBaseInfoNet.getStrPhoneNumber());
		
		try {
			SetUserInfoResponse stSetUserInfoResponse = blockingUserService.setUserBaseInfoSetting(controller, stSetUserInfoRequestBuilder.build());
			if(stSetUserInfoResponse!=null && stSetUserInfoResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stSetUserInfoResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置用户静音信息
	public UserBaseInfoNet.Builder setUserInfoLocalSetting(UserBaseInfoNet userBaseInfoNet){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserInfoSettingRequest.Builder stSetUserInfoSettingRequestBuilder = SetUserInfoSettingRequest.newBuilder();
		stSetUserInfoSettingRequestBuilder.setUuid(userBaseInfoNet.getUuid());
		stSetUserInfoSettingRequestBuilder.setIMute(userBaseInfoNet.getIMute());
		stSetUserInfoSettingRequestBuilder.setIVibrate(userBaseInfoNet.getIMute());
		
		try {
			SetUserInfoSettingResponse stSetUserInfoSettingResponse = blockingUserService.setUserBaseInfoLocalSetting(controller, stSetUserInfoSettingRequestBuilder.build());
			if(stSetUserInfoSettingResponse!=null && stSetUserInfoSettingResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stSetUserInfoSettingResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置新用户
	public UserBaseInfoNet.Builder addNewUserBaseInfo(UserBaseInfoNet userBaseInfoNet){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		AddNewUserBaseInfoRequest.Builder stAddNewUserBaseInfoRequestBuilder = AddNewUserBaseInfoRequest.newBuilder();
		stAddNewUserBaseInfoRequestBuilder.setStUserBaseInfosNet(userBaseInfoNet);
		
		try {
			AddNewUserBaseInfoResponse stAddNewUserBaseInfoResponse = blockingUserService.addNewUserBaseInfo(controller, stAddNewUserBaseInfoRequestBuilder.build());
			if(stAddNewUserBaseInfoResponse!=null && stAddNewUserBaseInfoResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stAddNewUserBaseInfoResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//上传用户相册
	public UserBaseInfoNet.Builder uploadUserCover(long uuid,int iPos,String strBigCover,String sSmallCover){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		UploadUserCoverRequest.Builder stUploadUserCoverRequestBuilder = UploadUserCoverRequest.newBuilder();
		stUploadUserCoverRequestBuilder.setUuid(uuid);
		stUploadUserCoverRequestBuilder.setIPos(iPos);
		stUploadUserCoverRequestBuilder.setSBigAlbums(strBigCover);
		stUploadUserCoverRequestBuilder.setSSmallAlbums(sSmallCover);
		
		try {
			UploadUserCoverResponse stUploadUserCoverResponse = blockingUserService.uploadUserCover(controller, stUploadUserCoverRequestBuilder.build());
			if(stUploadUserCoverResponse!=null && stUploadUserCoverResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stUploadUserCoverResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//删除用户相册
	public UserBaseInfoNet.Builder deleteUserCover(long uuid,int iPos){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		DeleteUserCoverRequest.Builder stDeleteUserCoverRequestBuilder = DeleteUserCoverRequest.newBuilder();
		stDeleteUserCoverRequestBuilder.setUuid(uuid);
		stDeleteUserCoverRequestBuilder.setIPos(iPos);
		
		try {
			DeleteUserCoverResponse stDeleteUserCoverResponse = blockingUserService.deleteUserCover(controller, stDeleteUserCoverRequestBuilder.build());
			if(stDeleteUserCoverResponse!=null && stDeleteUserCoverResponse.getIErrorCode() == 0){
				UserBaseInfoNet stUserBaseInfoNet = stDeleteUserCoverResponse.getStUserBaseInfoNet();
				if(stUserBaseInfoNet!=null){
					return stUserBaseInfoNet.toBuilder();
				}
			}else{
				return null;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//设置用户快速局
	public void setUserQuickGameRoom(long uuid,long lQuickGameRoom){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserQuickRoomsRequest.Builder stSetUserQuickRoomsRequestBuilder= SetUserQuickRoomsRequest.newBuilder();
		stSetUserQuickRoomsRequestBuilder.setUuid(uuid);
		stSetUserQuickRoomsRequestBuilder.setLQuickGameRooms(lQuickGameRoom);
		
		try {
			System.out.println("setUserQuickGameRoomsetUserQuickGameRoom 1 : " + uuid);
			System.out.println("setUserQuickGameRoomsetUserQuickGameRoom 2 : " + lQuickGameRoom);
			blockingUserService.setUserQuickRooms(controller, stSetUserQuickRoomsRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//删除用户快速局
	public void removeUserQuickGameRoom(long uuid){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		RemoveUserQuickRoomsRequest.Builder stRemoveUserQuickRoomsRequestBuilder= RemoveUserQuickRoomsRequest.newBuilder();
		stRemoveUserQuickRoomsRequestBuilder.setUuid(uuid);
		
		try {
			blockingUserService.removeUserQuickRooms(controller, stRemoveUserQuickRoomsRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//获取用户快速局
	public Long getUserQuickGameRoom(long uuid){
//		System.out.println("getUserQuickGameRoomgetUserQuickGameRoom 1 : " + uuid);
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		GetUserQuickRoomsRequest.Builder stGetUserQuickRoomsRequestBuilder = GetUserQuickRoomsRequest.newBuilder();
		stGetUserQuickRoomsRequestBuilder.setUuid(uuid);
		
		try {
//			System.out.println("getUserQuickGameRoomgetUserQuickGameRoom 2.");
			GetUserQuickRoomsResponse stGetUserQuickRoomsResponse = blockingUserService.getUserQuickRooms(controller, stGetUserQuickRoomsRequestBuilder.build());
//			System.out.println("getUserQuickGameRoomgetUserQuickGameRoom 3." + stGetUserQuickRoomsResponse);
			if(stGetUserQuickRoomsResponse!=null && stGetUserQuickRoomsResponse.getIErrorCode() == 0){
//				System.out.println("getUserQuickGameRoomgetUserQuickGameRoom 3." + stGetUserQuickRoomsResponse.getIErrorCode());
//				System.out.println("getUserQuickGameRoomgetUserQuickGameRoom 4." + stGetUserQuickRoomsResponse.getVQuickGameRooms());
				return stGetUserQuickRoomsResponse.getVQuickGameRooms();
			}else{
				return (long) 0;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (long) 0;
	}
	
	//设置用户旁观信息
	public void setUserStandbyRooms(long uuid,long lGameRoom){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		SetUserStandByRoomsRequest.Builder stSetUserStandByRoomsRequestBuilder= SetUserStandByRoomsRequest.newBuilder();
		stSetUserStandByRoomsRequestBuilder.setUuid(uuid);
		stSetUserStandByRoomsRequestBuilder.setLGameRoomID(lGameRoom);
		
		try {
//			System.out.println("setUserQuickGameRoomsetUserQuickGameRoom 1 : " + uuid);
//			System.out.println("setUserQuickGameRoomsetUserQuickGameRoom 2 : " + lQuickGameRoom);
			blockingUserService.setUserStandByRooms(controller, stSetUserStandByRoomsRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//删除用户快速局
	public void removeUserStandbyRooms(long uuid){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		RemoveUserStandByRoomsRequest.Builder stSetUserStandByRoomsRequestBuilder= RemoveUserStandByRoomsRequest.newBuilder();
		stSetUserStandByRoomsRequestBuilder.setUuid(uuid);
		
		try {
			blockingUserService.removeUserStandByRooms(controller, stSetUserStandByRoomsRequestBuilder.build());
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//获取用户快速局
	public Long getUserStandbyRooms(long uuid){
		TexasPokerUserService.UserService.BlockingInterface blockingUserService =   TexasPokerUserService.UserService.newBlockingStub(channel);
		
		GetUserStandByRoomsRequest.Builder stSetUserStandByRoomsRequestBuilder = GetUserStandByRoomsRequest.newBuilder();
		stSetUserStandByRoomsRequestBuilder.setUuid(uuid);
		
		try {
			GetUserStandByRoomsResponse stGetUserStandByRoomsRequest = blockingUserService.getUserStandByRooms(controller, stSetUserStandByRoomsRequestBuilder.build());
			if(stGetUserStandByRoomsRequest!=null && stGetUserStandByRoomsRequest.getIErrorCode() == 0){
				return stGetUserStandByRoomsRequest.getLGameRoomID();
			}else{
				return (long) 0;
			} 
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (long) 0;
	}
}
