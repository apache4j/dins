package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.AdvancedTimeoutHashMap;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.dubbo.game.insurance.InsuranceApi;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.logger.TranscationLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.BUYIN_CONTROL_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.BuyInsuranceInfo;
import com.texaspoker.moment.TexasPoker.BuyinControl;
import com.texaspoker.moment.TexasPoker.BuyinControlMsg;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_SENIOR_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.InsuranceInfo;
import com.texaspoker.moment.TexasPoker.InsuranceUserInfo;
import com.texaspoker.moment.TexasPoker.USER_GAME_ROLE;
import com.texaspoker.moment.TexasPoker.USER_GAME_STATE;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerCommon.UserTokenInfoNet;
import com.texaspoker.moment.TexasPokerHttpClub.ClubInfo;
import com.texaspoker.moment.TexasPokerHttpClub.ClubUserInfo;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.user.UserManager;

/**
 * @author afei
 *
 */
public class GameRoomDetailInfo extends GameRoomAbstractInfo{
	private Map<Long,Long> mDelayPlayer = new ConcurrentHashMap<Long,Long>();
	private Map<Long,Long> mBuyinControlInfo = new ConcurrentHashMap<Long,Long>();
	private Map<Long,Long> mBuyinControlTimes = new ConcurrentHashMap<Long,Long>();
	private TimeoutHashMap<Long,Long> mBuyinControlTimeOut; //房间相关的数据
	
	//是否已经结算用户数据结构
	private ConcurrentHashMap<Long,Long> mapUserNoCharge = new ConcurrentHashMap<Long,Long>();
	
	//结账保护
	public boolean bOver = false;	
	
