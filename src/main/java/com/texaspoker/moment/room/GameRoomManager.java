package com.texaspoker.moment.room;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.ChatRoomInfo;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_SENIOR_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.USER_GAME_ROLE;
import com.texaspoker.moment.TexasPoker.USER_GAME_STATE;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ID_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerCommon.VIP_TYPE;
import com.texaspoker.moment.TexasPokerHttpClub.ClubInfo;
import com.texaspoker.moment.TexasPokerHttpClub.ClubUserInfo;
import com.texaspoker.moment.setting.GlobalData;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.user.UserManager;

public class GameRoomManager  {
	public TimeoutHashMap<Long,GameRoomDetailInfo> mGameRoomDetailInfos; //房间相关的数据
	private static GameRoomManager instance; 
	
	Log   mLogNormals   =   LogFactory.getLog("fornormal");
	
    public ArrayList<GameRoomDetailInfo> getTimeOutGameRoom(){
        return mGameRoomDetailInfos.timeout();
}
    
	private GameRoomManager (){
		mGameRoomDetailInfos = new TimeoutHashMap<Long,GameRoomDetailInfo>();
		mGameRoomDetailInfos.setTimeout(800);
	}
	
	//单例模式
	public static GameRoomManager getInstance() {  
		if (instance == null) { 
			synchronized(GameRoomManager.class){
				instance = new GameRoomManager();
				ConnectDatabase.getInstance().loadAllGameRooms(instance.mGameRoomDetailInfos);
				
				instance.init();
			}
		}  
		return instance;  
	}
	
	void init(){
		GameRoomService stGameRoomService = new GameRoomService();
        ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
        service1.scheduleAtFixedRate(stGameRoomService, 100, 1500, TimeUnit.MILLISECONDS);

        GameRoomServiceWait stGameRoomServiceWait = new GameRoomServiceWait();
        ScheduledExecutorService service2 = Executors.newSingleThreadScheduledExecutor();
        service2.scheduleAtFixedRate(stGameRoomServiceWait, 100, 1500, TimeUnit.MILLISECONDS);
	}
	
	public static String getErrorInfoFromException(Exception e) {  
        try {  
            StringWriter sw = new StringWriter();  
            PrintWriter pw = new PrintWriter(sw);  
            e.printStackTrace(pw);  
            return "\r\n" + sw.toString() + "\r\n";  
        } catch (Exception e2) {  
            return "bad getErrorInfoFromException";  
        }  
    }  
	
	public GameRoomDetailInfo getGameRoomDetailInfo(long lGameRoomID){
		return mGameRoomDetailInfos.get(lGameRoomID);
	}

	public void  setGameRoomDetailInfo(GameRoomDetailInfo stGameRoomDetailInfo){
		mGameRoomDetailInfos.setCurrent(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomDetailInfo);
	}
	
