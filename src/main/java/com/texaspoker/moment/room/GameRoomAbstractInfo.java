package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.texaspoker.bill.BillManager;
import com.texaspoker.comm.AdvancedTimeoutHashMap;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.dubbo.game.insurance.InsuranceApi;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.GAME_ACT_TYPE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.GamePotResultInfo;
import com.texaspoker.moment.TexasPoker.GameResultInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.INSURANCE_STATUS;
import com.texaspoker.moment.TexasPoker.InsuranceInfo;
import com.texaspoker.moment.TexasPoker.SCGameRoomStateChange;
import com.texaspoker.moment.TexasPoker.USER_GAME_STATE;
import com.texaspoker.moment.TexasPoker.UseItemPushMsg;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.DEVICE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.shuffle.ShuffleCard;
import com.texaspoker.user.UserManager;

public class GameRoomAbstractInfo implements GameRoomAbstractInterface{
	//dealer的位置
	protected int mDealPos = 7;
	//小盲的位置
	protected int mSmallBlindPos;
	//大盲的位置
	protected int mBigBlindPos;
	//结束的位置
	protected int mEndPos;
	
	private boolean bFlodResult = false;
	
	//初始筹码数量
	protected ArrayList<Long> vOriginalStacks = new ArrayList<Long>();
	protected ArrayList<Integer> vTestHandCards1 = new ArrayList<Integer>();
	protected ArrayList<Integer> vTestHandCards2 = new ArrayList<Integer>();
	protected ArrayList<Integer> vTestHandCards3 = new ArrayList<Integer>();
	protected ArrayList<Integer> vTestTableCards = new ArrayList<Integer>();
	
	protected Map<Long,Long> mOriginalStacks = new HashMap<Long,Long>();
	
	protected boolean bGameCanceled = false;
	
	protected Map<Long,UserGameInfoNet.Builder> mapStandByUserGameInfos = new ConcurrentHashMap<Long,UserGameInfoNet.Builder>();
	
	//目前的手牌ID
	protected int mGameHandID=0;
	
	//牌局信息
	protected GameRoomInfo stGameRoomInfo;
	
	//旁观用户列表
	protected Set<Long>  mStandBys = new HashSet<Long>(); 
	//所有旁观用户列表
	protected Set<Long>  mStandingBys = new HashSet<Long>(); 
	public TimeoutHashMap<Long,Long> mStandByTimeOut; //房间相关的数据
	
	//在桌上的用户列表
	protected Set<Long> mPlayers = new HashSet<Long>(); 
	
	protected AdvancedTimeoutHashMap<Long,Long> mTimeoutGameUsers;
	
	//目前已经抓的牌的数量
	protected int iDrawCardNum = 0;
	
	//用户的操作超时
	protected Map<Long,Integer> mapTimeoutTimesUser = new ConcurrentHashMap<Long,Integer>();
	
	//其他的数据
	protected long lStepMaxStacks = 0;
	
	//可以参加游戏的人数
	protected int iCouldPlayNum = 0;
	
	//用户在牌局的位置信息
	protected ConcurrentHashMap<Long,Integer> mapUserPos = new ConcurrentHashMap<Long,Integer>();
	
	protected final static boolean NOT_DEBUG=true;

	//是否所有人都allin状态
	public boolean bAllAllin;
	
	//分池数据
	protected ArrayList<PotInfo> mPotList = new ArrayList<PotInfo>();
	
	//用户计算最大池底
	protected long lMaxPot = 0;
	
	protected int mCards[] = new int[52];
	
	protected Map<Long,HandInfo> mHandMap =  new HashMap<Long,HandInfo>();
	protected List<Long> vHandList =  new ArrayList<Long>();
	protected Map<Integer,Long> mHandPos =  new HashMap<Integer,Long>();
	
	protected String LOG_TAG="";
	
	protected long lFromRoomID=0;
	protected long lGameRoomID=0;
	protected boolean bPause = false;
	protected boolean bStarted = false;
	protected int iGameRoomUserMaxNums = 0;

	
	/**
	 * 初始化函数
	 */
	public GameRoomAbstractInfo(){
		mTimeoutGameUsers = new AdvancedTimeoutHashMap<Long,Long>();

		vTestHandCards1.add(10);
		vTestHandCards1.add(35);
		
		vTestHandCards2.add(19);
		vTestHandCards2.add(46);
		
		vTestHandCards3.add(30);
		vTestHandCards3.add(41);
		
		vTestTableCards.add(21);
		vTestTableCards.add(20);
		vTestTableCards.add(29);
		vTestTableCards.add(0);
		vTestTableCards.add(40);
	}
	
	@Override  
	public GameRoomInfo getStGameRoomInfo() {
		return stGameRoomInfo;
	}

	@Override  
	public void setStGameRoomInfo(GameRoomInfo stGameRoomInfo) {
		this.stGameRoomInfo = stGameRoomInfo;
	}
	
	@Override  
	public Set<Long> getStandByUuids(){
		return mStandingBys;
	}
	
	
	@Override  
	public Set<Long> getAllStandByUuids(){
		return mStandBys;
	}
	@Override  
	public void addStandByUuid(long uuid){
		mStandBys.add(uuid);
		mStandingBys.add(uuid);
		mStandByTimeOut.setCurrent(uuid,uuid);
	}
	
	@Override  
	public void removeStandByUuid(long uuid){
		mStandingBys.remove(uuid);
		mStandByTimeOut.removeKey(uuid);
	}
	
	@Override
	public int getLastHandID(){
		if(mGameHandID -1 >0){
			return mGameHandID-1;
		}else{
			return 0;
		}
	}
	
	@Override  
	public void removeAllStandByStatusUuid(long uuid){
		mStandingBys.remove(uuid);
		mStandBys.remove(uuid);
		mStandByTimeOut.removeKey(uuid);
	}
	
	@Override
	public int getHandID(){
		if(mGameHandID>0){
			return mGameHandID;
		}else{
			return 0;
		}
	}
	
	@Override
	public long getMaxPot(){
		return lMaxPot;
	}
	/**
	 * 弃牌函数，不可以通过uuid获取位置的弃牌（站起坐下弃牌用这个函数，且此时的lbettingid为-2）
	 * @param uuid			弃牌的用户
	 * @param userPos		用户所在位置
	 * @return
	 */
	@Override  
	public ERROR_CODE_TYPE doFlodAction(long uuid,int userPos,boolean bDeleggate){
		return doFlodAction(uuid,userPos,-2,bDeleggate);
	}
	
	/**
	 * 弃牌函数，可以通过uuid获取位置的弃牌（非站起坐下弃牌用这个函数）
	 * @param uuid			弃牌的用户
	 * @param lBettingID	手牌动作ID
	 * @return
	 */
	
	@Override  
	public ERROR_CODE_TYPE doFlodAction(long uuid,long lBettingID,boolean bDeleggate){
		Integer userPos = getUserPos(uuid);
		if(userPos!=null){
			return doFlodAction(uuid,userPos,lBettingID,bDeleggate);
		}else{
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
		}
	}
	
	@Override  
	public ERROR_CODE_TYPE doCheckAction(long uuid,long lBettingID){
		ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		synchronized(this){

			if(bPause == true){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			if(checkGameActionValidation()==false){
				return eErrorCode;
			}
			
			int victoryUser = -1;
			Integer userPos = getUserPos(uuid);
			
			if (userPos != null) {
				// ������������������
				int iLoop = 0;
				
				// ������������������������������
				for (int j = userPos; iLoop < iGameRoomUserMaxNums; j++,iLoop++) {
					int userIndex = 0;
					if (j < iGameRoomUserMaxNums) {
						userIndex = j;
					} else {
						userIndex = j - iGameRoomUserMaxNums;
					}

					// ������������������
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
					
					if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
						if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING 
								||(lBettingID!=-1&&lBettingID!=stUserGameInfoBuilder.getLBettingID()&&lBettingID!=0)){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}
						
						if(lStepMaxStacks!=stUserGameInfoBuilder.getLBetStacks()){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}
						
						TableLogService.getInstance().error(lFromRoomID
								+"," + lGameRoomID
								+"," + stGameRoomInfo.getLGameHandID()
								+"," + uuid
								+"," + "check" 
								+"," + 0);
						
						// ������������������������
						stUserGameInfoBuilder
								.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
						stUserGameInfoBuilder.setIDelayLong(0);
						stUserGameInfoBuilder.setIDelaySingleTime(20);

						// ������������gameroominfo���������������������
						stGameRoomInfoBuilder.setVUserGameInfos(userIndex,stUserGameInfoBuilder);
						
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						
						//���������������������������������������������������������������������������������������������
						victoryUser = iGameOver();
						if(victoryUser != -1){		
							//������������������push������������
							sendAllGameStateChange(uuid,null);
							
							flodWinProcess();
							
							sendAllGameStateChange(uuid,null);
							break;
						}
					}else if(stUserGameInfoBuilder.getStUserInfo().getUuid() != 0){
						
						if (userIndex == mEndPos) {
							// ������������������������������������
							sendAllGameStateChange(uuid,null);
							
							SidePot();
							stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
							
							//���������river���������������������������������������������������������������
							if(stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River){
								//���������������allin������
								if(checkAllAllin() == true){
									stGameRoomInfoBuilder.setBAllAllin(1);
									bAllAllin = true;
								}

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
														stResetUserGameInfoBuilder.setIDelayLong(0);
														stResetUserGameInfoBuilder.setIDelaySingleTime(20);
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
														stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

													}else{
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

													}

													removeAllTimeOutUser();
													setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
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
														stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
														stResetUserGameInfoBuilder.setIDelayLong(0);
														stResetUserGameInfoBuilder.setIDelaySingleTime(20);
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
														stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
													}else{
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
													}

													removeAllTimeOutUser();
													setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
													iBetting = 1;
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
										
										stResetUserGameInfoBuilder.setLBetStacks(0);
									
										stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
									}
								}
							}
							
							stGameRoomInfo = stGameRoomInfoBuilder.build();
							switch (stGameRoomInfoBuilder.getEGameState()
									.getNumber()) {
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
								gotoFlopAllStatus(uuid);
							}
								break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
								checkGotoTurnStatus(uuid);
							}
								break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE: {
								checkGotoRiverStatus(uuid);
							}
								break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE: {
								gotoResultStatus(uuid);
							}
								break;
							}
							break;
						} else if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
								&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
								&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){
							if(stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin==false){
								stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
								stUserGameInfoBuilder.setIDelayLong(0);
								stUserGameInfoBuilder.setIDelaySingleTime(20);
								stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
								stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

								removeAllTimeOutUser();
								setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

								// ������������gameroominfo���������������������
								stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
								stGameRoomInfo = stGameRoomInfoBuilder.build();

								sendAllGameStateChange(uuid,null);
								break;
							}

							}
						}
					}
				}else{
					eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
				}
		}
		
		return eErrorCode;
	}
	