	/**
	 * 日志相关变量
	 */
	Log   mLogFortransaction   =   LogFactory.getLog("fortransaction");
	
		
	/****************************牌局操作函数*******************************/
	public ERROR_CODE_TYPE startGame(long uuid){
		synchronized(this){
			if(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid){
	
				//赋初值
				bPause = false;
				bStarted = true;
				lFromRoomID=stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
				lGameRoomID=stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId();
				iGameRoomUserMaxNums = stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums();

				
				if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted()== 0){
					TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() 
							+"," + "open" 
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds() + "/" + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()
							+"," + stGameRoomInfo.getEGameRoomType().getNumber()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
					
					
					TableLogService.getInstance().error(lFromRoomID
							+"," + lGameRoomID
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() 
							+"," + "opentype" 
							+"," + 0
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums());
				}

				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

				GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();
		
				stGameRoomBaseInfoBuilder.setBStarted(1);
				stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);

				stGameRoomInfo = stGameRoomInfoBuilder.build();
				iGameRoomUserMaxNums = stGameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();
				

				pushAppPushStartGame();
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_START_GAMEROOM;
			}
		}
	}
	
	/***
	 * 站起操作
	 * @param uuid 要站起的用户
	 * @param bAuto 是否为系统自动站起
	 * @return
	 */
	public ERROR_CODE_TYPE doStandUpAction(long uuid,boolean bAuto){
		synchronized(this){
			
			
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				boolean bExist = false;
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
					UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
					if(stUserGameInfoBuilderTemp.getStUserInfo().getUuid() == uuid ){
						if(stUserGameInfoBuilderTemp.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
								&& stUserGameInfoBuilderTemp.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_READY){
							bExist = true;
							break;
						}
					}
				}
				if(bExist){
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
				}
			}else{
				if(bAuto==false && stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1){
					boolean bPlaying = true;
					long lRemainStacks = 0;
					for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
						UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
						if(stUserGameInfoBuilderTemp.getStUserInfo().getUuid() == uuid ){
							if(stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
									|| stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
									|| stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_STANDBY){
								bPlaying = false;
								break;
							}else{
								lRemainStacks = stUserGameInfoBuilderTemp.getLRemainStacks();
							}
						}
					}
					
					
					
					if(bPlaying){
						if(checkAllAllin() == true)
							return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ALLOW_STANDUP;
						
						if(lRemainStacks==0)
							return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ALLOW_STANDUP;
						}
					}	
			}
			
			//站起用户的副本，push使用
			UserGameInfoNet.Builder stUserMyGameInfoNetBuilder = null;
			
			//牌局副本
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			
			int pos = 0;
			boolean bRemoved = false;
			if(this.containUser(uuid)){
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();

					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
						//获取用户副本
						stUserMyGameInfoNetBuilder = stUserGameInfoBuilder.clone();
						
						//保存弃牌状态
						pos = i;
						
						//设置牌局状态
				
						removeUserPos(uuid);
						addStandByUuid(uuid);
						mapStandByUserGameInfos.put(uuid, stUserGameInfoBuilder.clone());
						mPlayers.remove(uuid);
						
						//设置牌局中用户状态，站起操作
						UserBaseInfoNet.Builder stUserBaseInfoBuilder = stUserGameInfoBuilder.getStUserInfoBuilder();
						stUserBaseInfoBuilder.setUuid(-1);
						stUserGameInfoBuilder.setStUserInfo(stUserBaseInfoBuilder);
						stGameRoomInfoBuilder.setVUserGameInfos(i,stUserGameInfoBuilder);
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						bRemoved = true;

						//如果用户正在打牌，则需要要先完成弃牌操作
						if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1 
								&& stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE
								&& stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result_VALUE
								&& stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE
								&& stUserMyGameInfoNetBuilder.getEGameState().getNumber() != USER_GAME_STATE.USER_GAME_STATE_WAIT_VALUE){
							if(bAllAllin == false){
								if(checkAllAllin()!=true){
									if(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING){
										doFlodAction(uuid , pos);
									}else{
										//如果用户还参与牌局，并且没有轮到他打牌，则默认直接设置为弃牌状态即可
										if(stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT 
												&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_STANDBY
												&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
												&& stUserGameInfoBuilder.getLRemainStacks()!=0){
											stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
											stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_FOLD);
											stGameRoomInfoBuilder.setVUserGameInfos(i,stUserGameInfoBuilder);
											stGameRoomInfo = stGameRoomInfoBuilder.build();
											
						                    //主动弃牌日志
											TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
													+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
													+"," + stGameRoomInfo.getLGameHandID()
													+"," + uuid
													+"," + "fold" 
													+"," + 0);
											
											int victoryUser = iGameOver();
											if(victoryUser != -1){		
												//如果已经决出胜负，则进入结算阶段
												flodWinProcess();
												
												TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
														+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
														+"," + stGameRoomInfo.getLGameHandID()
														+"," + uuid
														+"," + "standup" 
														+"," + 0);
												
												sendAllGameStateChange(uuid,null);
											}
										}else{
											
										}
									}
								}else{
									if(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING){
										doFlodAction(uuid , pos);
									}else{
										SidePot();
										resetStepStatus();
									}
								}
							}
						}
						
						//重新计算牌局参与人数
						if(stUserGameInfoBuilder.getLRemainStacks() != 0){
							if(stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart
									&&stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop
									&&stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop
									&&stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn
									&&stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River){
								iCouldPlayNum--;
								if(iCouldPlayNum < 0){
									iCouldPlayNum = 0;
								}
							}
						}
						break;
					}
				}
				
				stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				if(bRemoved==false){
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
				}
				
				//各种站起push
				if(stUserMyGameInfoNetBuilder!=null){
						for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
							UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

							if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid){			
								pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stUserGameInfoNet.getStUserInfo().getUuid(),
										GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP,pos);
							}
						}
						
						for (long standUuid  : getStandByUuids()) {
							if(standUuid > 0){
								pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),standUuid,
										GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP,pos);
							}
						}
						
						if(bAuto){
							pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(),
									GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP,pos);
						}
				}
				
				//APP push如果当时是满桌的话，就push，已经站起一个用户，所以说明如果只有1个空位，则push
				int iUserNums = 0;
				for(UserGameInfoNet stUserGameInfoNet:stGameRoomInfo.getVUserGameInfosList()){
					if(stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
						iUserNums++;
					}
				}
				
				if(iUserNums == (stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums()-1)){
					pushAppPushGameSeatEmpty();
				}
				stGameRoomInfo = stGameRoomInfoBuilder.build();
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
			}
		}
	}

	public ERROR_CODE_TYPE pauseGame(long uuid){
		synchronized(this){
			if(getGameStarted() == true){
				if(uuid == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
					
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();
					stGameRoomBaseInfoBuilder.setLGamePauseTime(System.currentTimeMillis());
					stGameRoomBaseInfoBuilder.setBGamePause(1);
					stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					bPause = true;
					pushAllGameRoomUserMsgs(null,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_PAUSE,stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
					InsuranceApi.setRoomPause(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
					
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS; 
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_OWNER;
				}
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_START;
			}
		}
	}
	
	public ERROR_CODE_TYPE resumeGame(long uuid){
		synchronized(this){
			if(getGameStarted() == true){
				if(uuid == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();
					
					//重新设置时间
					long lEclipseTime = (System.currentTimeMillis() - stGameRoomBaseInfoBuilder.getLGamePauseTime())/1000;
					//正在动作的用户延时
					for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
						UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
						if(stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING){
							this.delayTimeout(stUserGameInfoBuilderTemp.getStUserInfo().getUuid(), lEclipseTime);
							break;
						}
					}
			
					stGameRoomBaseInfoBuilder.setLGamePauseTime(0);
					stGameRoomBaseInfoBuilder.setBGamePause(0);
					stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					bPause = false;
					InsuranceApi.setRoomPlay(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
					pushAllGameRoomUserMsgs(null,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESUME,0);
					
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS; 
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_OWNER;
				}
				
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_START;
			}
		}
	}
	
	public UserGameInfoNet leaveGameRoom(long uuid){
		synchronized(this){
			
			if(getStandByUuids().contains(uuid)){
				removeStandByUuid(uuid);
			}
			
			return null;
		}
	}
	
	
	public ERROR_CODE_TYPE  resetBuyinState(int bBuyinControl){
		synchronized(this){
            if(bBuyinControl == 1){
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                    stGameRoomBaseInfoBuilder.setBBuyinControl(1);
                    stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                    setStGameRoomInfo(stGameRoomInfoBuilder.build());
                    enableBuyinControl();
            }else if(bBuyinControl == 0){
                    GameRoomInfo.Builder stGameRoomInfoBuilder = getStGameRoomInfo().toBuilder();
                    GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                    stGameRoomBaseInfoBuilder.setBBuyinControl(0);
                    stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                    setStGameRoomInfo(stGameRoomInfoBuilder.build());
                    disableBuyinControl();
            }
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		}
	}
		
	public ERROR_CODE_TYPE  buyBuyinControl(long uuid,long lStacks){
		synchronized(this){
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			
			if(mBuyinControlInfo.containsKey(uuid)){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_ALREADY_INVITE_BUYIN;
			}
			
			long popularity = (100+GameRoomDetailInfo.getServiceFeeRat((int)stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()))*lStacks/100;
			
			//首先获取用户的基本信息
			UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

			
			if(stUserBaseInfoNetBuilder!=null && stUserBaseInfoNetBuilder.getLPopularity() >= popularity){
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				
				for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
					//找到购买的筹码
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
						long lCreateTime = System.currentTimeMillis();
						stUserGameInfoBuilder.setBWaitingBuyinConfirmation(1);
						stUserGameInfoBuilder.setLBuyinTime(lCreateTime);
						stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						mBuyinControlInfo.put(uuid, lStacks);
						mBuyinControlTimes.put(uuid, lCreateTime);
						mBuyinControlTimeOut.setCurrent(uuid, uuid);
						
						BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
						stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
						stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
						stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_INVITE);
						stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
						stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
						stBuyinControlMsgBuilder.setLActionUuid(uuid);
						GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
								, stBuyinControlMsgBuilder.build());
						
						pushAppPushBuyinInvite(uuid);
						return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
					}
				}
				
				UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);	
				if(stUserGameInfoBuilder!=null){
					long lCreateTime = System.currentTimeMillis();
					stUserGameInfoBuilder.setBWaitingBuyinConfirmation(1);
					stUserGameInfoBuilder.setLBuyinTime(lCreateTime);
					mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
					mBuyinControlInfo.put(uuid, lStacks);
					mBuyinControlTimes.put(uuid, lCreateTime);
					mBuyinControlTimeOut.setCurrent(uuid, uuid);
					
					BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
					stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
					stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
					stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_INVITE);
					stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
					stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					stBuyinControlMsgBuilder.setLActionUuid(uuid);
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
					
					
					pushAppPushBuyinInvite(uuid);
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}
				

				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
			}
		}
	}
	
	public ERROR_CODE_TYPE  buyinControlActionAccept(long uuid){
		synchronized(this){
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			
			//如果不存在请求，则显示错误
			if(!mBuyinControlInfo.containsKey(uuid)){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_INVITE_BUYIN;
			}
			
			long lStacks = mBuyinControlInfo.get(uuid);
			mBuyinControlInfo.remove(uuid);
			mBuyinControlTimes.remove(uuid);
			mBuyinControlTimeOut.removeKey(uuid);
			//发送成功的push
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
				//找到购买的筹码
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){

					long lCreateTime = System.currentTimeMillis();
					stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
					stUserGameInfoBuilder.setLBuyinTime(0);
					stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					
					ERROR_CODE_TYPE eErrorCodeType = buyBuyin(uuid, lStacks);
					
					if(eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY
							||eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS){
						eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYINCONTROL_NO_MONEY;
						BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
						stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
						stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
						stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
						stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
						stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
						stBuyinControlMsgBuilder.setLActionUuid(uuid);
						
						GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid , stBuyinControlMsgBuilder.build());
						
						GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
								, stBuyinControlMsgBuilder.build());
						
						if(stUserGameInfoBuilder.getLRemainStacks() == 0 && 
								(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD 
								||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
								||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)){
							this.doStandUpAction(uuid, true);
						}
					}
					else{
						BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
						stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
						stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
						stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
						stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ACCEPT);
						stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
						stBuyinControlMsgBuilder.setLActionUuid(uuid);
						
						GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid , stBuyinControlMsgBuilder.build());
						
						GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
								, stBuyinControlMsgBuilder.build());
					}
					

					return eErrorCodeType;
				}
			}
			UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);	
			if(stUserGameInfoBuilder!=null){
				long lCreateTime = System.currentTimeMillis();
				stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
				stUserGameInfoBuilder.setLBuyinTime(0);
				mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
				
				ERROR_CODE_TYPE eErrorCodeType = buyBuyin(uuid, lStacks);
				
				if(eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY
						||eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS){
					eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYINCONTROL_NO_MONEY;
					BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
					stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
					stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
					stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
					stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
					stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					stBuyinControlMsgBuilder.setLActionUuid(uuid);
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid , stBuyinControlMsgBuilder.build());
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
				}
				else{
					BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
					stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
					stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
					stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ACCEPT);
					stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
					stBuyinControlMsgBuilder.setLActionUuid(uuid);
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid
							, stBuyinControlMsgBuilder.build());
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
				}
				
				return eErrorCodeType;
			}		
			
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
		}
	}
	
	public ERROR_CODE_TYPE  buyinControlActionDeny(long uuid,boolean auto){
		synchronized(this){
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			
			//如果不存在请求，则显示错误
			if(!mBuyinControlInfo.containsKey(uuid)){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_INVITE_BUYIN;
			}
			
			long lStacks = mBuyinControlInfo.get(uuid);
					
			mBuyinControlInfo.remove(uuid);
			mBuyinControlTimes.remove(uuid);
			mBuyinControlTimeOut.removeKey(uuid);
	
			//发送成功的push
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
				//找到购买的筹码
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
					long lCreateTime = System.currentTimeMillis();
					stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
					stUserGameInfoBuilder.setLBuyinTime(0);
					stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					
					BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
					stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
					stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
					stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
					stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					stBuyinControlMsgBuilder.setLActionUuid(uuid);
					stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid
							, stBuyinControlMsgBuilder.build());
					
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
					if(stUserGameInfoBuilder.getLRemainStacks() == 0 && 
							(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD 
							||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
							||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)){
						this.doStandUpAction(uuid, true);
					}
					
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}
			}
			
			UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);	
			if(stUserGameInfoBuilder!=null){
				long lCreateTime = System.currentTimeMillis();
				stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
				stUserGameInfoBuilder.setLBuyinTime(0);
				mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
				
				BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
				stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
				stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
				stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
				stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
				stBuyinControlMsgBuilder.setLActionUuid(uuid);
				stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
				
				//给申请人发push消息
				GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid
						, stBuyinControlMsgBuilder.build());
				
				//给房主发push消息
				GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
						, stBuyinControlMsgBuilder.build());
				
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			}else{
				if(auto){
					BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
					stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
					stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
					stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
					stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					stBuyinControlMsgBuilder.setLActionUuid(uuid);
					
					//给申请人发push消息
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(uuid
							, stBuyinControlMsgBuilder.build());
					
					//给房主发push消息
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
					
				}
			}

			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
		}
	}
	
	public ERROR_CODE_TYPE  disableBuyinControl(){
		synchronized(this){
			//如果不存在请求，则显示错误
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			
			Iterator iter = mBuyinControlInfo.entrySet().iterator(); 
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next(); 
				Long uuid = (Long) entry.getKey();
				
				//发送成功的push
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
					//找到购买的筹码
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
						stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
						stUserGameInfoBuilder.setLBuyinTime(0);
						stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
						stGameRoomInfo = stGameRoomInfoBuilder.build();
					}
				}
				
				UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);	
				if(stUserGameInfoBuilder!=null){
					stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
					stUserGameInfoBuilder.setLBuyinTime(0);
					mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
				}		
				
				
				stGameRoomInfo = stGameRoomInfoBuilder.build();
				
				mBuyinControlInfo.remove(uuid);
				mBuyinControlTimes.remove(uuid);
				mBuyinControlTimeOut.removeKey(uuid);
			}
			
			//发送成功的push
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
				//找到购买的筹码
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
								
				BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
				stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
				stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DISABLE);
				stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0){
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stUserGameInfoBuilder.getStUserInfo().getUuid()
							, stBuyinControlMsgBuilder.build());
			
					if(stUserGameInfoBuilder.getLRemainStacks() == 0 && 	
							(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD 
							||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
							||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)){
						this.doStandUpAction(stUserGameInfoBuilder.getStUserInfo().getUuid(), true);
					}
				}
			}
				
					
			for (long push_uuid  : getStandByUuids()) {		
				BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
				stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
				stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DISABLE);
	//							stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
				stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					
				GameRoomMsgManager.getInstance().sendBuyinControlMsg(push_uuid
							, stBuyinControlMsgBuilder.build());
			}		
			
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		}
	}
	
	public ERROR_CODE_TYPE  enableBuyinControl(){
		synchronized(this){
			
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			//发送成功的push
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
				//找到购买的筹码
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
								
				BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
				stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
				stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ENABLE);
				stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0)
					GameRoomMsgManager.getInstance().sendBuyinControlMsg(stUserGameInfoBuilder.getStUserInfo().getUuid()
								, stBuyinControlMsgBuilder.build());
			}
				
					
			for (long push_uuid  : getStandByUuids()) {		
				BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
				stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
				stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ENABLE);
				stBuyinControlMsgBuilder.setStGameRoomInfo(stGameRoomInfo);
					
				GameRoomMsgManager.getInstance().sendBuyinControlMsg(push_uuid
							, stBuyinControlMsgBuilder.build());
			}		
			
			
					
			stGameRoomInfo = stGameRoomInfoBuilder.build();
				
			mBuyinControlInfo.clear();
			mBuyinControlTimes.clear();
			mBuyinControlTimeOut.clear();
			
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		}
	}
	
	/**
	 * 用户坐下
	 * @param uuid	坐下用户的uuid
	 * @param lStacks	买入的金额
	 */
	public ERROR_CODE_TYPE  buyBuyin(long uuid,long lStacks){
		synchronized(this){
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			// 生成银币扣除数据
			long popularity = 0;
			
			UserGameInfoNet.Builder stUserMyGameInfoNetBuilder = null;
			
			int iPos=0;
			
			boolean bSetted = false;
			
			ERROR_CODE_TYPE eErrorCodeType;
			
			popularity = (100+GameRoomDetailInfo.getServiceFeeRat((int)stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()))*lStacks/100;
			
			//首先获取用户的基本信息
			UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
			
			if(stUserBaseInfoNetBuilder!=null && stUserBaseInfoNetBuilder.getLPopularity() >= popularity){
				eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				//获取要增加筹码的用户
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
					//找到购买的筹码
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
						stUserMyGameInfoNetBuilder = stUserGameInfoBuilder;
						iPos = i;
						
						//如果这个是弃牌或者不在牌局里面，则直接加盲注
						if(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD 
								||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
								||(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY && getGameStarted() == false)
								||(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY && this.iCouldPlayNum == 1)){
							
							
							//玩家购买的筹码
							if((stUserGameInfoBuilder.getLRemainStacks() +  lStacks) < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio()/10+1)*stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()){
								long buyStacks = lStacks;
								stUserGameInfoBuilder.setLRemainStacks(buyStacks+ stUserGameInfoBuilder.getLRemainStacks());
								stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
								//修改账单信息
								BillManager.getInstance().buyStackInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
										stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), buyStacks);
								
								if(stUserGameInfoBuilder!=null){
									BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(), 
											stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserGameInfoBuilder.getLRemainStacks());
								}
								//减去用户的银币
								stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);							
								String sLog = uuid + " " + 0 + " " + 110000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() 
								+ " 0 " + " 0 " + lStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() +" " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