	public GameRoomIDResultInfo createGameRoom(long uuid,GameRoomBaseInfo stGameRoomBaseInfo){
		
		GameRoomIDResultInfo stGameRoomIDResultInfo = new GameRoomIDResultInfo();
		// 获取该用户已经开的牌局数量
		UserGameRooms stUserCreateGames = UserManager.getInstance().getUserCreateGameRooms(uuid);
		int userPlaysNumber = 0;
		if (stUserCreateGames != null && stUserCreateGames.mRoomMap!=null) {
			userPlaysNumber = stUserCreateGames.mRoomMap.size();
		}
		
		if(QuickGameRoomManager.getInstance().getUserCreateQuickGameRoom(uuid)!=null 
				&& QuickGameRoomManager.getInstance().getUserCreateQuickGameRoom(uuid)!=0){
			userPlaysNumber++;
		}
		
		// 获取创建牌局的用户的vip类型
		UserBaseInfoNet.Builder stUserBaseInfoNet = UserManager.getInstance().getUserBaseInfo(uuid);
	
		// 判断用户是否可以在创建房间
		if (userPlaysNumber >= GlobalData.getSimultaneousPlays(stUserBaseInfoNet.getEVipType())) {	
			// 生成回包
			stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_GAMEROOM_OVERFLOW);
			return stGameRoomIDResultInfo;
		} else {
			// 先检查是否在别的游戏中			
			// 生成银币扣除数据
				if(stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NO){
					synchronized(RoomIDManager.class){
						TexasPoker.GameRoomBaseInfo.Builder gameRoomBaseInfoBuilder = stGameRoomBaseInfo.toBuilder();
						gameRoomBaseInfoBuilder.setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()));
						gameRoomBaseInfoBuilder.setBBuyIn(1);
						gameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
						gameRoomBaseInfoBuilder.setIMaxBuyinRatio(40);
						gameRoomBaseInfoBuilder.setIMinBuyinRatio(10);
						gameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
					
						// 设置房主vip信息
						gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));
	
	
						TexasPoker.GameRoomInfo.Builder gameRoomInfo = TexasPoker.GameRoomInfo.newBuilder();
						gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
											
						GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();
											
						List<UserBaseInfoNet> userList = new ArrayList<UserBaseInfoNet>();
						
						if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CAHT_ROOM){
							ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
							if(stChatRoomInfo!=null){
								userList = stChatRoomInfo.getVUserBaseInfosList();
													
								// 将牌局ID存到关联的聊天室里面
								ChatRoomManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(),gameRoomBaseInfoBuilder.getLGameRoomId());
							}
						}else if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
							ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
							if(stClubInfoBuilder!=null){
								for(int i = 0 ; i < stClubInfoBuilder.getVClubUserInfosCount();i++){
									ClubUserInfo.Builder stClubUserInfoBuilder = stClubInfoBuilder.getVClubUserInfosBuilder(i);
									userList.add(stClubUserInfoBuilder.getStUserBaseInfo());
								}
													
								//添加俱乐部积分
								stClubInfoBuilder = ClubManager.getInstance().addClubScore(stClubInfoBuilder.getStClubBaseInfo().getLClubID(), 5);
												
								// 将牌局ID存到关联的聊天室里面
								ClubManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(),gameRoomBaseInfoBuilder.getLGameRoomId());
							}
						}
											
						gameRoomInfo.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
						gameRoomInfo.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
										
						// 设置房间的其他玩家，因为现在还没有玩家同意，所以设置7个空用户
						for (int i = 0; i < gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums(); i++) {
							TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();
	
							// 设置userbaseinfo
							UserBaseInfoNet.Builder userBaseInfoBuilder = UserBaseInfoNet.newBuilder();
							userBaseInfoBuilder.setUuid(0);
							userBaseInfoBuilder.setEType(ID_TYPE.ID_TYPE_NA);
							userBaseInfoBuilder.setStrID("");
							userBaseInfoBuilder.setStrCover("");
							userBaseInfoBuilder.setStrNick("");
							userBaseInfoBuilder.setStrPhoneNumber("");
							userBaseInfoBuilder.setStrPhoneNumber("");
							userBaseInfoBuilder.setIGender(0);
							userBaseInfoBuilder.setEVipType(VIP_TYPE.BLUE_CARD);
							userBaseInfoBuilder.setLLoginTime(0);
							userGameInfoBuilder.setStUserInfo(userBaseInfoBuilder);
							userGameInfoBuilder.setIDelaySingleTime(20);
												
							// 设置其他属性
							userGameInfoBuilder.setLRoomId(0);
							userGameInfoBuilder.setLRemainStacks(0);
							userGameInfoBuilder.setLBetStacks(0);
												
							userGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
							userGameInfoBuilder.setBGameDealer(0);
							userGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);
	
							gameRoomInfo.addVUserGameInfos(userGameInfoBuilder);
						}
	
						gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
						stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfo.build());
						stGameRoomDetailInfo.setiGameRoomUserMaxNums(stGameRoomBaseInfo.getIGameRoomUserMaxNums());
						stGameRoomDetailInfo.lGameRoomID = gameRoomBaseInfoBuilder.getLGameRoomId();
						stGameRoomDetailInfo.lFromRoomID = gameRoomBaseInfoBuilder.getLChatRoomID();
											
						// 设置牌局的可玩用户数，用在开局
						stGameRoomDetailInfo.iGameRoomUserMaxNums = gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();
	
						// 放入GameManager中
						GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
						
						ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
						
						ConnectDatabase.getInstance().insertInGameUser(stGameRoomDetailInfo.getStGameRoomInfo()
											.getStGameRoomBaseInfo().getLGameRoomId(),uuid);
	
						BillManager.getInstance().createBillInfo(0,gameRoomBaseInfoBuilder.build());
						
						// 把房间放到房主的房间列表里面
						UserManager.getInstance().addUserCreateGameRooms(uuid,gameRoomBaseInfoBuilder.getLGameRoomId());
				
						if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
							ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
							if(stClubInfoBuilder!=null){
								// 发送APP Push
								pushAppCreateMsg(0,stClubInfoBuilder.getStClubBaseInfo().getLClubID()
										,gameRoomBaseInfoBuilder.getStrRoomName()
										,gameRoomBaseInfoBuilder.getLDuration()
										,gameRoomBaseInfoBuilder.getLSmallBlinds()
										,gameRoomBaseInfoBuilder.getIAnte());
							}	
						}			
						
						TexasPoker.UserGameInfoNet.Builder userSelfGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();
						
						UserBaseInfoNet.Builder stCreateUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

						userSelfGameInfoBuilder.setStUserInfo(stCreateUserBaseInfoBuilder);
						userSelfGameInfoBuilder.setLRoomId(gameRoomBaseInfoBuilder.getLGameRoomId());

						userSelfGameInfoBuilder.setLRemainStacks(0);
						userSelfGameInfoBuilder.setLBetStacks(0);
						userSelfGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
						userSelfGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
						userSelfGameInfoBuilder.setBGameDealer(0);
						userSelfGameInfoBuilder.setIDelaySingleTime(20);
						
						BillManager.getInstance().buyStackInRoom(stUserBaseInfoNet.getUuid(),
								stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),0);

						// 将被邀请的用户设置要被邀请的名单中,弄发送邀请通知
						for (int i = 0; i < userList.size(); i++) {
							UserBaseInfoNet stUserBaseInfo = userList.get(i);
							if (stUserBaseInfo.getUuid() != uuid) {
								// 把用户放到nocahrge队列里面

								pushGametRoomUserMsgs(userSelfGameInfoBuilder.build(),
												stUserBaseInfo.getUuid(),
												GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_INVITE,
												stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
								
							}
						}
						
						stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
						stGameRoomIDResultInfo.setlGameRoomID(gameRoomBaseInfoBuilder.getLGameRoomId());
						return stGameRoomIDResultInfo;
					}		
				}else if(stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL){
					if(stUserBaseInfoNet.getLCoin() >= 200){
						synchronized(RoomIDManager.class){
							TexasPoker.GameRoomBaseInfo.Builder gameRoomBaseInfoBuilder = stGameRoomBaseInfo.toBuilder();
							gameRoomBaseInfoBuilder.setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()));
							gameRoomBaseInfoBuilder.setBBuyIn(1);
							gameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
											
							// 设置房主vip信息
							gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));

							TexasPoker.GameRoomInfo.Builder gameRoomInfo = TexasPoker.GameRoomInfo.newBuilder();
							gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
											
							// 修改用户的popularity
							// 修改UserBaseInfo				
							Log   mLog   =  LogFactory.getLog("fortransaction");
