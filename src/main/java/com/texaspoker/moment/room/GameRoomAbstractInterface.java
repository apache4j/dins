package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.texaspoker.moment.TexasPoker.GAME_ACT_TYPE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_MESSAGE_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.SCGameRoomStateChange;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;

public interface GameRoomAbstractInterface {
	
	GameRoomInfo getStGameRoomInfo();

	void setStGameRoomInfo(GameRoomInfo stGameRoomInfo);
	
	Set<Long> getStandByUuids();
	
	void addStandByUuid(long uuid);
	
	/**
	 * 弃牌函数，不可以通过uuid获取位置的弃牌（站起坐下弃牌用这个函数，且此时的lbettingid为-2）
	 * @param uuid			弃牌的用户
	 * @param userPos		用户所在位置
	 * @return
	 */
	ERROR_CODE_TYPE doFlodAction(long uuid,int userPos);
	
	/**
	 * 弃牌函数，可以通过uuid获取位置的弃牌（非站起坐下弃牌用这个函数）
	 * @param uuid			弃牌的用户
	 * @param lBettingID	手牌动作ID
	 * @return
	 */
	ERROR_CODE_TYPE doFlodAction(long uuid,long lBettingID);
	
	/**
	 * 弃牌公共函数
	 * @param uuid			弃牌的用户
	 * @param userPos		用户所在位置
	 * @param lBettingID	手牌动作ID
	 * @return
	 */	
	ERROR_CODE_TYPE doFlodAction(long uuid,int userPos,long lBettingID);
	
	ERROR_CODE_TYPE doCheckAction(long uuid,long lBettingID);
	
	ERROR_CODE_TYPE doRaiseAction(long uuid,GAME_ACT_TYPE eGameActType,long lStacks,long lBettingID);
	
	ERROR_CODE_TYPE doCallAction(long uuid,long betstacks,long lBettingID);
	
	public ERROR_CODE_TYPE showcard(long lHandID,long uuid,int bShowCardOne,int bShowCardTwo);

	public ERROR_CODE_TYPE useItem(long uuid,long toUuid,long lMsgID,int itemID,String sExtra);
	
	void calculateWinner();
	
	void calculateFlush(GameUserResultInfo stGameUserResultInfo,
			int iMaxStraightNum, List<Integer> maxFlushList,
			List<Integer> handCards);
	
	/**
	 * 是否可以打牌，牌局所处时机正确性验证
	 * @return
	 */
	boolean checkGameActionValidation();
	
	//这把牌是否结束
	int iGameOver();
	
	void sendAllGameStateChange(long fromUuid,ArrayList<Integer> cards);
	
	void SidePot();
	
	/****************************比牌相关函数*******************************/
	int CompareSameLevelUser(GameUserResultInfo user1 , GameUserResultInfo user2 );
	
	/****************************牌局逻辑辅助函数*******************************/
	boolean checkAllAllin();
	
	/****************************发牌辅助函数*******************************/
	/**
	 * 发牌函数
	 * @return 返回一张手牌
	 */
	int getCard();
	
	/**
	 * 发牌函数，测试用，发固定牌
	 * @param uuid   发牌的用户
	 * @param time   第几张牌
	 * @param bTable 是桌牌还是手牌
	 * @return
	 */
	int getCard(long uuid,int time,boolean bTable);
	
	/**
	 * 洗牌相关算法
	 */
	void resetCard();

	/****************************玩家超时辅助函数*******************************/
	/**
	 * 重置玩家超时
	 * @param uuid  设置超时的玩家
	 */
	 void setTimeoutUserCurrent(long uuid);
	
	/**
	 * 获取超时的玩家
	 * @return
	 */
	 ArrayList<Long> getTimeoutUsers();
	
	/**
	 * 延长超时，主要是给延时道具用
	 * @param uuid 延时的用户
	 * @param delay 延时的时间
	 * @return
	 */
	boolean delayTimeout(long uuid,long delay);
	
	/**
	 * 删除所有timeout相关
	 */
	void removeAllTimeOutUser();
	
	/****************************玩家超时次数辅助函数*******************************/
	void addUserTimeoutTimes(long uuid);
	
	void resetUserTimeoutTimes(long uuid);
	
	void removeTimeoutTimesUser(long uuid);
	
	void removeAllTimeOutTimesUser();
	
	int getTimeoutTimesUser(long uuid);
	
	void flodWinProcess();
	
	/****************************牌局用户位置辅助函数*******************************/
	int getUserPos(long uuid);

	void setUserPos(long uuid,int pos);
	
	void removeUserPos(long uuid);
	
	void removeStandByUuid(long uuid);
	
	boolean containUser(long uuid);
	
	//用户计算弃牌赢牌似的情况
	void calculateWinnerFlod();
	
	boolean getGameStarted();
	
	void saveOriginalStacks();
	
	GameUserResultInfo calculateCardStrength(List<Integer> handCards);
	
	boolean sendGameStateChange(long toUuid,SCGameRoomStateChange stGameStateChange);
	
	int getLastHandID();
	
	public int getHandID();
	
	public long getMaxPot();
	
	void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
			long stToUserUuid,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType ,int pos);
	
	void pushGameRoomUserMsgs(GameRoomInfo.Builder stGameRoomInfoBuilder ,UserGameInfoNet stFromUserGamenfo,
			long stToUserUuid,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType);
	
	void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType);

	void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType,long exceptionUuid);
	
	public void pushAllGameRoomUserMsgsIncludeSelf(UserGameInfoNet stFromUserGamenfo,GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType);
}