//								mLog.error(sLog);
								TranscationLogService.getInstance().error(sLog);
								ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLChatRoomID());
			
								if(stClubInfoBuilder!=null && stClubInfoBuilder.getStClubBaseInfo().getLClubID() > 0){									
									//添加俱乐部积分
									stClubInfoBuilder = ClubManager.getInstance()
											.addClubScore(stClubInfoBuilder.getStClubBaseInfo().getLClubID(), 5);
								}
								
								stGameRoomInfo = stGameRoomInfoBuilder.build();
								if(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE == stGameRoomInfoBuilder.getEGameState().getNumber()){
									resetGameRoom();
									stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
								}
							}else{
								eErrorCodeType =  ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS;
							}
						}else{
							//把好友状态从游戏玩家列表中删除
							stUserGameInfoBuilder.setBBuyin(1);
							stUserGameInfoBuilder.setLBuyinStacks((int)lStacks);
							stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
							stGameRoomInfo = stGameRoomInfoBuilder.build();
							if(stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result ){
//								resetGameRoom();
								stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
							}
							bSetted = true;
						}
						break;
					}
				} 
				
				if(bSetted==false){
					//查看所有在旁观队列
					if(getStandByUuids().contains(uuid)){
						UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);
						if(stUserGameInfoBuilder!=null){
							stUserMyGameInfoNetBuilder = stUserGameInfoBuilder;
							if((stUserGameInfoBuilder.getLRemainStacks() +  lStacks) < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio()/10+1)*stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()){
								//玩家购买的筹码
								long buyStacks = lStacks;
								stUserGameInfoBuilder.setLRemainStacks(buyStacks + stUserGameInfoBuilder.getLRemainStacks());
								stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);

								//修改账单信息
								BillManager.getInstance().buyStackInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
										stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), buyStacks);
								
								if(stUserGameInfoBuilder!=null){
									BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(), 
											stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserGameInfoBuilder.getLRemainStacks());
								}
								
								//减去用户的银币
								stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);
								
								String sLog = uuid + " " + 0 + " " + 110000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() 
										+ " 0 " + " 0 " + lStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() +" " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
								TranscationLogService.getInstance().error(sLog);
								
								ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLChatRoomID());
								if(stClubInfoBuilder!=null){
									//添加俱乐部积分
									stClubInfoBuilder = ClubManager.getInstance()
											.addClubScore(stClubInfoBuilder.getStClubBaseInfo().getLClubID(), 5);
								}								
							}else{
								eErrorCodeType =  ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS;
							}
						}
					}
					
				}
				
				//如果房间没有坐下的话，会push消息
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
					UserGameInfoNet stUserGameInfoSendNet = stGameRoomInfo.getVUserGameInfos(i);
					
					if(stUserGameInfoSendNet.getStUserInfo().getUuid() == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
						//首先找到离开房间的用户基本信息				
						pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stUserGameInfoSendNet.getStUserInfo().getUuid(),
								GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUYIN,iPos);
						break;
					}
				}
					
				if(mapStandByUserGameInfos.get(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid())!=null){
					//首先找到离开房间的用户基本信息	
					pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(),
							GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUYIN,iPos);
				}
			}else{
				eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
			}
			return eErrorCodeType;
		}
	}
	
	public ERROR_CODE_TYPE  sitdown(long uuid,int iPos){
		synchronized(this){
			if(this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			
			if(containUser(uuid)){
				//如果用户已经存在房间了，就直接返回
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
			}
			
			ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
			
			//坐下用户的信息
			UserGameInfoNet.Builder stUserMyGameInfoNetBuilder = null;

			if(iPos >= 0 && iPos < iGameRoomUserMaxNums){
				//获取牌局信息
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				
				//找到的需要坐下的位置
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(iPos).toBuilder();
		
				//该位置可以坐下
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() == 0 || stUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
					//获取该玩家站起前的游戏信息
					stUserMyGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
						
					//如果玩家在旁观的玩家中，则开始设置坐下玩家信息
					if(stUserMyGameInfoNetBuilder!=null){
						//获取人气值，查看是否还有足够的人气值
						UserBaseInfoNet.Builder stUserBaseInfoBuilder =  UserManager.getInstance().getUserBaseInfo(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());

						if(inNoChargeList(stUserBaseInfoBuilder.getUuid()) || stUserBaseInfoBuilder.getUuid() == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
							//先检查是否在别的游戏中

							//不需要付费
							stUserMyGameInfoNetBuilder.clearVCurrentHands();
							stUserMyGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
							stUserMyGameInfoNetBuilder.setBGameDealer(0);
							stUserMyGameInfoNetBuilder.setBJustSit(1);
								
							UserGameInfoNet.Builder stUserMyTempGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(iPos);	
							if(stUserMyTempGameInfoNetBuilder!=null){
								stUserMyGameInfoNetBuilder.setEGameState(stUserMyTempGameInfoNetBuilder.getEGameState());
								stUserMyGameInfoNetBuilder.setLBetStacks(stUserMyTempGameInfoNetBuilder.getLBetStacks());
							}
							//在该位置下坐下
							stGameRoomInfoBuilder.setVUserGameInfos(iPos, stUserMyGameInfoNetBuilder);
									
							//设置座位和用户的对应关系
							mPlayers.add(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());
							setUserPos(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), iPos);
								
								
							//从旁观人中删除该用户
							removeStandByUuid(uuid);
									
							stGameRoomInfo =  stGameRoomInfoBuilder.build();
								
							UserManager.getInstance().setUserInTable(uuid,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
							
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
						}else{															
							stUserMyGameInfoNetBuilder.clearVCurrentHands();
							stUserMyGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
							stUserMyGameInfoNetBuilder.setBGameDealer(0);
							stUserMyGameInfoNetBuilder.setStUserInfo(stUserBaseInfoBuilder);
							stUserMyGameInfoNetBuilder.setBJustSit(1);
								
							UserGameInfoNet.Builder stUserMyTempGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(iPos);
							if(stUserMyTempGameInfoNetBuilder!=null){
								stUserMyGameInfoNetBuilder.setEGameState(stUserMyTempGameInfoNetBuilder.getEGameState());
								stUserMyGameInfoNetBuilder.setLBetStacks(stUserMyTempGameInfoNetBuilder.getLBetStacks());
							}
								
							//在该位置下坐下
							stGameRoomInfoBuilder.setVUserGameInfos(iPos, stUserMyGameInfoNetBuilder);
									
							//设置座位和用户的对应关系
							mPlayers.add(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());
		
							setUserPos(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), iPos);
								
							//从旁观人中删除该用户
							removeStandByUuid(uuid);
									
							stGameRoomInfo =  stGameRoomInfoBuilder.build();
								
							//把用户放到nocahrge队列里面
							addNorChargeList(stUserBaseInfoBuilder.getUuid());
								
							UserManager.getInstance().setUserInTable(uuid,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
								
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
						}
					}
					else{
						eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
					}
				}else{
					eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS;
				}
			}else{
				eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS;
			}
			
			if(stUserMyGameInfoNetBuilder!=null){
				//通知房间的其他人有人离开房间了
				if(iPos >= 0 && iPos < iGameRoomUserMaxNums && eErrorCode == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					//给在牌局的人push
					for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
						UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
						
						if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid){
							//首先找到离开房间的用户基本信息				
							pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stUserGameInfoNet.getStUserInfo().getUuid(),
									GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SITDOWN,iPos);
						}
					}
					
					//给旁观的人push
					for (long push_uuid  : getStandByUuids()) {
						UserBaseInfoNet stUserBaseInfoNet = mapStandByUserGameInfos.get(push_uuid).getStUserInfo();
						
						pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(),stUserBaseInfoNet.getUuid(),
								GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SITDOWN,iPos);
					}
				}
			}
			return eErrorCode;
		}
	}

	//检查buyin请求的超时
	public void checkBuyinControlTimeout(){
		ArrayList<Long> timeoutArray = mBuyinControlTimeOut.timeout();
		if(timeoutArray!=null){
			for(int i = 0 ; i < timeoutArray.size() ; i++){
				Long uuid = (Long) timeoutArray.get(i);
				if(uuid!=null){			
					this.buyinControlActionDeny(uuid,true);
				}
			}
		}
	}
	
	//检查buyin请求的超时
	public void checkStandByTimeout(){
		ArrayList<Long> timeoutArray = mStandByTimeOut.timeout();
		if(timeoutArray!=null){
			for(int i = 0 ; i < timeoutArray.size() ; i++){
				Long uuid = (Long) timeoutArray.get(i);
				removeStandByUuid(uuid);
			}
		}
	}
	
	public void gameRoomTimeoutHandle(TimeoutHashMap<Long,GameRoomDetailInfo> mGameRoomDetailInfos){
		synchronized(this){			
			if(stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1){
				if((System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLGamePauseTime())/1000 > 600){
					this.resumeGame(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
				}
				return;
			}
			
			//先检查买入超时
			checkBuyinControlTimeout();
			
			checkStandByTimeout();
			
			//首先判断每个房间的游戏状态
			switch(stGameRoomInfo.getEGameState().getNumber()){					
			//如果游戏状态是等待，则需要当进入房间人数大于等于2是，开始牌局
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE:
			{
				long interval = System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime();

				if(System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime() > stGameRoomInfo.getStGameRoomBaseInfo().getLDuration()*1000){
					if(bOver == true){
						bOver = true;
						mGameRoomDetailInfos.removeKey(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
					}else{						
						TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
								+"," +stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
								+"," + stGameRoomInfo.getLGameHandID()
								+"," + 0
								+"," + "closetable" );
												
						BillManager.getInstance().finishGameRoomAndReturn(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						
						pushAllGameRoomUserMsgs(null,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_OVER);
						bOver = true;
						//房间到时，删除房间
						ConnectDatabase.getInstance().removeGameRoomInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						ConnectDatabase.getInstance().removeGameRoomMember(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						UserManager.getInstance().removeUserCreateGameRooms(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
								,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						
						ChatRoomManager.getInstance().removeChatRoomGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID(),stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						ClubManager.getInstance().removeChatRoomGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID(),stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						
						BillManager.getInstance().setGameOverTag(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),lMaxPot,mGameHandID);
						
						
						QuickGameRoomManager.getInstance().finishQuickGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID());
						mGameRoomDetailInfos.removeKey(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
							
						List<Long> vPushUsers = BillManager.getInstance().getBillInfoUsers(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
						for(long uuid:vPushUsers){
							pushAppPushGameFinish(uuid);
						}
					}

					break;
				}
				else{
					//房间没有到时见，继续游戏
					//等待10秒钟以后，如果牌局里面大于两个人，需要把游戏状态改成start，并通知改牌局的所有玩家
					resetGameRoom();
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					if(iCouldPlayNum>= 2 && getGameStarted() == true){
						//首先设置牌局的状态
						stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart);
						stGameRoomInfoBuilder.setBStateChange(1);	
						mGameHandID++;
						stGameRoomInfoBuilder.setLGameHandID(mGameHandID);
						
						//最后把最新的牌局信息push给每一个进入牌局的用户
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						sendAllGameStateChange(0,null);
					}
					
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
				}
			}
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart_VALUE:{
				resetGameRoom();
				reCalculateCouldPlayNum();
			
				if(iCouldPlayNum >=2){
					TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							+"," + stGameRoomInfo.getLGameHandID()
							+",startround"
							+","+stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()
							+","+stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()
							+","+stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
					
					saveOriginalStacks();
					
					//重置Dealer位置
					rsetetDealerPos();
					
					//重新可以进行牌局的用户，去掉刚坐在小盲位的用户
					couldPlayNumAfterSetDealer();
					
					//每个选手发两张牌,并把游戏的状态设置为FirstRound	
					beforeTheGame();
					if(iCouldPlayNum>=2){
						//每个选手发两张牌,并把游戏的状态设置为FirstRound	
						beforeTheGame();
						if(this.stGameRoomInfo.getStGameRoomBaseInfo().getIAnte() > 0 ){
							gotoAnteStatus();
						}
					
						gotoStartStatus();
					}else{
						//重置刚坐下的状态
						rsetetJustSitTag();
						
						//重新可以进行牌局的用户，去掉刚坐在小盲位的用户
						couldPlayNumAfterSetDealer();
						
						//每个选手发两张牌,并把游戏的状态设置为FirstRound	
						beforeTheGame();
						if(this.stGameRoomInfo.getStGameRoomBaseInfo().getIAnte() > 0 ){
							gotoAnteStatus();
						}
					
						gotoStartStatus();
					}
				}
			}
			break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result_VALUE:{
				//从result状态需要编导wait状态，并且清空整个房间
				//重置整个房间的状态
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD);
				stGameRoomInfo = stGameRoomInfoBuilder.build();
				
				removeAllTimeOutUser();
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
				
				sendAllGameStateChange(0,null);
			}
			
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE:
				resetGameRoom();
				
				TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
						+"," +stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
						+"," + stGameRoomInfo.getLGameHandID()
						+"," + 0
						+"," + "cleartable" );
				removeAllTimeOutUser();
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:{
				//检测机器人自动弃牌
				robotFlod();
				//如果allin状态，则发牌跳转到下一个状态
				if(stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true){
					gotoFlopAllStatus(0);
				}

			}
			mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One_VALUE:
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two_VALUE:
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three_VALUE:{				
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
			}
			break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:{
				System.out.println("ROOM_GAME_STATE_Flop_VALUE");
				if(checkAllAllin()){
					checkGotoTurnStatus(0);
				}else{
					//检测机器人自动弃牌
					robotFlod();
				}
				
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
			}
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:{
				System.out.println("ROOM_GAME_STATE_Turn_VALUE");
				if(checkAllAllin()){
					checkGotoRiverStatus(0);
				}else{
					//检测机器人自动弃牌
					robotFlod();
				}
				
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
			}
				break;
			case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:{
				System.out.println("ROOM_GAME_STATE_River_VALUE");
				if(checkAllAllin()){
					gotoResultStatus(0);
				}else{
					//检测机器人自动弃牌
					robotFlod();
				}

				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
			}
				break;
			default:
				mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
				break;
			}
		}
	}
	
	/**
	 * 每首结束，重置牌局状态
	 */
	private void resetGameRoom(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		iCouldPlayNum=0;		
		bAllAllin = false;
		lStepMaxStacks = 0;
		stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
		mHandMap.clear();
		vHandList.clear();
		mHandPos.clear();
		
		//���������������������������������������������
		stGameRoomInfoBuilder.clearVCurrentCards();
		stGameRoomInfoBuilder.clearVCurrentPots();
		stGameRoomInfoBuilder.clearVPotInfos();
		stGameRoomInfoBuilder.clearStGameResultInfo();
		
		stGameRoomInfoBuilder.setBAllAllin(0);
		//���������������������������������
		for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
			UserGameInfoNet.Builder stUserGameInfoNetBuilder =  stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
			stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
			stUserGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
			stUserGameInfoNetBuilder.setBGameDealer(0);
			stUserGameInfoNetBuilder.clearVCurrentHands();
			stUserGameInfoNetBuilder.setLBetStacks(0);
			stUserGameInfoNetBuilder.setIDelayLong(0);
			stUserGameInfoNetBuilder.setIDelaySingleTime(20);
			
			if(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() == -1){
				UserBaseInfoNet.Builder stUserBaseInfoBuilder = stUserGameInfoNetBuilder.getStUserInfoBuilder();
				stUserBaseInfoBuilder.setUuid(0);
				stUserGameInfoNetBuilder.setStUserInfo(stUserBaseInfoBuilder);
			}
			else if(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() > 0){
				stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
				
				//重置延时道具
				if(isDelayPlayer(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid())){
					stUserGameInfoNetBuilder.setIDelayTotalTimes(1);
					stUserGameInfoNetBuilder.setIDelayCost(getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
				}else{
					stUserGameInfoNetBuilder.setIDelayTotalTimes(0);
					stUserGameInfoNetBuilder.setIDelayCost(getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
				}
				stUserGameInfoNetBuilder.setIDelayLong(0);
				stUserGameInfoNetBuilder.setIDelaySingleTime(20);
				stUserGameInfoNetBuilder.setBShowCardOne(0);
				stUserGameInfoNetBuilder.setBShowCardTwo(0);
				
				if(stUserGameInfoNetBuilder.getBBuyin() ==1){
					iCouldPlayNum++;
					UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stUserGameInfoNetBuilder.getStUserInfo().getUuid());
					//������������������
					
						// ������������������������
						long popularity = 0;
						
						popularity = (100+GameRoomDetailInfo.getServiceFeeRat((int)stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()))*stUserGameInfoNetBuilder.getLBuyinStacks()/100;
						
						if(stUserBaseInfoNetBuilder!= null && stUserBaseInfoNetBuilder.getLPopularity() >= popularity){
							long buyStacks = stUserGameInfoNetBuilder.getLBuyinStacks();
							
							if(stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio()/10+1)*stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()){
								stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks()+buyStacks);
								
								//������������������
								BillManager.getInstance().buyStackInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
										stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), buyStacks);
								
								if(stUserGameInfoNetBuilder!=null){
									BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), 
											stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserGameInfoNetBuilder.getLRemainStacks());
								}
								
								//���������������������
								stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), - popularity);
								
								String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 110000 + " " 
								+ stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " 0 " + " 0 " 
										+ buyStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() +" " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
								TranscationLogService.getInstance().error(sLog);
								ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID());
								if(stClubInfoBuilder!=null && stClubInfoBuilder.getStClubBaseInfo().getLClubID() > 0){
									//添加俱乐部积分
									stClubInfoBuilder = ClubManager.getInstance()
											.addClubScore(stClubInfoBuilder.getStClubBaseInfo().getLClubID(), 5);
								}
							}
						}
					
					stUserGameInfoNetBuilder.setBBuyin(0);					
				}else if(stUserGameInfoNetBuilder.getLRemainStacks() == 0){
					stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
				}else{
					iCouldPlayNum++;
				}
			}
			
			stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
		}
		
		stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
		
		mPotList.clear();
		resetCard();
		//������������������������
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		
	}
	
	private void resetStepStatus(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		if(iCouldPlayNum== 2){
			mEndPos =  mBigBlindPos;
			lStepMaxStacks = 0;
			stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
			//reset������
			for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
				UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
				
				if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
					
					if(stResetUserGameInfoBuilder.getLRemainStacks() > 0){
						if(mBigBlindPos == k){
							//���������������������������
							if(bAllAllin==false){
								stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
								stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
								stResetUserGameInfoBuilder.setIDelayLong(0);
								stResetUserGameInfoBuilder.setIDelaySingleTime(20);
								
								removeAllTimeOutUser();
								setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
							}
						}else{
							if(bAllAllin==true){
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
							}
						}
					}else{
						stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
						
					}
					
					stResetUserGameInfoBuilder.setLBetStacks(0);
					
					if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
						UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
						stUserBaseInfo.setUuid(0);
						stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
					}
				}
				
				//������������������������������������
				stResetUserGameInfoBuilder.setLBetStacks(0);
				//������������gameroominfo���������������������
				stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
			}
		}else{
			mEndPos = mSmallBlindPos;
			lStepMaxStacks = 0;
			stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
			
			int iBetting = 0;
			int iLoop1 = 0;

			//���������������������������������������
			for(int k = mSmallBlindPos ; iLoop1 < iGameRoomUserMaxNums;iLoop1++,k++ ){
				int userIndex2 = 0;
				if(k<iGameRoomUserMaxNums){
					userIndex2 = k;
				}else{
					userIndex2 = k - iGameRoomUserMaxNums;
				}
				
				UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex2).toBuilder();
				if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
						&& stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
						&& stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){
						
						if(stResetUserGameInfoBuilder.getLRemainStacks() > 0){
							if(iBetting==0){
								//���������������������������
								if(bAllAllin==false){
									stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
									stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

									stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
									stResetUserGameInfoBuilder.setIDelayLong(0);
									stResetUserGameInfoBuilder.setIDelaySingleTime(20);
									
									removeAllTimeOutUser();
									setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
									iBetting = 1;
								}
							}else{
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
							}
						}
						else{
							stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
						}
						
						if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
							UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
							stUserBaseInfo.setUuid(0);
							stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
						}
				}
				
				//������������������������������������
				stResetUserGameInfoBuilder.setLBetStacks(0);
			
				//������������gameroominfo���������������������
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
			}
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}

	private void resetGameUserStatus(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		if(iCouldPlayNum== 2){
			stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
			//reset������
			for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
				UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
				
				if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
					
					if(stResetUserGameInfoBuilder.getLRemainStacks() > 0){
						if(mBigBlindPos == k){
							//���������������������������
							if(bAllAllin==false){
								stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
								stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
								stResetUserGameInfoBuilder.setIDelayLong(0);
								stResetUserGameInfoBuilder.setIDelaySingleTime(20);
								
								
								removeAllTimeOutUser();
								setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
							}
							
						}else{
							if(bAllAllin==true){
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
							}
						}
					}else{
						stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
						
					}
					
					stResetUserGameInfoBuilder.setLBetStacks(0);
					
					if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
						UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
						stUserBaseInfo.setUuid(0);
						stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
					}
				}
				
				//������������������������������������
				stResetUserGameInfoBuilder.setLBetStacks(0);
				//������������gameroominfo���������������������
				stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
			}
		}else{
			stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
			
			int iBetting = 0;
			int iLoop1 = 0;

			//���������������������������������������
			for(int k = mSmallBlindPos ; iLoop1 < iGameRoomUserMaxNums;iLoop1++,k++ ){
				int userIndex2 = 0;
				if(k<iGameRoomUserMaxNums){
					userIndex2 = k;
				}else{
					userIndex2 = k - iGameRoomUserMaxNums;
				}
				
				UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex2).toBuilder();
				if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
						&& stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
						&& stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){
						
						if(stResetUserGameInfoBuilder.getLRemainStacks() > 0){
							if(iBetting==0){
								//���������������������������
								if(bAllAllin==false){
									stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
									stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

									stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
									stResetUserGameInfoBuilder.setIDelayLong(0);
									stResetUserGameInfoBuilder.setIDelaySingleTime(20);
									
									removeAllTimeOutUser();
									setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
									iBetting = 1;
								}
							}else{
								stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
							}
						}
						else{
							stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
						}
						
						if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
							UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
							stUserBaseInfo.setUuid(0);
							stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
						}
				}
				
				//������������������������������������
				stResetUserGameInfoBuilder.setLBetStacks(0);
			
				//������������gameroominfo���������������������
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
			}
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
	
	private void robotFlod(){
		ArrayList<Long> timeoutUser = getTimeoutUsers();

		if(timeoutUser !=null){
			if(timeoutUser.size() > 0){
				long uuid = timeoutUser.get(0);
				
				if( getTimeoutTimesUser(uuid) > 1){
					removeTimeoutTimesUser(uuid);
					doStandUpAction(uuid,true);
				}else{
					
					
					UserGameInfoNet.Builder stUserGameInfoBuilder = null;
					for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
						UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
						
						if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
							stUserGameInfoBuilder = stResetUserGameInfoBuilder;
							break;
						}
					}
					
					if(stUserGameInfoBuilder !=null){
						if(lStepMaxStacks == stUserGameInfoBuilder.getLBetStacks()){
							addUserTimeoutTimes(uuid);
							doCheckAction(uuid, 0);
						}else{
							addUserTimeoutTimes(uuid);
							doFlodAction(uuid,(long)-1);
						}
					}else{
						addUserTimeoutTimes(uuid);
						doFlodAction(uuid,(long)-1);
					}
				}
			}
		}
	}
	
	private void beforeTheGame(){
		
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		if(iCouldPlayNum > 2){
			
			//大于两个人的时候牌局出事状态的设置
			//然后设置牌局中玩家的状态
			//用户的较色
			int userRole=0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
			//循环次数
			int iLoop=0;
				
			//首先设置纽扣，小盲，大盲位
			for(int j = mDealPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j - iGameRoomUserMaxNums;
				}
				
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
				//判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0&&stUserGameInfoBuilder.getLRemainStacks() > 0){
					//如果是小盲，并且用户刚刚坐下，则用户不能参加牌局
					
					if(stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1){
						//设置游戏的用户状态
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
						stUserGameInfoBuilder.setBGameDealer(0);
						stUserGameInfoBuilder.setIDelayLong(0);
						stUserGameInfoBuilder.setIDelaySingleTime(20);
					}else{
						//设置游戏的用户状态
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
						stUserGameInfoBuilder.setBGameDealer(0);
						stUserGameInfoBuilder.setIDelayLong(0);
						stUserGameInfoBuilder.setIDelaySingleTime(20);

						//设置用户的较色状态
						if(0 == userRole){
							stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_DEAL);
							stUserGameInfoBuilder.setBGameDealer(1);
							//设置dealer的index
							mDealPos = userIndex;
							//寻找下一个小盲注
							userRole++;
						}else if( 1 == userRole){
							//设置小盲的index
							mSmallBlindPos = userIndex;
							stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_SMALL_BLIND);
							//寻找下一个大盲
							userRole++;
							
						}else if(2 == userRole){
							//设置大盲的index
							mBigBlindPos = userIndex;	
							stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
							//大盲为第一轮最后一个说话的人
							mEndPos = userIndex;
							//寻找普通玩家
							userRole++;
						
						}else{
							stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
							stUserGameInfoBuilder.setBGameDealer(0);
						}
						stUserGameInfoBuilder.setBJustSit(0);
					}
					
				}
				else{
					stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
					stUserGameInfoBuilder.setBGameDealer(0);
				}
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex,stUserGameInfoBuilder.build());
			}
			
			
			
			int startIndex = mBigBlindPos + 1;
			
			if(startIndex >= iGameRoomUserMaxNums){
				startIndex -= iGameRoomUserMaxNums;
			}
			iLoop = 0;
			//寻找第一轮的理论结束位的后一位，枪口位
			for(int j = startIndex ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j-iGameRoomUserMaxNums;
				}
				
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
				//判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0){
					//找到大盲位后面的第一个用户的位置
					mEndPos = userIndex;
					break;
				}
			}
			
			//最后把最新的牌局信息push给每一个进入牌局的用户
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			
			
			//把牌局的信息push给每个人
			sendAllGameStateChange(0,null);
		}
		else{
			//如果两个人的时候，牌局初始状态的设置
			int userRole=0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
			//循环次数
			int iLoop=0;
			
			//首先设置纽扣，小盲，大盲位
			for(int j = mDealPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j - iGameRoomUserMaxNums;
				}
				
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
				//判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0&&stUserGameInfoBuilder.getLRemainStacks() > 0){
					//设置游戏的用户状态
					stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
					stUserGameInfoBuilder.setBGameDealer(0);
					stUserGameInfoBuilder.setIDelayLong(0);
					stUserGameInfoBuilder.setIDelaySingleTime(20);
					

					if(stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1){
						//直接跳过，不发牌
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
					}
					//设置用户的较色状态
					else if( 0 == userRole){
						stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_SMALL_BLIND);
						stUserGameInfoBuilder.setBGameDealer(1);
						//设置dealer的index
						mDealPos = userIndex;
						//设置小盲的index
						mSmallBlindPos = userIndex;
						mEndPos = userIndex;
						//寻找下一个小盲注
						userRole++;
						stUserGameInfoBuilder.setBJustSit(0);
					}else if( 1 == userRole){
						//寻找下一个大盲（因为是两个人的牌局，所以大盲就是dealer）
						mBigBlindPos = userIndex;	
						stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
						userRole++;
						stUserGameInfoBuilder.setBJustSit(0);
					}else if(2 == userRole){
						//设置大盲的index
						mBigBlindPos = userIndex;	
						//寻找普通玩家
						userRole++;
					}else{
						stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
						stUserGameInfoBuilder.setBGameDealer(0);
					}
					
				}
				else{
					stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
					stUserGameInfoBuilder.setBGameDealer(0);
				}
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex,stUserGameInfoBuilder.build());
			}
			
		
			
			//最后把最新的牌局信息push给每一个进入牌局的用户
			stGameRoomInfo = stGameRoomInfoBuilder.build();
		}
	}

	/**
	 * 初始化函数
	 */
	public GameRoomDetailInfo(){
		mTimeoutGameUsers = new AdvancedTimeoutHashMap<Long,Long>();
		
		mBuyinControlTimeOut =  new TimeoutHashMap<Long,Long>();
		mBuyinControlTimeOut.setTimeout(150000);
		
		mStandByTimeOut =  new TimeoutHashMap<Long,Long>();  
		mStandByTimeOut.setTimeout(12000);
		
		vTestHandCards1.add(2);
		vTestHandCards1.add(3);
		
		vTestHandCards2.add(26);
		vTestHandCards2.add(51);
		
		vTestTableCards.add(0);
		vTestTableCards.add(7);
		vTestTableCards.add(20);
		vTestTableCards.add(33);
		vTestTableCards.add(46);
	}
	
	public static int getServiceFeeRat(int lBigBlinds){
		switch(lBigBlinds){
		case 1:
		case 5:
		case 10:
			return 10;
		case 25:
		case 50:
		case 100:
		case 500:
			return 7;
		case 5000:
			return 5;
		}
		return 10;
	}
	
	public static int getDelayCost(int times){
		switch(times){
		case 0:
			return 0;
		case 1:
			return 20;
		}
		return 50*(times-1);
	}

	/****************************延时相关函数*******************************/
	public void addDelayPlayer(long uuid){
		mDelayPlayer.put(uuid, uuid);
	}
	
	public boolean isDelayPlayer(long uuid){
		if(mDelayPlayer.get(uuid)==null){
			return false;
		}else{
			return true;
		}
	}
	
	/****************************结算辅助函数*******************************/
	public boolean inNoChargeList(long uuid){
		if(mapUserNoCharge.containsKey(uuid)){
			return true;
		}
		else{
			return false;
		}
	}
	
	public void addNorChargeList(long uuid){
		mapUserNoCharge.put(uuid, uuid);
	}
	
	public void removeNorChargeList(long uuid){
		mapUserNoCharge.remove(uuid);
	}
	
	//牌局结束push
	void pushAppPushGameFinish(long touuid){
		UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(touuid);		
		//获取push相关信息
		UserTokenInfoNet stUserTokenInfoNet = UserManager.getInstance().getUserTokenInfo(touuid);

		if(stUserTokenInfoNet!=null && stFromUserBaseInfoNetBuilder!=null){
			APNManager.getInstance().gameOverPush(touuid, stUserTokenInfoNet.getStrAPNToken(), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 1);
		}
	}
	
	void pushAppPushBuyinInvite(long touuid){
		UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(touuid);		
		UserTokenInfoNet stUserTokenInfoNet = UserManager.getInstance().getUserTokenInfo(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());

        if(stUserTokenInfoNet!=null && stFromUserBaseInfoNetBuilder!=null){
        	APNManager.getInstance().buyinPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
        			, stUserTokenInfoNet.getStrAPNToken()
        			, stFromUserBaseInfoNetBuilder.getStrNick()
        			, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
        			, 1);
        }
	}
	
	//牌局进入push
	void pushAppPushEnterGame(long fromuuid){
		UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(fromuuid);		
		//获取push相关信息
        UserTokenInfoNet stUserTokenInfoNet = UserManager.getInstance().getUserTokenInfo(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());

        if(stUserTokenInfoNet!=null && stFromUserBaseInfoNetBuilder!=null){
        	APNManager.getInstance().enterGameRoomPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
        			, stUserTokenInfoNet.getStrAPNToken()
        			, stFromUserBaseInfoNetBuilder.getStrNick()
        			, getStGameRoomInfo().getStGameRoomBaseInfo().getStrRoomName()
        			, 1);
        }
	}
	
	void pushAppPushStartGame(){
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount() ; i ++){
			UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
			if(stUserGameInfoNet.getStUserInfo().getUuid() > 0){
				UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stUserGameInfoNet.getStUserInfo().getUuid());	
				UserTokenInfoNet stUserTokenInfoNet = UserManager.getInstance().getUserTokenInfo(stUserGameInfoNet.getStUserInfo().getUuid());
				if(stFromUserBaseInfoNetBuilder!=null && stUserTokenInfoNet!=null){
					APNManager.getInstance().StartGameRoomPush(stUserGameInfoNet.getStUserInfo().getUuid()
							, stUserTokenInfoNet.getStrAPNToken()
							, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
							, 1);
				}
			}
		}
		
		for (long standUuid  : getStandByUuids()) {
			if(standUuid > 0){
				UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(standUuid);	
				UserTokenInfoNet stUserTokenInfoNet = UserManager.getInstance().getUserTokenInfo(standUuid);
				if(stFromUserBaseInfoNetBuilder!=null && stUserTokenInfoNet!=null){
					APNManager.getInstance().StartGameRoomPush(stFromUserBaseInfoNetBuilder.getUuid()
							, stUserTokenInfoNet.getStrAPNToken()
							, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
							, 1);
				}

			}
		}
	}
	
	void pushAppPushGameSeatEmpty(){
		if(stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
			long sb = stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds();
			int ante = stGameRoomInfo.getStGameRoomBaseInfo().getIAnte();
			float fHour =  (float)stGameRoomInfo.getStGameRoomBaseInfo().getLDuration()/(float)3600;
			 APNManager.getInstance().emptyGameRoomPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
					 , stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
					 , stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
					 , ""+fHour , sb, ante, 1);  
		}
	}

	public ArrayList<BuyinControl> getAllBuyinControls(){
		ArrayList<BuyinControl> vBuyinControls = new ArrayList<BuyinControl>();
		Iterator iter = mBuyinControlInfo.entrySet().iterator(); 
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next(); 
			Long uuid = (Long) entry.getKey();
			
			UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
			if(stUserBaseInfoNetBuilder!=null){
				BuyinControl.Builder stBuyinControlBuilder = BuyinControl.newBuilder();
				long lStacks = mBuyinControlInfo.get(uuid);
				long lCreateTime = mBuyinControlTimes.get(uuid);
				
				stBuyinControlBuilder.setStUserBaseInfo(stUserBaseInfoNetBuilder);
				stBuyinControlBuilder.setLGameRoomID(this.stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
				stBuyinControlBuilder.setSGameRoomName(this.stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
				stBuyinControlBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL);
				stBuyinControlBuilder.setLStacks(lStacks);
				stBuyinControlBuilder.setLBuyinCreateTime(lCreateTime);
				vBuyinControls.add(stBuyinControlBuilder.build());
			}
		}
		return vBuyinControls;
	}
	
	//用户进入房间
	public ERROR_CODE_TYPE enterGameRoom(long uuid){
		try{
			//把用户加入房间
			//获取gameroom builder,这里要注意toBuilder的方式，看看toBuilder是否会有问题
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			
			//开发游戏
			ClubInfo.Builder stClubInfo = ClubManager.getInstance().getClubInfo(this.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID());

			boolean bInClub = false;
			if(stClubInfo != null){
				for(ClubUserInfo stClubUserInfo:stClubInfo.getVClubUserInfosList()){
					if(stClubUserInfo!=null && stClubUserInfo.getStUserBaseInfo().getUuid() > 0) {
						if(stClubUserInfo.getStUserBaseInfo().getUuid() == uuid){
							bInClub = true;
							break;
						}
					}
				}
			}else{
				bInClub = true;
			}

			if(bInClub == false){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_CLUB_NO_USER;
			}
			if(containUser(uuid)){
				//如果用户已经存在房间了，就直接返回
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount() ; i++){
					UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);
					if(stUserGameInfo.getStUserInfo().getUuid() == uuid){
						return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
					}
				}
			}else if(mapStandByUserGameInfos.containsKey(uuid)){
				//如果用户已经存在房间了，就直接返回
				addStandByUuid(uuid);
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			}
			else{
				//生成新的游戏玩家，放入旁观队列,暂离游戏
				TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();
				
				UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(uuid).build();
				if(stUserBaseInfo != null){
					userGameInfoBuilder.setStUserInfo(stUserBaseInfo);
					userGameInfoBuilder.setLRoomId(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
					userGameInfoBuilder.setLRemainStacks(0);
					
					userGameInfoBuilder.setLBetStacks(0);
					userGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);
					userGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
					userGameInfoBuilder.setBGameDealer(0);
					//设置座位和用户的对应关系
					mapStandByUserGameInfos.put(uuid, userGameInfoBuilder);
					addStandByUuid(uuid);
					setStGameRoomInfo(stGameRoomInfoBuilder.build());
					
					ConnectDatabase.getInstance().insertInGameUser(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							,uuid);
					
					//APP push
					if(stGameRoomInfo!=null && stGameRoomInfo.getStGameRoomBaseInfo()!=null 
							&& stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo()!=null){
						pushAppPushEnterGame(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
					}
					
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}
				else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
				}
			}

			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
		}catch(Exception e){
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
		}
	
	}
	
	public int getDealyTimes(long uuid){
		for(int i = 0 ; i < this.stGameRoomInfo.getVUserGameInfosCount() ; i++){
			UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
		
			if(stUserGameInfoNet.getStUserInfo().getUuid() == uuid){
				System.out.println("getDealyTimes uuid : " + uuid + "   " +  stUserGameInfoNet.getIDelayTotalTimes());
				return stUserGameInfoNet.getIDelayTotalTimes();
			}
		}
		return 0;
	}
	
	public void setDealyTimes(long uuid,int times){
		synchronized(this){
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount() ; i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfoBuilder.getVUserGameInfos(i);
				if(stUserGameInfoNet!=null){
					UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
					if(stUserGameInfoNetBuilder.getStUserInfo().getUuid() == uuid){
						stUserGameInfoNetBuilder.setIDelayTotalTimes(times);
						stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
						addDelayPlayer(uuid);
					}	
				}
			}
			
			stGameRoomInfo = stGameRoomInfoBuilder.build();
		}
	}

	public ERROR_CODE_TYPE useDelayItem(long uuid,long lBettingID){
		synchronized(this){
			for(int i = 0 ; i < this.stGameRoomInfo.getVUserGameInfosCount() ; i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				
				if(stUserGameInfoNet.getStUserInfo().getUuid() == uuid
						&& stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
						&& stUserGameInfoNet.getLBettingID() == lBettingID){
					
					if(stUserGameInfoNet.getIDelayTotalTimes()==0){
						//直接使用延时道具
						//延时功能
						if(delayTimeout(stUserGameInfoNet.getStUserInfo().getUuid(),20)){					
							//打印日志
							UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
							if(stUserBaseInfoNetBuilder!=null){
								Log   mLog   =   LogFactory.getLog("fortransaction");
								mLog.error(uuid + " " + 0 + " " + 430003 + " " + 1 + " 0 " + "0 0 "  + stUserBaseInfoNetBuilder.getLPopularity());
							}
							
							//首先设置回包
							UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
							stUserGameInfoNetBuilder.setIDelayTotalTimes(stUserGameInfoNetBuilder.getIDelayTotalTimes() + 1);
							stUserGameInfoNetBuilder.setIDelayLong(stUserGameInfoNetBuilder.getIDelayLong() + 20);
							stUserGameInfoNetBuilder.setIDelayCost(GameRoomDetailInfo.getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
							stUserGameInfoNetBuilder.setIDelaySingleTime(20);
							GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
							stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
							stGameRoomInfo = stGameRoomInfoBuilder.build();
							
							addDelayPlayer(uuid);
							
							pushAllGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELAY);
						
							return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
						}
						break;
					}else{
						UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
						if(stUserBaseInfoNetBuilder!=null){
							if(stUserBaseInfoNetBuilder.getLCoin() >= stUserGameInfoNet.getIDelayCost()){
								//延时功能
								if(delayTimeout(stUserGameInfoNet.getStUserInfo().getUuid(),20)){								
									//首先设置回包
									UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
									stUserBaseInfoNetBuilder = UserManager.getInstance().addUserCoin(stUserBaseInfoNetBuilder.getUuid(), - stUserGameInfoNet.getIDelayCost());
									stUserGameInfoNetBuilder.setIDelayTotalTimes(stUserGameInfoNetBuilder.getIDelayTotalTimes() + 1);
									stUserGameInfoNetBuilder.setIDelayLong(stUserGameInfoNetBuilder.getIDelayLong() + 20);
									stUserGameInfoNetBuilder.setIDelayCost(GameRoomDetailInfo.getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
									stUserGameInfoNetBuilder.setStUserInfo(stUserBaseInfoNetBuilder);
									
									GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
									stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
									stGameRoomInfo = stGameRoomInfoBuilder.build();
									
									//打印日志
									Log   mLog   =   LogFactory.getLog("fortransaction");
									mLog.error(uuid + " " + 0 + " " + 430003 + " " + 1 + " 0 " + "0 "+ stUserGameInfoNet.getIDelayCost() +" "  + stUserBaseInfoNetBuilder.getLCoin());
									
									addDelayPlayer(uuid);
									
									pushAllGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELAY);
									return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS; 
								}
								break;
							}else{
								return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN; 
							}
						}else{
							return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER; 
						}
					}
				}
			}
			
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS; 
		}
	}
	
	public ERROR_CODE_TYPE resetBuyinRatio(long uuid,int iMinBuyinRatio,int iMaxBuyinRatio){
		synchronized(this){
			if(stGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL){
				if(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid){
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
					stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(iMaxBuyinRatio);
					stGameRoomBaseInfoBuilder.setIMinBuyinRatio(iMinBuyinRatio);
					stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
							
					//发送push消息
					for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
						UserGameInfoNet stUserGameInfoSendNet = stGameRoomInfo.getVUserGameInfos(i);
						
						
						if((stUserGameInfoSendNet.getStUserInfo().getUuid() > 0) &&
								(stUserGameInfoSendNet.getStUserInfo().getUuid() != stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid())){
							//首先找到离开房间的用户基本信息				
							pushGameRoomUserMsgs(stGameRoomInfoBuilder,null,stUserGameInfoSendNet.getStUserInfo().getUuid(),
									GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESET_BUYIN_RATIO);
						}
					}
					
					if(mapStandByUserGameInfos.get(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid())!=null){
						//首先找到离开房间的用户基本信息	
						pushGameRoomUserMsgs(stGameRoomInfoBuilder,null,stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(),
								GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESET_BUYIN_RATIO);
					}
					
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_GAME_ROOM_OWNER;
				}
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_SENIOR_GAME_ROOM;
			}
		}
	}
	
	private void reCalculateCouldPlayNum(){
		iCouldPlayNum = 0;
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
			UserGameInfoNet stUserGameInfoNet =  stGameRoomInfo.getVUserGameInfos(i);

			if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 
					&& stUserGameInfoNet.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT
					&& stUserGameInfoNet.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
					&& stUserGameInfoNet.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_STANDBY){
				iCouldPlayNum++;
			}
		}
	}
	
	private void gotoAnteStatus(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Ante);
		stGameRoomInfoBuilder.setBStateChange(1);
		
		int iLoop=0;

		if(iCouldPlayNum >= 2){
			//每个用户都下注
			for(int j = mSmallBlindPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j-iGameRoomUserMaxNums;
				}

				//从dealer开始，每次找到
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();

				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0&&stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY){

					if(stUserGameInfoBuilder.getLRemainStacks() <  stGameRoomInfo.getStGameRoomBaseInfo().getIAnte()){
						TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
								+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
								+"," + stGameRoomInfoBuilder.getLGameHandID()
								+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
								+"," + "ante" 
								+"," + stUserGameInfoBuilder.getLRemainStacks() );
					}else{
						TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
								+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
								+"," + stGameRoomInfoBuilder.getLGameHandID()
								+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
								+"," + "ante" 
								+"," + stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
					}
					
					if(stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIAnte()){
						//先下小盲注
						stUserGameInfoBuilder.setLBetStacks(stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
						
						//设置剩余赌注数
						stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() 
								- stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
					}else{
						//先下小盲注
						stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());
						
						//设置剩余赌注数
						stUserGameInfoBuilder.setLRemainStacks(0);
					}

					mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());
				}
				
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex,stUserGameInfoBuilder.build());
			}
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			sendAllGameStateChange(0,null);
			
			//设置房间的allin状态
			if(stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true){
				SidePot();
				stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setBAllAllin(1);
				stGameRoomInfo = stGameRoomInfoBuilder.build();

				resetGameUserStatus();

				sendAllGameStateChange(0,null);
			}else{
				SidePot();
				resetGameUserStatus();
			}
		}else{
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			resetGameRoom();
			sendAllGameStateChange(0,null);
		}

	}
	
	public boolean usedInGame(long uuid){
		return mapStandByUserGameInfos.containsKey(uuid);
	}

	private void gotoStartStatus(){
		//每个选手发两张牌,并把游戏的状态设置为FirstRound		
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop);
		stGameRoomInfoBuilder.setBStateChange(1);
		
		int userRole=1;//0 代表deal 1代表小盲 2代表大盲 3代表普通
		int iLoop=0;
		
		if(iCouldPlayNum > 2){
			for(int j = mSmallBlindPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j-iGameRoomUserMaxNums;
				}

				//从dealer开始，每次找到
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
						&&(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
						||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
						||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)){
					int card1 = 0;
					int card2 = 0;
					
					if(GameRoomDetailInfo.NOT_DEBUG){
						card1 = getCard();
						card2 = getCard();	
					}else{											
						card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(),0,false);
						card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(),1,false);
					}

					//首先给每个人发两张手牌
					stUserGameInfoBuilder.addVCurrentHands(card1);
					stUserGameInfoBuilder.addVCurrentHands(card2);
					
					Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
					if(lRemainStacks==null){
						lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
					}
					HandInfo stHandInfo = new HandInfo();
					stHandInfo.addCards(card1, card2);
					stHandInfo.setlRemainStacks(lRemainStacks);
					mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid() , stHandInfo);
					vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
					mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());
					
					
					String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							+"," + stGameRoomInfo.getLGameHandID()
							+"," + stUserGameInfoBuilder.getStUserInfo().getUuid()
							+"," + "hand" 
							+"," + card1
							+"," + card2
							+"," + lRemainStacks;
					
					InsuranceApi.parseGameAction(content);
					
					switch (userRole){
					case 1:
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
						
						if(stUserGameInfoBuilder.getLRemainStacks() <  stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()){
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "sb" 
									+"," + stUserGameInfoBuilder.getLRemainStacks());
						}else{
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "sb" 
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
						}
						
						//如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
						if(stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()){
							//先下小盲注
							stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() 
									- stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
						}else{
							//先下小盲注
							stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(0);
						}
						if(lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()){
							lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
						}
						

						userRole=2;
						break;
					case 2:
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
						if(stUserGameInfoBuilder.getLRemainStacks() <  stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()){
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "bb" 
									+"," + stUserGameInfoBuilder.getLRemainStacks() );
						}else{
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "bb" 
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
						}
						
						//如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
						if(stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()){
							//先下大盲注
							stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() 
									- stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
						}else{
							//先下大盲注
							stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(0);
						}
						if(lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()){
							lStepMaxStacks =stUserGameInfoBuilder.getLBetStacks();
						}
				
						userRole=3;
						break;
					case 3:
						//设置需要进行下注的人的状态		
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						
						if(stUserGameInfoBuilder.getLRemainStacks() > 0 && checkAllAllin()==false){
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
							stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
							stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
							stUserGameInfoBuilder.setIDelayLong(0);
							stUserGameInfoBuilder.setIDelaySingleTime(20);
							
							//把玩家放到超时检测队列里面
							removeAllTimeOutUser();
							setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
							userRole=4;
						}

						break;
						
					}
				}
				stGameRoomInfoBuilder.setVUserGameInfos(userIndex,stUserGameInfoBuilder.build());
			}
			
			if(lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()){
				lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
			}
			iLoop=0;

			stGameRoomInfo = stGameRoomInfoBuilder.build();
			if(userRole==3 && checkAllAllin()==false){
				for(int p = mSmallBlindPos ; iLoop < iGameRoomUserMaxNums;iLoop++,p++ ){
					int userIndex_spec = 0;
					if(p<iGameRoomUserMaxNums){
						userIndex_spec = p;
					}else{
						userIndex_spec = p-iGameRoomUserMaxNums;
					}
					
					//从dealer开始，每次找到
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex_spec).toBuilder();
	
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
							&&(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND)){	
							//设置需要进行下注的人的状态
						if(stUserGameInfoBuilder.getLRemainStacks() > 0){
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
							stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
							stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
							stUserGameInfoBuilder.setIDelayLong(0);
							stUserGameInfoBuilder.setIDelaySingleTime(20);
							
							//把玩家放到超时检测队列里面
							removeAllTimeOutUser();
							setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
							userRole=4;
							
							stGameRoomInfoBuilder.setVUserGameInfos(userIndex_spec,stUserGameInfoBuilder.build());
						}

						break;
						
					}
				}
			}
			stGameRoomInfo = stGameRoomInfoBuilder.build();

			sendAllGameStateChange(0,null);
			//设置房间的allin状态
			if(stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true){
				SidePot();
				stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setBAllAllin(1);
				stGameRoomInfo = stGameRoomInfoBuilder.build();
				resetStepStatus();
				sendAllGameStateChange(0,null);
			}
		}else  if(iCouldPlayNum == 2){
			
			//把牌局的信息push给每个人，目前的实现还有一些问题。最后需要把push和设置用户状态分离
			//说先设置房间里面的各个成员的状态
			for(int j = 0 ; j < iGameRoomUserMaxNums;j++ ){
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();

				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
						&&(stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
						||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
						||stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)){
					//生成两张手牌
					int card1 = 0;
					int card2 = 0;
					if(GameRoomDetailInfo.NOT_DEBUG){
						card1 = getCard();
						card2 = getCard();	
					}else{											
						card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(),0,false);
						card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(),1,false);
					}
					
					Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
					if(lRemainStacks==null){
						lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
					}
					
					//发牌到到用户手上
					stUserGameInfoBuilder.addVCurrentHands(card1);
					stUserGameInfoBuilder.addVCurrentHands(card2);
					
					HandInfo stHandInfo = new HandInfo();
					stHandInfo.addCards(card1, card2);
					stHandInfo.setlRemainStacks(lRemainStacks);
					mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid() , stHandInfo);
					vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
					mHandPos.put(j, stUserGameInfoBuilder.getStUserInfo().getUuid());
					
					
					String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							+"," + stGameRoomInfo.getLGameHandID()
							+"," + stUserGameInfoBuilder.getStUserInfo().getUuid()
							+"," + "hand" 
							+"," + card1
							+"," + card2
							+"," + lRemainStacks;
					
					InsuranceApi.parseGameAction(content);
					
					if(j == mBigBlindPos){
						//设置大盲的游戏状态
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
						
						if(stUserGameInfoBuilder.getLRemainStacks() <  stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()){
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "bb" 
									+"," + stUserGameInfoBuilder.getLRemainStacks() );
						}else{
							TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
									+"," + stGameRoomInfoBuilder.getLGameHandID()
									+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
									+"," + "bb" 
									+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
						}
						
						//如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
						if(stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()){
							//先下大盲注
							stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
						}else{
							//先下大盲注
							stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());
							
							//设置剩余赌注数
							stUserGameInfoBuilder.setLRemainStacks(0);
						}
						if(lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()){
							lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
						}
						
						
					}else {					
						//设置小盲的游戏状态						
						if(stUserGameInfoBuilder.getLRemainStacks() > 0){
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
							stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
							stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
							stUserGameInfoBuilder.setIDelayLong(0);
							stUserGameInfoBuilder.setIDelaySingleTime(20);
							
							if(stUserGameInfoBuilder.getLRemainStacks() <  stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()){
								TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
										+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
										+"," + stGameRoomInfoBuilder.getLGameHandID()
										+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
										+"," + "sb" 
										+"," + stUserGameInfoBuilder.getLRemainStacks() );
							}else{
								TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
										+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
										+"," + stGameRoomInfoBuilder.getLGameHandID()
										+"," + stUserGameInfoBuilder.getStUserInfo().getUuid() 
										+"," + "sb" 
										+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
							}
							
							//如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
							if(stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()){
								//先下小盲注
								stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
								
								//设置剩余赌注数
								stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
							}else{
								//先下小盲注
								stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());
								
								//设置剩余赌注数
								stUserGameInfoBuilder.setLRemainStacks(0);
							}
							
							//把要动作的用户设置到超时队列里面去
							removeAllTimeOutUser();
							setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
							
							
						}
						
						if(lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()){
							lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
						}
					}
					
					//设置房间的游戏状态
					stGameRoomInfoBuilder.setVUserGameInfos(j,stUserGameInfoBuilder.build());
				}
			}
			if(lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()){
				lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
			}
			stGameRoomInfo = stGameRoomInfoBuilder.build();
	
			sendAllGameStateChange(0,null);
			//设置房间的allin状态
			if(stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true){
				SidePot();
				stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setBAllAllin(1);
				stGameRoomInfo = stGameRoomInfoBuilder.build();
				resetStepStatus();
				sendAllGameStateChange(0,null);
			}
		}else{
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			resetGameRoom();
			sendAllGameStateChange(0,null);
		}
	}
	
	public void helloGame(long uuid){
		addStandByUuid(uuid);
	}
	
	public void gotoNextStep(){
		synchronized(this){
			switch (stGameRoomInfo.getEGameState().getNumber()) {
//			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//				gotoFlopSeparateSatus(0,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One,true);
//			}
//				break;
//			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One_VALUE: {
//				gotoFlopSeparateSatus(0,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two,true);
//			}
//				break;
//			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two_VALUE: {
//				gotoFlopSeparateSatus(0,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three,true);
//			}
//				break;
			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
				gotoTurnStatus(0,true);
			}
				break;
			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE: {
				gotoRiverStatus(0,true);
			}
				break;
			case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE: {
				gotoResultStatus(0);
			}
				break;
			}
		}
	}
	
	/**
	 * 买入保险
	 * @param uuid	    	 	买保险的用户
	 * @param lGameHandID		买入牌局的手ID
	 * @param eGameRoomStatus		买入时候的牌局状态
	 * @param vBuyInsuranceInfo		买入动作集合
	 */
	public GameRoomInfo buyInsurance(long uuid,long lGameHandID,GAME_ROOM_GAME_STATE eGameRoomStatus,List<BuyInsuranceInfo> vBuyInsuranceInfo){
		synchronized(this){
			InsuranceInfo stInsuranceInfo = InsuranceApi.doInsuranceAction(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),lGameHandID,eGameRoomStatus,uuid,vBuyInsuranceInfo);
			if(stInsuranceInfo!=null){
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
				stGameRoomInfo =  stGameRoomInfoBuilder.build();
						
				UserGameInfoNet stUserGameInfoNet=null;
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
					UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
					if(stTempUserGameInfoNet.getStUserInfo().getUuid() > 0 && stTempUserGameInfoNet.getStUserInfo().getUuid() == uuid){			
						stUserGameInfoNet = stTempUserGameInfoNet;
					}
				}
				//push相关
				pushAllGameRoomUserMsgsIncludeSelf(stUserGameInfoNet, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUY_INSURANCE);
				
				return stGameRoomInfo;
			}else{
				return null;
			}
		}
	}
	
	/**
	 * 房主设置的用户是否可以买入保险
	 * @param uuid	    		 	房主
	 * @param lSettedUuid			被设置的用户
	 * @param bCanInsurance			是否可以买入保险
	 */
	public ERROR_CODE_TYPE creatorInsuranceSetting(long uuid,long lSettedUuid,int bCanInsurance){
		synchronized(this){
//			InsuranceInfo stInsuranceInfo = null;
//			if(uuid!=stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
//				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_GAME_ROOM_OWNER;
//			}
//			
//			if(bCanInsurance > 0){
//				stInsuranceInfo = InsuranceApi.setCreaterInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lSettedUuid, false);
//			}else{
//				stInsuranceInfo = InsuranceApi.setCreaterInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lSettedUuid, true);
//			}
//			
//			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//			stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
//			stGameRoomInfo = stGameRoomInfoBuilder.build();
//			
//			//push相关
//			pushAllGameRoomUserMsgs(null, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_INSURANCEABLE_CHANGE);
			
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS; 
			
		}
	}
	
	/**
	 * 用户设置的是否可以买入保险
	 * @param uuid	    	 		设置的用户
	 * @param bDefaultInsurance		是否可以买入保险
	 */
	public ERROR_CODE_TYPE userInsuranceSetting(long uuid,int bDefaultInsurance){
		synchronized(this){
			boolean bInsurance = false;
			if(bDefaultInsurance > 0){
				bInsurance = InsuranceApi.setUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, true);
			}else{
				bInsurance = InsuranceApi.setUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, false);
			}
			
			if(bInsurance==false){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SET_INSURANCE; 
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS; 
			}
		}
	}
	
	/**
	 * 保险延时道具
	 * @param uuid	    	 		设置的用户
	 * @param lGameHandID			手ID
	 * @param eGameRoomState		牌局的状态
	 */
	public InsuranceUserInfo buyInsuranceDealy(long uuid,long lGameHandID,GAME_ROOM_GAME_STATE eGameRoomState){
		synchronized(this){					
			InsuranceInfo stInsuranceInfo = InsuranceApi.delayInsuranceAction(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),lGameHandID,eGameRoomState, uuid);

			if(stInsuranceInfo!=null){
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
				stGameRoomInfo =  stGameRoomInfoBuilder.build();
				
				UserGameInfoNet stUserGameInfoNet=null;
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
					UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
					if(stTempUserGameInfoNet.getStUserInfo().getUuid() > 0 && stTempUserGameInfoNet.getStUserInfo().getUuid() == uuid){			
						stUserGameInfoNet = stTempUserGameInfoNet;
					}
				}
							
				//push相关
				pushAllGameRoomUserMsgsIncludeSelf(stUserGameInfoNet, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_INSURANCE_DELAY);
				
				for(InsuranceUserInfo stInsuranceUserInfo:stInsuranceInfo.getVInsuranceUserInfosList()){

					if(stInsuranceUserInfo.getUuid() == uuid){
						return stInsuranceUserInfo; 
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * 计算dealer位置
	 */
	private  void rsetetDealerPos(){
		int startIndex = mDealPos + 1;
		if(startIndex >= iGameRoomUserMaxNums){
			startIndex -= iGameRoomUserMaxNums;
		}
		int iLoop=0;
		for(int j = startIndex ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
			int userIndex = 0;
			if(j<iGameRoomUserMaxNums){
				userIndex = j;
			}else{
				userIndex = j - iGameRoomUserMaxNums;
			}
			
			UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(userIndex).toBuilder();
			
			if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
					&&stUserGameInfoBuilder.getLRemainStacks() > 0
					&&stUserGameInfoBuilder.getBJustSit() == 0){
				mDealPos =  userIndex;
				return;
			}	
		}
		mDealPos = startIndex;
		
	}
	
	/**
	 * 保险超时
	 */
	public void doInsuranceTimeout(){
		synchronized(this){					
			InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
		}
	}
	
	/**
	 * 获取保险状态
	 */
	public int getGameSetting(long uuid){
		boolean bNotBuyInsurance = InsuranceApi.getUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),uuid);
		if(bNotBuyInsurance == true){
			return 1;
		}else{
			return 0;
		}
	}
	
	private void couldPlayNumAfterSetDealer(){
		iCouldPlayNum = 0;
		//首先设置纽扣，小盲，大盲位
		int iLoop=0;
		
		int userRole = 0;
		for(int j = mDealPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
			int userIndex = 0;
			if(j<iGameRoomUserMaxNums){
				userIndex = j;
			}else{
				userIndex = j - iGameRoomUserMaxNums;
			}
			
			UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(userIndex).toBuilder();
			//判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
			if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0&&stUserGameInfoBuilder.getLRemainStacks() > 0){
				//如果是小盲，并且用户刚刚坐下，则用户不能参加牌局
				if(stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1){
					
				}else{
					//设置用户的较色状态
					if(0 == userRole){
						userRole++;
					}else if( 1 == userRole){
						//寻找下一个大盲
						userRole++;
					}
					iCouldPlayNum ++;
				}
			}
		}
	}
	
	//计算dealer位置
	private  void rsetetJustSitTag(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		for(int i = 0 ; i < iGameRoomUserMaxNums ; i++ ){
			UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
			
			stUserGameInfoBuilder.setBJustSit(0);
			
			stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
	
	public void pushInsuranceTimeout(long uuid,InsuranceInfo stInsuranceInfo){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		System.out.println("pushInsuranceTimeout gameroomid : " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
			UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

			if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() == uuid){		
				System.out.println("GAME_ROOM_MESSAGE_BUY_INSURANCE : " + uuid);
				pushGameRoomUserMsgs(stUserGameInfoNet,uuid,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUY_INSURANCE,i);
			}
		}
	}
}