//							String sLog = uuid + " " + 0 + " " + 110000 + " " + gameRoomBaseInfoBuilder.getLGameRoomId() 
//									+ " 0 " + " 0 " + gameRoomBaseInfoBuilder.getLBigBlinds()*100 + " " + stUserBaseInfoNet.getLPopularity() +" " + gameRoomBaseInfoBuilder.getLChatRoomID();;
//							mLog.error(sLog);
//							TranscationLogService.getInstance().error(sLog);
											
							// 保存设置
							stUserBaseInfoNet = UserManager.getInstance().addUserCoin(stUserBaseInfoNet.getUuid(),  - 200);
							
							gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stUserBaseInfoNet);
							GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();
											
							List<UserBaseInfoNet> userList = new ArrayList<UserBaseInfoNet>();
											
							if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CAHT_ROOM){
								ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
								if(stChatRoomInfo!=null){
									userList = stChatRoomInfo.getVUserBaseInfosList();
													
									// 将牌局ID存到关联的聊天室里面
									ChatRoomManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(),gameRoomBaseInfoBuilder.getLGameRoomId());
								}
							}else if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
								ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
								if(stClubInfoBuilder!=null){
									for(int i = 0 ; i < stClubInfoBuilder.getVClubUserInfosCount();i++){
										ClubUserInfo.Builder stClubUserInfoBuilder = stClubInfoBuilder.getVClubUserInfosBuilder(i);
										userList.add(stClubUserInfoBuilder.getStUserBaseInfo());
									}
										
									//添加俱乐部积分
									stClubInfoBuilder = ClubManager.getInstance().addClubScore(stClubInfoBuilder.getStClubBaseInfo().getLClubID(), 5);
										
									// 将牌局ID存到关联的聊天室里面
									ClubManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(),gameRoomBaseInfoBuilder.getLGameRoomId());
								}
							}
							
							gameRoomInfo.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
							gameRoomInfo.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
							gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);			
							// 设置房间的其他玩家，因为现在还没有玩家同意，所以设置7个空用户
							for (int i = 0; i < gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums(); i++) {
								TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet
										.newBuilder();

								// 设置userbaseinfo
								UserBaseInfoNet.Builder userBaseInfoBuilder = UserBaseInfoNet.newBuilder();
								userBaseInfoBuilder.setUuid(0);
								userBaseInfoBuilder.setEType(ID_TYPE.ID_TYPE_NA);
								userBaseInfoBuilder.setStrID("");
								userBaseInfoBuilder.setStrCover("");
								userBaseInfoBuilder.setStrNick("");
								userBaseInfoBuilder.setStrPhoneNumber("");
								userBaseInfoBuilder.setStrPhoneNumber("");
								userBaseInfoBuilder.setIGender(0);
								userBaseInfoBuilder.setEVipType(VIP_TYPE.BLUE_CARD);
								userBaseInfoBuilder.setLLoginTime(0);
								userGameInfoBuilder.setStUserInfo(userBaseInfoBuilder);
								userGameInfoBuilder.setIDelaySingleTime(20);
								
								// 设置其他属性
								userGameInfoBuilder.setLRoomId(0);
								userGameInfoBuilder.setLRemainStacks(0);
								userGameInfoBuilder.setLBetStacks(0);
								
								userGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
								userGameInfoBuilder.setBGameDealer(0);
								userGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);

								gameRoomInfo.addVUserGameInfos(userGameInfoBuilder);
							}

							stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfo.build());

							// 设置牌局的可玩用户数，用在开局
							stGameRoomDetailInfo.iCouldPlayNum++;
							stGameRoomDetailInfo.lGameRoomID = gameRoomBaseInfoBuilder.getLGameRoomId();
							stGameRoomDetailInfo.lFromRoomID = gameRoomBaseInfoBuilder.getLChatRoomID();
							stGameRoomDetailInfo.iGameRoomUserMaxNums = gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();

							// 放入GameManager中
							GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
							ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
							ConnectDatabase.getInstance().insertInGameUser(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),uuid);

							BillManager.getInstance().createBillInfo(0,gameRoomBaseInfoBuilder.build());
							
							// 把房间放到房主的房间列表里面
							UserManager.getInstance().addUserCreateGameRooms(uuid,gameRoomBaseInfoBuilder.getLGameRoomId());
							
							BillManager.getInstance().buyStackInRoom(stUserBaseInfoNet.getUuid(),
									stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),0);

							// 生成回包
							mLog.error(stUserBaseInfoNet.getUuid() + " " + gameRoomBaseInfoBuilder.getLGameRoomId() + " " + 110001 + " " + gameRoomBaseInfoBuilder.getLDuration()
												+ " " + gameRoomBaseInfoBuilder.getIMinBuyinRatio() 
												+ " " + gameRoomBaseInfoBuilder.getIMaxBuyinRatio() + " 200 "  + stUserBaseInfoNet.getLCoin());
										
							if(gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
								ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
								if(stClubInfoBuilder!=null){
									// 如果是俱乐部开局，则对俱乐部里的每个人进行push操作
									// 发送APP Push
									pushAppCreateMsg(0,stClubInfoBuilder.getStClubBaseInfo().getLClubID()
											,gameRoomBaseInfoBuilder.getStrRoomName(),gameRoomBaseInfoBuilder.getLDuration()
											,gameRoomBaseInfoBuilder.getLSmallBlinds()
											,gameRoomBaseInfoBuilder.getIAnte());
								}
							}	
							
							TexasPoker.UserGameInfoNet.Builder userSelfGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();
							
							UserBaseInfoNet.Builder stCreateUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

							userSelfGameInfoBuilder.setStUserInfo(stCreateUserBaseInfoBuilder);
							userSelfGameInfoBuilder.setLRoomId(gameRoomBaseInfoBuilder.getLGameRoomId());
							userSelfGameInfoBuilder.setLRemainStacks(0);
							userSelfGameInfoBuilder.setLBetStacks(0);
							userSelfGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
							userSelfGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
							userSelfGameInfoBuilder.setBGameDealer(0);
							userSelfGameInfoBuilder.setIDelaySingleTime(20);
							