//	void checkGotoFlopStatus(long uuid){
//		if(checkAllAllin()){
//			checkGotoFlopSeparateSatus(uuid,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One);
//		}else{
//			gotoFlopAllStatus(uuid);
//		}
//	}
//	
	protected void gotoFlopAllStatus(long uuid){
		
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		if(stGameRoomInfoBuilder.getVCurrentCardsCount() == 0){
			int card1 = 0;
			int card2 = 0;
			int card3 = 0;
			if(NOT_DEBUG){
				card1 = getCard();
				card2 = getCard();
				card3 = getCard();
			}else{
				card1 = getCard(0,0,true);
				card2 = getCard(0,1,true);
				card3 = getCard(0,2,true);
			}
			
			stGameRoomInfoBuilder.addVCurrentCards(card1);
			stGameRoomInfoBuilder.addVCurrentCards(card2);
			stGameRoomInfoBuilder.addVCurrentCards(card3);
			
			stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);
			stGameRoomInfoBuilder.setBStateChange(1);

			stGameRoomInfo = stGameRoomInfoBuilder.build();

			ArrayList<Integer> cards = new ArrayList<Integer>();
			cards.add(card1);
			cards.add(card2);
			cards.add(card3);
			sendAllGameStateChange(uuid,cards);
			
			
			TableLogService.getInstance().error(lFromRoomID
					+"," + lGameRoomID
					+"," + stGameRoomInfo.getLGameHandID()
					+"," + 0
					+"," + "flop" 
					+"," + card1
					+"," + card2
					+"," + card3);
		}else{
			stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);
			stGameRoomInfoBuilder.setBStateChange(1);
		}
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
//	
//	public void checkGotoFlopSeparateSatus(long uuid,GAME_ROOM_GAME_STATE eGameRoomState){
//		if(stGameRoomInfo.getEInsuranceStatus() == INSURANCE_STATUS.INSURANCE_STATUS_NO){
//			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//			InsuranceInfo vInsurancePotInfos = InsuranceApi.checkInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),stGameRoomInfo.getLGameHandID(),eGameRoomState);
//			
//			if(vInsurancePotInfos != null){
//				//进入保险模式
//				stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_BETTING);
//				stGameRoomInfoBuilder.setVInsuranceInfos(vInsurancePotInfos);
//				stGameRoomInfo = stGameRoomInfoBuilder.build();			
//				sendAllGameStateChange(0,null);
//				
//				//记录cent需要的最后一个下牌的玩家
//				stGameRoomInfo = stGameRoomInfoBuilder.build();				
//			}else{
//				gotoFlopSeparateSatus(uuid,eGameRoomState,false);
//			}
//		}
//	}
//	
//	public void gotoFlopSeparateSatus(long uuid,GAME_ROOM_GAME_STATE eGameRoomState,boolean bInsurance){
//		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//		int card1 = 0;
//		if(NOT_DEBUG){
//			card1 = getCard();
//		}else{
//			card1 = getCard(0,0,true);
//		}
//		
//		stGameRoomInfoBuilder.addVCurrentCards(card1);
//		
//		stGameRoomInfoBuilder.setEGameState(eGameRoomState);
//		stGameRoomInfoBuilder.setBStateChange(1);
//		
//		String sFlopStatus = "";
//		switch(eGameRoomState.getNumber()){
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One_VALUE:
//			sFlopStatus="flop1";
//			break;
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two_VALUE:
//			sFlopStatus="flop2";
//			break;
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three_VALUE:
//			sFlopStatus="flop3";
//			break;
//		}
//		
//		TableLogService.getInstance().error(lFromRoomID
//				+"," + lGameRoomID
//				+"," + stGameRoomInfo.getLGameHandID()
//				+"," + 0
//				+"," + sFlopStatus 
//				+"," + card1
//		);
//		
//		if(bInsurance==true){
//			InsuranceInfo  stInsuranceInfo = InsuranceApi.getInsuranceResult(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), card1);
//			stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
//			stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_RESULT);
//		}
//
//		stGameRoomInfo = stGameRoomInfoBuilder.build();
//
//		ArrayList<Integer> cards = new ArrayList<Integer>();
//		cards.add(card1);
//		sendAllGameStateChange(uuid,cards);
//		
//		stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_NO);
//		stGameRoomInfo = stGameRoomInfoBuilder.build();
//		
//		switch(eGameRoomState.getNumber()){
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One_VALUE:
//			checkGotoFlopSeparateSatus(uuid,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two);
//			break;
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two_VALUE:
//			checkGotoFlopSeparateSatus(uuid,GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three);
//			break;
//		case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three_VALUE:
//			checkGotoTurnStatus(uuid);
//			break;
//		}
//	}
	
	void checkGotoTurnStatus(long uuid){
		if(checkAllAllin() == true){
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			InsuranceInfo stInsuranceInfo = null;
			if(stGameRoomInfoBuilder.getEInsuranceStatus() == INSURANCE_STATUS.INSURANCE_STATUS_NO){
				stInsuranceInfo = InsuranceApi.checkInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),stGameRoomInfo.getLGameHandID(),GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);
	
				if(stInsuranceInfo != null){
					//进入保险模式
					stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_BETTING);
					stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
					stGameRoomInfo = stGameRoomInfoBuilder.build();					
					sendAllGameStateChange(0,null);
					//记录cent需要的最后一个下牌的玩家
					stGameRoomInfo = stGameRoomInfoBuilder.build();		
					
					InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
				}else{
					gotoTurnStatus(uuid,false);
				}
			}
		}else{
			gotoTurnStatus(uuid,false);
		}
		
	}
	
	void gotoTurnStatus(long uuid,boolean bInsurance){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		int card1 = 0;
		ArrayList<Integer> cards = new ArrayList<Integer>();
		
        //保护，发这张牌时，排面上只能有3张牌
		if(stGameRoomInfo.getVCurrentCardsCount() <= 3){
            //发turn牌
			if(NOT_DEBUG){
				card1 = getCard();
			}else{
				card1 =  getCard(0,3,true);
			}
			
			stGameRoomInfoBuilder.addVCurrentCards(card1);
			TableLogService.getInstance().error(lFromRoomID
					+"," + lGameRoomID
					+"," + stGameRoomInfo.getLGameHandID()
					+"," + 0
					+"," + "turn" 
					+"," + card1);
			
			cards.add(card1);
		}
		
		stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn);	
		stGameRoomInfoBuilder.setBStateChange(1);
		
		if(bInsurance){
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			stGameRoomInfo = InsuranceApi.getInsuranceResult(stGameRoomInfo , stGameRoomInfo.getLGameHandID() ,card1);
			stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_RESULT);
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();

		sendAllGameStateChange(uuid,cards);
		
		stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_NO);
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		
	}
	
	void checkGotoRiverStatus(long uuid){
		if(checkAllAllin() == true){
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			InsuranceInfo stInsuranceInfo = null;
			if(stGameRoomInfoBuilder.getEInsuranceStatus() == INSURANCE_STATUS.INSURANCE_STATUS_NO){

				stInsuranceInfo = InsuranceApi.checkInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),stGameRoomInfo.getLGameHandID(),GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn);

				if(stInsuranceInfo != null){
					//进入保险模式
					stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_BETTING);
					stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
					stGameRoomInfo = stGameRoomInfoBuilder.build();					
					sendAllGameStateChange(0,null);
					//记录cent需要的最后一个下牌的玩家
					stGameRoomInfo = stGameRoomInfoBuilder.build();		
					
					InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
				}else{
					gotoRiverStatus(uuid,false);
				}
			}
		}else{
			gotoRiverStatus(uuid,false);
		}
		
	}
	
	void gotoRiverStatus(long uuid,boolean bInsurance){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		int card1 = 0;
		ArrayList<Integer> cards = new ArrayList<Integer>();
		if(stGameRoomInfo.getVCurrentCardsCount() <= 4){
			if(NOT_DEBUG){
				card1 = getCard();
			}else{
				card1 = getCard(0,4,true);
			}	
			
			stGameRoomInfoBuilder.addVCurrentCards(card1);
			
			TableLogService.getInstance().error(lFromRoomID
					+"," + lGameRoomID
					+"," + stGameRoomInfo.getLGameHandID()
					+"," + 0
					+"," + "river" 
					+"," + card1);
			
			cards.add(card1);
		}
								
		stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River);	
		stGameRoomInfoBuilder.setBStateChange(1);
		
		if(bInsurance){
			stGameRoomInfo = stGameRoomInfoBuilder.build();
			stGameRoomInfo = InsuranceApi.getInsuranceResult(stGameRoomInfo , stGameRoomInfo.getLGameHandID() ,card1);
			stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_RESULT);
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		
		
		if(checkAllAllin() == true){
			stGameRoomInfoBuilder.setBAllAllin(1);
			bAllAllin = true;
		}
		
		sendAllGameStateChange(uuid,cards);
		
		stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_NO);
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
	
	void gotoResultStatus(long uuid){
		//如果是和牌
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();					
		stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result);
		stGameRoomInfoBuilder.setBStateChange(1);
		stGameRoomInfo = stGameRoomInfoBuilder.build();

        //计算结果
		calculateWinner();
		
		stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();				

		//重置结尾allin状态
		for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
			UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

			if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
					&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
				
				if(stResetUserGameInfoBuilder.getLRemainStacks() == 0||bAllAllin == true){
					stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
				}
			}
	
			stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
		}
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		
		//Push下发结果
		sendAllGameStateChange(uuid,null);
			
	}
	
	
	@Override  
	public ERROR_CODE_TYPE doRaiseAction(long uuid,GAME_ACT_TYPE eGameActType,long lStacks,long lBettingID){
		ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		synchronized(this){
			if(bPause == true){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();			
			if(checkGameActionValidation()==false){
				return eErrorCode;
			}
			
			int victoryUser = -1;
			Integer userPos = getUserPos(uuid);
			if(userPos != null){
				//������������������
				int iLoop=0;
				
				//������������������������������
				for(int j = userPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
					int userIndex = 0;
					if(j<iGameRoomUserMaxNums){
						userIndex = j;
					}else{
						userIndex = j-iGameRoomUserMaxNums;
					}

					//������������������
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){	
						if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
								||(lBettingID!=-1&&lBettingID!=stUserGameInfoBuilder.getLBettingID()&&lBettingID!=0)){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}

						if((lStepMaxStacks>stUserGameInfoBuilder.getLBetStacks() + lStacks) && (stUserGameInfoBuilder.getLRemainStacks()-lStacks)!=0){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}
						
						TableLogService.getInstance().error(lFromRoomID
								+"," + lGameRoomID
								+"," + stGameRoomInfo.getLGameHandID()
								+"," + uuid
								+"," + "raise" 
								+"," + lStacks);
						
						//������������������������
						if(GAME_ACT_TYPE.GAME_ACT_TYPE_ALLIN_VALUE == eGameActType.getNumber()){
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
						}else{
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_RAISE);
						}
						stUserGameInfoBuilder.setIDelayLong(0);
						stUserGameInfoBuilder.setIDelaySingleTime(20);
						
						//������������������
						stUserGameInfoBuilder.setLBetStacks(lStacks + stUserGameInfoBuilder.getLBetStacks());
						long lTempEffectiveStacks = stUserGameInfoBuilder.getLBetStacks() - lStepMaxStacks;
						if(lTempEffectiveStacks > stGameRoomInfoBuilder.getLEffectiveRaise()){
							stGameRoomInfoBuilder.setLEffectiveRaise(lTempEffectiveStacks);
						}
						
						lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
						//������������������
						stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - lStacks);
						
						//���������raise���������������������������������������������������
						mEndPos = userPos;
						
						//������������gameroominfo���������������������
						stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
						
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						
						victoryUser = iGameOver();
						if(victoryUser != -1){		
							//������������������push������������
							sendAllGameStateChange(uuid,null);
							
							flodWinProcess();
							
							sendAllGameStateChange(uuid,null);
							break;
						}
						
					}else if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
							&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
							&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){							

							
							if( stUserGameInfoBuilder.getLRemainStacks() > 0 &&bAllAllin==false){
								//���������������������������������������������������������������������
								stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
								stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
								
								stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
								stUserGameInfoBuilder.setIDelayLong(0);
								stUserGameInfoBuilder.setIDelaySingleTime(20);
								removeAllTimeOutUser();
								setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
								
								//������������gameroominfo���������������������
								stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
								
								stGameRoomInfo = stGameRoomInfoBuilder.build();
								
								sendAllGameStateChange(uuid,null);
								break;
							}
							else{

								stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
								if(checkAllAllin() == true){
									sendAllGameStateChange(uuid,null);
									bAllAllin = true;
									
									SidePot();
									
									stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
									stGameRoomInfoBuilder.setBAllAllin(1);
									
									for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
										UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
										
										if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
											&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
											&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
											
											if(bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0){
												stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
											}
										}
										
										//������������������������������������
										stResetUserGameInfoBuilder.setLBetStacks(0);
										
										//������������gameroominfo���������������������
										stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
									}
									
									lStepMaxStacks = 0;
									stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
									//������������
									stGameRoomInfo = stGameRoomInfoBuilder.build();
									//������������������
									switch(stGameRoomInfoBuilder.getEGameState().getNumber()){
										case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:{
											gotoFlopAllStatus(uuid);
										}
										break;
										case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:{
											checkGotoTurnStatus(uuid);
										}
										break;
										case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:{											
											checkGotoRiverStatus(uuid);
										}
										break;
										case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:{
											gotoResultStatus(uuid);
										}
										break;
									}
								}
							}
					}
				}
				
			}else{
				//������������������������
				eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
			}
		}
		return eErrorCode;
	}
	
	/****************************Push相关函数*******************************/
	@Override
	public void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
			long stToUserUuid,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType ,int pos){
		
		//给被邀请用户发通知
		TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();
			
		//获取时间
		long curTime = System.currentTimeMillis();
			
		// 设置msg id
		stGameRoomPushMsgBuilder.setLMsgID(curTime);
		// 设置发送时间
		stGameRoomPushMsgBuilder.setLCreateTime(curTime);
		
		// 设置发送人的userbaseinfo
		if(stFromUserGamenfo!=null){
			stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);
		}
		
		// 设置消息内容，暂时没有什么具体用处
		stGameRoomPushMsgBuilder.setSMsg("");
		
		stGameRoomPushMsgBuilder.setIPos(pos);
		
		// 设置 好友类型
		stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);
		
		checkAllAllin();
			
		
		if(stGameRoomInfo!=null){
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount() ; i ++){
				UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
				if(stUserGameInfoNetBuilder.getStUserInfo().getUuid()!=stToUserUuid){
					
					if(stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true){
						continue;
					}
					
					if(stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
							|| (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)){
						
						if(stUserGameInfoNetBuilder.getBShowCardOne()==1||stUserGameInfoNetBuilder.getBShowCardTwo()==1){
							continue;
						}
						
						if(stUserGameInfoNetBuilder.getEGameState()==USER_GAME_STATE.USER_GAME_STATE_FOLD){
							stUserGameInfoNetBuilder.clearVCurrentHands();
						}else{
							if(bFlodResult==true){
								stUserGameInfoNetBuilder.clearVCurrentHands();
							}
						}
					}else{
						stUserGameInfoNetBuilder.clearVCurrentHands();
					}
				}
				stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
			}
			
			stGameRoomPushMsgBuilder.setStGameRoomInfo(stGameRoomInfoBuilder);
		}
		
		
		TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
				.build();
	
		UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
		if(stUserBaseInfoNetBuilder!=null){			
			//发送push
			GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid,stGameRoomPushMsg);
		}
	}
	
	@Override
	public void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
			long stToUserUuid,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType){
		//给被邀请用户发通知
		TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder() ;
			
		//获取时间
		long curTime = System.currentTimeMillis();
			
		// 设置msg id
		stGameRoomPushMsgBuilder.setLMsgID(curTime);
		// 设置发送时间
		stGameRoomPushMsgBuilder.setLCreateTime(curTime);
		
		// 设置发送人的userbaseinfo
		if(stFromUserGamenfo!=null){
			stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);
		}
		
		// 设置消息内容，暂时没有什么具体用处
		stGameRoomPushMsgBuilder.setSMsg("");
		
		stGameRoomPushMsgBuilder.setIPos(0);
		
		// 设置 好友类型
		stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);
		
		checkAllAllin();
		
		if(stGameRoomInfo!=null){
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount() ; i ++){
				UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
				if(stUserGameInfoNetBuilder.getStUserInfo().getUuid()!=stToUserUuid){
					
					if(stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true){
						continue;
					}
					
					if(stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
							|| (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)){
						
						if(stUserGameInfoNetBuilder.getBShowCardOne()==1||stUserGameInfoNetBuilder.getBShowCardTwo()==1){
							continue;
						}
						
						if(stUserGameInfoNetBuilder.getEGameState()==USER_GAME_STATE.USER_GAME_STATE_FOLD){
							stUserGameInfoNetBuilder.clearVCurrentHands();
						}else{
							if(bFlodResult==true){
								stUserGameInfoNetBuilder.clearVCurrentHands();
							}
						}
					}else{
						stUserGameInfoNetBuilder.clearVCurrentHands();
					}
				}
				stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
			}
			
			stGameRoomPushMsgBuilder.setStGameRoomInfo(stGameRoomInfoBuilder);
		}
		
		TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
				.build();
	
		UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
		if(stUserBaseInfoNetBuilder!=null){
			//发送push
			GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid,stGameRoomPushMsg);
		}
	}
	
	@Override
	public void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType){
		if(stFromUserGamenfo!=null){
			int pos=-1;
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				if(stUserGameInfoNet.getStUserInfo().getUuid() == stFromUserGamenfo.getStUserInfo().getUuid()){
					pos = i;
				}
			}
			
			if(pos >=0){
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
					UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
					if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != stFromUserGamenfo.getStUserInfo().getUuid()){
						//给在牌桌上的人发push
						pushGameRoomUserMsgs(stFromUserGamenfo,stUserGameInfoNet.getStUserInfo().getUuid(),
								eGameRoomMessageType,pos);
					}
				}
				
				//给旁观的push
				for (long standUuid  : getStandByUuids()) {
					if(standUuid > 0){
						pushGameRoomUserMsgs(stFromUserGamenfo,standUuid,
								eGameRoomMessageType,pos);
					}
				}
			}
		}else{
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				pushGameRoomUserMsgs(stFromUserGamenfo,stUserGameInfoNet.getStUserInfo().getUuid(),
							eGameRoomMessageType,0);
			}
			
			//给旁观的push
			for (long standUuid  : getStandByUuids()) {
				if(standUuid > 0){
					pushGameRoomUserMsgs(stFromUserGamenfo,standUuid,
							eGameRoomMessageType,0);
				}
			}
		}
	}
	
	@Override
	public void pushAllGameRoomUserMsgsIncludeSelf(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType){
		if(stFromUserGamenfo!=null){
			int pos=-1;
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				if(stUserGameInfoNet.getStUserInfo().getUuid() == stFromUserGamenfo.getStUserInfo().getUuid()){
					pos = i;
				}
			}
			
			if(pos >=0){
				for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
					UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
					if(stUserGameInfoNet.getStUserInfo().getUuid() > 0){
						//给在牌桌上的人发push
						pushGameRoomUserMsgs(stFromUserGamenfo,stUserGameInfoNet.getStUserInfo().getUuid(),
								eGameRoomMessageType,pos);
					}
				}
				
				//给旁观的push
				for (long standUuid  : getStandByUuids()) {
					if(standUuid > 0){
						pushGameRoomUserMsgs(stFromUserGamenfo,standUuid,
								eGameRoomMessageType,pos);
					}
				}
			}
		}else{
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				pushGameRoomUserMsgs(stFromUserGamenfo,stUserGameInfoNet.getStUserInfo().getUuid(),
							eGameRoomMessageType,0);
			}
			
			//给旁观的push
			for (long standUuid  : getStandByUuids()) {
				if(standUuid > 0){
					pushGameRoomUserMsgs(stFromUserGamenfo,standUuid,
							eGameRoomMessageType,0);
				}
			}
		}
	}
	
	@Override
	public void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType,long exceptionUuid){
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
			UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
			if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid()!=exceptionUuid){
				pushGameRoomUserMsgs(stFromUserGamenfo,stUserGameInfoNet.getStUserInfo().getUuid(),
						eGameRoomMessageType,0);
			}
		}
		
		//给旁观的push
		for (long standUuid  : getStandByUuids()) {
			if(standUuid > 0 && standUuid!=exceptionUuid){
				pushGameRoomUserMsgs(stFromUserGamenfo,standUuid,
						eGameRoomMessageType,0);
			}
		}
	}
	
	@Override  
	public ERROR_CODE_TYPE doCallAction(long uuid,long betstacks,long lBettingID){
		ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
		synchronized(this){
			if(bPause == true){
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
			}
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			if(checkGameActionValidation()==false){
				return eErrorCode;
			}
			
			int victoryUser = -1;
			Integer userPos = getUserPos(uuid);
			
			if(userPos != null){
				//������������������
				int iLoop=0;
				
				//���������������������������
				for(int j = userPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				
					int userIndex = 0;
					if(j<iGameRoomUserMaxNums){
						userIndex = j;
					}else{
						userIndex = j- iGameRoomUserMaxNums;
					}
					
					//������������������
					UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
					
					if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){		
						
						if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
								||(lBettingID!=-1&&lBettingID!=stUserGameInfoBuilder.getLBettingID()&&lBettingID!=0)){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}
						
						if((stUserGameInfoBuilder.getLBetStacks() +  betstacks < this.lStepMaxStacks) 
								&& (stUserGameInfoBuilder.getLRemainStacks() - betstacks)!=0){
							
							boolean bCheck = (lStepMaxStacks < stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds())
									&& (betstacks+stUserGameInfoBuilder.getLBetStacks() == stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
							if(!bCheck){
								eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
								break;
							}
						}else if(stUserGameInfoBuilder.getLBetStacks() +  betstacks > this.lStepMaxStacks){
							eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
							break;
						}
						
						TableLogService.getInstance().error(lFromRoomID
								+"," + lGameRoomID
								+"," + stGameRoomInfo.getLGameHandID()
								+"," + uuid
								+"," + "call" 
								+"," + betstacks);
						
						//������������������������
						stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CALL);
						stUserGameInfoBuilder.setIDelayLong(0);
						stUserGameInfoBuilder.setIDelaySingleTime(20);
						
						//������������������
						stUserGameInfoBuilder.setLBetStacks(betstacks + stUserGameInfoBuilder.getLBetStacks());
						//������������������
						stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - betstacks);
						
						
						//������������gameroominfo���������������������
						stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);

						stGameRoomInfo = stGameRoomInfoBuilder.build();
						
						
						//���������������allin������
						if(stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River){
							
							//���������������allin������
							if(checkAllAllin() == true){
								sendAllGameStateChange(uuid,null);
								stGameRoomInfoBuilder.setBAllAllin(1);
								stGameRoomInfo = stGameRoomInfoBuilder.build();
								SidePot();
								bAllAllin = true;
								stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
								stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
								lStepMaxStacks = 0;
								
								//������������������������
								for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
									UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
									
									if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
											&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
											&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
										stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

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
									
									stGameRoomInfo = stGameRoomInfoBuilder.build();
								}
								break;
							}
						}
						
						victoryUser = iGameOver();
						if(victoryUser != -1){		
							//������������������push������������
							sendAllGameStateChange(uuid,null);
							
							flodWinProcess();
							
							sendAllGameStateChange(uuid,null);
							break;
						}
					}else if(stUserGameInfoBuilder.getStUserInfo().getUuid() != 0 ){
						
						//������������������������,������������������������������������������������������������������������������������������������������������������������
						if(userIndex == mEndPos){
							//������������������������������������
							sendAllGameStateChange(uuid,null);
							SidePot();
							stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
							
							if(stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River){
								
								//���������������allin������
								if(checkAllAllin() == true){
									bAllAllin = true;
									stGameRoomInfoBuilder.setBAllAllin(1);
								}
								
								if(iCouldPlayNum == 2){
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
														stResetUserGameInfoBuilder.setIDelayLong(0);
														stResetUserGameInfoBuilder.setIDelaySingleTime(20);
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
														stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
														
													}else{
														stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
													}
													
													removeAllTimeOutUser();
													setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
												}else{
													stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
												}
											}else{
												stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
											}

											if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
												UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
												stUserBaseInfo.setUuid(0);
												stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
											}
										}
										
										stResetUserGameInfoBuilder.setLBetStacks(0);
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
															stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
															stResetUserGameInfoBuilder.setIDelayLong(0);
															stResetUserGameInfoBuilder.setIDelaySingleTime(20);
															stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
															stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
														}else{
															stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
														}

														removeAllTimeOutUser();
														setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
														iBetting = 1;
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
							}
							
							stGameRoomInfo = stGameRoomInfoBuilder.build();
							//������������������
							switch(stGameRoomInfoBuilder.getEGameState().getNumber()){
								case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:{
									gotoFlopAllStatus(uuid);
								}
								break;
								case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:{
									checkGotoTurnStatus(uuid);
								}
								break;
								case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:{											
									checkGotoRiverStatus(uuid);
								}
								break;
								case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:{
									gotoResultStatus(uuid);
								}
								break;
							}
							break;
						}
						else if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
								&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
								&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT
								){
							//���������������������������������������������������������������������
							if(stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin==false){
								stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
								stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

								stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
								stUserGameInfoBuilder.setIDelayLong(0);
								stUserGameInfoBuilder.setIDelaySingleTime(20);
								removeAllTimeOutUser();
								setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
								
								//������������gameroominfo���������������������
								stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
								
								stGameRoomInfo = stGameRoomInfoBuilder.build();
								
								sendAllGameStateChange(uuid,null);
								break;
							}
						}
					}
				}
				
				
			}else{
				eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
			}
		}
		return eErrorCode;
	}
	
	/**
	 * 弃牌公共函数
	 * @param uuid			弃牌的用户
	 * @param userPos		用户所在位置
	 * @param lBettingID	手牌动作ID
	 * @return
	 */	
	@Override  
	public ERROR_CODE_TYPE doFlodAction(long uuid,int userPos,long lBettingID,boolean bDeleggate){
		ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;

		if(bPause == true){
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
		}
		
		synchronized(this){
			if(checkGameActionValidation()==false){
				return eErrorCode;
			}
			//初始化变量
			int victoryUser = -1;
			
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
				//初始化循环变量
				int iLoop=0;
			//遍历所有打牌的人
			for(int j = userPos ; iLoop < iGameRoomUserMaxNums;iLoop++,j++ ){
				int userIndex = 0;
				if(j<iGameRoomUserMaxNums){
					userIndex = j;
				}else{
					userIndex = j - iGameRoomUserMaxNums;
				}

				//获取打牌的人的信息
				UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
	
				if(userIndex ==  userPos){
	                //如果是弃牌的用户

	                //首先检查是否是弃牌状态，bettingID是否ok
					if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING 
							||(lBettingID!=-1&&lBettingID!=stUserGameInfoBuilder.getLBettingID()&&lBettingID!=-2)){
						eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
						break;
					}
					
					if(lBettingID==-1){
	                    //自动弃牌日志
						if(bDeleggate){
							TableLogService.getInstance().error(lFromRoomID
									+"," + lGameRoomID
									+"," + stGameRoomInfo.getLGameHandID()
									+"," + uuid
									+"," + "fold" 
									+"," + "tuoguan");
						}else{
							TableLogService.getInstance().error(lFromRoomID
									+"," + lGameRoomID
									+"," + stGameRoomInfo.getLGameHandID()
									+"," + uuid
									+"," + "fold" 
									+"," + "auto");
						}
					}else{
	                    //主动弃牌日志
						TableLogService.getInstance().error(lFromRoomID
								+"," + lGameRoomID
								+"," + stGameRoomInfo.getLGameHandID()
								+"," + uuid
								+"," + "fold" 
								+"," + 0);
					}

					//设置弃牌状态
					stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_FOLD);
					stUserGameInfoBuilder.setIDelayLong(0);
					stUserGameInfoBuilder.setIDelaySingleTime(20);

					//设置用户的牌局状态
					stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();


					//检查牌局是否结束
					victoryUser = iGameOver();
					if(victoryUser != -1){		
						//如果牌局已，则进入牌局输赢结算状态
						sendAllGameStateChange(uuid,null);
						
						flodWinProcess();
						
						sendAllGameStateChange(uuid,null);
						break;
					}
				}else if(stUserGameInfoBuilder.getStUserInfo().getUuid() != 0){			
					if(userIndex == mEndPos){
	                    //如果下一个位置就是结束的位置，说明本次弃牌是本轮的最后一个动作
	                    //此时需要变换牌局的状态
						sendAllGameStateChange(uuid,null);
						
						SidePot();
						stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

						if(checkAllAllin() == true){
							stGameRoomInfoBuilder.setBAllAllin(1);
							bAllAllin = true;
						}
						
						if(stGameRoomInfoBuilder.getEGameState()!=GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River){
	                        mEndPos = mSmallBlindPos;
	                        lStepMaxStacks = 0;
	                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
	                        int iBetting = 0;
	                        int iLoop1 = 0;
								
	                        //重置牌局状态
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
	                                    && stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_READY
	                                    && stResetUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){

	                                if(stResetUserGameInfoBuilder.getLRemainStacks() > 0){
	                                    //找到小盲后第一个可以出牌的用户
	                                    if(iBetting==0){
	                                        if(bAllAllin == false){
	                                            stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
	                                            stResetUserGameInfoBuilder.setIDelayLong(0);
	                                            stResetUserGameInfoBuilder.setIDelaySingleTime(20);
	                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
	                                            stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

	                                            //设置该用户的超时
	                                            removeAllTimeOutUser();
	                                            setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());

	                                            iBetting = 1;
	                                        }else{
	                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
	                                        }
	                                    }else{
	                                        //其他用户设置为Ready状态
	                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
	                                    }
	                                }
	                                else{
	                                    //没有筹码了，则为allin的状态
	                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
	                                }

	                                //如果该用户离开或者站起了，则重新设置该位置的用户信息
	                                if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
	                                    UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
	                                    stUserBaseInfo.setUuid(0);
	                                    stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
	                                }
	                            }
									
	                            //设置已下筹码为0
	                            stResetUserGameInfoBuilder.setLBetStacks(0);
								
	                            //重置用户牌局信息
	                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
	                        }
	                    }
								
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						//发牌相关操作
						switch(stGameRoomInfoBuilder.getEGameState().getNumber()){
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:{
								//Flop发牌操作
								gotoFlopAllStatus(uuid);
							}
							break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:{
								//发turn一张牌
								checkGotoTurnStatus(uuid);
							}
							break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:{
								//发river牌
								//保护，发这张牌时，排面上只能有3张牌
								checkGotoRiverStatus(uuid);
							}
							break;
							case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:{												
								gotoResultStatus(uuid);
							}
							break;
						}
						break;
					}
					else if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
							&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_FOLD
							&& stUserGameInfoBuilder.getEGameState()!=USER_GAME_STATE.USER_GAME_STATE_WAIT){
						
						if(checkAllAllin() == true){
							stGameRoomInfoBuilder.setBAllAllin(1);
						}
						
	                    //如果用户不是最后一个出牌的用户
						if(stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin==false){
							//设置该用户为下一个需要行动的用户
							stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
							stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
							stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
							stUserGameInfoBuilder.setIDelayLong(0);
							stUserGameInfoBuilder.setIDelaySingleTime(20);

	                        //重置牌局用户超时
							removeAllTimeOutUser();
							setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
							
	                        //设置状态
							stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
							stGameRoomInfo = stGameRoomInfoBuilder.build();

	                        //下发状态变化
							sendAllGameStateChange(uuid,null);
							break;
						}else{

							stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
							if(bAllAllin == true){
								sendAllGameStateChange(uuid,null);
								
								SidePot();
								
								stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
								stGameRoomInfoBuilder.setBAllAllin(1);
								
								for(int k = 0 ; k < iGameRoomUserMaxNums;k++ ){
									UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
									
									if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
										&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
										&&stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
										
										if(bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0){
											stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
										}
									}
									
									//������������������������������������
									stResetUserGameInfoBuilder.setLBetStacks(0);
									
									//������������gameroominfo���������������������
									stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
								}
								
								lStepMaxStacks = 0;
								stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
								//������������
								stGameRoomInfo = stGameRoomInfoBuilder.build();
								//������������������
								switch(stGameRoomInfoBuilder.getEGameState().getNumber()){
									case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE:{
										gotoFlopAllStatus(uuid);
									}
									break;
									case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE:{
										checkGotoTurnStatus(uuid);
									}
									break;
									case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE:{											
										checkGotoRiverStatus(uuid);
									}
									break;
									case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE:{
										gotoResultStatus(uuid);
									}
									break;
								}
							}
						}
					}
				}
			}	
			
		}
		return eErrorCode;
	}
	
	@Override  
	public void calculateWinner(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		
		List<GameUserResultInfo> cardStrengths = new ArrayList<GameUserResultInfo>();
		
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
			UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);
			
			Long uuid = mHandPos.get(i);
			if(uuid!=null && uuid > 0){
				if(stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CALL 
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CHECK
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_RAISE
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND
						|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY){
					HandInfo stHandInfo =  this.mHandMap.get(uuid);
					if(stHandInfo!=null && stHandInfo.getvCards()!=null &&stHandInfo.getvCards().size()==2){
						GameUserResultInfo stGameUserResultInfo = calculateCardStrength(stHandInfo.getvCards());
						cardStrengths.add(stGameUserResultInfo);
					}else{
						GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
						cardStrengths.add(stGameUserResultInfo);
					}
				}
				else{
					GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
					cardStrengths.add(stGameUserResultInfo);
				}
			}
			else{
				GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
				cardStrengths.add(stGameUserResultInfo);
			}
		}
		
		GameResultInfo.Builder stGameResultInfoBuilder = GameResultInfo.newBuilder();
		long lSingleMaxPot = 0;
		for(int index = 0 ; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++){
			List<GameUserResultInfo> winnerArray = new ArrayList<GameUserResultInfo>();
			Set<Integer> winnerIDSet = new HashSet<Integer>();
			Set<Long> winnerUuidSet = new HashSet<Long>();
			
			List<Integer> winnerIDArray = new ArrayList<Integer>(winnerIDSet);
			List<Long> winnerUuidArray = new ArrayList<Long>(winnerUuidSet);
			
			PotInfo stPotInfo = mPotList.get(index);
			List<Integer> posArray = stPotInfo.playerListpos;

			int maxStrength = 0;
			for(int i = 0 ; i < posArray.size() ; i++){
				int pos = posArray.get(i);
				long winuuid = stPotInfo.playerList.get(i);
				GameUserResultInfo stGameUserResultInfo = cardStrengths.get(pos);

				if(stGameUserResultInfo.mCardStrength == maxStrength){
					
					if(winnerArray.size() > 0){
						GameUserResultInfo stMaxGameUserResult = winnerArray.get(winnerArray.size()-1);
						int bMore =  CompareSameLevelUser(stGameUserResultInfo,stMaxGameUserResult);
						if(bMore > 0){
							winnerArray.clear();
							winnerArray.add(stGameUserResultInfo);
							
							winnerUuidSet.clear();
							winnerUuidArray.clear();
							winnerIDSet.clear();
							winnerIDArray.clear();
							
							winnerIDSet.add(pos);
							winnerIDArray.add(pos);
							winnerUuidSet.add(winuuid);
							winnerUuidArray.add(winuuid);
						}else if(bMore == 0){
							if(!winnerUuidSet.contains(winuuid)){
								winnerArray.add(stGameUserResultInfo);
								
								winnerIDSet.add(pos);
								winnerIDArray.add(pos);
								winnerUuidSet.add(winuuid);
								winnerUuidArray.add(winuuid);
							}
						}
					}else{
						if(!winnerUuidSet.contains(winuuid)){
							winnerArray.add(stGameUserResultInfo);
							
							winnerIDSet.add(pos);
							winnerIDArray.add(pos);
							winnerUuidSet.add(winuuid);
							winnerUuidArray.add(winuuid);
						}
					}
				}else if(stGameUserResultInfo.mCardStrength > maxStrength){
					winnerArray.clear();
					winnerArray.add(stGameUserResultInfo);
										
					winnerIDSet.clear();
					winnerIDArray.clear();
					winnerUuidSet.clear();
					winnerUuidArray.clear();
					
					winnerIDSet.add(pos);
					winnerIDArray.add(pos);
					winnerUuidSet.add(winuuid);
					winnerUuidArray.add(winuuid);
					
					maxStrength = stGameUserResultInfo.mCardStrength;
				}
			}
			
			if(winnerIDArray.size()!=0){
				GamePotResultInfo.Builder  stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();
				
				int winStacks = stGameRoomInfo.getVCurrentPots(index)/winnerIDArray.size();
				lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);
				for(int i = 0 ; i < winnerIDArray.size() ; i++){
					int userPos  =  winnerIDArray.get(i);
					long winUuid = winnerUuidArray.get(i);

					for(int j = 0 ; j < stGameRoomInfo.getVUserGameInfosCount();j++){
						if(j == userPos){
							UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();
							if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid() == winUuid ){		
								stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
								stGamePotResultInfoBuilder.addLUserPots(winStacks);
																			
								stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()+winStacks);

								stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());		
							}
							else{
								UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
								if(stUserStandByGameInfoBuilder != null){
									stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
									stGamePotResultInfoBuilder.addLUserPots(winStacks);
				
									stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks()+winStacks);

									mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);		
									BillManager.getInstance().setCurrentStacksInRoom(winUuid, 
											stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserStandByGameInfoBuilder.getLRemainStacks());
								}
								
								for(int chgangeIndex = 0 ; chgangeIndex < stGameRoomInfo.getVUserGameInfosCount();chgangeIndex++){
									
									UserGameInfoNet.Builder stUserGameInfoBuilderChange = stGameRoomInfoBuilder.getVUserGameInfos(chgangeIndex).toBuilder();

									if(stUserGameInfoBuilderChange!=null&&stUserGameInfoBuilderChange.getStUserInfo().getUuid()==winUuid){
										stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
										stGamePotResultInfoBuilder.addLUserPots(winStacks);

										stUserGameInfoBuilderChange.setLRemainStacks(stUserGameInfoBuilderChange.getLRemainStacks()+winStacks);

										stGameRoomInfoBuilder.setVUserGameInfos(chgangeIndex, stUserGameInfoBuilderChange.build());
									}
								}
							}	
						}
					}
				}
				stGameResultInfoBuilder.addStGamePotResultInfo(stGamePotResultInfoBuilder);
			}
		}
		
		//计算最大底池数据
		if(lSingleMaxPot > lMaxPot){
			lMaxPot = lSingleMaxPot;
		}
		
		for(int i = 0 ; i < vOriginalStacks.size() ; i++){
			long stack = vOriginalStacks.get(i);
			stGameResultInfoBuilder.addVlOriginalUserStacks(stack);
		}
		
		stGameRoomInfoBuilder.setStGameResultInfo(stGameResultInfoBuilder);
		
		//计算统计数据
		Map<Long,Long> mapWinStacks = new ConcurrentHashMap<Long,Long>();
		for(GamePotResultInfo stGamePotResultInfo:stGameResultInfoBuilder.getStGamePotResultInfoList()){
			for(int p = 0;p < stGamePotResultInfo.getStUserWinerCount();p++){
				long uuid = stGamePotResultInfo.getStUserWiner(p).getUuid();
				long winStacks = stGamePotResultInfo.getLUserPots(p);
				
				Long stacks = mapWinStacks.get(uuid);
				if(stacks==null){
					mapWinStacks.put(uuid, winStacks);
				}else{
					mapWinStacks.put(uuid, stacks+winStacks);
				}
			}
		}
		
		//打印日志
		Iterator iter = mapWinStacks.entrySet().iterator(); 
		while (iter.hasNext()) { 
			Map.Entry entry = (Map.Entry) iter.next(); 
			Long uuid = (Long) entry.getKey();
			Long winStacks = (Long) entry.getValue();
			
			TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
					+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
					+"," + stGameRoomInfo.getLGameHandID()
					+"," + uuid
					+"," + "showdown" 
					+"," + "show" 
					+"," + winStacks);
			
			stGameRoomInfo = stGameRoomInfoBuilder.build();
		}
	
		try{
			for(long uuid:vHandList){
				HandInfo stHandInfo = mHandMap.get(uuid);
				if(stHandInfo!=null){
					TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
							+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							+"," + stGameRoomInfo.getLGameHandID()
							+"," + uuid
							+"," + "hand" 
							+"," + stHandInfo.getvCards().get(0)
							+"," + stHandInfo.getvCards().get(1)
							+"," + stHandInfo.getlRemainStacks());
				}
			}
		}catch(Exception e){
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
			UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

			if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
				stResetUserGameInfoBuilder.setLBetStacks(0);
			}
			
			if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
				UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
				stUserBaseInfo.setUuid(0);
				stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
			}
			
			stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
		}

		stGameRoomInfo = stGameRoomInfoBuilder.build();
		stGameRoomInfo = InsuranceApi.getInsuranceBill(stGameRoomInfo,stGameRoomInfo.getLGameHandID());
		ArrayList<Long> vSitdownUsers = new ArrayList<Long>();
		//设置结果,实时账单信息
		for(int index = 0 ; index < stGameRoomInfo.getVUserGameInfosCount();index++){
			UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(index);
			if(stUserGameInfo.getStUserInfo().getUuid() > 0){
				System.out.println("结账111 : " + stUserGameInfo.getStUserInfo().getUuid());
				//设置账单
				BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfo.getStUserInfo().getUuid(), 
						stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
						, stUserGameInfo.getLRemainStacks());
				vSitdownUsers.add(stUserGameInfo.getStUserInfo().getUuid());
			}
		}
		
		Iterator iter1 = mapStandByUserGameInfos.entrySet().iterator(); 
		while (iter1.hasNext()) { 
			Map.Entry entry = (Map.Entry) iter1.next(); 
			UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder)entry.getValue(); 
			if(stUserGameInfoBuilder!=null){
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
						&& !vSitdownUsers.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())){
					System.out.println("结账112 : " + stUserGameInfoBuilder.getStUserInfo().getUuid());
					//设置账单
					BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(), 
							stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							, stUserGameInfoBuilder.getLRemainStacks());
				}
			}
		}
		
		bFlodResult = false;
	}

	
	/****************************基本辅助函数*******************************/
	Comparator<Integer> comparatorHands = new Comparator<Integer>(){
		   public int compare(Integer s1, Integer s2) {
			   int cardNumberLeft;			   
			   int cardNumberRight;
			   
			   //���������������
			   cardNumberLeft = s1%13 + 1;
			   cardNumberRight = s2%13 + 1;
			   
			   if(cardNumberLeft == 1){
				   cardNumberLeft = 14;
			   }
			   
			   if(cardNumberRight == 1){
				   cardNumberRight = 14;
			   }
			   return  cardNumberRight - cardNumberLeft;
		   }
	};
	
	@Override
	public GameUserResultInfo calculateCardStrength(List<Integer> handCards){
		List<Integer> pairList = new ArrayList<Integer>();
		List<Integer> triList = new ArrayList<Integer>();
		List<Integer> singleList = new ArrayList<Integer>();
		
		int four = 0 ;
		
		List<Integer> maxSpadeFlushList = new ArrayList<Integer>();
		List<Integer> maxHeartFlushList = new ArrayList<Integer>();
		List<Integer> maxClubFlushList = new ArrayList<Integer>();
		List<Integer> maxDiamondFlushList = new ArrayList<Integer>();
		
		List<Integer> maxStrightList = new ArrayList<Integer>();
		List<Integer> maxPairList = new ArrayList<Integer>();
		List<Integer> maxTriList = new ArrayList<Integer>();
		List<Integer> maxSingleList = new ArrayList<Integer>();
		List<Integer> maxFourList = new ArrayList<Integer>();
		
		List<Integer> cardList = new ArrayList<Integer>(handCards);
		cardList.addAll(stGameRoomInfo.getVCurrentCardsList());
		
		Collections.sort(cardList,comparatorHands);
		
		int preCard = 0;
		int pairCount = 0;
		int lastIndex = 0;
		for(int i = 0 ; i < cardList.size() ; i++){
			int cardNumber = cardList.get(i)%13 + 1;
			if(cardNumber == 1){
				cardNumber = 14;
			}
			if(i==0){
				pairCount ++;
			}
			else{
				if(preCard == cardNumber){
					pairCount ++;
				}else{
					switch(pairCount){
					case 1:
						singleList.add(preCard);

						maxSingleList.add(cardList.get(lastIndex));
						break;
					case 2:
						pairList.add(preCard);
						
						maxPairList.add(cardList.get(lastIndex));
						maxPairList.add(cardList.get(lastIndex+1));
						break;
					case 3:
						triList.add(preCard);
						
						maxTriList.add(cardList.get(lastIndex));
						maxTriList.add(cardList.get(lastIndex+1));
						maxTriList.add(cardList.get(lastIndex+2));
						break;
					case 4:
						four = preCard;
						
						maxFourList.add(cardList.get(lastIndex));
						maxFourList.add(cardList.get(lastIndex+1));
						maxFourList.add(cardList.get(lastIndex+2));
						maxFourList.add(cardList.get(lastIndex+3));
						break;
					}
					lastIndex = i;
					pairCount = 1;
				}
			}
			
			preCard = cardNumber;
		}
		
		//补充最后一个逻辑
		switch(pairCount){
		case 1:
			singleList.add(preCard);
			maxSingleList.add(cardList.get(lastIndex));
			break;
		case 2:
			pairList.add(preCard);
			
			maxPairList.add(cardList.get(lastIndex));
			maxPairList.add(cardList.get(lastIndex+1));
			break;
		case 3:
			triList.add(preCard);
			
			maxTriList.add(cardList.get(lastIndex));
			maxTriList.add(cardList.get(lastIndex+1));
			maxTriList.add(cardList.get(lastIndex+2));
			break;
		case 4:
			four = preCard;
			
			maxFourList.add(cardList.get(lastIndex));
			maxFourList.add(cardList.get(lastIndex+1));
			maxFourList.add(cardList.get(lastIndex+2));
			maxFourList.add(cardList.get(lastIndex+3));
			break;
		}

		int iMaxStraightNum = 0;
		int iStraightNum = 1;
		int iNextStraightCard = 0;
		preCard = 0;
		int iStraightCard = 0;
		int heartNum = 0;
		int spadeNum = 0;
		int clubNum = 0;
		int diamondNum = 0;
		
		ArrayList<Integer> vTempMaxStrightList = new ArrayList<Integer>();
		for(int i = 0 ; i < cardList.size() ; i++){
			
			int cardNumber = cardList.get(i)%13 + 1;
			if(cardNumber==1){
				cardNumber = 14;
			}
			
			int cardType = cardList.get(i)/13 + 1;
			
			switch(cardType){
			case 1:
				spadeNum++;
				maxSpadeFlushList.add(cardList.get(i));
				break;
			case 2:
				heartNum++;
				maxHeartFlushList.add(cardList.get(i));
				break;
			case 3:
				clubNum++;
				maxClubFlushList.add(cardList.get(i));
				break;
			case 4:
				diamondNum ++;
				maxDiamondFlushList.add(cardList.get(i));
				break;
			}
			
			if(i!= 0){
				if(preCard - 1 == cardNumber){
					iStraightNum++;
					vTempMaxStrightList.add(cardList.get(i));
				}else if(preCard - 1 > cardNumber){
					if(iStraightNum > iMaxStraightNum){
						iMaxStraightNum = iStraightNum;
						iStraightCard = iNextStraightCard;
						maxStrightList = new ArrayList<Integer>(vTempMaxStrightList);
					}
					
					iNextStraightCard = cardNumber;
					vTempMaxStrightList.clear();
					vTempMaxStrightList.add(cardList.get(i));
					iStraightNum = 1;
				}
			}else{
				iNextStraightCard = cardNumber;
				vTempMaxStrightList.add(cardList.get(i));
				iStraightNum = 1;
			}

			preCard = cardNumber;	
		}
		
		if(iStraightNum > iMaxStraightNum){
			iMaxStraightNum = iStraightNum;
			iStraightCard = iNextStraightCard;
			maxStrightList = vTempMaxStrightList;
		}
		
		if(heartNum >= 5){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			calculateFlush(stGameUserResultInfo,iMaxStraightNum,maxHeartFlushList,handCards);			
			return stGameUserResultInfo;
		}else if(spadeNum >= 5){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			calculateFlush(stGameUserResultInfo,iMaxStraightNum,maxSpadeFlushList,handCards);
			return stGameUserResultInfo;
		}else if(clubNum>= 5){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			calculateFlush(stGameUserResultInfo,iMaxStraightNum,maxClubFlushList,handCards);			
			return stGameUserResultInfo;
			
		}else if(diamondNum>=5){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			calculateFlush(stGameUserResultInfo,iMaxStraightNum,maxDiamondFlushList,handCards);	
			return stGameUserResultInfo;
		}
		
		if(iMaxStraightNum >= 5){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 5;
			stGameUserResultInfo.iStraightCard = iStraightCard;
			
			
			if(maxStrightList.size() >= 5){
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxStrightList.subList(0, 5));
			}
			
			return stGameUserResultInfo;
		}else if(iMaxStraightNum == 4){
			if(iStraightCard == 5 && cardList.size() == 7){
				int cardNumber = cardList.get(0)%13 + 1;
				
				if(cardNumber==1){
					GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
					stGameUserResultInfo.mCardStrength = 5;
					stGameUserResultInfo.iStraightCard = iStraightCard;
					
					if(maxStrightList.size() >= 4){
						stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxStrightList.subList(0, 4));
					}
					stGameUserResultInfo.finalCards.add(cardList.get(0));
					return stGameUserResultInfo;
				}
			}
		}

		if(four > 0){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 8;
			stGameUserResultInfo.four= four;
			
			if(maxFourList.size() == 4){
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxFourList);
			}
			
			if(maxTriList.size() > 0){
				stGameUserResultInfo.finalCards.add(maxTriList.get(0));
				stGameUserResultInfo.singleList.add(maxTriList.get(0));
			}else if(maxPairList.size() > 0){
				stGameUserResultInfo.finalCards.add(maxPairList.get(0));			
				int tempCard1 = maxPairList.get(0)%13 + 1;
				int tempCard2 = maxSingleList.get(0)%13 + 1;	
				
				if(tempCard1 == 1){
					tempCard1 = 14;
				}
				
				if(tempCard2 == 1){
					tempCard2 = 14;
				}
				
				if(tempCard1 > tempCard2){
					stGameUserResultInfo.finalCards.add(maxPairList.get(0));
					stGameUserResultInfo.singleList.add(maxPairList.get(0));
				}else{
					stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
					stGameUserResultInfo.singleList.add(maxSingleList.get(0));
				}
			}
			else if(maxSingleList.size() > 0){
				stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
				stGameUserResultInfo.singleList.add(maxSingleList.get(0));
			}
			return stGameUserResultInfo;
		}
		
		if(triList.size() == 2 ||(triList.size() == 1 && pairList.size() >= 1)){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 7;
			stGameUserResultInfo.triList.add(triList.get(0));
			
			if(pairList.size() > 0){
				stGameUserResultInfo.pairList.add(pairList.get(0));
			}else {
				stGameUserResultInfo.pairList.add(triList.get(1));
			}
			
			if(maxTriList.size() == 6){
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 5));
			}else{
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 3));
				stGameUserResultInfo.finalCards.add(maxPairList.get(0));
				stGameUserResultInfo.finalCards.add(maxPairList.get(1));
			}
			
			return stGameUserResultInfo;
		}
		
		if(triList.size() == 1){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 4;
			stGameUserResultInfo.triList.add(triList.get(0));
			stGameUserResultInfo.singleList.add(singleList.get(0));
			stGameUserResultInfo.singleList.add(singleList.get(1));
			
			if(maxTriList.size() == 3){
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 3));
				stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
				stGameUserResultInfo.finalCards.add(maxSingleList.get(1));
			}
			
			return stGameUserResultInfo;
		}
		
		if(pairList.size() >= 2){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 3;
			stGameUserResultInfo.pairList.add(pairList.get(0));
			stGameUserResultInfo.pairList.add(pairList.get(1));
					
			if(pairList.size() == 2){
				stGameUserResultInfo.singleList.add(singleList.get(0));
			}else{
				int pairSingle = pairList.get(2);
				int single = singleList.get(0);
				if(pairSingle > single){
					stGameUserResultInfo.singleList.add(pairSingle);
				}else{
					stGameUserResultInfo.singleList.add(single);
				}
			}
			
			if(maxPairList.size() >= 4){
				stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxPairList.subList(0, 4));
				
				if(maxPairList.size() == 4){
					stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
				}else{
					int pairSingle = pairList.get(2);
					int single = singleList.get(0);
					if(pairSingle > single){
						stGameUserResultInfo.finalCards.add(maxPairList.get(4));
					}else{
						stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
					}
				}
			}
			
			return stGameUserResultInfo;
		}
		
		if(pairList.size() ==1){
			GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
			stGameUserResultInfo.mCardStrength = 2;
			stGameUserResultInfo.pairList.add(pairList.get(0));
			stGameUserResultInfo.singleList.add(singleList.get(0));
			stGameUserResultInfo.singleList.add(singleList.get(1));
			stGameUserResultInfo.singleList.add(singleList.get(2));
			
			stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxPairList.subList(0, 2));
			stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
			stGameUserResultInfo.finalCards.add(maxSingleList.get(1));
			stGameUserResultInfo.finalCards.add(maxSingleList.get(2));
			
			return stGameUserResultInfo;
		}
		
		GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
		stGameUserResultInfo.mCardStrength = 1;
		stGameUserResultInfo.singleList.add(singleList.get(0));
		stGameUserResultInfo.singleList.add(singleList.get(1));
		stGameUserResultInfo.singleList.add(singleList.get(2));
		stGameUserResultInfo.singleList.add(singleList.get(3));
		stGameUserResultInfo.singleList.add(singleList.get(4));
		
		stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxSingleList.subList(0, 5));
		
		return stGameUserResultInfo;
	}
	
	@Override
	public void calculateFlush(GameUserResultInfo stGameUserResultInfo,
			int iMaxStraightNum, List<Integer> maxFlushList,
			List<Integer> handCards) 
	{
	    // 最大牌号，不带花色
	    int maxCard = 0;
	    // 如果顺子技术大于等于5，则有可能出现同花顺
	    if (iMaxStraightNum >= 4) {

	        int preNum = 0;
	        List<Integer> maxFlushStrengthList = new ArrayList<Integer>();
	        List<Integer> tempMaxFlushStrengthList = new ArrayList<Integer>();

	        for (int i = 0; i < maxFlushList.size(); i++) {
	            int cur = maxFlushList.get(i);
	            int curNum = cur % 13 +1;
	            if (curNum == 1) {
	                curNum = 14;
	            }
	            
	            if (i == 0) {
	                preNum = curNum;
	                tempMaxFlushStrengthList.add(cur);
	            }else {
	                if (preNum - curNum == 1) {
	                	tempMaxFlushStrengthList.add(cur);
	                }else {
	                    if (tempMaxFlushStrengthList.size() > maxFlushStrengthList.size()) {
	                    	maxFlushStrengthList = new ArrayList<Integer>(tempMaxFlushStrengthList);
	                    }
	                    tempMaxFlushStrengthList.clear();
	                    tempMaxFlushStrengthList.add(cur);
	                }
	                preNum = curNum;
	            }
	        }
	        
	        if (tempMaxFlushStrengthList.size() > maxFlushStrengthList.size()) {
	        	maxFlushStrengthList = new ArrayList<Integer>(tempMaxFlushStrengthList);
	        }
	        
	        if (maxFlushStrengthList.size() >= 5) {
	            stGameUserResultInfo.mCardStrength = 9;
	            int max = maxFlushStrengthList.get(0);
	            maxCard = max % 13 +1;
	            if (maxCard == 1) {
	                maxCard = 14;
	            }
	            stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(0));
	            stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(1));
	            stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(2));
	            stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(3));
	            stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(4));
	            
	            stGameUserResultInfo.iStraightCard = maxCard;
	        } else if (maxFlushStrengthList.size() == 4) {
	            
	            int firstNum = maxFlushList.get(0);
	            int first = firstNum % 13 +1;
	            if (first == 1) {
	                first = 14;
	            }
	            
	            int flushStrengthMaxNum = maxFlushStrengthList.get(0);
	            int flushStrengthMax = flushStrengthMaxNum % 13 +1;
	            if (flushStrengthMax == 1) {
	                flushStrengthMax = 14;
	            }
	            
	            if (flushStrengthMax == 5 && first == 14) {
	                stGameUserResultInfo.mCardStrength = 9;
	                stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(0));
	                stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(1));
	                stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(2));
	                stGameUserResultInfo.finalCards.add(maxFlushStrengthList.get(3));
	                
	                stGameUserResultInfo.iStraightCard = flushStrengthMax;
	            }else {
	                stGameUserResultInfo.mCardStrength = 6;
	            }
	        } else {
	            stGameUserResultInfo.mCardStrength = 6;
	        }
	    } else {
	        // 无顺子，牌型为同花
	        stGameUserResultInfo.mCardStrength = 6;
	    }
	    
	    if (stGameUserResultInfo.mCardStrength == 6) {
	    	int handCard1 = handCards.get(0)% 13 + 1;
	    	int handCard2 = handCards.get(1)% 13 + 1;
	        
	        if (handCard1 == 1) {
	            handCard1 = 14;
	        }
	       
	        if (handCard2 == 1) {
	            handCard2 = 14;
	        }
	        
	        int handCard1Type = handCards.get(0) / 13 + 1;
	        
	        int handCard2Type = handCards.get(1) / 13 + 1;
	        
	        // 首先找到手牌中的同花，如果两张同花，找比较大的那个
	        if (handCard1Type == 2 && handCard2Type == 2) {
	            if (handCard1 > handCard2) {
	                stGameUserResultInfo.flushMaxHandCard = handCard1;
	            } else {
	                stGameUserResultInfo.flushMaxHandCard = handCard2;
	            }
	        } else if (handCard1Type == 2) {
	            stGameUserResultInfo.flushMaxHandCard = handCard1;
	        } else if (handCard2Type == 2) {
	            stGameUserResultInfo.flushMaxHandCard = handCard2;
	        }
	        
	        // 取出同花中最小的有效牌
	        int tableCard = maxFlushList.get(4)%13+1;
	        // 通手牌中较大的同花比较，如果手牌大，则同为同花的参考是手牌
	        // 如果手牌小，则面上的牌就是比较牌型
	        if (tableCard > stGameUserResultInfo.flushMaxHandCard) {
	            stGameUserResultInfo.flushMaxHandCard = 0;
	        }
	        
	        if (maxFlushList.size() >= 5) {
	            stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxFlushList.subList(0, 5));
	        }
	    }
	}
	
	/**
	 * 是否可以打牌，牌局所处时机正确性验证
	 * @return
	 */
	@Override
	public boolean checkGameActionValidation(){
		if(stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
		&& stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD
		&& stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait){
			return true;
		}else{
			return false;
		}
	}
	
	//这把牌是否结束
	@Override
	public int iGameOver(){
		int count = 0;
		int index = 0;
		for(int j = 0 ; j < stGameRoomInfo.getVUserGameInfosCount();j++ ){
			//获取所有牌局上的用户
			UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();
			
			if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0&&
					(stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_STANDBY 
					&& stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_WAIT 
					&& stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_FOLD)){							
				//检查状态，算人数
				count++;
				index = j;
			}
		}
		
		//���������������������������������������������������������������
		if(count > 1){
			return -1;
		}else{
			return index;
		}
	}
	
	@Override
	public void sendAllGameStateChange(long fromUuid,ArrayList<Integer> cards){
		
		if( mPlayers.size() > 0){
			//������������������������������
			for (long uuid  : mPlayers) {
				if(uuid > 0){
					SCGameRoomStateChange.Builder stGameStateChangeBuilder = SCGameRoomStateChange.newBuilder();
					stGameStateChangeBuilder.setStGameRoomInfo(stGameRoomInfo);
					stGameStateChangeBuilder.setUuid(fromUuid);
				
					if(cards!=null){
						for(int cardIndex = 0 ; cardIndex < cards.size();cardIndex++){
							int cardNum = cards.get(cardIndex);
							stGameStateChangeBuilder.addVGivenCards(cardNum);
						}
					}
					
					sendGameStateChange(uuid,stGameStateChangeBuilder.build());
				}
			}
			
			//������������������������������
			for (long uuid  : getStandByUuids()) {
				if(uuid > 0){
					if(!mPlayers.contains(uuid)){
						SCGameRoomStateChange.Builder stGameStateChangeBuilder = SCGameRoomStateChange.newBuilder();
						stGameStateChangeBuilder.setStGameRoomInfo(stGameRoomInfo);
						stGameStateChangeBuilder.setUuid(fromUuid);
						if(cards!=null){
							for(int cardIndex = 0 ; cardIndex < cards.size();cardIndex++){
								int cardNum = cards.get(cardIndex);
								stGameStateChangeBuilder.addVGivenCards(cardNum);
							}
						}
						
						sendGameStateChange(uuid,stGameStateChangeBuilder.build());
					}
				}
			}
		}
	}
	
	@Override
	public boolean sendGameStateChange(long toUuid,SCGameRoomStateChange stGameStateChange){
		if(stGameStateChange!=null){

			SCGameRoomStateChange.Builder stSCGameRoomStateChangeBuilder = stGameStateChange.toBuilder();
			GameRoomInfo.Builder stGameRoomInfoBuilder = stSCGameRoomStateChangeBuilder.getStGameRoomInfo().toBuilder();
			for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount() ; i ++){
				UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);

				if(stUserGameInfoNetBuilder.getStUserInfo().getUuid()!=toUuid){
					
					if(bAllAllin == true){
						if(stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
								&&stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
								&&stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_DEAD
								&&stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
							continue;
						}
					}
					
					if(stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN  && bAllAllin == true){
						continue;
					}
					
					if(stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
							|| (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)){
						if(stUserGameInfoNetBuilder.getBShowCardOne()==1||stUserGameInfoNetBuilder.getBShowCardTwo()==1){
							continue;
						}
						
						if(stUserGameInfoNetBuilder.getEGameState()==USER_GAME_STATE.USER_GAME_STATE_FOLD){
								stUserGameInfoNetBuilder.clearVCurrentHands();
						}else{
							if(bFlodResult==true){
								stUserGameInfoNetBuilder.clearVCurrentHands();
							}
						}
					}else{
						stUserGameInfoNetBuilder.clearVCurrentHands();
					}
				}
	
				stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
			}
			
			stSCGameRoomStateChangeBuilder.setStGameRoomInfo(stGameRoomInfoBuilder);
			
			GameRoomMsgManager.getInstance().sendGameRoomStateChangeMsg(toUuid, stSCGameRoomStateChangeBuilder.build());
			return true;
		}else{
			return false;
		}
	}	
	
	@Override
	public void SidePot(){
		Map<Integer,UserGameInfoNet.Builder> mapExistUsers = new ConcurrentHashMap<Integer,UserGameInfoNet.Builder>();
		ArrayList<PotInfo> mTempPotList = new ArrayList<PotInfo>();

		long lOtherPosts=0;
		
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
			Long uuid = mHandPos.get(i);
			UserGameInfoNet.Builder stUserGameInfoBuilder= stGameRoomInfo.getVUserGameInfos(i).toBuilder();
			
			if((uuid!=null && uuid > 0 && 
					(stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_STANDBY 
					&& stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_WAIT ))
					||stUserGameInfoBuilder.getEGameState() ==  USER_GAME_STATE.USER_GAME_STATE_FOLD){
				mapExistUsers.put(i, stUserGameInfoBuilder);
			}
		}
		
		while(mapExistUsers.size() > 0){
			int potPlayerSize=0;
			long lMinStacks=0;

			Iterator iter = mapExistUsers.entrySet().iterator(); 
			while (iter.hasNext()) { 
				Map.Entry entry = (Map.Entry) iter.next(); 
				UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder)entry.getValue(); 
				if(stUserGameInfoBuilder.getLBetStacks()> 0){
					potPlayerSize ++;
				}else{
					iter.remove();
					continue;
				}
				
				if(lMinStacks==0&&stUserGameInfoBuilder.getLBetStacks()>0){
					lMinStacks = stUserGameInfoBuilder.getLBetStacks();
				}else if(lMinStacks > stUserGameInfoBuilder.getLBetStacks()&&stUserGameInfoBuilder.getLBetStacks()>0){
					lMinStacks = stUserGameInfoBuilder.getLBetStacks();
				}
			}
			
			if(lMinStacks == 0){
				break;
			}
			
			PotInfo stPotInfo = new PotInfo();

			stPotInfo.potSize = lMinStacks* potPlayerSize;
			stPotInfo.allinSize = 0;
			iter = mapExistUsers.entrySet().iterator(); 

			while (iter.hasNext()) { 
			    Map.Entry entry = (Map.Entry) iter.next(); 
			    Integer pos = (Integer)entry.getKey(); 
			    UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder)entry.getValue(); 

			    stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLBetStacks() - lMinStacks);
			    
			    if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0){
			    	stPotInfo.playerList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
			    	stPotInfo.playerListpos.add(pos);
			    	
			    	if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD){
			    		stPotInfo.playerNotFoldList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
			    	}
			    }else{
			    	if(mHandPos.get(pos)!=null){
			    		stPotInfo.playerList.add(mHandPos.get(pos));
			    		stPotInfo.playerListpos.add(pos);
			    	}
			    	
			    	if(stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD){
			    		stPotInfo.playerNotFoldList.add(mHandPos.get(pos));
			    	}
			    }
			    
			    if(stUserGameInfoBuilder.getLRemainStacks() == 0){
			    	stPotInfo.allinSize ++;
			    }
			    
			    if(stUserGameInfoBuilder.getLBetStacks() == 0){
			    	
			    	iter.remove();
			    }
			} 
			
			mTempPotList.add(stPotInfo);
			
			//如果有需要，合池子
			if(mTempPotList.size() >= 2){
				PotInfo stLastPot = mTempPotList.get(mTempPotList.size() - 1);
				PotInfo stBeforeLastPot = mTempPotList.get(mTempPotList.size() - 2);
				if(stLastPot.playerNotFoldList.size() == stBeforeLastPot.playerNotFoldList.size()){
					//两个池子应该是一个池子
					PotInfo stPotInfoNew = new PotInfo();
					stPotInfoNew.potSize = stBeforeLastPot.potSize + stLastPot.potSize;
					stPotInfoNew.allinSize = stBeforeLastPot.allinSize;
					stPotInfoNew.playerList.addAll(stBeforeLastPot.playerList);
					stPotInfoNew.playerListpos.addAll(stBeforeLastPot.playerListpos);
					stPotInfoNew.playerNotFoldList.addAll(stBeforeLastPot.playerNotFoldList);
					
					mTempPotList.remove(mTempPotList.size() - 1);
					mTempPotList.remove(mTempPotList.size() - 1);
					mTempPotList.add(stPotInfoNew);
				}	
			}
		}

		if(mTempPotList.size() > 0){
			PotInfo stFristPot = mTempPotList.get(0);
			stFristPot.potSize += lOtherPosts;
		}else{
			if(lOtherPosts > 0){
				PotInfo stPotInfo = new PotInfo();
				stPotInfo.potSize = lOtherPosts;
				stPotInfo.allinSize = 0;
				
				mTempPotList.add(stPotInfo);
			}
		}
		
		if(mTempPotList.size() > 0){
			if(mPotList.size()==0){
				mPotList=mTempPotList;
			}
			else{
					PotInfo tempPot = mTempPotList.get(0);
					PotInfo stLastPot = mPotList.get(mPotList.size()-1);
					
					if(stLastPot.allinSize == 0){
						
						if(stLastPot!=null){
							stLastPot.potSize += tempPot.potSize;
							stLastPot.allinSize = tempPot.allinSize;
							ArrayList<Integer> stLastPos = new ArrayList<Integer>(stLastPot.playerListpos);

							for(int i=0;i<tempPot.playerListpos.size();i++){
								int tempPos = tempPot.playerListpos.get(i);
								long tempUuid = tempPot.playerList.get(i);
								for(int j=0;j<stLastPos.size();j++){
									int lastPos = stLastPos.get(j);
									if(lastPos!=tempPos){
										stLastPot.playerListpos.add(tempPos);
										stLastPot.playerList.add(tempUuid);
									}
								}
							}
						}
					}
					else{
						mPotList.add(tempPot);
					}	
					
					for(int i = 1 ; i < mTempPotList.size() ;i++ ){
						PotInfo tempPot1 = mTempPotList.get(i);
						mPotList.add(tempPot1);
					}
			}
		}	
		
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount();i++){
			Long uuid = mHandPos.get(i);
			UserGameInfoNet.Builder stUserGameInfoBuilder= stGameRoomInfo.getVUserGameInfos(i).toBuilder();

			if((uuid!=null && uuid > 0 && 
					(stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_STANDBY 
					&& stUserGameInfoBuilder.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_WAIT ))
					||stUserGameInfoBuilder.getEGameState() ==  USER_GAME_STATE.USER_GAME_STATE_FOLD){
				mapExistUsers.put(i, stUserGameInfoBuilder);
			}
		}
		
		boolean bAllFold = true;
		int iAllFoldStacks = 0; 
		for(int i = mPotList.size() - 1 ; i >= 0 ; i--){
			bAllFold = true;
			PotInfo stPotInfo = mPotList.get(i);

			for(int pos:stPotInfo.playerListpos){
				UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapExistUsers.get(pos);
				
				if(stUserGameInfoNetBuilder!=null){
					if(stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD){
						
						bAllFold = false;
						break;
					}
				}
			}
			if(bAllFold==true){
				iAllFoldStacks += stPotInfo.potSize;
				mPotList.remove(i);
			}else{
				stPotInfo.potSize+=iAllFoldStacks;
				break;
			}
		}
		
		if(mPotList.size() == 0 && iAllFoldStacks > 0){
			int iLengths=0; 
			int iInPos = -1;
			long uuid=0;
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount() ;i++){
				UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				if(stTempUserGameInfoNet.getStUserInfo().getUuid() > 0 
						&& stTempUserGameInfoNet.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_STANDBY 
						&& stTempUserGameInfoNet.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_WAIT 
						&& stTempUserGameInfoNet.getEGameState() !=  USER_GAME_STATE.USER_GAME_STATE_FOLD){
							uuid = stTempUserGameInfoNet.getStUserInfo().getUuid();
							iLengths++;
							iInPos = i;
						}
			}
			
			if(uuid> 0 && iLengths==1 && iLengths!=-1){
				PotInfo stPotInfo = new PotInfo();
				//������������������������������������������
				stPotInfo.potSize = iAllFoldStacks;
				stPotInfo.allinSize = 0;
				stPotInfo.playerListpos.add(iInPos);
				stPotInfo.playerList.add(uuid);
				mPotList.add(stPotInfo);
			}

		}
		
		
		//���������������pot������
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
		stGameRoomInfoBuilder.clearVPotInfos();
		stGameRoomInfoBuilder.clearVCurrentPots();
		for(int i = 0 ; i < mPotList.size();i++){
			PotInfo stPotInfo = mPotList.get(i);
			//设置Pot相关
			stGameRoomInfoBuilder.addVCurrentPots((int)stPotInfo.potSize);
			
			//设置新pot结构
			TexasPoker.PotInfo.Builder stPotInfoBuilder = TexasPoker.PotInfo.newBuilder();
			stPotInfoBuilder.setLPotStacks(stPotInfo.potSize);
			for(int j = 0 ; j < stPotInfo.playerList.size() ;j ++ ){
				long uuid = stPotInfo.playerList.get(j);
				stPotInfoBuilder.addVUuids(uuid);
			}
			
			stGameRoomInfoBuilder.addVPotInfos(stPotInfoBuilder);
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
	
	/****************************比牌相关函数*******************************/
	@Override
	public int CompareSameLevelUser(GameUserResultInfo user1 , GameUserResultInfo user2 ){
		switch(user1.mCardStrength){
		case 1:{			
			for(int i = 0 ; i < user1.singleList.size();i++){
				int user1CardNum = user1.singleList.get(i);
				int user2CardNum = user2.singleList.get(i);

				if(user1CardNum == 1){
					user1CardNum = 14;
				}
				
				if(user2CardNum == 1){
					user2CardNum = 14;
				}
				
				if(user1CardNum!=user2CardNum){
					return user1CardNum-user2CardNum;
				}
			}
			return 0;
		}
		case 2:{
			int user1pair = user1.pairList.get(0);
			int user2pair = user2.pairList.get(0);
			
			
			if(user1pair == user2pair){
				for(int i = 0 ; i < user1.singleList.size();i++){
					int user1CardNum = user1.singleList.get(i);
					int user2CardNum = user2.singleList.get(i);
					
					if(user1CardNum == 1){
						user1CardNum = 14;
					}
					
					if(user2CardNum == 1){
						user2CardNum = 14;
					}
					
					if(user1CardNum!=user2CardNum){
						return user1CardNum-user2CardNum;
					}
				}
				return 0;
			}
			else{
				if(user1pair == 1){
					user1pair = 14;
				}
				
				if(user2pair == 1){
					user2pair = 14;
				}
				return user1pair - user2pair;
			}
		}			
		case 3:{
			
			for(int i = 0 ; i < user1.pairList.size();i++){
				int user1CardNum = user1.pairList.get(i);
				int user2CardNum = user2.pairList.get(i);
				
				if(user1CardNum == 1){
					user1CardNum = 14;
				}
				
				if(user2CardNum == 1){
					user2CardNum = 14;
				}
				
				if(user1CardNum!=user2CardNum){
					return user1CardNum-user2CardNum;
				}
			}
			int user1single = user1.singleList.get(0);
			int user2single = user2.singleList.get(0);
			
			if(user1single == 1){
				user1single = 14;
			}
			
			if(user2single == 1){
				user2single = 14;
			}
			
			return user1single-user2single;
		}
		case 4:{
			int user1tri = user1.triList.get(0);
			int user2tri = user2.triList.get(0);
			
			
			if(user1tri == user2tri){
				for(int i = 0 ; i < user1.singleList.size();i++){
					int user1CardNum = user1.singleList.get(i);
					int user2CardNum = user2.singleList.get(i);
					
					if(user1CardNum == 1){
						user1CardNum = 14;
					}
					
					if(user2CardNum == 1){
						user2CardNum = 14;
					}
					
					if(user1CardNum!=user2CardNum){
						return user1CardNum-user2CardNum;
					}
				}
				return 0; 
			}
			else{
				if(user1tri == 1){
					user1tri = 14;
				}
				
				if(user2tri == 1){
					user2tri = 14;
				}
				
				return user1tri - user2tri;
			}
		}
		case 5:{
			return user1.iStraightCard - user2.iStraightCard;
		}
		case 6:{
			for(int i = 0; i < 5;i++){
				int flushMaxHandCard1 = user1.finalCards.get(i)%13 + 1;
				int flushMaxHandCard2 = user2.finalCards.get(i)%13 + 1;
				
				if(flushMaxHandCard1 == 1){
					flushMaxHandCard1 = 14;
				}
				
				if(flushMaxHandCard2 == 1){
					flushMaxHandCard2 = 14;
				}
				
				if(flushMaxHandCard1!=flushMaxHandCard2){
					return flushMaxHandCard1 - flushMaxHandCard2;
				}
			}
			return 0;
		}
		case 7:{
			int user1tri = user1.triList.get(0);
			int user2tri = user2.triList.get(0);
			if(user1tri == user2tri){
				int user1CardNum = user1.pairList.get(0);
				int user2CardNum = user2.pairList.get(0);
				
				if(user1CardNum == 1){
					user1CardNum = 14;
				}
				
				if(user1CardNum == 1){
					user1CardNum = 14;
				}
				
				return user1CardNum - user2CardNum;
			}
			else{	
				if(user1tri == 1){
					user1tri = 14;
				}
				
				if(user2tri == 1){
					user2tri = 14;
				}
				
				return user1tri - user2tri;
			}
		}
		case 8:
			
			int four1 = user1.four;
			int four2 = user2.four;
			
			if(four1 == 1){
				four1 = 14;
			}
			
			if(four2 == 1){
				four2 = 14;
			}
			if(four1 != four2){
				return four1 - four2;
			}else{

				int cardNum1 = 0;
//				System.out.println("four1four1four1four11 : " + user1.singleList.size());
//				System.out.println("four1four1four1four12 : " + user1.pairList.size());
//				System.out.println("four1four1four1four13 : " + user1.triList.size());
				if(user1.singleList.size() > 0){
					cardNum1 = user1.singleList.get(0)%13 + 1;
					if(cardNum1 == 1){
						cardNum1 = 14;
					}
				} 
				
				if(user1.pairList.size() > 0){
					int tempCardNum1 = user1.pairList.get(0)%13 + 1;
					if(tempCardNum1 == 1){
						tempCardNum1 = 14;
					}
					
					if(tempCardNum1>cardNum1){
						cardNum1 = tempCardNum1;
					}
					
				}
				
				if(user1.triList.size() > 0){
					int tempCardNum1 = user1.triList.get(0)%13 + 1;
					if(tempCardNum1 == 1){
						tempCardNum1 = 14;
					}
					
					if(tempCardNum1>cardNum1){
						cardNum1 = tempCardNum1;
					}
				}
				
				int cardNum2 = 0;
				if(user2.singleList.size() > 0){
					cardNum2 = user2.singleList.get(0)%13 + 1;
					if(cardNum2 == 1){
						cardNum2 = 14;
					}
				} 
				
				if(user2.pairList.size() > 0){
					int tempCardNum2 = user2.pairList.get(0)%13 + 1;
					if(tempCardNum2 == 1){
						tempCardNum2 = 14;
					}
					
					if(tempCardNum2>cardNum2){
						cardNum2 = tempCardNum2;
					}
				}
				
				if(user2.triList.size() > 0){
					int tempCardNum2 = user2.triList.get(0)%13 + 1;
					if(tempCardNum2 == 1){
						tempCardNum2 = 14;
					}
					
					if(tempCardNum2>cardNum2){
						cardNum2 = tempCardNum2;
					}
				}
				
				if(cardNum1 == 1){
					cardNum1 = 14;
				}
				
				if(cardNum2 == 1){
					cardNum2 = 14;
				}
				return cardNum1 - cardNum2;
			}	
		case 9:
			if(user1.finalCards.size() >= 4 && user2.finalCards.size() >= 4){
				int flushMaxHandCard1 = user1.iStraightCard;
				int flushMaxHandCard2 = user2.iStraightCard;
				if(flushMaxHandCard1!=flushMaxHandCard2){
					return flushMaxHandCard1 - flushMaxHandCard2;
				}
			}
		}
		
		return 0;
	}
	
	/****************************牌局逻辑辅助函数*******************************/
	@Override
	public boolean checkAllAllin(){
		int iAllAllin = 0;
		long onlyBetStacks = 0;
		long allinStacks = 0;
		int iNoFold = 0;
		
		if(bAllAllin!= true){
			for(int i = 0; i < stGameRoomInfo.getVUserGameInfosCount();i++){
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 &&
						stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
						&&stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
						&&stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT){
					iNoFold++;
					if(stUserGameInfoNet.getLRemainStacks() > 0){
						iAllAllin ++;
						onlyBetStacks = stUserGameInfoNet.getLBetStacks();
					}
					else{
						if(stUserGameInfoNet.getLBetStacks() > allinStacks){
							allinStacks = stUserGameInfoNet.getLBetStacks();
						}
					}
				}
			}
			
			if(iAllAllin > 1){
				bAllAllin = false; 
			}else if(iAllAllin == 1){
				if(allinStacks > onlyBetStacks)
					bAllAllin= false;
				else{
					if(iNoFold>1){
						bAllAllin = true;
					}else{
						bAllAllin = false;
					}
				}
			}else{
				if(iNoFold>1){
					bAllAllin = true;
				}else{
					bAllAllin = false;
				}
				
			}
		}	

		return bAllAllin;
	}
	
	/****************************发牌辅助函数*******************************/
	/**
	 * 发牌函数
	 * @return 返回一张手牌
	 */
	@Override
	public int getCard(){
		return mCards[iDrawCardNum++];
	}
	
	/**
	 * 发牌函数，测试用，发固定牌
	 * @param uuid   发牌的用户
	 * @param time   第几张牌
	 * @param bTable 是桌牌还是手牌
	 * @return
	 */
	@Override
	public int getCard(long uuid,int time,boolean bTable){
		if(bTable){
			return vTestTableCards.get(time);
		}else{
			if(uuid==40732){
				return vTestHandCards1.get(time);
			}else if(uuid==35116){
				return vTestHandCards2.get(time);
			}else{
				return vTestHandCards3.get(time);
			}
		}
	}
	
	/**
	 * 洗牌相关算法
	 */
	@Override
	public void resetCard(){
		//洗牌
		mCards = new int[52];
		
		ShuffleCard.initShuffle(mCards);
		
		// 安全洗牌一次
		ShuffleCard.secureShuffle(mCards);
		
		//重置获取牌的数量
		iDrawCardNum = 0;
	}

	/****************************玩家超时辅助函数*******************************/
	/**
	 * 重置玩家超时
	 * @param uuid  设置超时的玩家
	 */
	@Override
	public void setTimeoutUserCurrent(long uuid){
		mTimeoutGameUsers.setCurrent(uuid);
		mTimeoutGameUsers.setTimeout(uuid, (long)20);
	}
	
	/**
	 * 获取超时的玩家
	 * @return
	 */
	@Override
	public ArrayList<Long> getTimeoutUsers(){
		return mTimeoutGameUsers.timeout();
	}
	
	/**
	 * 延长超时，主要是给延时道具用
	 * @param uuid 延时的用户
	 * @param delay 延时的时间
	 * @return
	 */
	@Override
	public boolean delayTimeout(long uuid,long delay){
		return mTimeoutGameUsers.addTimeout(uuid,delay);
	}
	
	/**
	 * 删除所有timeout相关
	 */
	@Override
	public void removeAllTimeOutUser(){
		mTimeoutGameUsers.removeAllKey();
	}
	
	/****************************玩家超时次数辅助函数*******************************/
	@Override
	public void addUserTimeoutTimes(long uuid){
		Integer times = mapTimeoutTimesUser.get(uuid);
		if(times==null){
			mapTimeoutTimesUser.put(uuid, 1);
		}else{
			mapTimeoutTimesUser.put(uuid, times+1);
		}
	}
	
	@Override
	public void resetUserTimeoutTimes(long uuid){
		Integer times = mapTimeoutTimesUser.get(uuid);
		if(times!=null){
			mapTimeoutTimesUser.put(uuid, 0);
		}
	}
	
	@Override
	public void removeTimeoutTimesUser(long uuid){
		mapTimeoutTimesUser.remove(uuid);
	}
	
	@Override
	public void removeAllTimeOutTimesUser(){
		mapTimeoutTimesUser.clear();
	}
	
	@Override
	public int getTimeoutTimesUser(long uuid){
		Integer times = mapTimeoutTimesUser.get(uuid);
		if(times==null){
			return 0;
		}else{
			return times;
		}
	}
	
	@Override
	public void flodWinProcess(){
		//分池
		SidePot();
		
		//计算结果
		calculateWinnerFlod();
			
		//设置牌局状态
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();	
		stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result);	
		stGameRoomInfoBuilder.setBStateChange(1);
		stGameRoomInfo = stGameRoomInfoBuilder.build();
	}
	
	/****************************牌局用户位置辅助函数*******************************/
	@Override
	public int getUserPos(long uuid){
		Integer iPos = mapUserPos.get(uuid);
		if(iPos!=null){
			return iPos;
		}else{
			return 0;
		}
	}
	
	@Override
	public void setUserPos(long uuid,int pos){
		mapUserPos.put(uuid, pos);
	}
	
	@Override
	public void removeUserPos(long uuid){
		mapUserPos.remove(uuid);
	}
	
	@Override
	public boolean containUser(long uuid){
		return mapUserPos.containsKey(uuid);
	}
	
	//用户计算弃牌赢牌似的情况
	@Override
	public void calculateWinnerFlod(){
		GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

		GameResultInfo.Builder stGameResultInfoBuilder = GameResultInfo.newBuilder();
		long lSingleMaxPot = 0;

		for(int index = 0 ; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++){
			List<Integer> winnerIDArray = new ArrayList<Integer>();
			List<Long> winnerUuidArray = new ArrayList<Long>();
			
			Set<Integer> winnerIDSet = new HashSet<Integer>();
			Set<Long> winnerUuidSet = new HashSet<Long>();
			
			PotInfo stPotInfo = mPotList.get(index);
			List<Integer> posArray = stPotInfo.playerListpos;
			
			ArrayList<Long> allFoldUuids = new ArrayList<Long>();
			ArrayList<Integer> allFoldPos = new ArrayList<Integer>();
			
			Set<Long> allFoldUuidSet = new HashSet<Long>();
			Set<Integer> allFoldPosSet = new HashSet<Integer>();
			
			for(int i = 0 ; i < posArray.size() ; i++){
				
				int pos = posArray.get(i);
				long winuuid = stPotInfo.playerList.get(i);
				
				UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(pos);
				
				if(stUserGameInfo.getStUserInfo().getUuid() > 0){
					if(stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CALL 
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CHECK
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_RAISE
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND
							|| stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY){
						if(!winnerUuidSet.contains(winuuid)){
							winnerIDSet.add(pos);
							winnerIDArray.add(pos);
							winnerUuidSet.add(winuuid);
							winnerUuidArray.add(winuuid);
						}
					}else if(stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD){
						if(mHandPos.get(pos)!=null){
							if(!allFoldUuidSet.contains(mHandPos.get(pos))){
								allFoldUuids.add(mHandPos.get(pos));
								allFoldPos.add(pos);
								
								allFoldUuidSet.add(mHandPos.get(pos));
								allFoldPosSet.add(pos);
							}
						}
					}
				}else{
					if(mHandPos.get(pos)!=null){
						if(!allFoldUuidSet.contains(mHandPos.get(pos))){
							allFoldUuids.add(mHandPos.get(pos));
							allFoldPos.add(pos);
							
							allFoldUuidSet.add(mHandPos.get(pos));
							allFoldPosSet.add(pos);
						}
					}
				}
			}
		
			if(winnerIDArray.size()!=0){
				GamePotResultInfo.Builder  stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();
				
				int winStacks = stGameRoomInfo.getVCurrentPots(index)/winnerIDArray.size();
				lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);
				for(int i = 0 ; i < winnerIDArray.size() ; i++){
					int userPos  =  winnerIDArray.get(i);
					long winUuid = winnerUuidArray.get(i);
					for(int j = 0 ; j < stGameRoomInfo.getVUserGameInfosCount();j++){
						if(j == userPos){
							UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();
							
							if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid() == winUuid){
								stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
								stGamePotResultInfoBuilder.addLUserPots(winStacks/winnerIDArray.size());
								
								stGameRoomInfo = stGameRoomInfoBuilder.build();								
																			
								stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()+winStacks);

								stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());			
							}
							else{
								UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
								if(stUserStandByGameInfoBuilder != null){
									stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
									stGamePotResultInfoBuilder.addLUserPots(winStacks/winnerIDArray.size());
																			
									stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks()+winStacks);
									
									mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);		
									
									BillManager.getInstance().setCurrentStacksInRoom(winUuid, 
											stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserStandByGameInfoBuilder.getLRemainStacks());
								}
							}		
						}
					}
				}
				stGameResultInfoBuilder.addStGamePotResultInfo(stGamePotResultInfoBuilder);
			}else{
				if(allFoldPos.size() > 0){
					
					GamePotResultInfo.Builder  stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();
					
					int winStacks = stGameRoomInfo.getVCurrentPots(index)/allFoldPos.size();
					lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);
					
					for(int i = 0 ; i < allFoldPos.size() ; i++){

						int pos = allFoldPos.get(i);
						long winUuid = 0;

						winUuid = allFoldUuids.get(i);
						if(pos >= 0){
							UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(pos).toBuilder();
							if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid()==winUuid){
								stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
								stGamePotResultInfoBuilder.addLUserPots(winStacks);

								stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()+winStacks);

								stGameRoomInfoBuilder.setVUserGameInfos(pos, stUserGameInfoBuilder.build());			
							}
							else{
//								boolean bSettled = false;
								UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
								if(stUserStandByGameInfoBuilder != null){
									stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
									stGamePotResultInfoBuilder.addLUserPots(winStacks);
															
									stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks()+winStacks);

									mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);			
									
									BillManager.getInstance().setCurrentStacksInRoom(winUuid, 
											stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
											, stUserStandByGameInfoBuilder.getLRemainStacks());
//									bSettled=true;
								}
