package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.texaspoker.bill.BillManager;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.ChatRoomInfo;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_SENIOR_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPoker.QUICK_GAME_ROOM_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.QUICK_GAME_ROOM_STATUS;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPoker.QuickGameRoomPushMsg;
import com.texaspoker.moment.TexasPoker.USER_GAME_ROLE;
import com.texaspoker.moment.TexasPoker.USER_GAME_STATE;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ID_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerCommon.VIP_TYPE;
import com.texaspoker.moment.setting.GlobalData;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.user.UserManager;

public class QuickGameRoomManager {
	
	private ConcurrentHashMap<Long,QuickGameRoomInfo.Builder> mQuickGameRoomInfos; //房间相关的数据
	private ConcurrentHashMap<Long,HashSet<Long>> mUserQuickGameRooms; //房间相关的数据
	private ConcurrentHashMap<String,Long> mCodeQuickGameRooms; //房间相关的数据
	
	private static QuickGameRoomManager instance; 
	Log   mLogNormals   =   LogFactory.getLog("fornormal");
	
//	设置timer方法
	Timer timer = new Timer(true);   
	
	//私有的构造函数
	private QuickGameRoomManager (){
		mQuickGameRoomInfos = new ConcurrentHashMap<Long,QuickGameRoomInfo.Builder>();
		mUserQuickGameRooms  = new ConcurrentHashMap<Long,HashSet<Long>>();
		mCodeQuickGameRooms   = new ConcurrentHashMap<String,Long>();
		//加载快速房间信息
		TimerTask task = new TimerTask() {   
			public void run() {   
				//检查是否有超时的房间
				Iterator iter = mQuickGameRoomInfos.entrySet().iterator();
				while (iter.hasNext()) { 
				    Map.Entry entry = (Map.Entry) iter.next(); 
				    Long  key = (Long) entry.getKey();
				    QuickGameRoomInfo.Builder  stQuickGameRoomInfoBuilder = (QuickGameRoomInfo.Builder) entry.getValue();
				    
				    if(stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART){
				    	//如果快速房间没有开始，检查其是否已经超时
				    	if(System.currentTimeMillis() - stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLCreateTime() 
				    			> stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLDuration()*1000 ){
				    		//如果房间过了一个牌局的时间后还没有开局，则自动将房间关闭
				    		finishQuickGameRoom(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
				    	}
				    }
				} 
			}   
		};    
		
		timer.schedule(task, 1000,60000); 
	}
	
	//单例模式
	public static QuickGameRoomManager getInstance() {  
			synchronized(QuickGameRoomManager.class){
				if (instance == null) {
					instance = new QuickGameRoomManager();
					ConnectDatabase.getInstance().loadAllQuickGameRooms(instance.mQuickGameRoomInfos,instance.mCodeQuickGameRooms,instance.mUserQuickGameRooms);
				}
			}
		return instance;  
	}
	
	public void setQuickGameRoomInfos(QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder){
		mQuickGameRoomInfos.put(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId(), stQuickGameRoomInfoBuilder);
		mCodeQuickGameRooms.put(stQuickGameRoomInfoBuilder.getSCryptCode(), stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
		ConnectDatabase.getInstance().insertQuickRoomInfo(stQuickGameRoomInfoBuilder.build());
	}
	
	public void addUserCreateQuickGameRoom(long uuid,long lQuickGameRoomID){
		System.out.println("addUserCreateQuickGameRoom : " + lQuickGameRoomID);
		UserManager.getInstance().setUserQuickGameRoom(uuid, lQuickGameRoomID);
	}
	
	public void removeUserCreateQuickGameRoom(long uuid,long lQuickGameRoomID){
		UserManager.getInstance().removeUserQuickGameRoom(uuid);
		UserManager.getInstance().removeUserCreateGameRooms(uuid,lQuickGameRoomID);
	}
	
	public Long getUserCreateQuickGameRoom(long uuid){
		return UserManager.getInstance().getUserQuickGameRoom(uuid);
	}
	
	public QuickGameRoomInfo.Builder getQuickGameRoomInfoByRoomID(long lRoomID){
		return mQuickGameRoomInfos.get(lRoomID);
	}
	
	public void removeQuickGameRoomInfos(long lQuickGameRoomID){
		mQuickGameRoomInfos.remove(lQuickGameRoomID);
	}
	
	public void setUserQuickGameRoomInfos(long uuid,long lQuickGameRoomID){
		HashSet<Long> vQuickGameRoomIDs = mUserQuickGameRooms.get(uuid);
		if(vQuickGameRoomIDs == null){
			vQuickGameRoomIDs = new HashSet<Long>();
		}
		vQuickGameRoomIDs.add(lQuickGameRoomID);
		mUserQuickGameRooms.put(uuid, vQuickGameRoomIDs);
	}
	
	public void removeUserQuickGameRoomInfos(long uuid,long lQuickGameRoomID){
		HashSet<Long> vQuickGameRoomIDs = mUserQuickGameRooms.get(uuid);
		if(vQuickGameRoomIDs == null){
			vQuickGameRoomIDs = new HashSet<Long>();
		}
		vQuickGameRoomIDs.remove(lQuickGameRoomID);
		mUserQuickGameRooms.put(uuid, vQuickGameRoomIDs);
	}

	public HashSet<Long> getUserQuickGameRoomInfos(long uuid){
		return mUserQuickGameRooms.get(uuid);
	}
	
	public Long getQuickGameRoomInfoByCode(String code){
		return mCodeQuickGameRooms.get(code);
	}
	
	public void finishQuickGameRoom(long lQuickGameRoomID){
		QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
		if(stQuickGameRoomInfoBuilder!=null){
			stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH);
			removeQuickGameRoomInfos(lQuickGameRoomID);

			ConnectDatabase.getInstance().insertQuickRoomInfo(stQuickGameRoomInfoBuilder.build());
			//消除牌局开局
			removeUserCreateQuickGameRoom(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLCreateUser(),lQuickGameRoomID);
			//发送房间结束的push
			for(int i = 0 ; i < stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfosCount();i++){
				UserBaseInfoNet stUserBaseInfoNet = stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfos(i);
				
				QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = QuickGameRoomPushMsg.newBuilder();
				stQuickGameRoomPushMsgBuilder.setEMessageType(QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_FINISH);
				stQuickGameRoomPushMsgBuilder.setLMsgID(System.currentTimeMillis());
				stQuickGameRoomPushMsgBuilder.setSMsg("");
				stQuickGameRoomPushMsgBuilder.setLCreateTime(System.currentTimeMillis());
				stQuickGameRoomPushMsgBuilder.setLChatRoomID(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
				stQuickGameRoomPushMsgBuilder.setLGameRoomID(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
				
				GameRoomMsgManager.getInstance().sendQuickGameRoomMsg(stUserBaseInfoNet.getUuid(), stQuickGameRoomPushMsgBuilder.build());
			}
			

			if(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL){
				UserManager.getInstance().addUserCoin(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), 200);
			}
		}
	}
	
	
	public QuickGameRoomResultInfo createQuickGameRoom(long uuid,GameRoomBaseInfo stGameRoomBaseInfo){
		//设置发送用户
		UserBaseInfoNet.Builder stFromUserBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
		
		Log   mLog   =   LogFactory.getLog("fortransaction");
		QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();
		
		if(stFromUserBuilder !=null){
			//先检测是否已经创建过快速牌局
//			if(QuickGameRoomManager.getInstance().getUserCreateQuickGameRoom(uuid)==null
//				||	QuickGameRoomManager.getInstance().getUserCreateQuickGameRoom(uuid)==0){
				
				// 获取该用户已经开的牌局数量
				UserGameRooms stUserCreateGames = UserManager.getInstance().getUserCreateGameRooms(uuid);
				int userPlaysNumber = 0;
				if (stUserCreateGames != null) 
					userPlaysNumber = stUserCreateGames.mRoomMap.size();
				
				if (userPlaysNumber >= GlobalData.getSimultaneousPlays(stFromUserBuilder.getEVipType())) {		
					stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_GAMEROOM_OVERFLOW);
					return stQuickGameRoomResultInfo;
				}else{
					System.out.println("createQuickGameRoom 1 : " + stGameRoomBaseInfo.getEGameRoomSeniorType());
					if(stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NO){
						synchronized(RoomIDManager.class){
							
							//生成一个新的快速开局房间
							QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomInfo.newBuilder();
							
							//生成房间结构体
							ChatRoomInfo.Builder stChatRoomInfoBuilder = ChatRoomInfo.newBuilder();
							//创建房间流程								
							TexasPoker.ChatRoomBaseInfo.Builder chatRoomBaseInfo = TexasPoker.ChatRoomBaseInfo.newBuilder();
							long lRoomID = RoomIDManager.getInstance().getNewRoomID(stFromUserBuilder.getUuid());
							chatRoomBaseInfo.setLChatRoomId(lRoomID);					
							chatRoomBaseInfo.setLCreateUser(uuid);
							chatRoomBaseInfo.setNMaxMembers(20);
							chatRoomBaseInfo.setNCurMembers(1);
							chatRoomBaseInfo.setIRoomType(3);
							chatRoomBaseInfo.setNOwnerCreate(1);
							chatRoomBaseInfo.setNOwnerInvite(0);
							chatRoomBaseInfo.setIMute(1);
							chatRoomBaseInfo.setStrRoomIcon("");
							chatRoomBaseInfo.setStrRoomName(stGameRoomBaseInfo.getStrRoomName());
							chatRoomBaseInfo.setStrRoomTempName("");

							stChatRoomInfoBuilder.setStChatRoomBaseInfo(chatRoomBaseInfo);
							
							//设置房间的用户		
							stChatRoomInfoBuilder.addVUserBaseInfos(stFromUserBuilder);
										
							//加入每个人的房间列表
							QuickGameRoomManager.getInstance().setUserQuickGameRoomInfos(stFromUserBuilder.getUuid(), lRoomID);

							//生成设置房间的基本信息
							GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = GameRoomBaseInfo.newBuilder();
							stGameRoomBaseInfoBuilder.setLSmallBlinds(stGameRoomBaseInfo.getLSmallBlinds());
							stGameRoomBaseInfoBuilder.setLBigBlinds(stGameRoomBaseInfo.getLBigBlinds());
							stGameRoomBaseInfoBuilder.setLBuyIn(stGameRoomBaseInfo.getLBuyIn());
							stGameRoomBaseInfoBuilder.setBBuyIn(stGameRoomBaseInfo.getBBuyIn());
							stGameRoomBaseInfoBuilder.setLDuration(stGameRoomBaseInfo.getLDuration());
							stGameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
							stGameRoomBaseInfoBuilder.setBPrivateRoom(0);
							stGameRoomBaseInfoBuilder.setLChatRoomID(lRoomID);
							stGameRoomBaseInfoBuilder.setLGameRoomId(0);
							stGameRoomBaseInfoBuilder.setStrRoomName(stGameRoomBaseInfo.getStrRoomName());
							stGameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stFromUserBuilder);
							stGameRoomBaseInfoBuilder.setBStarted(0);
							stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(40);
							stGameRoomBaseInfoBuilder.setIMinBuyinRatio(10);
							stGameRoomBaseInfoBuilder.setEGameRoomSeniorType(stGameRoomBaseInfo.getEGameRoomSeniorType());
							stGameRoomBaseInfoBuilder.setBBuyinControl(stGameRoomBaseInfo.getBBuyinControl());
							stGameRoomBaseInfoBuilder.setIGameRoomUserMaxNums(stGameRoomBaseInfo.getIGameRoomUserMaxNums());
							stGameRoomBaseInfoBuilder.setIAnte(stGameRoomBaseInfo.getIAnte());
							stGameRoomBaseInfoBuilder.setBInsurance(stGameRoomBaseInfo.getBInsurance());
							stGameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
							
							stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
							stQuickGameRoomInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
							ChatRoomManager.getInstance().setChatRoomInfo(lRoomID, stChatRoomInfoBuilder.build());
							ConnectDatabase.getInstance().insertRoomInfo(stChatRoomInfoBuilder.build());
							
							stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
							stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART);
							
							// 把房间放到房主的房间列表里面
							UserManager.getInstance().addUserCreateGameRooms(uuid,lRoomID);
							
							//生成
							String sCode = RoomIDManager.getInstance().getRandomCode();
							stQuickGameRoomInfoBuilder.setSCryptCode(sCode);
							
							//把房间放到管理器里面，用于管理
							QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);
							QuickGameRoomManager.getInstance().addUserCreateQuickGameRoom(stFromUserBuilder.getUuid(), lRoomID);
							//组织回包
							stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
							return stQuickGameRoomResultInfo;
						}
					}else if(stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL){
						if(stFromUserBuilder.getLCoin() >= 200){
							synchronized(RoomIDManager.class){
//								Long oldRoomID = UserManager.getInstance().getUserInTable(uuid);
//								if (oldRoomID != null) {
//									// 说明玩家在别的屋子中，首先需要进行玩家离开房间的操作
//									GameRoomDetailInfo stOldGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(oldRoomID);
//									
//									if (stOldGameRoomDetailInfo != null) {
//										if(stOldGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBGamePause() == 1){
//											stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED);
//											return stQuickGameRoomResultInfo;
//										}
//										
//										stOldGameRoomDetailInfo.leaveGameRoom(uuid);
//									}
//								}
								
								//生成一个新的快速开局房间
								QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomInfo.newBuilder();
								
								//生成房间结构体
								ChatRoomInfo.Builder stChatRoomInfoBuilder = ChatRoomInfo.newBuilder();
								//创建房间流程								
								TexasPoker.ChatRoomBaseInfo.Builder chatRoomBaseInfo = TexasPoker.ChatRoomBaseInfo.newBuilder();
								long lRoomID = RoomIDManager.getInstance().getNewRoomID(stFromUserBuilder.getUuid());
								chatRoomBaseInfo.setLChatRoomId(lRoomID);					
								chatRoomBaseInfo.setLCreateUser(uuid);
								chatRoomBaseInfo.setNMaxMembers(20);
								chatRoomBaseInfo.setNCurMembers(1);
								chatRoomBaseInfo.setIRoomType(3);
								chatRoomBaseInfo.setNOwnerCreate(1);
								chatRoomBaseInfo.setNOwnerInvite(0);
								chatRoomBaseInfo.setIMute(1);
								chatRoomBaseInfo.setStrRoomIcon("");
								chatRoomBaseInfo.setStrRoomName(stGameRoomBaseInfo.getStrRoomName());
								chatRoomBaseInfo.setStrRoomTempName("");

								stChatRoomInfoBuilder.setStChatRoomBaseInfo(chatRoomBaseInfo);
								
								//设置房间的用户		
								stChatRoomInfoBuilder.addVUserBaseInfos(stFromUserBuilder);
								
								// 把房间放到房主的房间列表里面
								UserManager.getInstance().addUserCreateGameRooms(uuid,lRoomID);
											
								//加入每个人的房间列表
								QuickGameRoomManager.getInstance().setUserQuickGameRoomInfos(stFromUserBuilder.getUuid(), lRoomID);

								//减去高级房开房费用
								stFromUserBuilder = UserManager.getInstance().addUserCoin(stFromUserBuilder.getUuid(), -200);
								
								//生成设置房间的基本信息
								GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = GameRoomBaseInfo.newBuilder();
								stGameRoomBaseInfoBuilder.setLSmallBlinds(stGameRoomBaseInfo.getLSmallBlinds());
								stGameRoomBaseInfoBuilder.setLBigBlinds(stGameRoomBaseInfo.getLBigBlinds());
								stGameRoomBaseInfoBuilder.setLBuyIn(stGameRoomBaseInfo.getLBuyIn());
								stGameRoomBaseInfoBuilder.setBBuyIn(stGameRoomBaseInfo.getBBuyIn());
								stGameRoomBaseInfoBuilder.setLDuration(stGameRoomBaseInfo.getLDuration());
								stGameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
								stGameRoomBaseInfoBuilder.setBPrivateRoom(0);
								stGameRoomBaseInfoBuilder.setLChatRoomID(lRoomID);
								stGameRoomBaseInfoBuilder.setLGameRoomId(0);
								stGameRoomBaseInfoBuilder.setStrRoomName(stGameRoomBaseInfo.getStrRoomName());
								stGameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stFromUserBuilder);
								stGameRoomBaseInfoBuilder.setBStarted(0);
								stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(stGameRoomBaseInfo.getIMaxBuyinRatio());
								stGameRoomBaseInfoBuilder.setIMinBuyinRatio(stGameRoomBaseInfo.getIMinBuyinRatio());
								stGameRoomBaseInfoBuilder.setEGameRoomSeniorType(stGameRoomBaseInfo.getEGameRoomSeniorType());
								stGameRoomBaseInfoBuilder.setBBuyinControl(stGameRoomBaseInfo.getBBuyinControl());
								stGameRoomBaseInfoBuilder.setIGameRoomUserMaxNums(stGameRoomBaseInfo.getIGameRoomUserMaxNums());
								stGameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stFromUserBuilder);
								stGameRoomBaseInfoBuilder.setIAnte(stGameRoomBaseInfo.getIAnte());
								stGameRoomBaseInfoBuilder.setBInsurance(stGameRoomBaseInfo.getBInsurance());
								stGameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
								
								stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
								
								ChatRoomManager.getInstance().setChatRoomInfo(lRoomID, stChatRoomInfoBuilder.build());
								ConnectDatabase.getInstance().insertRoomInfo(stChatRoomInfoBuilder.build());
								
								stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
								stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART);
								stQuickGameRoomInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
								//生成
								String sCode = RoomIDManager.getInstance().getRandomCode();
								stQuickGameRoomInfoBuilder.setSCryptCode(sCode);
								
								//把房间放到管理器里面，用于管理
								QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);
										