//							stGameRoomDetailInfo.mapStandByUserGameInfos.put(stCreateUserBaseInfoBuilder.getUuid(), userSelfGameInfoBuilder);
//							stGameRoomDetailInfo.addStandByUuid(stCreateUserBaseInfoBuilder.getUuid());
							// 将被邀请的用户设置要被邀请的名单中,弄发送邀请通知
							for (int i = 0; i < userList.size(); i++) {
								UserBaseInfoNet stUserBaseInfo = userList.get(i);
								if (stUserBaseInfo.getUuid() != uuid) {
									//发送邀请通知
									pushGametRoomUserMsgs(
												userSelfGameInfoBuilder.build(),
												stUserBaseInfo.getUuid(),
												GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_INVITE,
												stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
									
								}
							}
							stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
							stGameRoomIDResultInfo.setlGameRoomID(gameRoomBaseInfoBuilder.getLGameRoomId());
							return stGameRoomIDResultInfo;
						}
					}else{
						stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);
						return stGameRoomIDResultInfo;
					}
				}
		
		}
		stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
		return stGameRoomIDResultInfo;
	}

	
	public void pushAppCreateMsg(long uuid,long lClubID,String sGameName,long lDuration,long sb,int ante){
		//获取push相关信息
    	float fHour =  (float)lDuration/(float)3600;
    	APNManager.getInstance().createGameRoomPush(uuid, lClubID, sGameName, ""+fHour, sb, ante, 1);  
	}
	
	void pushGametRoomUserMsgs(UserGameInfoNet stFromUserGamenfo/* 邀请人 */,
			long stToUserUuid, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType,
			long lRoomID) {
		// 给被邀请用户发通知
		TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg
				.newBuilder();

		// 获取时间
		long curTime =  System.currentTimeMillis();

		// 设置msg id
		stGameRoomPushMsgBuilder.setLMsgID(curTime);
		// 设置发送时间
		stGameRoomPushMsgBuilder.setLCreateTime(curTime);

		// 设置发送人的userbaseinfo
		stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);

		// 设置消息内容，暂时没有什么具体用处
		stGameRoomPushMsgBuilder.setSMsg("");

		// 设置 好友类型
		stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);

		// 设置房间ID
		GameRoomInfo stGameRoomInfo = GameRoomManager.getInstance().mGameRoomDetailInfos
				.get(lRoomID).getStGameRoomInfo();
		if (stGameRoomInfo != null)
			stGameRoomPushMsgBuilder.setStGameRoomInfo(stGameRoomInfo);

		TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
				.build();

		// 发送push消息
		GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
	}
	
	public void gotoNextStep(long lGameRoomID){
		GameRoomDetailInfo stGameRoomDetailInfo = getGameRoomDetailInfo(lGameRoomID);
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.gotoNextStep();
		}
	}
}