//								if(bSettled == false){
									for(int chgangeIndex = 0 ; chgangeIndex < stGameRoomInfo.getVUserGameInfosCount();chgangeIndex++){
										
										UserGameInfoNet.Builder stUserGameInfoBuilderChange = stGameRoomInfoBuilder.getVUserGameInfos(chgangeIndex).toBuilder();

										if(stUserGameInfoBuilderChange!=null&&stUserGameInfoBuilderChange.getStUserInfo().getUuid()==winUuid){
											stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
											stGamePotResultInfoBuilder.addLUserPots(winStacks);

											stUserGameInfoBuilderChange.setLRemainStacks(stUserGameInfoBuilderChange.getLRemainStacks()+winStacks);

											stGameRoomInfoBuilder.setVUserGameInfos(chgangeIndex, stUserGameInfoBuilderChange.build());
										}
									}
//								}
							}
							
						}
					}
					stGameResultInfoBuilder.addStGamePotResultInfo(stGamePotResultInfoBuilder);
				}
			}
		}
		
		if(lSingleMaxPot > lMaxPot){
			lMaxPot = lSingleMaxPot;
		}
		
		
		for(int i = 0 ; i < vOriginalStacks.size() ; i++){
			long stack = vOriginalStacks.get(i);
			stGameResultInfoBuilder.addVlOriginalUserStacks(stack);
		}
		stGameRoomInfoBuilder.setStGameResultInfo(stGameResultInfoBuilder);
		
		
		Map<Long,Long> mapWinStacks = new ConcurrentHashMap<Long,Long>();
		for(GamePotResultInfo stGamePotResultInfo:stGameResultInfoBuilder.getStGamePotResultInfoList()){
			for(int p = 0;p < stGamePotResultInfo.getStUserWinerCount();p++){
				long uuid = stGamePotResultInfo.getStUserWiner(p).getUuid();
				long winStacks = stGamePotResultInfo.getLUserPots(p);
				
				Long stacks = mapWinStacks.get(uuid);
				if(stacks==null){
					mapWinStacks.put(uuid, winStacks);
				}else{
					mapWinStacks.put(uuid, stacks+winStacks);
				}
			}
		}
		
		//���������������������������������������
		Iterator iter = mapWinStacks.entrySet().iterator(); 
		while (iter.hasNext()) { 
			Map.Entry entry = (Map.Entry) iter.next(); 
			Long uuid = (Long) entry.getKey();
			Long winStacks = (Long) entry.getValue();
			
			TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
					+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
					+"," + stGameRoomInfo.getLGameHandID()
					+"," + uuid
					+"," + "showdown" 
					+"," + "noshow" 
					+"," + winStacks);
			
			stGameRoomInfo = stGameRoomInfoBuilder.build();
		}
		
		for(long uuid:vHandList){
			HandInfo stHandInfo = mHandMap.get(uuid);
			if(stHandInfo!=null){
				TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
						+"," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
						+"," + stGameRoomInfo.getLGameHandID()
						+"," + uuid
						+"," + "hand" 
						+"," + stHandInfo.getvCards().get(0)
						+"," + stHandInfo.getvCards().get(1)
						+"," + stHandInfo.getlRemainStacks());	
			}
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();
		for (int k = 0; k < iGameRoomUserMaxNums; k++) {
			UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder
					.getVUserGameInfos(k)
					.toBuilder();
			
			if(stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1){
				UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
				stUserBaseInfo.setUuid(0);
				stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
			}
			
			stResetUserGameInfoBuilder.setLBetStacks(0);
			stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
			stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
			
		}
		
		stGameRoomInfo = stGameRoomInfoBuilder.build();	