								QuickGameRoomManager.getInstance().addUserCreateQuickGameRoom(stFromUserBuilder.getUuid(), lRoomID);
								
								mLog.error(uuid + " " + lRoomID + " " + 110001 + " " + stGameRoomBaseInfoBuilder.getLDuration()
										+ " " + stGameRoomBaseInfoBuilder.getIMinBuyinRatio() 
										+ " " + stGameRoomBaseInfoBuilder.getIMaxBuyinRatio() + " 200 "  + (stFromUserBuilder.getLCoin()-200));
								
								//组织回包
								stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
								stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
								return stQuickGameRoomResultInfo;
							}
						}else{
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);
							return stQuickGameRoomResultInfo;
						}
					}	
				}		
//			}else{
//				stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_CREATE_QUICK_GAMEROOM_ALREADY);
//				return stQuickGameRoomResultInfo;
//			}
		}
		else{
			stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
			return stQuickGameRoomResultInfo;
		}
		stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
		return stQuickGameRoomResultInfo;
	}	
	
	public GameRoomResultInfo startQuickGameRoom(long uuid,long lQuickGameRoomID){
		UserBaseInfoNet.Builder stUserBaseInfoNet = UserManager.getInstance().getUserBaseInfo(uuid);
		
		GameRoomResultInfo stGameRoomResultInfo = new GameRoomResultInfo();
		//获取快速开局房间信息
		QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
		if(stQuickGameRoomInfoBuilder!=null ){
			if(stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() != QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART){
				// 生成回包
				stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_QUICK_GAMEROOM_ALREADY_START);
			}
			// 判断用户是否可以在创建房间
			else {
				// 先检查是否在别的游戏中
					ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
					if(stChatRoomInfoBuilder!=null){
						GameRoomBaseInfo.Builder stSettingGameRoomBaseInfoBuilder = stQuickGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
						if(stSettingGameRoomBaseInfoBuilder != null){
							// 生成银币扣除数据
								synchronized(RoomIDManager.class){
									TexasPoker.GameRoomBaseInfo.Builder gameRoomBaseInfoBuilder = TexasPoker.GameRoomBaseInfo.newBuilder();
									gameRoomBaseInfoBuilder.setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()));
									gameRoomBaseInfoBuilder.setStrRoomName(stSettingGameRoomBaseInfoBuilder.getStrRoomName());
									gameRoomBaseInfoBuilder.setBPrivateRoom(stSettingGameRoomBaseInfoBuilder.getBPrivateRoom());
									gameRoomBaseInfoBuilder.setBBuyIn(1);
									gameRoomBaseInfoBuilder.setLSmallBlinds(stSettingGameRoomBaseInfoBuilder.getLSmallBlinds());
									gameRoomBaseInfoBuilder.setLBigBlinds(stSettingGameRoomBaseInfoBuilder.getLBigBlinds());
									gameRoomBaseInfoBuilder.setLBuyIn(stSettingGameRoomBaseInfoBuilder.getLBuyIn());
									gameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
									gameRoomBaseInfoBuilder.setLDuration(stSettingGameRoomBaseInfoBuilder.getLDuration());
									gameRoomBaseInfoBuilder.setECreateRoomType(CREATE_ROOM_TYPE.QUICK_GAME_ROOM);
								
									gameRoomBaseInfoBuilder.setIMaxBuyinRatio(stSettingGameRoomBaseInfoBuilder.getIMaxBuyinRatio());
									gameRoomBaseInfoBuilder.setIMinBuyinRatio(stSettingGameRoomBaseInfoBuilder.getIMinBuyinRatio());
									gameRoomBaseInfoBuilder.setBBuyinControl(stSettingGameRoomBaseInfoBuilder.getBBuyinControl());
									gameRoomBaseInfoBuilder.setEGameRoomSeniorType(stSettingGameRoomBaseInfoBuilder.getEGameRoomSeniorType());
									gameRoomBaseInfoBuilder.setIGameRoomUserMaxNums(stSettingGameRoomBaseInfoBuilder.getIGameRoomUserMaxNums());
									gameRoomBaseInfoBuilder.setIAnte(stSettingGameRoomBaseInfoBuilder.getIAnte());
									gameRoomBaseInfoBuilder.setBInsurance(stSettingGameRoomBaseInfoBuilder.getBInsurance());
									gameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
									// 设置房主vip信息
									gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));
									gameRoomBaseInfoBuilder.setLChatRoomID(lQuickGameRoomID);

									TexasPoker.GameRoomInfo.Builder gameRoomInfoBuilder = TexasPoker.GameRoomInfo.newBuilder();
									gameRoomInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

									GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();
										
									List<UserBaseInfoNet> userList = new ArrayList<UserBaseInfoNet>();;
										
									ChatRoomInfo stChatRoomInfo = stQuickGameRoomInfoBuilder.getStChatRoomInfo();
									if(stChatRoomInfo!=null){
										userList = stChatRoomInfo.getVUserBaseInfosList();
												
										// 将牌局ID存到关联的聊天室里面
										ChatRoomManager.getInstance().addGameRooms(stChatRoomInfo.getStChatRoomBaseInfo().getLChatRoomId(),gameRoomBaseInfoBuilder.getLGameRoomId());
									}
									gameRoomInfoBuilder.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);	
									gameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
										
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

										gameRoomInfoBuilder.addVUserGameInfos(userGameInfoBuilder);
									}
									stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfoBuilder.build()) ;
									stGameRoomDetailInfo.setiGameRoomUserMaxNums(gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums());

									// 设置牌局的可玩用户数，用在开局
									stGameRoomDetailInfo.lGameRoomID = gameRoomBaseInfoBuilder.getLGameRoomId();
									stGameRoomDetailInfo.lFromRoomID = gameRoomBaseInfoBuilder.getLChatRoomID();
									//生成自己的数据，放到旁观队列里面
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
//									stGameRoomDetailInfo.mapStandByUserGameInfos.put(stCreateUserBaseInfoBuilder.getUuid(), userSelfGameInfoBuilder);
									
									// 放入GameManager中
									GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
									ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
									ConnectDatabase.getInstance().insertInGameUser(
												stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),uuid);
									
									BillManager.getInstance().createBillInfo(0,gameRoomBaseInfoBuilder.build());
									
									BillManager.getInstance().buyStackInRoom(
												stUserBaseInfoNet.getUuid(),
												stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),0);
									
									//设置快速聊天室对应的牌局ID
									stSettingGameRoomBaseInfoBuilder.setLGameRoomId(gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
									stChatRoomInfoBuilder.addVGameRoomBaseInfos(stSettingGameRoomBaseInfoBuilder);
									
									stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
									stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
									stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_START);
									
									QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);
					
									// 将被邀请的用户设置要被邀请的名单中,弄发送邀请通知
									for (int i = 0; i < userList.size(); i++) {
										UserBaseInfoNet stUserBaseInfo = userList.get(i);
										if (stUserBaseInfo.getUuid() != uuid) {
											// 发送邀请通知
											if (userSelfGameInfoBuilder != null) {
												pushQuickGameRoomUserMsgs(stUserBaseInfo,
														stUserBaseInfo.getUuid(),
														QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_STARTGAME,
														stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID(),
														stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
											}
										}
									}
									stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
									stGameRoomResultInfo.setStGameRoomInfo(gameRoomInfoBuilder.build());
								}
							}
						
					}else{
						// 生成回包
						stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_CHATROOM);
					}

				}
		}else{
			// 生成回包
			stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
		}
		
		return stGameRoomResultInfo;
	}
	
	public QuickGameRoomResultInfo enterQuickGameRoom(long uuid,String sCryptCode){
		//加入快速房间逻辑
		Long lQuickGameRoomID = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByCode(sCryptCode);
		QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();
		if(lQuickGameRoomID!=null){
			//如果快速房间存在，则读取房间基本信息
			QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
		
			if(stQuickGameRoomInfoBuilder!=null&&stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus()!=QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH){
				//首先把用户加到聊天室中
				UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
				if(stUserBaseInfoNetBuilder!=null){
					ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
					//把用户加入聊天室
					stChatRoomInfoBuilder.addVUserBaseInfos(stUserBaseInfoNetBuilder);
					ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());
					
					stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
					//网数据库里存放聊天室用户数据
					ConnectDatabase.getInstance().insertChatRoomMembers(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(),uuid);
					
					QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);
					
					//push消息
					for(int i = 0 ; i < stChatRoomInfoBuilder.getVUserBaseInfosCount();i++){
						UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
						
						QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = QuickGameRoomPushMsg.newBuilder();
						stQuickGameRoomPushMsgBuilder.setEMessageType(QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_ENTER);
						stQuickGameRoomPushMsgBuilder.setLMsgID(System.currentTimeMillis());
						stQuickGameRoomPushMsgBuilder.setSMsg("");
						stQuickGameRoomPushMsgBuilder.setLCreateTime(System.currentTimeMillis());
						stQuickGameRoomPushMsgBuilder.setLChatRoomID(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId());
						stQuickGameRoomPushMsgBuilder.setLGameRoomID(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
						stQuickGameRoomPushMsgBuilder.setStFromUser(UserManager.getInstance().getUserBaseInfo(uuid));
						
						GameRoomMsgManager.getInstance().sendQuickGameRoomMsg(stUserBaseInfoNet.getUuid(), stQuickGameRoomPushMsgBuilder.build());
					}
					
					//加入每个人的房间列表
					QuickGameRoomManager.getInstance().setUserQuickGameRoomInfos(uuid, lQuickGameRoomID);
					stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
					
					GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
					if(stGameRoomDetailInfo!=null&&stGameRoomDetailInfo.getStGameRoomInfo()!=null){
						stQuickGameRoomResultInfo.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
					}
					
					return stQuickGameRoomResultInfo;
				}else{
					stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
					return stQuickGameRoomResultInfo;
				}
			}else{
				stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
				return stQuickGameRoomResultInfo;
			}
		}else{
			stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
			return stQuickGameRoomResultInfo;
		}
	}
	
	public QuickGameRoomResultInfo quitQuickGameRoom(long uuid,long lQuickGameRoomID){
		//获取房间基本信息
		QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
		QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();
		if(stQuickGameRoomInfoBuilder!=null){
			
			ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
			if(stChatRoomInfoBuilder!=null){

				if(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLCreateUser() != uuid){
					int kickIndex = -1;
					long removeUuid = 0;
					int removeCount=0;
					UserBaseInfoNet.Builder stRemoveUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
					
					if(stRemoveUserBaseInfoBuilder!=null){
						//检查房间的每一个用户，如果被删除用户在房间中，则把用户删除
						for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
							UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
							if(stUserBaseInfoNet.getUuid() == stRemoveUserBaseInfoBuilder.getUuid()){
								//把人踢出房间
								kickIndex = i;
								removeUuid = stUserBaseInfoNet.getUuid();
								break;
							}
						}
								
						//把人踢出房间
						if(kickIndex != -1&&removeUuid!=0){
							removeCount ++;
							stChatRoomInfoBuilder.removeVUserBaseInfos(kickIndex);
							
							//重新设置房间的名称
							String sChatRoomName = "";
							for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
								UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
										
								if(stUserBaseInfoNet!=null){
									if(stUserBaseInfoNet.getStrNick()!=null){
										sChatRoomName += stUserBaseInfoNet.getStrNick();
									}
								}
							}
							
							//重新设置chatroom的值
							ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());
									
							//重新设置数据库数据
							ConnectDatabase.getInstance().removeFromChatRoomUser(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId() ,removeUuid);
									
							UserManager.getInstance().removeUserChatRooms(removeUuid, stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId());
									
							//然后给屋子中剩下的人发送
							for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
								UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
								pushQuickGameRoomUserMsg(stRemoveUserBaseInfoBuilder.build(),stRemoveUserBaseInfoBuilder.build()
										,stUserBaseInfoNet.getUuid(),QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_QUIT,stChatRoomInfoBuilder.build()
										,stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
							}
						}
								
						//把人踢出房间
						if(removeCount!=0){
							//然后退出对应牌局
							if(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() > 0){
								GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
								if(stGameRoomDetailInfo!=null){
									stGameRoomDetailInfo.leaveGameRoom(uuid);	
								}
							}
							
							//最后删除快速房间
							QuickGameRoomManager.getInstance().removeUserQuickGameRoomInfos(uuid, lQuickGameRoomID);
							stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
							return stQuickGameRoomResultInfo;
						}else{
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_QUICK_GAMEROOM);
							return stQuickGameRoomResultInfo;
						}
					}
					else{
						stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);	
						return stQuickGameRoomResultInfo;
					}
				}else{
					int kickIndex = -1;
					long removeUuid = 0;
					int removeCount=0;
					UserBaseInfoNet.Builder stRemoveUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
					
					if(stRemoveUserBaseInfoBuilder!=null){
						//检查房间的每一个用户，如果被删除用户在房间中，则把用户删除
						for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
							UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
							if(stUserBaseInfoNet.getUuid() == stRemoveUserBaseInfoBuilder.getUuid()){
								//把人踢出房间
								kickIndex = i;
								removeUuid = stUserBaseInfoNet.getUuid();
								break;
							}
						}
								
						//把人踢出房间
						if(kickIndex != -1&&removeUuid!=0){
							removeCount ++;
							stChatRoomInfoBuilder.removeVUserBaseInfos(kickIndex);
							
							//重新设置房间的名称
							String sChatRoomName = "";
							for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
								UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
										
								if(stUserBaseInfoNet!=null){
									if(stUserBaseInfoNet.getStrNick()!=null){
										sChatRoomName += stUserBaseInfoNet.getStrNick();
									}
								}
							}
							
							//重新设置chatroom的值
							ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());
									
							//重新设置数据库数据
							ConnectDatabase.getInstance().removeFromChatRoomUser(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId() ,removeUuid);
							UserManager.getInstance().removeUserChatRooms(removeUuid, stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId());
									
							//然后给屋子中剩下的人发送
							for(int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount() ; i++){
								UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
								pushQuickGameRoomUserMsg(stRemoveUserBaseInfoBuilder.build(),stRemoveUserBaseInfoBuilder.build()
										,stUserBaseInfoNet.getUuid(),QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_REMOVEROOM,stChatRoomInfoBuilder.build(),
										stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
							}
						}
				
								
						//把人踢出房间
						if(removeCount!=0){
							//然后退出对应牌局
							if(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() > 0){
								GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
								if(stGameRoomDetailInfo!=null){
									stGameRoomDetailInfo.leaveGameRoom(uuid);	
								}
							}
							
							//最后删除快速房间
							QuickGameRoomManager.getInstance().removeQuickGameRoomInfos(lQuickGameRoomID);
							QuickGameRoomManager.getInstance().removeUserQuickGameRoomInfos(uuid, lQuickGameRoomID);
							QuickGameRoomManager.getInstance().removeUserCreateQuickGameRoom(uuid,lQuickGameRoomID);

							stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
							
							if(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL){
								UserManager.getInstance().addUserCoin(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), 200);
							}
							return stQuickGameRoomResultInfo;
							
						}else{
							stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_QUICK_GAMEROOM);	
							return stQuickGameRoomResultInfo;
						}
					}
					else{
						stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);	
						return stQuickGameRoomResultInfo;
					}
				}
				
			}else{
				stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);	
				return stQuickGameRoomResultInfo;
			}
		}else{
			stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);	
			return stQuickGameRoomResultInfo;
		}
	}
	
	void pushQuickGameRoomUserMsgs(UserBaseInfoNet stFromUserInfo/* 邀请人 */,
			long stToUserUuid, QUICK_GAME_ROOM_MESSAGE_TYPE  eQuickGameRoomMessageType,
			long lRoomChatRoomID,long lRoomGameRoomID) {
		//给被邀请用户发通知	
		TexasPoker.QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = TexasPoker.QuickGameRoomPushMsg.newBuilder() ;
		
		//获取时间
		long curTime = System.currentTimeMillis();
				
		//设置msg id
		stQuickGameRoomPushMsgBuilder.setLMsgID(curTime);
		//设置发送时间
		stQuickGameRoomPushMsgBuilder.setLCreateTime(curTime);
				
		//设置引发游戏的用户
		stQuickGameRoomPushMsgBuilder.setStFromUser(stFromUserInfo);
						
		//设置消息内容，暂时没有什么具体用处
		stQuickGameRoomPushMsgBuilder.setSMsg("");
				
		//设置 好友类型
		stQuickGameRoomPushMsgBuilder.setEMessageType(eQuickGameRoomMessageType);
			
		//设置房间ID
		stQuickGameRoomPushMsgBuilder.setLChatRoomID(lRoomChatRoomID);
		stQuickGameRoomPushMsgBuilder.setLGameRoomID(lRoomGameRoomID);
				
		//找到好友的session
		GameRoomMsgManager.getInstance().sendQuickGameRoomMsg(stToUserUuid, stQuickGameRoomPushMsgBuilder.build());
	}
	
	void pushQuickGameRoomUserMsg(UserBaseInfoNet stFromUserBaseInfo,UserBaseInfoNet stRemoveUserBaseInfo,long stToUserUuid,
			QUICK_GAME_ROOM_MESSAGE_TYPE eQUICK_GAME_ROOM_MESSAGE_TYPE , ChatRoomInfo stChatRoomInfo,long lGameRoomID){
		//给被邀请用户发通知
		TexasPoker.QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = TexasPoker.QuickGameRoomPushMsg.newBuilder() ;
		
		//获取时间
		long curTime = System.currentTimeMillis();
		
		//设置msg id
		stQuickGameRoomPushMsgBuilder.setLMsgID(curTime);
		//设置发送时间
		stQuickGameRoomPushMsgBuilder.setLCreateTime(curTime);
		
		//设置发送人的userbaseinfo
		stQuickGameRoomPushMsgBuilder.addVUsers(stRemoveUserBaseInfo);
		//设置引发游戏的用户
		stQuickGameRoomPushMsgBuilder.setStFromUser(stFromUserBaseInfo);
		
		//设置消息内容，暂时没有什么具体用处
		stQuickGameRoomPushMsgBuilder.setSMsg("");
		
		//设置 好友类型
		stQuickGameRoomPushMsgBuilder.setEMessageType(eQUICK_GAME_ROOM_MESSAGE_TYPE);
		//设置房间ID
		stQuickGameRoomPushMsgBuilder.setLChatRoomID(stChatRoomInfo.getStChatRoomBaseInfo().getLChatRoomId());
		stQuickGameRoomPushMsgBuilder.setLGameRoomID(lGameRoomID);

		//找到好友的session
		GameRoomMsgManager.getInstance().sendQuickGameRoomMsg(stToUserUuid, stQuickGameRoomPushMsgBuilder.build());
	}
	
}