//		stGameRoomInfo = InsuranceApi.getInsuranceBill(stGameRoomInfo,stGameRoomInfo.getLGameHandID());
		ArrayList<Long> vSitdownUsers = new ArrayList<Long>();
		//设置结果,实时账单信息
		for(int index = 0 ; index < stGameRoomInfo.getVUserGameInfosCount();index++){
			UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(index);
			if(stUserGameInfo.getStUserInfo().getUuid() > 0){
				System.out.println("结账113 : " + stUserGameInfo.getStUserInfo().getUuid());
				//设置账单
				BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfo.getStUserInfo().getUuid(), 
						stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
						, stUserGameInfo.getLRemainStacks());
				vSitdownUsers.add(stUserGameInfo.getStUserInfo().getUuid());
			}
		}
		
		Iterator iter1 = mapStandByUserGameInfos.entrySet().iterator(); 
		while (iter1.hasNext()) { 
			Map.Entry entry = (Map.Entry) iter1.next(); 
			UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder)entry.getValue(); 
			if(stUserGameInfoBuilder!=null){
				if(stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 
						&& !vSitdownUsers.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())){
					System.out.println("结账114 : " + stUserGameInfoBuilder.getStUserInfo().getUuid());
					//设置账单
					BillManager.getInstance().setCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(), 
							stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
							, stUserGameInfoBuilder.getLRemainStacks());
				}
			}
		}
		
		bFlodResult = true;
	}

	@Override
	public boolean getGameStarted(){
		return bStarted;
	}
	
	@Override
	public void saveOriginalStacks(){
		vOriginalStacks.clear();
		
		for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount() ; i ++){
			UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
			vOriginalStacks.add(stUserGameInfoNet.getLRemainStacks());
			if(stUserGameInfoNet.getStUserInfo().getUuid() > 0){
				mOriginalStacks.put(stUserGameInfoNet.getStUserInfo().getUuid(), stUserGameInfoNet.getLRemainStacks());
			}
		}
	}
	
	@Override
	public ERROR_CODE_TYPE showcard(long lHandID,long uuid,int bShowCardOne,int bShowCardTwo){
		synchronized(this){
			UserBaseInfoNet.Builder stUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
			
			if(stUserBaseInfoBuilder!=null){
				if(stGameRoomInfo!=null){
					//亮牌的状态位
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

					if(stGameRoomInfoBuilder.getLGameHandID() == lHandID){
						for(int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){

							UserGameInfoNet stUserGameInfoNet = stGameRoomInfoBuilder.getVUserGameInfos(i);
							
							if(stUserGameInfoNet.getStUserInfo().getUuid() == uuid && stUserGameInfoNet.getVCurrentHandsCount() > 1){
								//如果是该用户，则设置亮牌状态
								UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
								stUserGameInfoNetBuilder.setBShowCardOne(bShowCardOne);
								stUserGameInfoNetBuilder.setBShowCardTwo(bShowCardTwo);
								stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
						
								String sShowCardLog = "";
								if(bShowCardOne!= 0){
									sShowCardLog +=stUserGameInfoNetBuilder.getVCurrentHands(0);
									sShowCardLog +=",";
								}
								
								if(bShowCardTwo!= 0){
									sShowCardLog +=stUserGameInfoNetBuilder.getVCurrentHands(1);
								}
								if(sShowCardLog.length() == 0){
									sShowCardLog = "none";
								}
								TableLogService.getInstance().error(lFromRoomID
										+"," + lGameRoomID
										+"," + stGameRoomInfoBuilder.getLGameHandID()
										+"," + stUserGameInfoNetBuilder.getStUserInfo().getUuid() 
										+"," + "showcard" 
										+"," + sShowCardLog);
							}
						}
						stGameRoomInfo = stGameRoomInfoBuilder.build();
						return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
					}else{
						return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
					}				
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
				}
			}else{
				return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
			}
		}
	}

	@Override
	public ERROR_CODE_TYPE useItem(long uuid,long toUuid,long lMsgID,int itemID,String sExtra){
		UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
		if(stUserBaseInfoNetBuilder!=null){
			switch(itemID){
			case 10010:
				if(UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), 100)){
					//组织push包
					UseItemPushMsg.Builder stUseItemPushMsgBuilder = UseItemPushMsg.newBuilder();
					stUseItemPushMsgBuilder.setLItemID(itemID);
					stUseItemPushMsgBuilder.setLToUuid(toUuid);
					stUseItemPushMsgBuilder.setLMsgID(System.currentTimeMillis());
					stUseItemPushMsgBuilder.setLFromUuid(uuid);
					stUseItemPushMsgBuilder.setLRoomID(lGameRoomID);
					stUseItemPushMsgBuilder.setSExtra(sExtra);
					

					stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), - 10);
					Log   mLog   =   LogFactory.getLog("fortransaction");
					if(stUserBaseInfoNetBuilder.getEDeviceType() == DEVICE_TYPE.DEVICE_TYPE_ANDROID){
						mLog.error(stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 430001 + " " + sExtra + " 0 " + "0 10 "  + stUserBaseInfoNetBuilder.getLAndroidPopularity());
					}else{
						mLog.error(stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 430001 + " " + sExtra + " 0 " + "0 10 "  + stUserBaseInfoNetBuilder.getLPopularity());
					}
					
					
					for(int i = 0; i < stGameRoomInfo.getVUserGameInfosCount();i++){
						UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
						
						if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid()!=stUserBaseInfoNetBuilder.getUuid()){
							GameRoomMsgManager.getInstance().sendUseItemMsg(stUserGameInfoNet.getStUserInfo().getUuid() ,stUseItemPushMsgBuilder.build());
						}
					}
					
					for(long standUuid:getStandByUuids()){
						UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(standUuid).build();
						
						if(stUserBaseInfo != null && stUserBaseInfo.getUuid() > 0 && stUserBaseInfo.getUuid()!=stUserBaseInfoNetBuilder.getUuid()){
							GameRoomMsgManager.getInstance().sendUseItemMsg(stUserBaseInfo.getUuid() ,stUseItemPushMsgBuilder.build());
						}
					}
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
				}
			case 20001:
				if(UserManager.getInstance().checkUserCoin(stUserBaseInfoNetBuilder.getUuid(),6)){
					//组织push包
					UseItemPushMsg.Builder stUseItemPushMsgBuilder = UseItemPushMsg.newBuilder();
					stUseItemPushMsgBuilder.setLItemID(itemID);
					stUseItemPushMsgBuilder.setLToUuid(toUuid);
					stUseItemPushMsgBuilder.setLMsgID(System.currentTimeMillis());
					stUseItemPushMsgBuilder.setLFromUuid(uuid);
					stUseItemPushMsgBuilder.setLRoomID(lGameRoomID);
					stUseItemPushMsgBuilder.setSExtra(sExtra);
					

					stUserBaseInfoNetBuilder = UserManager.getInstance().addUserCoin(stUserBaseInfoNetBuilder.getUuid(), - 6);
					
					Log   mLog   =   LogFactory.getLog("fortransaction");
					if(stUserBaseInfoNetBuilder.getEDeviceType() == DEVICE_TYPE.DEVICE_TYPE_ANDROID){
						mLog.error(stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 430002 + " " + sExtra + " 0 " + "0 6 "  + stUserBaseInfoNetBuilder.getLAndroidCoin());
					}else{
						mLog.error(stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 430002 + " " + sExtra + " 0 " + "0 6 "  + stUserBaseInfoNetBuilder.getLCoin());
					}
					
					
					for(int i = 0; i < stGameRoomInfo.getVUserGameInfosCount();i++){
						UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
						
						if(stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid()!=stUserBaseInfoNetBuilder.getUuid()){
							GameRoomMsgManager.getInstance().sendUseItemMsg(stUserGameInfoNet.getStUserInfo().getUuid() ,stUseItemPushMsgBuilder.build());
						}
					}
					
					for(long standUuid:getStandByUuids()){
						UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(standUuid).build();
						
						if(stUserBaseInfo != null && stUserBaseInfo.getUuid() > 0 && stUserBaseInfo.getUuid()!=stUserBaseInfoNetBuilder.getUuid()){
							GameRoomMsgManager.getInstance().sendUseItemMsg(stUserBaseInfo.getUuid() ,stUseItemPushMsgBuilder.build());
						}
					}
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				}else{
					return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN;
				}
			}
		}else{
			return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
		}
		return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
	}
	
	public void setStandByUserGameInfos(long uuid,UserGameInfoNet.Builder stUserGameInfoNetBuilder){
		mapStandByUserGameInfos.put(uuid,stUserGameInfoNetBuilder);
	}
	
	public UserGameInfoNet.Builder getStandByUserGameInfos(long uuid){
		return mapStandByUserGameInfos.get(uuid);
	}
	
	public void removeStandByUserGameInfos(long uuid){
		mapStandByUserGameInfos.remove(uuid);
	}
	
	public int getiGameRoomUserMaxNums() {
		return iGameRoomUserMaxNums;
	}

	public void setiGameRoomUserMaxNums(int iGameRoomUserMaxNums) {
		this.iGameRoomUserMaxNums = iGameRoomUserMaxNums;
	}
	
	//代理操作
	public void userDelegate(long uuid){
		synchronized(this){
			for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount() ; i++){
				UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

				//找到离开房间的用户
				if(stUserGameInfo.getStUserInfo().getUuid() == uuid){
					//获取牌局对象
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					
					//获取托管用户信息
					TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();
					
					//设置为托管状态
					userGameInfoBuilder.setBDelegate(1);
					
					//将用户设置回房间
					stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);	
					
					//通知保险用户进入代理状态
					InsuranceApi.doGameDelegateOpen(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);
					
					stGameRoomInfo = stGameRoomInfoBuilder.build();
				
					userGameInfoBuilder.clearVCurrentHands();
					
					//给牌桌上其他用户push托管信息
					for(int j = 0 ; j < stGameRoomInfo.getVUserGameInfosCount(); j++){
						UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);
						
						if(stUserGameInfoNet.getStUserInfo().getUuid() > 0){		
							pushGameRoomUserMsgs(userGameInfoBuilder.build(),stUserGameInfoNet.getStUserInfo().getUuid(),
									GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELEGATE,i);
						}
					}
						
					//给旁观的人发送托管消息
					for (long push_uuid  : mStandBys) {
						pushGameRoomUserMsgs(userGameInfoBuilder.build(),push_uuid,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELEGATE,i);
					}
				}
			}
		}
	}
	
	//取消代理
	public void userReturnGame(long uuid){
		synchronized(this){
			for(int i = 0 ; i < this.stGameRoomInfo.getVUserGameInfosCount();i++){
				//获取炮狙用户信息
				UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
				
				//找到要取消托管状态的用户
				if(stUserGameInfoNet!=null 
						&& stUserGameInfoNet.getStUserInfo().getUuid()>0
						&& stUserGameInfoNet.getStUserInfo().getUuid() == uuid
						&& stUserGameInfoNet.getBDelegate() == 1){
					//重新设置用户的托管信息
					UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
					
					//通知保险用户进入代理状态
					InsuranceApi.doGameDelegateClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);
					
					//解除用户代理状态
					stUserGameInfoNetBuilder.setBDelegate(0);
					
					GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
					stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
					stGameRoomInfo = stGameRoomInfoBuilder.build();
					stUserGameInfoNetBuilder.clearVCurrentHands();
					
					//给牌桌上的用户发送返回牌桌的消息
					for(int j = 0 ; j < stGameRoomInfo.getVUserGameInfosCount(); j++){
						UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);
						
						if(stUserGameInfoNetIn.getStUserInfo().getUuid() > 0){			
							pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(),stUserGameInfoNetIn.getStUserInfo().getUuid(),
									GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RETURNGAME,i);
						}
					}
					//给旁观的人发返回牌桌消息
					for (long push_uuid : mStandBys) {
						pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(),push_uuid,GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RETURNGAME,i);
					}
					break;
				}
			}
		}
	}
}
