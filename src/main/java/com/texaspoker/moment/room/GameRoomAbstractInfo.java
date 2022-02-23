package com.texaspoker.moment.room;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.AdvancedTimeoutHashMap;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.dubbo.game.insurance.InsuranceApi;
import com.texaspoker.dubbo.game.insurance.InsuranceManager;
import com.texaspoker.dubbo.game.insurance.OutsInfo;
import com.texaspoker.floplotto.FlopLottoManager;
import com.texaspoker.logger.*;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.*;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ClubBaseInfo;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerHttpClub;
import com.texaspoker.moment.TexasPokerSecureProtocolService;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.msg.GameRoomStateMsgManager;
import com.texaspoker.security.SecureManager;
import com.texaspoker.shuffle.ShuffleAlgorithm;
import com.texaspoker.shuffle.ShuffleCard;
import com.texaspoker.user.UserManager;
import com.texaspoker.voice.VoiceManager;
import org.apache.commons.lang.math.RandomUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoomAbstractInfo implements GameRoomAbstractInterface {
    //dealer的位置
    protected int mDealPos = 7;
    //小盲的位置
    protected int mSmallBlindPos;
    //大盲的位置
    protected int mBigBlindPos;
    //结束的位置
    protected int mEndPos;

    private boolean bFlodResult = false;

    protected ArrayList<Integer> vTableCards = new ArrayList<Integer>();

    //初始筹码数量
    protected ArrayList<Long> vOriginalStacks = new ArrayList<Long>();
    protected ArrayList<Integer> vTestHandCards1 = new ArrayList<Integer>();
    protected ArrayList<Integer> vTestHandCards2 = new ArrayList<Integer>();
    protected ArrayList<Integer> vTestHandCards3 = new ArrayList<Integer>();
    protected ArrayList<Integer> vTestHandCards4 = new ArrayList<Integer>();
    protected ArrayList<Integer> vTestTableCards = new ArrayList<Integer>();

    protected Map<Long, Long> mOriginalStacks = new HashMap<Long, Long>();

    protected boolean bGameCanceled = false;

    protected Map<Long, UserGameInfoNet.Builder> mapStandByUserGameInfos = new ConcurrentHashMap<Long, UserGameInfoNet.Builder>();

    //目前的手牌ID
    protected int mGameHandID = 0;

    //牌局信息
    protected GameRoomInfo stGameRoomInfo;

    //旁观用户列表
    protected Set<Long> mStandBys = new HashSet<Long>();
    //所有旁观用户列表
    protected Set<Long> mStandingBys = new HashSet<Long>();
    public TimeoutHashMap<Long, Long> mStandByTimeOut; //房间相关的数据

    //在桌上的用户列表
    protected Set<Long> mPlayers = new HashSet<Long>();

    protected AdvancedTimeoutHashMap<Long, Long> mTimeoutGameUsers;

    //目前已经抓的牌的数量
    protected int iDrawCardNum = 0;

    //用户的操作超时
    protected Map<Long, Integer> mapTimeoutTimesUser = new ConcurrentHashMap<Long, Integer>();

    //其他的数据
    protected long lStepMaxStacks = 0;

    //可以参加游戏的人数
    protected int iCouldPlayNum = 0;

    //用户在牌局的位置信息
    protected ConcurrentHashMap<Long, Integer> mapUserPos = new ConcurrentHashMap<Long, Integer>();

    protected final static boolean NOT_DEBUG = true;

    //是否所有人都allin状态
    public boolean bAllAllin;

    //分池数据
    protected ArrayList<PotInfo> mPotList = new ArrayList<PotInfo>();

    //临时分池数据用于弃牌模式下底池抽水
    protected ArrayList<PotInfo> mTempPotDrawList = new ArrayList<PotInfo>();

    //用户计算最大池底
    protected long lMaxPot = 0;

    protected int mCards[] = new int[52];

    protected Map<Long, HandInfo> mHandMap = new HashMap<Long, HandInfo>();

    protected Map<Long, HandInfo> mHandSecureMap = new HashMap<Long, HandInfo>();//加密手牌
    public Map<Long, HandInfo> getmHandSecureMap() {
        return mHandSecureMap;
    }

    protected long stageUpdateTime;

    public long getStageUpdateTime() {
        return stageUpdateTime;
    }

    public void setStageUpdateTime(long stageUpdateTime) {
        this.stageUpdateTime = stageUpdateTime;
    }

    public HandInfo getmHandSecureMapByid(long uuid) {
        return mHandSecureMap.get(uuid);
    }

    public boolean setMHandSecureMapByid(int card1, int card2, long uuid) {
        boolean encryptFlag = false;
        if (card1 != -1 && card2 != -1){
            List<Integer> cardList = new ArrayList();
            cardList.add(card1);
            cardList.add(card2);
            TexasPokerSecureProtocolService.EncryptDataResponse encryptDataResponse = SecureManager.getInstance().encryptData(lGameRoomID, uuid, cardList, (int)stGameRoomInfo.getLGameHandID());
            if (encryptDataResponse != null && encryptDataResponse.getCipherData() != null ){
                encryptFlag = true;
                TexasPokerSecureProtocolService.CipherDataMessage vPlainDatas = encryptDataResponse.getCipherData();
                HandInfo stHandInfo = new HandInfo();
                if (vPlainDatas != null && vPlainDatas.getVData() != null && vPlainDatas.getVData().size() > 0){
                    stHandInfo.addSecureCards(vPlainDatas.getVData());
                    mHandSecureMap.put(vPlainDatas.getLuuid(), stHandInfo);
                }
            }
        }
        return encryptFlag;

    }



    public void setHandSecureMap(List<TexasPokerSecureProtocolService.PlainDataMessage> plainDataMessageList){
        TexasPokerSecureProtocolService.EncryptDataListResponse encryptDataListResponse = SecureManager.getInstance().encryptDataList(plainDataMessageList);
        if (encryptDataListResponse != null) {
            for (int i = 0; i < encryptDataListResponse.getVPlainDatasCount(); i++) {
                TexasPokerSecureProtocolService.CipherDataMessage vPlainDatas = encryptDataListResponse.getVPlainDatas(i);
                HandInfo stHandInfo = new HandInfo();
                if (vPlainDatas != null && vPlainDatas.getVData() != null && vPlainDatas.getVData().size() > 0){
                    stHandInfo.addSecureCards(vPlainDatas.getVData());
                    mHandSecureMap.put(vPlainDatas.getLuuid(), stHandInfo);
                }
            }
        }
    }



    protected List<Long> vHandList = new ArrayList<Long>();
    protected Map<Integer, Long> mHandPos = new HashMap<Integer, Long>();

    protected String LOG_TAG = "";

    protected long lFromRoomID = 0;
    protected long lGameRoomID = 0;
    protected boolean bPause = false;
    protected boolean bStarted = false;
    protected int iGameRoomUserMaxNums = 0;

    //留座离桌超时map
    protected TimeoutHashMap<Long, Long> mTemporarilyLeaveUsersTimeout; //房间相关的数据

    //下手进入留座离桌的队列
    protected Map<Long, Long> mTemporarilyLeaveUsers = new HashMap<Long, Long>();

    //查看是否可以看牌
    public boolean bShowTableCard = false;
    public boolean bFirstWaitShowTableCard = true;

    protected Map<Long, Long> mUserOutStacks = new HashMap<Long, Long>();

    protected Set<Integer> mCurrentTableCards = new HashSet<Integer>();

    //打牌位置优先级
    protected ArrayList<Long> vPosPriority = new ArrayList<Long>();

    //是否为授信牌局
    public boolean bCredit = false;

    //抽水方式 0不抽水 1战绩抽水2小时一抽 2底池抽水
    public int iDrawType = 0;

    //是否本手结束抽水
    public boolean bDraw = false;

    //玩家离开牌桌计时map
    public TimeoutHashMap<Long, Long> mUserLeaveTime;

    //玩家离开牌桌计时辅助map
    public TimeoutHashMap<Long, Long> mUserLeaveTimeMap;

    //是否本手下发flop3张牌
    public boolean bFlopStatus = false;

    //底池抽水每个玩家的贡献值 玩家uuid-抽水贡献值(包含获胜玩家和未获胜玩家)
    public Map<Long, Long> mUserDrawStacks = new HashMap<Long, Long>();

    //底池抽水每个玩家的原始贡献值 玩家uuid-原始抽水贡献值(只包含获胜玩家)
    public Map<Long, Long> mUserOriginDrawStacks = new HashMap<Long, Long>();

    //实际参加游戏的玩家
    protected int iUserNum = 0;

    //是否为声纹牌局
    public boolean bVoice = false;

    //玩家本手是否验证
    public ConcurrentHashMap<Long,Boolean> mUserIsCheck = new ConcurrentHashMap<Long,Boolean>();

    //玩家首次坐下随机生成的声纹检测手数
    public ConcurrentHashMap<Long,Integer> mUserVoiceCheck = new ConcurrentHashMap<Long,Integer>();

    //玩家在本牌局的验证次数
    public ConcurrentHashMap<Long,Integer> mUserCheckCount = new ConcurrentHashMap<Long,Integer>();

    //玩家声纹验证开始时间
    public ConcurrentHashMap<Long,Long> mUserVerifiyTime = new ConcurrentHashMap<Long,Long>();

    //玩家是否声纹验证中
    public ConcurrentHashMap<Long,Boolean> mUserVerifiying = new ConcurrentHashMap<Long,Boolean>();

    //同时行动玩家下注筹码量
    public ConcurrentHashMap<Long,Long> mSimUserBetStacks = new ConcurrentHashMap<Long,Long>();

    //同时行动下注最大值
    public long lSimBetMaxStacks = 0l;

    //同时行动玩家位置记录 小盲,大盲... uuid-位置
    public ConcurrentHashMap<Long,Integer> mSimUserPosit = new ConcurrentHashMap<Long,Integer>();

    //同时行动玩家是否被迫allin
    public ConcurrentHashMap<Long,Boolean> mSimUserAllin = new ConcurrentHashMap<Long,Boolean>();

    //同时行动玩家弃牌日志记录
    public ConcurrentHashMap<Long,String> mUserFlodRecode = new ConcurrentHashMap<Long,String>();

    //玩家在本牌局进行的手数
    public ConcurrentHashMap<Long,Integer> mUserPlayHand = new ConcurrentHashMap<Long,Integer>();

    /**
     * 初始化函数
     */
    public GameRoomAbstractInfo() {
        mTimeoutGameUsers = new AdvancedTimeoutHashMap<Long, Long>();

        vTestHandCards1.add(4);
        vTestHandCards1.add(5);

        vTestHandCards2.add(6);
        vTestHandCards2.add(7);

        vTestHandCards3.add(10);
        vTestHandCards3.add(11);

        vTestHandCards4.add(8);
        vTestHandCards4.add(7);

        vTestTableCards.add(31);
        vTestTableCards.add(41);
        vTestTableCards.add(15);
        vTestTableCards.add(16);
        vTestTableCards.add(19);

        mTemporarilyLeaveUsersTimeout = new TimeoutHashMap<Long, Long>();
        mTemporarilyLeaveUsersTimeout.setTimeout(180000);

        mUserLeaveTime = new TimeoutHashMap<Long, Long>();
        mUserLeaveTime.setTimeout(ConfigaturationManager.iDrawTime * 60 * 60 * 1000); // 玩家离开牌桌超时map

        mUserLeaveTimeMap = new TimeoutHashMap<Long, Long>();
        mUserLeaveTimeMap.setTimeout(ConfigaturationManager.iDrawTime * 60 * 60 * 1000); // 玩家离开牌桌超时辅助map

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
    public Set<Long> getStandByUuids() {
        return mStandingBys;
    }


    @Override
    public Set<Long> getAllStandByUuids() {
        return mStandBys;
    }

    @Override
    public void addStandByUuid(long uuid) {
        mStandBys.add(uuid);
        mStandingBys.add(uuid);
        mStandByTimeOut.setCurrent(uuid, uuid);
    }

    @Override
    public void removeStandByUuid(long uuid) {
        mStandingBys.remove(uuid);
        mStandByTimeOut.removeKey(uuid);
    }

    @Override
    public int getLastHandID() {
        if (mGameHandID - 1 > 0) {
            return mGameHandID - 1;
        } else {
            return 0;
        }
    }

    @Override
    public void removeAllStandByStatusUuid(long uuid) {
        mStandingBys.remove(uuid);
        mStandBys.remove(uuid);
        mStandByTimeOut.removeKey(uuid);
    }

    @Override
    public int getHandID() {
        if (mGameHandID > 0) {
            return mGameHandID;
        } else {
            return 0;
        }
    }

    @Override
    public long getMaxPot() {
        return lMaxPot;
    }

    /**
     * 弃牌函数，不可以通过uuid获取位置的弃牌（站起坐下弃牌用这个函数，且此时的lbettingid为-2）
     *
     * @param uuid    弃牌的用户
     * @param userPos 用户所在位置
     * @return
     */
    @Override
    public ERROR_CODE_TYPE doFlodAction(long uuid, int userPos, boolean bDeleggate) {
        return doFlodAction(uuid, userPos, -2, bDeleggate);
    }

    /**
     * 弃牌函数，可以通过uuid获取位置的弃牌（非站起坐下弃牌用这个函数）
     *
     * @param uuid       弃牌的用户
     * @param lBettingID 手牌动作ID
     * @return
     */

    @Override
    public ERROR_CODE_TYPE doFlodAction(long uuid, long lBettingID, boolean bDeleggate) {
        Integer userPos = getUserPos(uuid);
        if (userPos != null) {
            return doFlodAction(uuid, userPos, lBettingID, bDeleggate);
        } else {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
        }
    }

    @Override
    public ERROR_CODE_TYPE doCheckAction(long uuid, long lBettingID,int buttonLogType) {
        ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        synchronized (this) {
            try {
                GameRoomInfo.Builder st = stGameRoomInfo.toBuilder();
                st.setBStateChange(0);
                stGameRoomInfo = st.build();

                if (bPause == true) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
                }
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (checkGameActionValidation() == false) {
                    return eErrorCode;
                }

                int victoryUser = -1;
                Integer userPos = getUserPos(uuid);

                if (userPos != null) {
                    // ������������������
                    int iLoop = 0;

                    // ������������������������������
                    for (int j = userPos; iLoop < iGameRoomUserMaxNums; j++, iLoop++) {
                        int userIndex = 0;
                        if (j < iGameRoomUserMaxNums) {
                            userIndex = j;
                        } else {
                            userIndex = j - iGameRoomUserMaxNums;
                        }

                        // ������������������
                        UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();

                        if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                            if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
                                    || (lBettingID != -1 && lBettingID != stUserGameInfoBuilder.getLBettingID() && lBettingID != 0)) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            if (lStepMaxStacks != stUserGameInfoBuilder.getLBetStacks()) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            TableLogService.getInstance().error(lFromRoomID
                                    + "," + lGameRoomID
                                    + "," + stGameRoomInfo.getLGameHandID()
                                    + "," + uuid
                                    + "," + "check"
                                    + "," + 0);

                            if (buttonLogType == 1){
                                //牌局ID，玩家ID，手数ID，按钮编号，点击位置
                                ButtonLogService.getInstance().error(lGameRoomID //牌局ID
                                        + " " + stUserGameInfoBuilder.getStUserInfo().getUuid() //玩家ID
                                        + " " + stGameRoomInfo.getLGameHandID() //手数ID
                                        + " " + "61" //按钮编号 系统看牌
                                        + " " + "0"); //点击位置
                            }
                            // ������������������������
                            stUserGameInfoBuilder
                                    .setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            stUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                            // ������������gameroominfo���������������������
                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);

                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            //���������������������������������������������������������������������������������������������
                            victoryUser = iGameOver();
                            if (victoryUser != -1) {
                                //������������������push������������
                                sendAllGameStateChange(uuid, null);

                                flodWinProcess();

                                sendAllGameStateChange(uuid, null);
                                break;
                            }
                        } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() != 0) {

                            if (userIndex == mEndPos) {
                                // ������������������������������������
                                sendAllGameStateChange(uuid, null);

                                SidePot();
                                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                                //���������river���������������������������������������������������������������
                                if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River) {
                                    //���������������allin������
                                    if (checkAllAllin() == true) {
                                        stGameRoomInfoBuilder.setBAllAllin(1);
                                        bAllAllin = true;
                                    }

                                    if (iCouldPlayNum == 2) {
                                        mEndPos = mBigBlindPos;
                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());


                                        //reset������
                                        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                                                if (stResetUserGameInfoBuilder.getLRemainStacks() > 0) {
                                                    if (mBigBlindPos == k) {
                                                        //���������������������������

                                                        if (bAllAllin == false) {
                                                            stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                                            stResetUserGameInfoBuilder.setIDelayLong(0);
                                                            stResetUserGameInfoBuilder.setIDelaySingleTime(20);
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                                            stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                                            //如果依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                                            if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                                                stResetUserGameInfoBuilder.clearVCurrentHands();
                                                                stResetUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                                            }

                                                        } else {
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

                                                        }

                                                        removeAllTimeOutUser();
                                                        setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                                    } else {
                                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                                                    }
                                                } else {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }

                                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                                    UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                                    stUserBaseInfo.setUuid(0);
                                                    stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                                }
                                            }

                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                            //������������gameroominfo���������������������
                                            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                        }
                                    } else {
                                        mEndPos = mSmallBlindPos;
                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                        int iBetting = 0;
                                        int iLoop1 = 0;
                                        //���������������������������������������
                                        for (int k = mSmallBlindPos; iLoop1 < iGameRoomUserMaxNums; iLoop1++, k++) {
                                            int userIndex2 = 0;
                                            if (k < iGameRoomUserMaxNums) {
                                                userIndex2 = k;
                                            } else {
                                                userIndex2 = k - iGameRoomUserMaxNums;
                                            }

                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex2).toBuilder();
                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                if (stResetUserGameInfoBuilder.getLRemainStacks() > 0) {
                                                    if (iBetting == 0) {
                                                        //���������������������������
                                                        if (bAllAllin == false) {
                                                            stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                                            stResetUserGameInfoBuilder.setIDelayLong(0);
                                                            stResetUserGameInfoBuilder.setIDelaySingleTime(20);
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                                            stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                                            //如果依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                                            if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                                                stResetUserGameInfoBuilder.clearVCurrentHands();
                                                                stResetUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                                            }

                                                        } else {
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                        }

                                                        removeAllTimeOutUser();
                                                        setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                                        iBetting = 1;
                                                    } else {
                                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                                                    }
                                                } else {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }

                                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                                    UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                                    stUserBaseInfo.setUuid(0);
                                                    stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                                }
                                            }

                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
                                        }
                                    }
                                }

                                stGameRoomInfo = stGameRoomInfoBuilder.build();
                                switch (stGameRoomInfoBuilder.getEGameState()
                                        .getNumber()) {
                                    case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//								TableLogService.getInstance().error1(lFromRoomID
//										+"," + lGameRoomID
//										+"," + stGameRoomInfo.getLGameHandID()
//										+",gotoFlopAllStatus enter2" + stGameRoomInfo.getEGameState());
                                        gotoFlopAllStatus(uuid);
                                    }
                                    break;
                                    case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//								TableLogService.getInstance().error1(lFromRoomID
//								+"," + lGameRoomID
//								+"," + stGameRoomInfo.getLGameHandID()
//								+",checkGotoTurnStatus enter1" + stGameRoomInfo.getEGameState());

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
                            } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                                if (stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin == false) {
                                    stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                    stUserGameInfoBuilder.setIDelayLong(0);
                                    stUserGameInfoBuilder.setIDelaySingleTime(20);
                                    stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                    stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                    //如果依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                        stUserGameInfoBuilder.clearVCurrentHands();
                                        stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                    }

                                    removeAllTimeOutUser();
                                    setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                                    // ������������gameroominfo���������������������
                                    stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    sendAllGameStateChange(uuid, null);
                                    break;
                                }

                            }
                        }
                    }
                } else {
                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }catch (Exception e){
                e.printStackTrace();
                try {
                    StuckGameLogService.getInstance().error("doCheckAction : "+e.toString());
                    Iterator<StackTraceElement> iterator = Arrays.asList(e.getStackTrace()).iterator();
                    while(iterator.hasNext()){
                        StuckGameLogService.getInstance().error(iterator.next().toString());
                    }
                    GameRoomInfo.Builder gameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    StuckGameLogService.getInstance().error("doCheckAction "+ gameRoomInfoBuilder.toString());
                    StuckGameLogService.getInstance().error("doCheckAction "+
                            gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                            +"=" + gameRoomInfoBuilder.getLGameHandID()
                            +"=" + mEndPos
                            +"="+mBigBlindPos
                            +"="+mSmallBlindPos
                            +"=" + bAllAllin
                            +"=" + stGameRoomInfo.getEGameState()
                            +"=" + stageUpdateTime
                            +"="+ iCouldPlayNum);

                    for(int i=0;i <iGameRoomUserMaxNums;i++){
                        UserGameInfoNet vUserGameInfos = gameRoomInfoBuilder.getVUserGameInfos(i);
                        if(vUserGameInfos!=null){
                            StuckGameLogService.getInstance().error(
                                    "doCheckAction "
                                            + (vUserGameInfos.getStUserInfo()==null?"null":vUserGameInfos.getStUserInfo().getUuid())
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())== null ? false:true)
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())==null?"null":(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid()).getvCards()))
                                            +"=" + vUserGameInfos.getEGameState()
                                            +"=" + vUserGameInfos.getEGameRole()
                                            +"=" + vUserGameInfos.getBGameDealer()
                                            +"=" + vUserGameInfos.getBDelegate()
                                            +"=" + vUserGameInfos.getBJustSit()
                                            +"=" + vUserGameInfos.getLBetStacks()
                                            +"="+ vUserGameInfos.getIDelaySingleTime());
                        }

                    }
                }catch (Exception e2){
                    e2.printStackTrace();
                }
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
    protected void gotoFlopAllStatus(long uuid) {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        //AOF模式下且依次看牌此时需要强制将所有在桌玩家手牌下发
        if ((stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1) || stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++){
                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() > 0
                        && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                        && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY){
                    stUserGameInfoNetBuilder.clearVCurrentHands();

                    ArrayList<Integer> cards = new ArrayList<Integer>();
                    if (mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()) != null && mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()).getvCards() != null){
                        cards.addAll(mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()).getvCards());
                    } else {
                        cards.add(-1);
                        cards.add(-1);
                    }
                    stUserGameInfoNetBuilder.addAllVCurrentHands(cards);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
            }
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();

//        if (stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
//            sendAllGameStateChange(0, null);  //下发push动画过度
//        }

        if (stGameRoomInfoBuilder.getVCurrentCardsCount() == 0) {
            int card1 = 0;
            int card2 = 0;
            int card3 = 0;
            if (NOT_DEBUG) {
                card1 = getTableCard(0);
                card2 = getTableCard(1);
                card3 = getTableCard(2);
            } else {
                card1 = getCard(0, 0, true);
                card2 = getCard(0, 1, true);
                card3 = getCard(0, 2, true);
            }

            stGameRoomInfoBuilder.addVCurrentCards(card1);
            stGameRoomInfoBuilder.addVCurrentCards(card2);
            stGameRoomInfoBuilder.addVCurrentCards(card3);

            stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);
            setStageUpdateTime(System.currentTimeMillis());
            stGameRoomInfoBuilder.setBStateChange(1);

            stGameRoomInfo = stGameRoomInfoBuilder.build();

            ArrayList<Integer> cards = new ArrayList<Integer>();
            cards.add(card1);
            cards.add(card2);
            cards.add(card3);

            //push野保险3张牌
            FlopLottoManager.getInstance().pushFlopLottoCards(lGameRoomID,cards,System.currentTimeMillis(),stGameRoomInfo.getLGameHandID());

            sendAllGameStateChange(uuid, cards);

            TableLogService.getInstance().error(lFromRoomID
                    + "," + lGameRoomID
                    + "," + stGameRoomInfo.getLGameHandID()
                    + "," + 0
                    + "," + "flop"
                    + "," + card1
                    + "," + card2
                    + "," + card3);
        } else {
            stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);
            stGameRoomInfoBuilder.setBStateChange(1);
            setStageUpdateTime(System.currentTimeMillis());
        }
        stGameRoomInfo = stGameRoomInfoBuilder.build();
        bFlopStatus = true;
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

    void checkGotoTurnStatus(long uuid) {
        if (checkAllAllin() == true) {
//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",checkGotoTurnStatus begin" + stGameRoomInfo.getEGameState());

            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            InsuranceInfo stInsuranceInfo = null;
            if (stGameRoomInfoBuilder.getEInsuranceStatus() == INSURANCE_STATUS.INSURANCE_STATUS_NO) {
//				TableLogService.getInstance().error1(lFromRoomID
//						+"," + lGameRoomID
//						+"," + stGameRoomInfo.getLGameHandID()
//						+",checkGotoTurnStatus 1" + stGameRoomInfo.getEGameState());
                stInsuranceInfo = InsuranceApi.checkInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop);

                if (stInsuranceInfo != null) {
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",checkGotoTurnStatus 2" + stGameRoomInfo.getEGameState());
                    //进入保险模式
                    stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_BETTING);
                    stGameRoomInfoBuilder.setBStateChange(1);
                    stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    sendAllGameStateChange(0, null);
                    //记录cent需要的最后一个下牌的玩家
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",checkGotoTurnStatus 3" + stGameRoomInfo.getEGameState());
                } else {
                    gotoTurnStatus(uuid, false);
                }
            }

//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",checkGotoTurnStatus end" + stGameRoomInfo.getEGameState());
        } else {
            gotoTurnStatus(uuid, false);
        }

    }

    void gotoTurnStatus(long uuid, boolean bInsurance) {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        int card1 = 0;
        ArrayList<Integer> cards = new ArrayList<Integer>();

        //保护，发这张牌时，排面上只能有3张牌
        if (stGameRoomInfo.getVCurrentCardsCount() <= 3) {
            //发turn牌
            if (NOT_DEBUG) {
                card1 = getTableCard(3);
            } else {
                card1 = getCard(0, 3, true);
            }

            stGameRoomInfoBuilder.addVCurrentCards(card1);
            TableLogService.getInstance().error(lFromRoomID
                    + "," + lGameRoomID
                    + "," + stGameRoomInfo.getLGameHandID()
                    + "," + 0
                    + "," + "turn"
                    + "," + card1);

            cards.add(card1);
        }

        stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn);
        stGameRoomInfoBuilder.setBStateChange(1);
        setStageUpdateTime(System.currentTimeMillis());

        if (bInsurance) {
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            stGameRoomInfo = InsuranceApi.getInsuranceResult(stGameRoomInfo, stGameRoomInfo.getLGameHandID(), card1);
            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_RESULT);
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();

        sendAllGameStateChange(uuid, cards);

        stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_NO);
        stGameRoomInfo = stGameRoomInfoBuilder.build();

    }

    void checkGotoRiverStatus(long uuid) {
        if (checkAllAllin() == true) {
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            InsuranceInfo stInsuranceInfo = null;
            if (stGameRoomInfoBuilder.getEInsuranceStatus() == INSURANCE_STATUS.INSURANCE_STATUS_NO) {

                stInsuranceInfo = InsuranceApi.checkInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn);

                if (stInsuranceInfo != null) {
                    //进入保险模式
                    stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_BETTING);
                    stGameRoomInfoBuilder.setBStateChange(1);
                    stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    sendAllGameStateChange(0, null);
                    //记录cent需要的最后一个下牌的玩家
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                } else {
                    gotoRiverStatus(uuid, false);
                }
            }
        } else {
            gotoRiverStatus(uuid, false);
        }

    }

    void gotoRiverStatus(long uuid, boolean bInsurance) {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        int card1 = 0;
        ArrayList<Integer> cards = new ArrayList<Integer>();
        if (stGameRoomInfo.getVCurrentCardsCount() <= 4) {
            if (NOT_DEBUG) {
                card1 = getTableCard(4);
            } else {
                card1 = getCard(0, 4, true);
            }

            stGameRoomInfoBuilder.addVCurrentCards(card1);

            TableLogService.getInstance().error(lFromRoomID
                    + "," + lGameRoomID
                    + "," + stGameRoomInfo.getLGameHandID()
                    + "," + 0
                    + "," + "river"
                    + "," + card1);

            cards.add(card1);
        }

        stGameRoomInfoBuilder.setEGameState(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River);
        stGameRoomInfoBuilder.setBStateChange(1);
        setStageUpdateTime(System.currentTimeMillis());

        if (bInsurance) {
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            stGameRoomInfo = InsuranceApi.getInsuranceResult(stGameRoomInfo, stGameRoomInfo.getLGameHandID(), card1);
            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_RESULT);
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();


        if (checkAllAllin() == true) {
            stGameRoomInfoBuilder.setBAllAllin(1);
            bAllAllin = true;
        }

        sendAllGameStateChange(uuid, cards);

        stGameRoomInfoBuilder.setEInsuranceStatus(INSURANCE_STATUS.INSURANCE_STATUS_NO);
        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    void gotoResultStatus(long uuid) {
        //如果是和牌
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result);
        stGameRoomInfoBuilder.setBStateChange(1);
        setStageUpdateTime(System.currentTimeMillis());
        stGameRoomInfo = stGameRoomInfoBuilder.build();

        bShowTableCard = true;
        bFirstWaitShowTableCard = true;

        mCurrentTableCards.clear();
        mCurrentTableCards.addAll(stGameRoomInfo.getVCurrentCardsList());

        //计算结果
        calculateWinner();

        stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        //重置结尾allin状态
        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                if (stResetUserGameInfoBuilder.getLRemainStacks() == 0 || bAllAllin == true) {
                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                }
            }

            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
        }
        stGameRoomInfo = stGameRoomInfoBuilder.build();

        //Push下发结果
        sendAllGameStateChange(uuid, null);

    }


    @Override
    public ERROR_CODE_TYPE doRaiseAction(long uuid, GAME_ACT_TYPE eGameActType, long lStacks, long lBettingID) {
        ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        synchronized (this) {
            try {
                GameRoomInfo.Builder st = stGameRoomInfo.toBuilder();
                st.setBStateChange(0);
                stGameRoomInfo = st.build();

                if (bPause == true) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
                }
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (checkGameActionValidation() == false) {
                    return eErrorCode;
                }

                int victoryUser = -1;
                Integer userPos = getUserPos(uuid);
                if (userPos != null) {
                    //������������������
                    int iLoop = 0;

                    //������������������������������
                    for (int j = userPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                        int userIndex = 0;
                        if (j < iGameRoomUserMaxNums) {
                            userIndex = j;
                        } else {
                            userIndex = j - iGameRoomUserMaxNums;
                        }

                        //������������������
                        UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                        if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                            if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
                                    || (lBettingID != -1 && lBettingID != stUserGameInfoBuilder.getLBettingID() && lBettingID != 0)) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            if ((lStepMaxStacks > stUserGameInfoBuilder.getLBetStacks() + lStacks) && (stUserGameInfoBuilder.getLRemainStacks() - lStacks) != 0) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            //玩家下注筹码安全性校验
                            if ((stUserGameInfoBuilder.getLRemainStacks() - lStacks) < 0){
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            //AOF模式校验
                            if(getStGameRoomInfo().getStGameRoomBaseInfo().getBFullAllIn() == 1 && stUserGameInfoBuilder.getLRemainStacks() != lStacks){
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            TableLogService.getInstance().error(lFromRoomID
                                    + "," + lGameRoomID
                                    + "," + stGameRoomInfo.getLGameHandID()
                                    + "," + uuid
                                    + "," + "raise"
                                    + "," + lStacks);

                            //设置玩家下注筹码量
                            setSimUserBetStacks(uuid,lStacks);

                            //������������������������
                            if (GAME_ACT_TYPE.GAME_ACT_TYPE_ALLIN_VALUE == eGameActType.getNumber()) {
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                            } else {
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_RAISE);
                            }
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            stUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                            stUserGameInfoBuilder.setLBetStacks(lStacks + stUserGameInfoBuilder.getLBetStacks());
                            long lTempEffectiveStacks = stUserGameInfoBuilder.getLBetStacks() - lStepMaxStacks;
                            if (lTempEffectiveStacks > stGameRoomInfoBuilder.getLEffectiveRaise()) {
                                stGameRoomInfoBuilder.setLEffectiveRaise(lTempEffectiveStacks);
                            }

                            lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();

                            stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - lStacks);

                            mEndPos = userPos;

                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);

                            //找到目前所有与动作为更助的用户，重新设置这些用户用户的状态
                            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                                if (stUserGameInfoNetBuilder.getEPreAction() == USER_GAME_STATE.USER_GAME_STATE_CALL) {
                                    stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                }
                            }

                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            //同时行动
                            if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBSimultAction() == 1){

                                //检测是否还有betting玩家
                                if (!checkIsBetting()){
                                    if (checkAllInNum() == 1){

                                        //只有一个allin玩家
                                        List<Long> uuidList = getAllInUuid();
                                        long userid = uuidList.get(0);

                                        //1.检测当前allin玩家是否为被迫allin
                                        //a.如果为被迫allin获取场上最后行动玩家进行比牌
                                        //b.如果玩家正常allin则玩家直接获胜
                                        long lastActionUuid = getLastActionUuid();

                                        //如果本身是最后一个玩家直接获胜
                                        if (userid == lastActionUuid){
                                            sendAllGameStateChange(uuid, null);

                                            //打印flod日志
                                            for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                                TableLogService.getInstance().error(lFromRoomID
                                                        + "," + lGameRoomID
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + map.getKey()
                                                        + "," + "fold"
                                                        + "," + map.getValue());
                                            }

                                            flodWinProcess();
                                            sendAllGameStateChange(uuid, null);
                                            break;

                                        } else {

                                            long otherUuid = 0l;

                                            //和最后一个玩家进行比牌流程
                                            GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                            for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                                //获取所有牌局上的用户
                                                UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                                if (stUserGameInfo.getStUserInfo().getUuid() == lastActionUuid) {
                                                    stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                                    stGameRoom.setVUserGameInfos(k,stUserGameInfo);

                                                    TableLogService.getInstance().error(lFromRoomID
                                                            + "," + lGameRoomID
                                                            + "," + stGameRoomInfo.getLGameHandID()
                                                            + "," + lastActionUuid
                                                            + "," + "check"
                                                            + "," + 0);



                                                    otherUuid = lastActionUuid;

                                                    break;
                                                }
                                            }
                                            stGameRoomInfo = stGameRoom.build();

                                            sendAllGameStateChange(uuid, null);

                                            //打印flod日志
                                            for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                                if (map.getKey() != otherUuid){
                                                    TableLogService.getInstance().error(lFromRoomID
                                                            + "," + lGameRoomID
                                                            + "," + stGameRoomInfo.getLGameHandID()
                                                            + "," + map.getKey()
                                                            + "," + "fold"
                                                            + "," + map.getValue());
                                                }
                                            }

                                            bAllAllin = true;

                                            SidePot();

                                            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                            stGameRoomInfoBuilder.setBAllAllin(1);

                                            for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                                UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                        && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                        && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                    if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                    }
                                                }

                                                //������������������������������������
                                                stResetUserGameInfoBuilder.setLBetStacks(0);
                                                stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                                //������������gameroominfo���������������������
                                                stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                            }

                                            lStepMaxStacks = 0;
                                            stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                                            gotoFlopAllStatus(uuid);
                                            break;
                                        }

                                    } else if (checkAllInNum() > 1){

                                        //allin玩家有多个时
                                        //a.allin玩家中存在没有被迫allin的直接走比牌结算
                                        //b.allin玩家中都是被迫allin的需要和最后行动玩家比牌
                                        //最后行动玩家
                                        long lastActionUuid = getLastActionUuid();
                                        Boolean bool = false;

                                        long otherUuid = 0l;

                                        //如果最后行动的玩家在allin玩家内直接比牌
                                        List<Long> uuidList = getAllInUuid();
                                        for (Long userid : uuidList){
                                            if (lastActionUuid == userid){
                                                bool = true;
                                            }
                                        }

                                        //最后行动的玩家在allin玩家里直接比牌
                                        if (!bool){
                                            //最后行动的玩家不在allin内最后行动玩家需要参与比牌
                                            GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                            for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                                //获取所有牌局上的用户
                                                UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                                if (stUserGameInfo.getStUserInfo().getUuid() == lastActionUuid) {
                                                    stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                                    stGameRoom.setVUserGameInfos(k,stUserGameInfo);

                                                    TableLogService.getInstance().error(lFromRoomID
                                                            + "," + lGameRoomID
                                                            + "," + stGameRoomInfo.getLGameHandID()
                                                            + "," + lastActionUuid
                                                            + "," + "check"
                                                            + "," + 0);


                                                    otherUuid = lastActionUuid;

                                                    break;
                                                }
                                            }
                                            stGameRoomInfo = stGameRoom.build();
                                        }

                                        //打印flod日志
                                        for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                            if (map.getKey() != otherUuid){
                                                TableLogService.getInstance().error(lFromRoomID
                                                        + "," + lGameRoomID
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + map.getKey()
                                                        + "," + "fold"
                                                        + "," + map.getValue());
                                            }
                                        }

                                        sendAllGameStateChange(uuid, null);
                                        bAllAllin = true;

                                        SidePot();

                                        stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                        stGameRoomInfoBuilder.setBAllAllin(1);

                                        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }
                                            }

                                            //������������������������������������
                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                            //������������gameroominfo���������������������
                                            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                        }

                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                                        gotoFlopAllStatus(uuid);

                                        break;

                                    } else {
                                        //所有玩家都弃牌了,上面已经处理了
                                    }
                                }
                                break;
                            }

                            victoryUser = iGameOver();
                            if (victoryUser != -1) {
                                //������������������push������������
                                sendAllGameStateChange(uuid, null);

                                flodWinProcess();

                                sendAllGameStateChange(uuid, null);
                                break;
                            }

                        } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {


                            if (stUserGameInfoBuilder.getLRemainStacks() > 0 && checkAllAllin() == false) {
                                //���������������������������������������������������������������������
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stUserGameInfoBuilder.setIDelayLong(0);
                                stUserGameInfoBuilder.setIDelaySingleTime(20);

                                //如果在AOF模式下依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                if ((stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1) || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1)){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }

                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                                //������������gameroominfo���������������������
                                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);

                                stGameRoomInfo = stGameRoomInfoBuilder.build();

                                sendAllGameStateChange(uuid, null);
                                break;
                            } else {

                                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                if (checkAllAllin() == true) {
                                    sendAllGameStateChange(uuid, null);
                                    bAllAllin = true;

                                    SidePot();

                                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                    stGameRoomInfoBuilder.setBAllAllin(1);

                                    for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                            if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                            }
                                        }

                                        //������������������������������������
                                        stResetUserGameInfoBuilder.setLBetStacks(0);
                                        stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                        //������������gameroominfo���������������������
                                        stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                    }

                                    lStepMaxStacks = 0;
                                    stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                    //������������
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                                    //������������������
                                    switch (stGameRoomInfoBuilder.getEGameState().getNumber()) {
                                        case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//											TableLogService.getInstance().error1(lFromRoomID
//													+"," + lGameRoomID
//													+"," + stGameRoomInfo.getLGameHandID()
//													+",gotoFlopAllStatus enter3" + stGameRoomInfo.getEGameState());
                                            gotoFlopAllStatus(uuid);
                                        }
                                        break;
                                        case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//											TableLogService.getInstance().error1(lFromRoomID
//													+"," + lGameRoomID
//													+"," + stGameRoomInfo.getLGameHandID()
//													+",checkGotoTurnStatus enter2" + stGameRoomInfo.getEGameState());

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
                                }
                            }
                        }
                    }

                } else {
                    //������������������������
                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }catch (Exception e){
                e.printStackTrace();
                try {
                    StuckGameLogService.getInstance().error("doRaiseAction : "+e.toString());
                    Iterator<StackTraceElement> iterator = Arrays.asList(e.getStackTrace()).iterator();
                    while(iterator.hasNext()){
                        StuckGameLogService.getInstance().error(iterator.next().toString());
                    }
                    GameRoomInfo.Builder gameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    StuckGameLogService.getInstance().error("doRaiseAction "+ gameRoomInfoBuilder.toString());
                    StuckGameLogService.getInstance().error("doRaiseAction "+
                            gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                            +"=" + gameRoomInfoBuilder.getLGameHandID()
                            +"=" + mEndPos
                            +"="+mBigBlindPos
                            +"="+mSmallBlindPos
                            +"=" + bAllAllin
                            +"=" + stGameRoomInfo.getEGameState()
                            +"=" + stageUpdateTime
                            +"="+ iCouldPlayNum);

                    for(int i=0;i <iGameRoomUserMaxNums;i++){
                        UserGameInfoNet vUserGameInfos = gameRoomInfoBuilder.getVUserGameInfos(i);
                        if(vUserGameInfos!=null){
                            StuckGameLogService.getInstance().error(
                                    "doRaiseAction "
                                            + (vUserGameInfos.getStUserInfo()==null?"null":vUserGameInfos.getStUserInfo().getUuid())
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())== null ? false:true)
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())==null?"null":(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid()).getvCards()))
                                            +"=" + vUserGameInfos.getEGameState()
                                            +"=" + vUserGameInfos.getEGameRole()
                                            +"=" + vUserGameInfos.getBGameDealer()
                                            +"=" + vUserGameInfos.getBDelegate()
                                            +"=" + vUserGameInfos.getBJustSit()
                                            +"=" + vUserGameInfos.getLBetStacks()
                                            +"="+ vUserGameInfos.getIDelaySingleTime());
                        }
//
                    }
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }

        }
        return eErrorCode;
    }

    /**
     * 处理下发房间信息
     * <br/>
     * @param
     * @return void
     * @author piness
     * @date 2021-03-05 15:12
     */
    public GameRoomInfo.Builder dealPushGameRoom(GameRoomInfo.Builder stGameRoomInfoBuilder, long stToUserUuid) {
        GameRoomInfo.Builder stGameRoomInfoBuilderCopy = getSecureGameRoomInfo(stGameRoomInfoBuilder.build(), 2).toBuilder();
        for (int i = 0; i < stGameRoomInfoBuilderCopy.getVUserGameInfosCount(); i++) {
            UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilderCopy.getVUserGameInfosBuilder(i);
//            UserGameInfoNet.Builder stUserGameInfoNetBuilder = getSecureUserGameInfoNet(stGameRoomInfoBuilder.getVUserGameInfos(i),2).toBuilder();

            HandInfo handInfo = getmHandSecureMapByid(stUserGameInfoNetBuilder.getStUserInfo().getUuid());
            if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {

                if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
                    continue;
                }

                if (stGameRoomInfoBuilderCopy.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
                        || (stGameRoomInfoBuilderCopy.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
                    if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
                        continue;
                    }
                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        stUserGameInfoNetBuilder.clearVCurrentHands();
                        stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                    } else {
                        if (bFlodResult == true) {
                            stUserGameInfoNetBuilder.clearVCurrentHands();
                            stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                        }
                    }
                } else {
                    stUserGameInfoNetBuilder.clearVCurrentHands();
                    stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                }
            }
            stGameRoomInfoBuilderCopy.setVUserGameInfos(i, stUserGameInfoNetBuilder);
        }
        return stGameRoomInfoBuilderCopy;
    }

    /****************************
     * Push相关函数
     *******************************/
    @Override
    public void pushFullGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
                                         long stToUserUuid, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType, int pos) {

        //给被邀请用户发通知
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();

        //获取时间
        long curTime = System.currentTimeMillis();

        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);

        // 设置发送人的userbaseinfo
        if (stFromUserGamenfo != null) {
            stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);
        }

        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");

        stGameRoomPushMsgBuilder.setIPos(pos);

        if (stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {

            ClubBaseInfo.Builder stClubBaseInfoBuilder = ClubManager.getInstance().getClubBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID());
            if (stClubBaseInfoBuilder != null && stClubBaseInfoBuilder.getLClubID() > 0) {
                stGameRoomPushMsgBuilder.setLClubID(stClubBaseInfoBuilder.getLClubID());
                stGameRoomPushMsgBuilder.setSClubName(stClubBaseInfoBuilder.getSClubName());
            }
        }


        // 设置 好友类型
        stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);

        checkAllAllin();

        if (stGameRoomInfo != null) {
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
//                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
//                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {
//
//                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
//                        continue;
//                    }
//
//                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
//                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
//                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
//                            continue;
//                        }
//                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
//                            stUserGameInfoNetBuilder.clearVCurrentHands();
//                        } else {
//                            if (bFlodResult == true) {
//                                stUserGameInfoNetBuilder.clearVCurrentHands();
//                            }
//                        }
//                    } else {
//                        stUserGameInfoNetBuilder.clearVCurrentHands();
//                    }
//                }
//                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
//            }

            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }


        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
                .build();

//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    /****************************
     * Push相关函数
     *******************************/
    @Override
    public void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
                                     long stToUserUuid, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType, int pos) {
        UserGameInfoNet secureUserGameInfoNet = getSecureUserGameInfoNet(stFromUserGamenfo, 2);
        //给被邀请用户发通知
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();

        //获取时间
        long curTime = System.currentTimeMillis();

        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);

        // 设置发送人的userbaseinfo
        if (secureUserGameInfoNet != null) {
            stGameRoomPushMsgBuilder.setStFromUser(secureUserGameInfoNet);
        }

        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");

        stGameRoomPushMsgBuilder.setIPos(pos);

        // 设置 好友类型
        stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);

        checkAllAllin();


        if (stGameRoomInfo != null) {
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
//                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
//                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {
//
//                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
//                        continue;
//                    }
//
//                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
//                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
//
//                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
//                            continue;
//                        }
//
//                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
//                            stUserGameInfoNetBuilder.clearVCurrentHands();
//                        } else {
//                            if (bFlodResult == true) {
//                                stUserGameInfoNetBuilder.clearVCurrentHands();
//                            }
//                        }
//                    } else {
//                        stUserGameInfoNetBuilder.clearVCurrentHands();
//                    }
//                }
//                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
//            }

            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }

        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
                .build();

//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    @Override
    public void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo,
                                     long stToUserUuid, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType) {
        //给被邀请用户发通知
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();

        //获取时间
        long curTime = System.currentTimeMillis();

        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);

        // 设置发送人的userbaseinfo
        if (stFromUserGamenfo != null) {
            stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);
        }

        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");

        stGameRoomPushMsgBuilder.setIPos(0);

        // 设置 好友类型
        stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);

        checkAllAllin();

        if (stGameRoomInfo != null) {
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
//                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
//                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {
//
//                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
//                        continue;
//                    }
//
//                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
//                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
//
//                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
//                            continue;
//                        }
//
//                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
//                            stUserGameInfoNetBuilder.clearVCurrentHands();
//                        } else {
//                            if (bFlodResult == true) {
//                                stUserGameInfoNetBuilder.clearVCurrentHands();
//                            }
//                        }
//                    } else {
//                        stUserGameInfoNetBuilder.clearVCurrentHands();
//                    }
//                }
//                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
//            }

            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }

        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
                .build();

//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    @Override
    public void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType) {
        if (stFromUserGamenfo != null) {
            int pos = -1;
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                if (stUserGameInfoNet.getStUserInfo().getUuid() == stFromUserGamenfo.getStUserInfo().getUuid()) {
                    pos = i;
                }
            }

            if (pos >= 0) {
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                    if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != stFromUserGamenfo.getStUserInfo().getUuid()) {
                        //给在牌桌上的人发push
                        pushGameRoomUserMsgs(stFromUserGamenfo, stUserGameInfoNet.getStUserInfo().getUuid(),
                                eGameRoomMessageType, pos);
                    }
                }

                //给旁观的push
                for (long standUuid : getStandByUuids()) {
                    if (standUuid > 0) {
                        pushGameRoomUserMsgs(stFromUserGamenfo, standUuid,
                                eGameRoomMessageType, pos);
                    }
                }
            }
        } else {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                pushGameRoomUserMsgs(stFromUserGamenfo, stUserGameInfoNet.getStUserInfo().getUuid(),
                        eGameRoomMessageType, 0);
            }

            //给旁观的push
            for (long standUuid : getStandByUuids()) {
                if (standUuid > 0) {
                    pushGameRoomUserMsgs(stFromUserGamenfo, standUuid,
                            eGameRoomMessageType, 0);
                }
            }
        }
    }

    @Override
    public void pushAllGameRoomUserMsgsIncludeSelf(UserGameInfoNet stFromUserGamenfo, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType) {
        if (stFromUserGamenfo != null) {
            int pos = -1;
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                if (stUserGameInfoNet.getStUserInfo().getUuid() == stFromUserGamenfo.getStUserInfo().getUuid()) {
                    pos = i;
                }
            }

            if (pos >= 0) {
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                    if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                        //给在牌桌上的人发push
                        pushGameRoomUserMsgs(stFromUserGamenfo, stUserGameInfoNet.getStUserInfo().getUuid(),
                                eGameRoomMessageType, pos);
                    }
                }

                //给旁观的push
                for (long standUuid : getStandByUuids()) {
                    if (standUuid > 0) {
                        pushGameRoomUserMsgs(stFromUserGamenfo, standUuid,
                                eGameRoomMessageType, pos);
                    }
                }
            }
        } else {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                pushGameRoomUserMsgs(stFromUserGamenfo, stUserGameInfoNet.getStUserInfo().getUuid(),
                        eGameRoomMessageType, 0);
            }

            //给旁观的push
            for (long standUuid : getStandByUuids()) {
                if (standUuid > 0) {
                    pushGameRoomUserMsgs(stFromUserGamenfo, standUuid,
                            eGameRoomMessageType, 0);
                }
            }
        }
    }

    @Override
    public void pushAllGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo, GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType, long exceptionUuid) {
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != exceptionUuid) {
                pushGameRoomUserMsgs(stFromUserGamenfo, stUserGameInfoNet.getStUserInfo().getUuid(),
                        eGameRoomMessageType, 0);
            }
        }

        //给旁观的push
        for (long standUuid : getStandByUuids()) {
            if (standUuid > 0 && standUuid != exceptionUuid) {
                pushGameRoomUserMsgs(stFromUserGamenfo, standUuid,
                        eGameRoomMessageType, 0);
            }
        }
    }

    @Override
    public ERROR_CODE_TYPE doCallAction(long uuid, long betstacks, long lBettingID) {
        ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        synchronized (this) {
            try {
                GameRoomInfo.Builder st = stGameRoomInfo.toBuilder();
                st.setBStateChange(0);
                stGameRoomInfo = st.build();

                if (bPause == true) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
                }
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (checkGameActionValidation() == false) {
                    return eErrorCode;
                }

                int victoryUser = -1;
                Integer userPos = getUserPos(uuid);

                if (userPos != null) {
                    //������������������
                    int iLoop = 0;

                    //���������������������������
                    for (int j = userPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {

                        int userIndex = 0;
                        if (j < iGameRoomUserMaxNums) {
                            userIndex = j;
                        } else {
                            userIndex = j - iGameRoomUserMaxNums;
                        }

                        //������������������
                        UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();

                        if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {

                            if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
                                    || (lBettingID != -1 && lBettingID != stUserGameInfoBuilder.getLBettingID() && lBettingID != 0)) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            if ((stUserGameInfoBuilder.getLBetStacks() + betstacks < this.lStepMaxStacks)
                                    && (stUserGameInfoBuilder.getLRemainStacks() - betstacks) != 0) {

                                boolean bCheck = (lStepMaxStacks < stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds())
                                        && (betstacks + stUserGameInfoBuilder.getLBetStacks() == stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                if (!bCheck) {
                                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                    break;
                                }
                            } else if (stUserGameInfoBuilder.getLBetStacks() + betstacks > this.lStepMaxStacks) {
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            //玩家下注筹码安全性校验
                            if ((stUserGameInfoBuilder.getLRemainStacks() - betstacks) < 0){
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                                break;
                            }

                            TableLogService.getInstance().error(lFromRoomID
                                    + "," + lGameRoomID
                                    + "," + stGameRoomInfo.getLGameHandID()
                                    + "," + uuid
                                    + "," + "call"
                                    + "," + betstacks);

                            //设置玩家下注筹码量
                            setSimUserBetStacks(uuid,betstacks);

                            //������������������������
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CALL);
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            stUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                            //������������������
                            stUserGameInfoBuilder.setLBetStacks(betstacks + stUserGameInfoBuilder.getLBetStacks());
                            //������������������
                            stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - betstacks);


                            //������������gameroominfo���������������������
                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);

                            stGameRoomInfo = stGameRoomInfoBuilder.build();
//                            try {
//                                StuckGameLogService.getInstance().error(lGameRoomID + stGameRoomInfo.getLGameHandID() +" 游戏状态 : " + stGameRoomInfoBuilder.getEGameState()+ " 玩家 : "+ (stUserGameInfoBuilder.getStUserInfo().getUuid() !=0?stUserGameInfoBuilder.getStUserInfo().getUuid():0)+" mEndPos : "+mEndPos +" iCouldPlayNum : "+iCouldPlayNum + " mBigBlind : "+mBigBlindPos + " mSmallBlind : "+mSmallBlindPos + "bAllAllin : "+checkAllAllin());
//                            }catch (Exception e){
//                                e.printStackTrace();
//                            }
                            //���������������allin������
                            if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River) {
                                //���������������allin������
                                if (checkAllAllin() == true) {
                                    sendAllGameStateChange(uuid, null);
                                    stGameRoomInfoBuilder.setBAllAllin(1);
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                                    SidePot();
                                    bAllAllin = true;
                                    stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                    lStepMaxStacks = 0;

                                    //������������������������
                                    for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                                UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                                stUserBaseInfo.setUuid(0);
                                                stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                            }
                                        }

                                        //������������������������������������
                                        stResetUserGameInfoBuilder.setLBetStacks(0);
                                        stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                                        //������������gameroominfo���������������������
                                        stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);

                                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                                    }
                                    break;
                                }
                            }

                            victoryUser = iGameOver();
                            if (victoryUser != -1) {
                                //������������������push������������
                                sendAllGameStateChange(uuid, null);

                                flodWinProcess();

                                sendAllGameStateChange(uuid, null);
                                break;
                            }
                        } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() != 0) {


                            //������������������������,������������������������������������������������������������������������������������������������������������������������
                            if (userIndex == mEndPos) {
                                //������������������������������������
                                sendAllGameStateChange(uuid, null);
                                SidePot();
                                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                                if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River) {

                                    //���������������allin������
                                    if (checkAllAllin() == true) {
                                        bAllAllin = true;
                                        stGameRoomInfoBuilder.setBAllAllin(1);
                                    }


                                    if (iCouldPlayNum == 2) {
                                        mEndPos = mBigBlindPos;
                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                        //reset������
                                        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                if (stResetUserGameInfoBuilder.getLRemainStacks() > 0) {
                                                    if (mBigBlindPos == k) {
                                                        //���������������������������
                                                        if (bAllAllin == false) {
                                                            stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                                            stResetUserGameInfoBuilder.setIDelayLong(0);
                                                            stResetUserGameInfoBuilder.setIDelaySingleTime(20);
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                                            stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                                            //如果在依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                                            if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                                                stResetUserGameInfoBuilder.clearVCurrentHands();
                                                                stResetUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                                            }

                                                        } else {
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                        }

                                                        removeAllTimeOutUser();
                                                        setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                                    } else {
                                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                                                    }
                                                } else {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }

                                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                                    UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                                    stUserBaseInfo.setUuid(0);
                                                    stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                                }
                                            }

                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                                            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                        }
                                    } else {
                                        mEndPos = mSmallBlindPos;
                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                        int iBetting = 0;
                                        int iLoop1 = 0;

                                        //���������������������������������������
                                        for (int k = mSmallBlindPos; iLoop1 < iGameRoomUserMaxNums; iLoop1++, k++) {
                                            int userIndex2 = 0;
                                            if (k < iGameRoomUserMaxNums) {
                                                userIndex2 = k;
                                            } else {
                                                userIndex2 = k - iGameRoomUserMaxNums;
                                            }

                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex2).toBuilder();
                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                if (stResetUserGameInfoBuilder.getLRemainStacks() > 0) {
                                                    if (iBetting == 0) {
                                                        //���������������������������
                                                        if (bAllAllin == false) {
                                                            stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                                            stResetUserGameInfoBuilder.setIDelayLong(0);
                                                            stResetUserGameInfoBuilder.setIDelaySingleTime(20);
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                                            stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                                            //如果在依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                                            if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                                                stResetUserGameInfoBuilder.clearVCurrentHands();
                                                                stResetUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                                            }

                                                        } else {
                                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                        }

                                                        removeAllTimeOutUser();
                                                        setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                                        iBetting = 1;
                                                    } else {
                                                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                                                    }
                                                } else {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }

                                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                                    UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                                    stUserBaseInfo.setUuid(0);
                                                    stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                                }
                                            }

                                            //������������������������������������
                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                            //������������gameroominfo���������������������
                                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
                                        }
                                    }
                                }

                                stGameRoomInfo = stGameRoomInfoBuilder.build();
                                //������������������
                                switch (stGameRoomInfoBuilder.getEGameState().getNumber()) {
                                    case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//									TableLogService.getInstance().error1(lFromRoomID
//											+"," + lGameRoomID
//											+"," + stGameRoomInfo.getLGameHandID()
//											+",gotoFlopAllStatus enter4" + stGameRoomInfo.getEGameState());
                                        gotoFlopAllStatus(uuid);
                                    }
                                    break;
                                    case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//									TableLogService.getInstance().error1(lFromRoomID
//											+"," + lGameRoomID
//											+"," + stGameRoomInfo.getLGameHandID()
//											+",checkGotoTurnStatus enter3" + stGameRoomInfo.getEGameState());

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
                            } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                            ) {
                                //���������������������������������������������������������������������
                                if (stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin == false) {
                                    stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                    stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                    stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                    stUserGameInfoBuilder.setIDelayLong(0);
                                    stUserGameInfoBuilder.setIDelaySingleTime(20);

                                    //如果在依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                        if (mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()) != null){
                                            stUserGameInfoBuilder.clearVCurrentHands();
                                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                        }
                                    }

                                    removeAllTimeOutUser();
                                    setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                                    //������������gameroominfo���������������������
                                    stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    sendAllGameStateChange(uuid, null);
                                    break;
                                }
                            }
                        }
                    }


                } else {
                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }catch (Exception e){
                e.printStackTrace();
                try {
                    StuckGameLogService.getInstance().error("uuid - " + uuid+"doCallAction " +e.toString());
                    Iterator<StackTraceElement> iterator = Arrays.asList(e.getStackTrace()).iterator();
                    while(iterator.hasNext()){
                        StuckGameLogService.getInstance().error(iterator.next().toString());
                    }
                    GameRoomInfo.Builder gameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    StuckGameLogService.getInstance().error("doCallAction "+ gameRoomInfoBuilder.toString());
                    StuckGameLogService.getInstance().error("doCallAction "+
                            gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                            +"=" + gameRoomInfoBuilder.getLGameHandID()
                            +"=" + mEndPos
                            +"="+mBigBlindPos
                            +"="+mSmallBlindPos
                            +"=" + bAllAllin
                            +"=" + stGameRoomInfo.getEGameState()
                            +"=" + stageUpdateTime
                            +"="+ iCouldPlayNum);

                    for(int i=0;i <iGameRoomUserMaxNums;i++){
                        UserGameInfoNet vUserGameInfos = gameRoomInfoBuilder.getVUserGameInfos(i);
                        if(vUserGameInfos!=null){
                            StuckGameLogService.getInstance().error(
                                    "doCallAction "
                                            + (vUserGameInfos.getStUserInfo()==null?"null":vUserGameInfos.getStUserInfo().getUuid())
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())== null ? false:true)
                                            +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())==null?"null":(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid()).getvCards()))
                                            +"=" + vUserGameInfos.getEGameState()
                                            +"=" + vUserGameInfos.getEGameRole()
                                            +"=" + vUserGameInfos.getBGameDealer()
                                            +"=" + vUserGameInfos.getBDelegate()
                                            +"=" + vUserGameInfos.getBJustSit()
                                            +"=" + vUserGameInfos.getLBetStacks()
                                            +"="+ vUserGameInfos.getIDelaySingleTime());
                        }
                    }
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }

        }
        return eErrorCode;
    }

    /**
     * 弃牌公共函数
     *
     * @param uuid       弃牌的用户
     * @param userPos    用户所在位置
     * @param lBettingID 手牌动作ID
     * @return
     */
    @Override
    public ERROR_CODE_TYPE doFlodAction(long uuid, int userPos, long lBettingID, boolean bDeleggate) {
        ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;

//		TableLogService.getInstance().error1(lFromRoomID
//				+"," + lGameRoomID
//				+"," + stGameRoomInfo.getLGameHandID()
//				+",doFlodAction begin" + stGameRoomInfo.getEGameState());

        if (bPause == true) {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
        }

        synchronized (this) {
            try {
                GameRoomInfo.Builder st = stGameRoomInfo.toBuilder();
                st.setBStateChange(0);
                stGameRoomInfo = st.build();

                if (checkGameActionValidation() == false) {
                    return eErrorCode;
                }
                //初始化变量
                int victoryUser = -1;

                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                //初始化循环变量
                int iLoop = 0;
                //遍历所有打牌的人
                for (int j = userPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                    int userIndex = 0;
                    if (j < iGameRoomUserMaxNums) {
                        userIndex = j;
                    } else {
                        userIndex = j - iGameRoomUserMaxNums;
                    }

                    //获取打牌的人的信息
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();

                    if (userIndex == userPos) {
                        //如果是弃牌的用户

                        //首先检查是否是弃牌状态，bettingID是否ok
                        if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_BETTING
                                || (lBettingID != -1 && lBettingID != stUserGameInfoBuilder.getLBettingID() && lBettingID != -2)) {
                            eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                            break;
                        }

                        //不是同时行动日志正常打印
                        //stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBSimultAction() == 0
                        if (stGameRoomInfo.getStGameRoomBaseInfo().getBSimultAction() == 0){
                            if (lBettingID == -1) {
                                //自动弃牌日志
                                if (bDeleggate) {
                                    TableLogService.getInstance().error(lFromRoomID
                                            + "," + lGameRoomID
                                            + "," + stGameRoomInfo.getLGameHandID()
                                            + "," + uuid
                                            + "," + "fold"
                                            + "," + "tuoguan");

                                    //牌局ID，玩家ID，手数ID，按钮编号，点击位置
                                    ButtonLogService.getInstance().error(lGameRoomID //牌局ID
                                            + " " + uuid //玩家ID
                                            + " " + stGameRoomInfo.getLGameHandID() //手数ID
                                            + " " + "8" //按钮编号 预弃牌（自动弃牌）
                                            + " " + "0"); //点击位置

                                } else {
                                    TableLogService.getInstance().error(lFromRoomID
                                            + "," + lGameRoomID
                                            + "," + stGameRoomInfo.getLGameHandID()
                                            + "," + uuid
                                            + "," + "fold"
                                            + "," + "auto");

                                    //牌局ID，玩家ID，手数ID，按钮编号，点击位置
                                    ButtonLogService.getInstance().error(lGameRoomID //牌局ID
                                            + " " + uuid //玩家ID
                                            + " " + stGameRoomInfo.getLGameHandID() //手数ID
                                            + " " + "62" //按钮编号 系统弃牌
                                            + " " + "0"); //点击位置
                                }

                            } else {
                                //主动弃牌日志
                                TableLogService.getInstance().error(lFromRoomID
                                        + "," + lGameRoomID
                                        + "," + stGameRoomInfo.getLGameHandID()
                                        + "," + uuid
                                        + "," + "fold"
                                        + "," + 0);

                                if (lBettingID == -2){
                                    //牌局ID，玩家ID，手数ID，按钮编号，点击位置
                                    ButtonLogService.getInstance().error(lGameRoomID //牌局ID
                                            + " " + uuid //玩家ID
                                            + " " + stGameRoomInfo.getLGameHandID() //手数ID
                                            + " " + "62" //按钮编号 系统弃牌
                                            + " " + "0"); //点击位置
                                }

//                            //牌局ID，玩家ID，手数ID，按钮编号，点击位置
//                            ButtonLogService.getInstance().error(lGameRoomID //牌局ID
//                                    + " " + stUserGameInfoBuilder.getStUserInfo().getUuid() //玩家ID
//                                    + " " + stGameRoomInfo.getLGameHandID() //手数ID
//                                    + " " + "2" //按钮编号 主动弃牌
//                                    + " " + "0"); //点击位置
                            }
                        } else {
                            //同时行动由于保险原因暂时先做记录
                            if (lBettingID == -1) {
                                //自动弃牌日志
                                if (bDeleggate) {
                                    mUserFlodRecode.put(uuid,"tuoguan");
                                } else {
                                    mUserFlodRecode.put(uuid,"auto");
                                }
                            } else {
                                //主动弃牌日志
                                mUserFlodRecode.put(uuid,"0");
                            }
                        }


                        //设置弃牌状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_FOLD);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);

                        //设置用户的牌局状态
                        stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        //同时行动
                        if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBSimultAction() == 1){

                            //检测牌局是否结束
                            victoryUser = iGameOver();
                            if (victoryUser != -1) {

                                //未弃牌玩家
                                long userUuid = checkNotFlodUser();

                                boolean bool = false; //是否可以直接结束牌局
                                boolean bStraddle = false; //是否有straddle玩家

                                //大盲或straddle获胜
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBStraddle() && stGameRoomInfo.getStGameRoomBaseInfo().getBStraddle() == 1 && iCouldPlayNum > 3){
                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() == userUuid && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_STRADDLE) {
                                            bool = true;
                                            break;
                                        }
                                    }

                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() > 0 && stUserGameInfo.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_STRADDLE) {
                                            bStraddle = true;
                                            break;
                                        }
                                    }

                                    if (!bStraddle){
                                        //没有抓大盲赢
                                        for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                            //获取所有牌局上的用户
                                            UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                            if (stUserGameInfo.getStUserInfo().getUuid() == userUuid && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND) {
                                                bool = true;
                                                break;
                                            }
                                        }
                                    }

                                } else {
                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() == userUuid && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND) {
                                            bool = true;
                                            break;
                                        }
                                    }
                                }

                                //大盲或抓获胜
                                if (bool && mSimUserAllin.get(userUuid) == null){
                                    //如果牌局已，则进入牌局输赢结算状态
                                    sendAllGameStateChange(uuid, null);

                                    //打印flod日志
                                    for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                        TableLogService.getInstance().error(lFromRoomID
                                                + "," + lGameRoomID
                                                + "," + stGameRoomInfo.getLGameHandID()
                                                + "," + map.getKey()
                                                + "," + "fold"
                                                + "," + map.getValue());
                                    }

                                    flodWinProcess();

                                    sendAllGameStateChange(uuid, null);
                                    break;
                                }
                            }

                            //大家都弃牌那就大盲或抓赢
                            if (checkIsFoldAll()){
                                //大盲或straddle获胜
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBStraddle() && stGameRoomInfo.getStGameRoomBaseInfo().getBStraddle() == 1 && iCouldPlayNum > 3){
                                    GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() > 0 && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_STRADDLE) {
                                            stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                            stGameRoom.setVUserGameInfos(k,stUserGameInfo);
                                            break;
                                        }
                                    }

                                    boolean bStraddle = false; //是否有straddle玩家
                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() > 0 && stUserGameInfo.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_STRADDLE) {
                                            bStraddle = true;
                                            break;
                                        }
                                    }

                                    if (!bStraddle){
                                        for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                            //获取所有牌局上的用户
                                            UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                            if (stUserGameInfo.getStUserInfo().getUuid() > 0 && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND) {
                                                stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                                stGameRoom.setVUserGameInfos(k,stUserGameInfo);
                                                break;
                                            }
                                        }
                                    }

                                    stGameRoomInfo = stGameRoom.build();
                                } else {
                                    GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                    for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                        //获取所有牌局上的用户
                                        UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                        if (stUserGameInfo.getStUserInfo().getUuid() > 0 && stUserGameInfo.getEGameRole() == USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND) {
                                            stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                            stGameRoom.setVUserGameInfos(k,stUserGameInfo);
                                            break;
                                        }
                                    }
                                    stGameRoomInfo = stGameRoom.build();
                                }

                                sendAllGameStateChange(uuid, null);

                                //打印flod日志
                                for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                    TableLogService.getInstance().error(lFromRoomID
                                            + "," + lGameRoomID
                                            + "," + stGameRoomInfo.getLGameHandID()
                                            + "," + map.getKey()
                                            + "," + "fold"
                                            + "," + map.getValue());
                                }

                                flodWinProcess();

                                sendAllGameStateChange(uuid, null);
                                break;
                            }

                            //检测是否还有betting玩家
                            if (!checkIsBetting()){
                                if (checkAllInNum() == 1){

                                    //只有一个allin玩家
                                    List<Long> uuidList = getAllInUuid();
                                    long userid = uuidList.get(0);

                                    //1.检测当前allin玩家是否为被迫allin
                                    //a.如果为被迫allin获取场上最后行动玩家进行比牌
                                    //b.如果玩家正常allin则玩家直接获胜
                                    long lastActionUuid = getLastActionUuid();

                                    //如果本身是最后一个玩家直接获胜
                                    if (userid == lastActionUuid){
                                        sendAllGameStateChange(uuid, null);

                                        //打印flod日志
                                        for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                            TableLogService.getInstance().error(lFromRoomID
                                                    + "," + lGameRoomID
                                                    + "," + stGameRoomInfo.getLGameHandID()
                                                    + "," + map.getKey()
                                                    + "," + "fold"
                                                    + "," + map.getValue());
                                        }

                                        flodWinProcess();
                                        sendAllGameStateChange(uuid, null);
                                        break;

                                    } else {

                                        long otherUuid = 0l;

                                        //和最后一个玩家进行比牌流程
                                        GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                        for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                            //获取所有牌局上的用户
                                            UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                            if (stUserGameInfo.getStUserInfo().getUuid() == lastActionUuid) {
                                                stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                                stGameRoom.setVUserGameInfos(k,stUserGameInfo);

                                                TableLogService.getInstance().error(lFromRoomID
                                                        + "," + lGameRoomID
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + lastActionUuid
                                                        + "," + "check"
                                                        + "," + 0);

                                                otherUuid = lastActionUuid;

                                                break;
                                            }
                                        }
                                        stGameRoomInfo = stGameRoom.build();

                                        sendAllGameStateChange(uuid, null);

                                        //打印flod日志
                                        for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                            if (map.getKey() != otherUuid){
                                                TableLogService.getInstance().error(lFromRoomID
                                                        + "," + lGameRoomID
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + map.getKey()
                                                        + "," + "fold"
                                                        + "," + map.getValue());
                                            }
                                        }

                                        bAllAllin = true;

                                        SidePot();

                                        stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                        stGameRoomInfoBuilder.setBAllAllin(1);

                                        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                    && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                                if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }
                                            }

                                            //������������������������������������
                                            stResetUserGameInfoBuilder.setLBetStacks(0);
                                            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                            //������������gameroominfo���������������������
                                            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                        }

                                        lStepMaxStacks = 0;
                                        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                                        gotoFlopAllStatus(uuid);
                                        break;
                                    }

                                } else if (checkAllInNum() > 1){

                                    //allin玩家有多个时
                                    //a.allin玩家中存在没有被迫allin的直接走比牌结算
                                    //b.allin玩家中都是被迫allin的需要和最后行动玩家比牌
                                    //最后行动玩家
                                    long lastActionUuid = getLastActionUuid();
                                    Boolean bool = false;

                                    long otherUuid = 0l;

                                    //如果最后行动的玩家在allin玩家内直接比牌
                                    List<Long> uuidList = getAllInUuid();
                                    for (Long userid : uuidList){
                                        if (lastActionUuid == userid){
                                            bool = true;
                                        }
                                    }

                                    //最后行动的玩家在allin玩家里直接比牌
                                    if (!bool){
                                        //最后行动的玩家不在allin内最后行动玩家需要参与比牌
                                        GameRoomInfo.Builder stGameRoom = stGameRoomInfo.toBuilder();
                                        for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                            //获取所有牌局上的用户
                                            UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                                            if (stUserGameInfo.getStUserInfo().getUuid() == lastActionUuid) {
                                                stUserGameInfo.setEGameState(USER_GAME_STATE.USER_GAME_STATE_CHECK);
                                                stGameRoom.setVUserGameInfos(k,stUserGameInfo);

                                                TableLogService.getInstance().error(lFromRoomID
                                                        + "," + lGameRoomID
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + lastActionUuid
                                                        + "," + "check"
                                                        + "," + 0);

                                                otherUuid = lastActionUuid;

                                                break;
                                            }
                                        }
                                        stGameRoomInfo = stGameRoom.build();
                                    }

                                    //打印flod日志
                                    for (Map.Entry<Long,String> map : mUserFlodRecode.entrySet()){
                                        if (map.getKey() != otherUuid){
                                            TableLogService.getInstance().error(lFromRoomID
                                                    + "," + lGameRoomID
                                                    + "," + stGameRoomInfo.getLGameHandID()
                                                    + "," + map.getKey()
                                                    + "," + "fold"
                                                    + "," + map.getValue());
                                        }
                                    }

                                    sendAllGameStateChange(uuid, null);
                                    bAllAllin = true;

                                    SidePot();

                                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                    stGameRoomInfoBuilder.setBAllAllin(1);

                                    for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                            if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                            }
                                        }

                                        //������������������������������������
                                        stResetUserGameInfoBuilder.setLBetStacks(0);
                                        stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                        //������������gameroominfo���������������������
                                        stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                    }

                                    lStepMaxStacks = 0;
                                    stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    gotoFlopAllStatus(uuid);

                                    break;

                                } else {
                                    //所有玩家都弃牌了,上面已经处理了
                                }
                            }

                            break;
                        }

                        //检查牌局是否结束
                        victoryUser = iGameOver();
                        if (victoryUser != -1) {
                            //如果牌局已，则进入牌局输赢结算状态
                            sendAllGameStateChange(uuid, null);

                            flodWinProcess();

                            sendAllGameStateChange(uuid, null);
                            break;
                        }
                    } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() != 0) {
                        if (userIndex == mEndPos) {
                            //如果下一个位置就是结束的位置，说明本次弃牌是本轮的最后一个动作
                            //此时需要变换牌局的状态
                            sendAllGameStateChange(uuid, null);

                            SidePot();
                            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                            if (checkAllAllin() == true) {
                                stGameRoomInfoBuilder.setBAllAllin(1);
                                bAllAllin = true;
                            }

                            if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River) {
                                mEndPos = mSmallBlindPos;
                                lStepMaxStacks = 0;
                                stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                int iBetting = 0;
                                int iLoop1 = 0;

                                //重置牌局状态
                                for (int k = mSmallBlindPos; iLoop1 < iGameRoomUserMaxNums; iLoop1++, k++) {
                                    int userIndex2 = 0;
                                    if (k < iGameRoomUserMaxNums) {
                                        userIndex2 = k;
                                    } else {
                                        userIndex2 = k - iGameRoomUserMaxNums;
                                    }

                                    UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex2).toBuilder();

                                    if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                            && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                            && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_READY
                                            && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                        if (stResetUserGameInfoBuilder.getLRemainStacks() > 0) {
                                            //找到小盲后第一个可以出牌的用户
                                            if (iBetting == 0) {
                                                if (bAllAllin == false) {
                                                    stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                                    stResetUserGameInfoBuilder.setIDelayLong(0);
                                                    stResetUserGameInfoBuilder.setIDelaySingleTime(20);
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                                    stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                                    //如果在AOF模式下依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                                    if ((stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1) || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1)){
                                                        stResetUserGameInfoBuilder.clearVCurrentHands();
                                                        stResetUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                                    }

                                                    //设置该用户的超时
                                                    removeAllTimeOutUser();
                                                    setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());

                                                    iBetting = 1;
                                                } else {
                                                    stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                                }
                                            } else {
                                                //其他用户设置为Ready状态
                                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                                            }
                                        } else {
                                            //没有筹码了，则为allin的状态
                                            stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                        }

                                        //如果该用户离开或者站起了，则重新设置该位置的用户信息
                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                                            UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                                            stUserBaseInfo.setUuid(0);
                                            stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                                        }
                                    }

                                    //设置已下筹码为0
                                    stResetUserGameInfoBuilder.setLBetStacks(0);
                                    stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                    //重置用户牌局信息
                                    stGameRoomInfoBuilder.setVUserGameInfos(userIndex2, stResetUserGameInfoBuilder);
                                }
                            }

                            stGameRoomInfo = stGameRoomInfoBuilder.build();
                            //发牌相关操作
                            switch (stGameRoomInfoBuilder.getEGameState().getNumber()) {
                                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//								TableLogService.getInstance().error1(lFromRoomID
//										+"," + lGameRoomID
//										+"," + stGameRoomInfo.getLGameHandID()
//										+",gotoFlopAllStatus enter5" + stGameRoomInfo.getEGameState());
                                    //Flop发牌操作
                                    gotoFlopAllStatus(uuid);
                                }
                                break;
                                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//								TableLogService.getInstance().error1(lFromRoomID
//								+"," + lGameRoomID
//								+"," + stGameRoomInfo.getLGameHandID()
//								+",checkGotoTurnStatus enter4" + stGameRoomInfo.getEGameState());

                                    //发turn一张牌
                                    checkGotoTurnStatus(uuid);
                                }
                                break;
                                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE: {
                                    //发river牌
                                    //保护，发这张牌时，排面上只能有3张牌
                                    checkGotoRiverStatus(uuid);
                                }
                                break;
                                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE: {
                                    gotoResultStatus(uuid);
                                }
                                break;
                            }
                            break;
                        } else if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                            if (checkAllAllin() == true) {
                                stGameRoomInfoBuilder.setBAllAllin(1);
                            }

                            //如果用户不是最后一个出牌的用户
                            if (stUserGameInfoBuilder.getLRemainStacks() > 0 && bAllAllin == false) {
                                //设置该用户为下一个需要行动的用户
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stUserGameInfoBuilder.setIDelayLong(0);
                                stUserGameInfoBuilder.setIDelaySingleTime(20);

                                //如果在AOF模式下依次看牌且游戏状态为ROOM_GAME_STATE_PreFlop_VALUE时给玩家下发手牌
                                if ((stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1) || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1)){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }

                                //重置牌局用户超时
                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                                //设置状态
                                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder);
                                stGameRoomInfo = stGameRoomInfoBuilder.build();

                                //下发状态变化
                                sendAllGameStateChange(uuid, null);
                                break;
                            } else {

                                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                if (bAllAllin == true) {
                                    sendAllGameStateChange(uuid, null);

                                    SidePot();

                                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                    stGameRoomInfoBuilder.setBAllAllin(1);

                                    for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                && stResetUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {

                                            if (bAllAllin == true || stResetUserGameInfoBuilder.getLRemainStacks() == 0) {
                                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                                            }
                                        }

                                        //������������������������������������
                                        stResetUserGameInfoBuilder.setLBetStacks(0);
                                        stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                                        //������������gameroominfo���������������������
                                        stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                    }

                                    lStepMaxStacks = 0;
                                    stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                                    //������������
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                                    //������������������
                                    switch (stGameRoomInfoBuilder.getEGameState().getNumber()) {
                                        case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
//										TableLogService.getInstance().error1(lFromRoomID
//												+"," + lGameRoomID
//												+"," + stGameRoomInfo.getLGameHandID()
//												+",gotoFlopAllStatus enter6" + stGameRoomInfo.getEGameState());
                                            gotoFlopAllStatus(uuid);
                                        }
                                        break;
                                        case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//										TableLogService.getInstance().error1(lFromRoomID
//												+"," + lGameRoomID
//												+"," + stGameRoomInfo.getLGameHandID()
//												+",checkGotoTurnStatus enter5" + stGameRoomInfo.getEGameState());

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
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            try {
                StuckGameLogService.getInstance().error("doFoldAction : "+e.toString());
                Iterator<StackTraceElement> iterator = Arrays.asList(e.getStackTrace()).iterator();
                while(iterator.hasNext()){
                    StuckGameLogService.getInstance().error(iterator.next().toString());
                }
                GameRoomInfo.Builder gameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                StuckGameLogService.getInstance().error("doFoldAction "+ gameRoomInfoBuilder.toString());
                StuckGameLogService.getInstance().error("doFoldAction "+
                        gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                        +"=" + gameRoomInfoBuilder.getLGameHandID()
                        +"=" + mEndPos
                        +"="+mBigBlindPos
                        +"="+mSmallBlindPos
                        +"=" + bAllAllin
                        +"=" + stGameRoomInfo.getEGameState()
                        +"=" + stageUpdateTime
                        +"="+ iCouldPlayNum);

                for(int i=0;i <iGameRoomUserMaxNums;i++){
                    UserGameInfoNet vUserGameInfos = gameRoomInfoBuilder.getVUserGameInfos(i);
                    if(vUserGameInfos!=null){
                        StuckGameLogService.getInstance().error(
                                "doFoldAction "
                                        + (vUserGameInfos.getStUserInfo()==null?"null":vUserGameInfos.getStUserInfo().getUuid())
                                        +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())== null ? false:true)
                                        +"="+(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid())==null?"null":(mHandMap.get(vUserGameInfos.getStUserInfo().getUuid()).getvCards()))
                                        +"=" + vUserGameInfos.getEGameState()
                                        +"=" + vUserGameInfos.getEGameRole()
                                        +"=" + vUserGameInfos.getBGameDealer()
                                        +"=" + vUserGameInfos.getBDelegate()
                                        +"=" + vUserGameInfos.getBJustSit()
                                        +"=" + vUserGameInfos.getLBetStacks()
                                        +"="+ vUserGameInfos.getIDelaySingleTime());
                    }

                }
            }catch (Exception e2){
                e2.printStackTrace();
            }

            }
        }
        return eErrorCode;
    }

    @Override
    public void calculateWinner() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        List<GameUserResultInfo> cardStrengths = new ArrayList<GameUserResultInfo>();

        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

            Long uuid = mHandPos.get(i);
            if (uuid != null && uuid > 0) {
                if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CALL
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CHECK
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_RAISE
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND
                        || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY) {
                    HandInfo stHandInfo = this.mHandMap.get(uuid);
                    if (stHandInfo != null && stHandInfo.getvCards() != null && stHandInfo.getvCards().size() == 2) {
                        GameUserResultInfo stGameUserResultInfo = calculateCardStrength(stHandInfo.getvCards());
                        cardStrengths.add(stGameUserResultInfo);
                    } else {
                        GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
                        cardStrengths.add(stGameUserResultInfo);
                    }
                } else {
                    GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
                    cardStrengths.add(stGameUserResultInfo);
                }
            } else {
                GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
                cardStrengths.add(stGameUserResultInfo);
            }
        }

        GameResultInfo.Builder stGameResultInfoBuilder = GameResultInfo.newBuilder();
        long lSingleMaxPot = 0;

        //统计抽水的池子(所有池子都抽水除了玩家自己贡献的池子)
        Map<Integer,Long> drawMap = new HashMap<Integer,Long>();
        for (int index = 0; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++){
            List<GameUserResultInfo> winnerArray = new ArrayList<GameUserResultInfo>();
            Set<Integer> winnerIDSet = new HashSet<Integer>();
            Set<Long> winnerUuidSet = new HashSet<Long>();

            List<Integer> winnerIDArray = new ArrayList<Integer>(winnerIDSet);
            List<Long> winnerUuidArray = new ArrayList<Long>(winnerUuidSet);

            PotInfo stPotInfo = mPotList.get(index);
            List<Integer> posArray = stPotInfo.playerListpos;

            int maxStrength = 0;
            for (int i = 0; i < posArray.size(); i++) {
                int pos = posArray.get(i);
                long winuuid = stPotInfo.playerList.get(i);
                GameUserResultInfo stGameUserResultInfo = cardStrengths.get(pos);

                if (stGameUserResultInfo.mCardStrength == maxStrength) {

                    if (winnerArray.size() > 0) {
                        GameUserResultInfo stMaxGameUserResult = winnerArray.get(winnerArray.size() - 1);
                        int bMore = CompareSameLevelUser(stGameUserResultInfo, stMaxGameUserResult);
                        if (bMore > 0) {
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
                        } else if (bMore == 0) {
                            if (!winnerUuidSet.contains(winuuid)) {
                                winnerArray.add(stGameUserResultInfo);

                                winnerIDSet.add(pos);
                                winnerIDArray.add(pos);
                                winnerUuidSet.add(winuuid);
                                winnerUuidArray.add(winuuid);
                            }
                        }
                    } else {
                        if (!winnerUuidSet.contains(winuuid)) {
                            winnerArray.add(stGameUserResultInfo);

                            winnerIDSet.add(pos);
                            winnerIDArray.add(pos);
                            winnerUuidSet.add(winuuid);
                            winnerUuidArray.add(winuuid);
                        }
                    }
                } else if (stGameUserResultInfo.mCardStrength > maxStrength) {
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


            // 记录本手所有池子信息 roomid handid pot
            StringBuilder potLog = new StringBuilder("")
                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //roomid
                    .append(" ").append(stGameRoomInfo.getLGameHandID()) //handid
                    .append(" ").append(index) //池子编号
                    .append(" ").append(stGameRoomInfoBuilder.getVCurrentPots(index)) //未抽水时底池大小
                    .append(" ").append(winnerUuidArray) //该底池获胜玩家
                    .append(" ").append(stPotInfo.potSize) //底池大小
                    .append(" ").append(stPotInfo.allinSize) //allin人数
                    .append(" ").append(stPotInfo.playerList) //改底池对应所有玩家
                    .append(" ").append(stPotInfo.playerNotFoldList); //改底池对应没有弃牌的玩家
            PotLogService.getInstance().error(potLog.toString());

            //若是玩家自己贡献一个池子则不抽水
            if (stPotInfo.playerList.size() > 1 && winnerUuidArray.size() <= 1){
                drawMap.put(index,stGameRoomInfoBuilder.getVCurrentPots(index));
            }
        }

        //进行底池抽水 条件 底池抽水,发了Flop3张牌
        Map<Integer,Long> userDrawStacks = new HashMap<Integer,Long>();
        if (drawMap != null && drawMap.size() > 0 && bCredit && iDrawType == 2 && bFlopStatus){//授信局抽水
            userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                    stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
        } else if (drawMap != null && drawMap.size() > 0 && !bCredit && iDrawType == 2 && bFlopStatus){//快速局抽水
            if (stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.QUICK_GAME_ROOM){
                userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
            } else {//普通联盟 普通俱乐部抽水
                userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
            }

        }



        for (int index = 0; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++) {

            List<GameUserResultInfo> winnerArray = new ArrayList<GameUserResultInfo>();
            Set<Integer> winnerIDSet = new HashSet<Integer>();
            Set<Long> winnerUuidSet = new HashSet<Long>();

            List<Integer> winnerIDArray = new ArrayList<Integer>(winnerIDSet);
            List<Long> winnerUuidArray = new ArrayList<Long>(winnerUuidSet);

            PotInfo stPotInfo = mPotList.get(index);
            List<Integer> posArray = stPotInfo.playerListpos;

            int maxStrength = 0;
            for (int i = 0; i < posArray.size(); i++) {
                int pos = posArray.get(i);
                long winuuid = stPotInfo.playerList.get(i);
                GameUserResultInfo stGameUserResultInfo = cardStrengths.get(pos);

                if (stGameUserResultInfo.mCardStrength == maxStrength) {

                    if (winnerArray.size() > 0) {
                        GameUserResultInfo stMaxGameUserResult = winnerArray.get(winnerArray.size() - 1);
                        int bMore = CompareSameLevelUser(stGameUserResultInfo, stMaxGameUserResult);
                        if (bMore > 0) {
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
                        } else if (bMore == 0) {
                            if (!winnerUuidSet.contains(winuuid)) {
                                winnerArray.add(stGameUserResultInfo);

                                winnerIDSet.add(pos);
                                winnerIDArray.add(pos);
                                winnerUuidSet.add(winuuid);
                                winnerUuidArray.add(winuuid);
                            }
                        }
                    } else {
                        if (!winnerUuidSet.contains(winuuid)) {
                            winnerArray.add(stGameUserResultInfo);

                            winnerIDSet.add(pos);
                            winnerIDArray.add(pos);
                            winnerUuidSet.add(winuuid);
                            winnerUuidArray.add(winuuid);
                        }
                    }
                } else if (stGameUserResultInfo.mCardStrength > maxStrength) {
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

            //本池子未获胜玩家
            List<Long> failUuidArray = new ArrayList<Long>();
            for (Long uuid : stPotInfo.playerList){
                if (!failUuidArray.contains(uuid) && !winnerUuidArray.contains(uuid)){
                    failUuidArray.add(uuid);
                }
            }

            if (winnerIDArray.size() != 0) {
                GamePotResultInfo.Builder stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();

                long winStacks = 0l;
                long winLeftStacks = 0l;
                //授信牌局和金币局 底池抽水且池子有抽水
                if (iDrawType == 2 && bFlopStatus && stPotInfo.playerList.size() > 1 && winnerUuidArray.size() <= 1){
                    winStacks = (stGameRoomInfo.getVCurrentPots(index) - userDrawStacks.get(index)) / winnerIDArray.size();
                    winLeftStacks = (stGameRoomInfo.getVCurrentPots(index) - userDrawStacks.get(index)) % winnerIDArray.size();

                    //记录玩家原始贡献抽水
                    int j = 1; //记录玩家数
                    long drawOriginStacks = userDrawStacks.get(index) / winnerIDArray.size(); //记录玩家原始贡献抽水值
                    long drawOriginLeftStacks = userDrawStacks.get(index) % winnerIDArray.size(); //记录剩余玩家原始贡献抽水值

                    //获胜玩家原始贡献的抽水
                    for (Long winUuid : winnerUuidArray){
                        if (mUserOriginDrawStacks.get(winUuid) == null){
                            if (j == winnerUuidArray.size()){
                                mUserOriginDrawStacks.put(winUuid, drawOriginStacks + drawOriginLeftStacks);
                            } else {
                                mUserOriginDrawStacks.put(winUuid, drawOriginStacks);
                            }
                        } else {
                            if (j == winnerUuidArray.size()){
                                mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks + drawOriginLeftStacks);
                            } else {
                                mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks);
                            }
                        }

                        j++;
                    }

                    //记录玩家贡献的抽水
                    int i = 1; //记录玩家数
                    long drawStacks = userDrawStacks.get(index) / (winnerIDArray.size() + failUuidArray.size()); //记录玩家贡献抽水值
                    long drawLeftStacks = userDrawStacks.get(index) % (winnerIDArray.size() + failUuidArray.size()); //记录剩余玩家贡献抽水值

                    //获胜玩家贡献的抽水
                    for (Long winUuid : winnerUuidArray){
                        //记录抽水日志
                        StringBuilder drawLog = new StringBuilder("")
                                .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                .append(" ").append(index) //底池编号
                                .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                .append(" ").append(winnerIDArray.size()) //该底池获胜人数
                                .append(" ").append(winnerIDArray.size() + failUuidArray.size()) //该底池贡献抽水的所有玩家数
                                .append(" ").append(1) //玩家类型 1 获胜玩家 2 未获胜玩家
                                .append(" ").append(winUuid); //玩家uuid

                        if (mUserDrawStacks.get(winUuid) == null){
                            if (i == winnerUuidArray.size() + failUuidArray.size()){
                                mUserDrawStacks.put(winUuid, drawStacks + drawLeftStacks);
                                drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家在该底池贡献抽水
                            } else {
                                mUserDrawStacks.put(winUuid, drawStacks);
                                drawLog.append(" ").append(drawStacks); //玩家在该底池贡献抽水
                            }
                        } else {
                            if (i == winnerUuidArray.size() + failUuidArray.size()){
                                mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks + drawLeftStacks);
                                drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家在该底池贡献抽水
                            } else {
                                mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks);
                                drawLog.append(" ").append(drawStacks); //玩家在该底池贡献抽水
                            }
                        }

                        drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                            .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                            .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                            .append(" ").append(iUserNum); //实际参人数
                        PotDrawLogService.getInstance().error(drawLog.toString());

                        i++;
                    }

                    //未获胜玩家贡献的抽水
                    if (!CollectionUtils.isEmpty(failUuidArray)){
                        for (Long failUuid : failUuidArray){
                            //记录抽水日志
                            StringBuilder drawLog = new StringBuilder("")
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                    .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                    .append(" ").append(index) //底池编号
                                    .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                    .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                    .append(" ").append(winnerIDArray.size()) //该底池获胜人数
                                    .append(" ").append(winnerIDArray.size() + failUuidArray.size()) //该底池贡献抽水的所有玩家数
                                    .append(" ").append(2) //玩家类型 1 获胜玩家 2 未获胜玩家
                                    .append(" ").append(failUuid); //玩家uuid

                            if (mUserDrawStacks.get(failUuid) == null){
                                if (i == winnerUuidArray.size() + failUuidArray.size()){
                                    mUserDrawStacks.put(failUuid, drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家在该底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(failUuid, drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家在该底池贡献抽水
                                }
                            } else {
                                if (i == winnerUuidArray.size() + failUuidArray.size()){
                                    mUserDrawStacks.put(failUuid, mUserDrawStacks.get(failUuid) + drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家在该底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(failUuid, mUserDrawStacks.get(failUuid) + drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家在该底池贡献抽水
                                }
                            }

                            drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                                    .append(" ").append(iUserNum); //实际参人数
                            PotDrawLogService.getInstance().error(drawLog.toString());

                            i++;
                        }
                    }

                } else {
                    winStacks = stGameRoomInfo.getVCurrentPots(index) / winnerIDArray.size();
                    winLeftStacks = stGameRoomInfo.getVCurrentPots(index) % winnerIDArray.size();
                }

                lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);


                Map<Long, Integer> mPosPriority = new HashMap<Long, Integer>();
                int iArrangePosPriority = 1;
                for (int i = 0; i < vPosPriority.size(); i++) {
                    long uuid = vPosPriority.get(i);
                    if (winnerUuidArray.contains(uuid)) {
                        mPosPriority.put(uuid, iArrangePosPriority);
                        iArrangePosPriority++;
                    }
                }

                for (int i = 0; i < winnerIDArray.size(); i++) {
                    int userPos = winnerIDArray.get(i);
                    long winUuid = winnerUuidArray.get(i);

                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        if (j == userPos) {
                            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();
                            //查看用户的位置优先级数
                            int iPosPriority = mPosPriority.get(winUuid);
                            int iExtraWinStacks = 0;
                            //NormalLogService.getInstance().error("mtt calculateWinner gameroomid : 35");
                            //如果这个数据小于等于剩余可分筹码
                            if (iPosPriority <= winLeftStacks) {
                                //则获胜的筹码加1
                                iExtraWinStacks = 1;
                            }
                            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid() == winUuid) {
                                stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
                                stGamePotResultInfoBuilder.addLUserPots(winStacks + iExtraWinStacks);

                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() + winStacks + iExtraWinStacks);

                                stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());
                            } else {
                                UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
                                if (stUserStandByGameInfoBuilder != null) {
                                    stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
                                    stGamePotResultInfoBuilder.addLUserPots(winStacks + iExtraWinStacks);

                                    stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks() + winStacks + iExtraWinStacks);

                                    mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);
                                    setBillCurrentStacksInRoom(winUuid,
                                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            , stUserStandByGameInfoBuilder.getLRemainStacks(), stUserStandByGameInfoBuilder.getLUnloadStacks());
                                }

                                for (int chgangeIndex = 0; chgangeIndex < stGameRoomInfo.getVUserGameInfosCount(); chgangeIndex++) {

                                    UserGameInfoNet.Builder stUserGameInfoBuilderChange = stGameRoomInfoBuilder.getVUserGameInfos(chgangeIndex).toBuilder();

                                    if (stUserGameInfoBuilderChange != null && stUserGameInfoBuilderChange.getStUserInfo().getUuid() == winUuid) {
                                        stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
                                        stGamePotResultInfoBuilder.addLUserPots(winStacks + iExtraWinStacks);

                                        stUserGameInfoBuilderChange.setLRemainStacks(stUserGameInfoBuilderChange.getLRemainStacks() + winStacks + iExtraWinStacks);

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
        if (lSingleMaxPot > lMaxPot) {
            lMaxPot = lSingleMaxPot;
        }

        for (int i = 0; i < vOriginalStacks.size(); i++) {
            long stack = vOriginalStacks.get(i);
            stGameResultInfoBuilder.addVlOriginalUserStacks(stack);
        }

        stGameRoomInfoBuilder.setStGameResultInfo(stGameResultInfoBuilder);

        //计算统计数据
        Map<Long, Long> mapWinStacks = new ConcurrentHashMap<Long, Long>();
        for (GamePotResultInfo stGamePotResultInfo : stGameResultInfoBuilder.getStGamePotResultInfoList()) {
            for (int p = 0; p < stGamePotResultInfo.getStUserWinerCount(); p++) {
                long uuid = stGamePotResultInfo.getStUserWiner(p).getUuid();
                long winStacks = stGamePotResultInfo.getLUserPots(p);

                Long stacks = mapWinStacks.get(uuid);
                if (stacks == null) {
                    mapWinStacks.put(uuid, winStacks);
                } else {
                    mapWinStacks.put(uuid, stacks + winStacks);
                }
            }
        }

        long potStacks = 0l;

        //记录获胜玩家的uuid
        List<Long> winUuidList = new ArrayList<Long>();

        //打印日志
        Iterator iter = mapWinStacks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Long uuid = (Long) entry.getKey();
            Long winStacks = (Long) entry.getValue();
            potStacks = potStacks + winStacks;
            winUuidList.add(uuid);

            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                    + "," + stGameRoomInfo.getLGameHandID()
                    + "," + uuid
                    + "," + "showdown"
                    + "," + "show"
                    + "," + winStacks);

            stGameRoomInfo = stGameRoomInfoBuilder.build();
        }

        long diffStacks = 0l;

        //检测本手战绩平不平
        if (iDrawType != 2 && potStacks != getUserTotalBetStacks()){

            diffStacks = getUserTotalBetStacks() - potStacks;

            //本手战绩不平数据
            RecordLogService.getInstance().error("roomid: " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " handid: " + stGameRoomInfo.getLGameHandID() + " " + stGameRoomInfo);
        }

        try {
            for (long uuid : vHandList) {
                HandInfo stHandInfo = mHandMap.get(uuid);
                if (stHandInfo != null) {
                    TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + uuid
                            + "," + "hand"
                            + "," + stHandInfo.getvCards().get(0)
                            + "," + stHandInfo.getvCards().get(1)
                            + "," + stHandInfo.getlRemainStacks());
                }
            }
        } catch (Exception e) {
        }

        long diffUuid = 0l;

        //有差值说明需要修平战绩
        if (diffStacks != 0){
            if (!CollectionUtils.isEmpty(winUuidList) && winUuidList.size() == 1){
                diffUuid = winUuidList.get(0);
            }
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
        for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

            if (diffUuid != 0 && diffUuid == stResetUserGameInfoBuilder.getStUserInfo().getUuid()){
                stResetUserGameInfoBuilder.setLRemainStacks(stResetUserGameInfoBuilder.getLRemainStacks() + diffStacks);
            }

            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                stResetUserGameInfoBuilder.setLBetStacks(0);
            }
            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                stUserBaseInfo.setUuid(0);
                stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
            }

            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
        stGameRoomInfo = InsuranceApi.getInsuranceBill(stGameRoomInfo, stGameRoomInfo.getLGameHandID());
        ArrayList<Long> vSitdownUsers = new ArrayList<Long>();
        TexasPoker.GameRoomInfo.Builder gameBuilder = stGameRoomInfo.toBuilder();
        //设置结果,实时账单信息
        for (int index = 0; index < stGameRoomInfo.getVUserGameInfosCount(); index++) {
            UserGameInfoNet.Builder stUserGameInfo = stGameRoomInfo.getVUserGameInfos(index).toBuilder();
            if (stUserGameInfo.getStUserInfo().getUuid() > 0) {
                System.out.println("结账111 : " + stUserGameInfo.getStUserInfo().getUuid());

                //如果为底池抽水,由于玩家买保险的情况可能导致玩家在桌筹码为负,为负时需要强制买入使玩家筹码量为0
                if (iDrawType == 2 && bFlopStatus && stUserGameInfo.getLRemainStacks() < 0){

                    String msgKey = System.currentTimeMillis()+""+stUserGameInfo.getStUserInfo().getUuid()+(RandomUtils.nextInt(9000) + 1000)+0;

                    //强制买入
                    ClubManager.getInstance().updateUserCreditInGame(stUserGameInfo.getLBuyinClubID(), stUserGameInfo.getStUserInfo().getUuid(),
                            -stUserGameInfo.getLRemainStacks(), stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), 1, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 1, msgKey);

                    //日志打印
                    TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(stUserGameInfo.getLBuyinClubID(), stUserGameInfo.getStUserInfo().getUuid());
                    String sLog = stUserGameInfo.getStUserInfo().getUuid() + " " + stUserGameInfo.getLBuyinClubID() + " " + 910000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                            + " " + msgKey + " " + " 1 " + -stUserGameInfo.getLRemainStacks() + " " + stClubUserCrditInfo.getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                    TranscationLogService.getInstance().error(sLog);

                    //修改账单信息
                    GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                    stGameRoomDetailInfo.buyStakcsInRoom(stUserGameInfo.getStUserInfo().getUuid(), -stUserGameInfo.getLRemainStacks(), stUserGameInfo.getLBuyinClubID(), 7);

                    stUserGameInfo.setLRemainStacks(0);
                    gameBuilder.setVUserGameInfos(index, stUserGameInfo);

                }

                //设置账单
                setBillCurrentStacksInRoom(stUserGameInfo.getStUserInfo().getUuid(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                        , stUserGameInfo.getLRemainStacks(), stUserGameInfo.getLUnloadStacks());
                vSitdownUsers.add(stUserGameInfo.getStUserInfo().getUuid());
            }
        }
        stGameRoomInfo = gameBuilder.build();

        Iterator iter1 = mapStandByUserGameInfos.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry entry = (Map.Entry) iter1.next();
            UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder) entry.getValue();
            if (stUserGameInfoBuilder != null) {
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && !vSitdownUsers.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                    System.out.println("结账112 : " + stUserGameInfoBuilder.getStUserInfo().getUuid());
                    //设置账单
                    setBillCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            , stUserGameInfoBuilder.getLRemainStacks(), stUserGameInfoBuilder.getLUnloadStacks());
                }
            }
        }

        bFlodResult = false;
    }


    /****************************
     * 基本辅助函数
     *******************************/
    Comparator<Integer> comparatorHands = new Comparator<Integer>() {
        public int compare(Integer s1, Integer s2) {
            int cardNumberLeft;
            int cardNumberRight;

            //���������������
            cardNumberLeft = s1 % 13 + 1;
            cardNumberRight = s2 % 13 + 1;

            if (cardNumberLeft == 1) {
                cardNumberLeft = 14;
            }

            if (cardNumberRight == 1) {
                cardNumberRight = 14;
            }
            return cardNumberRight - cardNumberLeft;
        }
    };

    public void setBillCurrentStacksInRoom(long uuid, long roomid, long stacks, long lUnloadStacks) {
        BillManager.getInstance().setCurrentStacksInRoom(uuid, roomid, stacks+lUnloadStacks, lUnloadStacks, mUserPlayHand.get(uuid) != null ? mUserPlayHand.get(uuid) : 0);
        mUserOutStacks.put(uuid, stacks);
    }

    @Override
    public GameUserResultInfo calculateCardStrength(List<Integer> handCards) {
        List<Integer> pairList = new ArrayList<Integer>();
        List<Integer> triList = new ArrayList<Integer>();
        List<Integer> singleList = new ArrayList<Integer>();

        int four = 0;

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

        Collections.sort(cardList, comparatorHands);

        int preCard = 0;
        int pairCount = 0;
        int lastIndex = 0;
        for (int i = 0; i < cardList.size(); i++) {
            int cardNumber = cardList.get(i) % 13 + 1;
            if (cardNumber == 1) {
                cardNumber = 14;
            }
            if (i == 0) {
                pairCount++;
            } else {
                if (preCard == cardNumber) {
                    pairCount++;
                } else {
                    switch (pairCount) {
                        case 1:
                            singleList.add(preCard);

                            maxSingleList.add(cardList.get(lastIndex));
                            break;
                        case 2:
                            pairList.add(preCard);

                            maxPairList.add(cardList.get(lastIndex));
                            maxPairList.add(cardList.get(lastIndex + 1));
                            break;
                        case 3:
                            triList.add(preCard);

                            maxTriList.add(cardList.get(lastIndex));
                            maxTriList.add(cardList.get(lastIndex + 1));
                            maxTriList.add(cardList.get(lastIndex + 2));
                            break;
                        case 4:
                            four = preCard;

                            maxFourList.add(cardList.get(lastIndex));
                            maxFourList.add(cardList.get(lastIndex + 1));
                            maxFourList.add(cardList.get(lastIndex + 2));
                            maxFourList.add(cardList.get(lastIndex + 3));
                            break;
                    }
                    lastIndex = i;
                    pairCount = 1;
                }
            }

            preCard = cardNumber;
        }

        //补充最后一个逻辑
        switch (pairCount) {
            case 1:
                singleList.add(preCard);
                maxSingleList.add(cardList.get(lastIndex));
                break;
            case 2:
                pairList.add(preCard);

                maxPairList.add(cardList.get(lastIndex));
                maxPairList.add(cardList.get(lastIndex + 1));
                break;
            case 3:
                triList.add(preCard);

                maxTriList.add(cardList.get(lastIndex));
                maxTriList.add(cardList.get(lastIndex + 1));
                maxTriList.add(cardList.get(lastIndex + 2));
                break;
            case 4:
                four = preCard;

                maxFourList.add(cardList.get(lastIndex));
                maxFourList.add(cardList.get(lastIndex + 1));
                maxFourList.add(cardList.get(lastIndex + 2));
                maxFourList.add(cardList.get(lastIndex + 3));
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
        for (int i = 0; i < cardList.size(); i++) {

            int cardNumber = cardList.get(i) % 13 + 1;
            if (cardNumber == 1) {
                cardNumber = 14;
            }

            int cardType = cardList.get(i) / 13 + 1;

            switch (cardType) {
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
                    diamondNum++;
                    maxDiamondFlushList.add(cardList.get(i));
                    break;
            }

            if (i != 0) {
                if (preCard - 1 == cardNumber) {
                    iStraightNum++;
                    vTempMaxStrightList.add(cardList.get(i));
                } else if (preCard - 1 > cardNumber) {
                    if (iStraightNum > iMaxStraightNum) {
                        iMaxStraightNum = iStraightNum;
                        iStraightCard = iNextStraightCard;
                        maxStrightList = new ArrayList<Integer>(vTempMaxStrightList);
                    }

                    iNextStraightCard = cardNumber;
                    vTempMaxStrightList.clear();
                    vTempMaxStrightList.add(cardList.get(i));
                    iStraightNum = 1;
                }
            } else {
                iNextStraightCard = cardNumber;
                vTempMaxStrightList.add(cardList.get(i));
                iStraightNum = 1;
            }

            preCard = cardNumber;
        }

        if (iStraightNum > iMaxStraightNum) {
            iMaxStraightNum = iStraightNum;
            iStraightCard = iNextStraightCard;
            maxStrightList = vTempMaxStrightList;
        }

        if (heartNum >= 5) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            calculateFlush(stGameUserResultInfo, iMaxStraightNum, maxHeartFlushList, handCards);
            return stGameUserResultInfo;
        } else if (spadeNum >= 5) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            calculateFlush(stGameUserResultInfo, iMaxStraightNum, maxSpadeFlushList, handCards);
            return stGameUserResultInfo;
        } else if (clubNum >= 5) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            calculateFlush(stGameUserResultInfo, iMaxStraightNum, maxClubFlushList, handCards);
            return stGameUserResultInfo;

        } else if (diamondNum >= 5) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            calculateFlush(stGameUserResultInfo, iMaxStraightNum, maxDiamondFlushList, handCards);
            return stGameUserResultInfo;
        }

        if (iMaxStraightNum >= 5) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            stGameUserResultInfo.mCardStrength = 5;
            stGameUserResultInfo.iStraightCard = iStraightCard;


            if (maxStrightList.size() >= 5) {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxStrightList.subList(0, 5));
            }

            return stGameUserResultInfo;
        } else if (iMaxStraightNum == 4) {
            if (iStraightCard == 5 && cardList.size() == 7) {
                int cardNumber = cardList.get(0) % 13 + 1;

                if (cardNumber == 1) {
                    GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
                    stGameUserResultInfo.mCardStrength = 5;
                    stGameUserResultInfo.iStraightCard = iStraightCard;

                    if (maxStrightList.size() >= 4) {
                        stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxStrightList.subList(0, 4));
                    }
                    stGameUserResultInfo.finalCards.add(cardList.get(0));
                    return stGameUserResultInfo;
                }
            }
        }

        if (four > 0) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            stGameUserResultInfo.mCardStrength = 8;
            stGameUserResultInfo.four = four;

            if (maxFourList.size() == 4) {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxFourList);
            }

            if (maxTriList.size() > 0) {
                stGameUserResultInfo.finalCards.add(maxTriList.get(0));
                stGameUserResultInfo.singleList.add(maxTriList.get(0));
            } else if (maxPairList.size() > 0) {
                stGameUserResultInfo.finalCards.add(maxPairList.get(0));
                int tempCard1 = maxPairList.get(0) % 13 + 1;
                int tempCard2 = maxSingleList.get(0) % 13 + 1;

                if (tempCard1 == 1) {
                    tempCard1 = 14;
                }

                if (tempCard2 == 1) {
                    tempCard2 = 14;
                }

                if (tempCard1 > tempCard2) {
                    stGameUserResultInfo.finalCards.add(maxPairList.get(0));
                    stGameUserResultInfo.singleList.add(maxPairList.get(0));
                } else {
                    stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
                    stGameUserResultInfo.singleList.add(maxSingleList.get(0));
                }
            } else if (maxSingleList.size() > 0) {
                stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
                stGameUserResultInfo.singleList.add(maxSingleList.get(0));
            }
            return stGameUserResultInfo;
        }

        if (triList.size() == 2 || (triList.size() == 1 && pairList.size() >= 1)) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            stGameUserResultInfo.mCardStrength = 7;
            stGameUserResultInfo.triList.add(triList.get(0));

            if (pairList.size() > 0) {
                stGameUserResultInfo.pairList.add(pairList.get(0));
            } else {
                stGameUserResultInfo.pairList.add(triList.get(1));
            }

            if (maxTriList.size() == 6) {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 5));
            } else {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 3));
                stGameUserResultInfo.finalCards.add(maxPairList.get(0));
                stGameUserResultInfo.finalCards.add(maxPairList.get(1));
            }

            return stGameUserResultInfo;
        }

        if (triList.size() == 1) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            stGameUserResultInfo.mCardStrength = 4;
            stGameUserResultInfo.triList.add(triList.get(0));
            stGameUserResultInfo.singleList.add(singleList.get(0));
            stGameUserResultInfo.singleList.add(singleList.get(1));

            if (maxTriList.size() == 3) {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxTriList.subList(0, 3));
                stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
                stGameUserResultInfo.finalCards.add(maxSingleList.get(1));
            }

            return stGameUserResultInfo;
        }

        if (pairList.size() >= 2) {
            GameUserResultInfo stGameUserResultInfo = new GameUserResultInfo();
            stGameUserResultInfo.mCardStrength = 3;
            stGameUserResultInfo.pairList.add(pairList.get(0));
            stGameUserResultInfo.pairList.add(pairList.get(1));

            if (pairList.size() == 2) {
                stGameUserResultInfo.singleList.add(singleList.get(0));
            } else {
                int pairSingle = pairList.get(2);
                int single = singleList.get(0);
                if (pairSingle > single) {
                    stGameUserResultInfo.singleList.add(pairSingle);
                } else {
                    stGameUserResultInfo.singleList.add(single);
                }
            }

            if (maxPairList.size() >= 4) {
                stGameUserResultInfo.finalCards = new ArrayList<Integer>(maxPairList.subList(0, 4));

                if (maxPairList.size() == 4) {
                    stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
                } else {
                    int pairSingle = pairList.get(2);
                    int single = singleList.get(0);
                    if (pairSingle > single) {
                        stGameUserResultInfo.finalCards.add(maxPairList.get(4));
                    } else {
                        stGameUserResultInfo.finalCards.add(maxSingleList.get(0));
                    }
                }
            }

            return stGameUserResultInfo;
        }

        if (pairList.size() == 1) {
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
                               List<Integer> handCards) {
        // 最大牌号，不带花色
        int maxCard = 0;
        // 如果顺子技术大于等于5，则有可能出现同花顺
        if (iMaxStraightNum >= 4) {

            int preNum = 0;
            List<Integer> maxFlushStrengthList = new ArrayList<Integer>();
            List<Integer> tempMaxFlushStrengthList = new ArrayList<Integer>();

            for (int i = 0; i < maxFlushList.size(); i++) {
                int cur = maxFlushList.get(i);
                int curNum = cur % 13 + 1;
                if (curNum == 1) {
                    curNum = 14;
                }

                if (i == 0) {
                    preNum = curNum;
                    tempMaxFlushStrengthList.add(cur);
                } else {
                    if (preNum - curNum == 1) {
                        tempMaxFlushStrengthList.add(cur);
                    } else {
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
                maxCard = max % 13 + 1;
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
                int first = firstNum % 13 + 1;
                if (first == 1) {
                    first = 14;
                }

                int flushStrengthMaxNum = maxFlushStrengthList.get(0);
                int flushStrengthMax = flushStrengthMaxNum % 13 + 1;
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
                } else {
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
            int handCard1 = handCards.get(0) % 13 + 1;
            int handCard2 = handCards.get(1) % 13 + 1;

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
            int tableCard = maxFlushList.get(4) % 13 + 1;
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
     *
     * @return
     */
    @Override
    public boolean checkGameActionValidation() {
        if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
                && stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD
                && stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait) {
            return true;
        } else {
            return false;
        }
    }

    //这把牌是否结束
    @Override
    public int iGameOver() {
        int count = 0;
        int index = 0;
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 &&
                    (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD)) {
                //检查状态，算人数
                count++;
                index = j;
            }
        }

        //���������������������������������������������������������������
        if (count > 1) {
            return -1;
        } else {
            return index;
        }
    }

    public Map<Long,Integer> bettingCount = new HashMap<>();
    /**
     *
     * <br/>
     * @param userGameInfoNet
     * @param type 1为处理回包 如果加密失败 明文密文都不返回 2为处理push 如果加密失败 返回明文
     * @return com.texaspoker.moment.TexasPoker.UserGameInfoNet
     * @author piness
     * @date 2021-06-23 14:06
     */
    public UserGameInfoNet getSecureUserGameInfoNet(UserGameInfoNet userGameInfoNet, int type){
        UserGameInfoNet.Builder userGameInfoNetBuilder = null;
        if (userGameInfoNet != null){
            //副本
            userGameInfoNetBuilder = userGameInfoNet.toBuilder();
        } else {
            return null;
        }

        HandInfo handInfo = getmHandSecureMapByid(userGameInfoNetBuilder.getStUserInfo().getUuid());
        if (handInfo != null && handInfo.getvSecureCards().size() > 0  && !handInfo.getvSecureCards().get(0).isEmpty()) {
            if (stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                if (stGameRoomInfo.getEGameState().getNumber() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop.getNumber()){

                    if (userGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING){
                        userGameInfoNetBuilder.clearVCurrentHands();
                        userGameInfoNetBuilder.clearVCurrentHandsByte();
                        userGameInfoNetBuilder.addVCurrentHandsByte(handInfo.getvSecureCards().get(0));
                        bettingCount.put(userGameInfoNetBuilder.getStUserInfo().getUuid(), 1);
                    } else {
                        if (bettingCount.get(userGameInfoNetBuilder.getStUserInfo().getUuid()) == null) {
                            userGameInfoNetBuilder.clearVCurrentHands();
                            userGameInfoNetBuilder.clearVCurrentHandsByte();
                            userGameInfoNetBuilder.addVCurrentHands(-1);
                            userGameInfoNetBuilder.addVCurrentHands(-1);
                        } else {
                            userGameInfoNetBuilder.clearVCurrentHands();
                            userGameInfoNetBuilder.clearVCurrentHandsByte();
                            userGameInfoNetBuilder.addVCurrentHandsByte(handInfo.getvSecureCards().get(0));
                        }

                    }

                } else {
                    bettingCount.clear();
                    userGameInfoNetBuilder.clearVCurrentHands();
                    userGameInfoNetBuilder.clearVCurrentHandsByte();
                    userGameInfoNetBuilder.addVCurrentHandsByte(handInfo.getvSecureCards().get(0));
                }

            } else {
                userGameInfoNetBuilder.clearVCurrentHands();
                userGameInfoNetBuilder.clearVCurrentHandsByte();
                userGameInfoNetBuilder.addVCurrentHandsByte(handInfo.getvSecureCards().get(0));
            }
        } else {
            userGameInfoNetBuilder.clearVCurrentHands();
        }

        return userGameInfoNetBuilder.build();

    }

    /**
     *
     * <br/>
     * @param gameRoomInfo
     * @param type 1为处理回包 如果加密失败 明文密文都不返回 2为处理push 如果加密失败 返回明文
     * @return com.texaspoker.moment.TexasPoker.GameRoomInfo
     * @author piness
     * @date 2021-06-23 14:07
     */
    public GameRoomInfo getSecureGameRoomInfo(GameRoomInfo gameRoomInfo, int type){
        //副本
        GameRoomInfo.Builder stGameRoomInfoBuilder = gameRoomInfo.toBuilder();
        for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
            //获取每一个UserGameInfoNet
            UserGameInfoNet userGameInfoNet = stGameRoomInfoBuilder.getVUserGameInfos(i);
            if (gameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD && gameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && !bAllAllin){
                UserGameInfoNet secureUserGameInfoNet = getSecureUserGameInfoNet(userGameInfoNet, type);
                stGameRoomInfoBuilder.setVUserGameInfos(i,secureUserGameInfoNet.toBuilder());
            }
        }
        return stGameRoomInfoBuilder.build();
    }

    @Override
    public void sendAllGameStateChange(long fromUuid, ArrayList<Integer> cards) {

        if (mPlayers.size() > 0) {
            //������������������������������
            for (long uuid : mPlayers) {
                if (uuid > 0) {
                    SCGameRoomStateChange.Builder stGameStateChangeBuilder = SCGameRoomStateChange.newBuilder();
                    stGameStateChangeBuilder.setStGameRoomInfo(getSecureGameRoomInfo(stGameRoomInfo, 2));
                    stGameStateChangeBuilder.setUuid(fromUuid);

                    if (cards != null) {
                        for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
                            int cardNum = cards.get(cardIndex);
                            stGameStateChangeBuilder.addVGivenCards(cardNum);
                        }
                    }

                    sendGameStateChange(uuid, stGameStateChangeBuilder.build());
                }
            }

            //������������������������������
            for (long uuid : getStandByUuids()) {
                if (uuid > 0) {
                    if (!mPlayers.contains(uuid)) {
                        SCGameRoomStateChange.Builder stGameStateChangeBuilder = SCGameRoomStateChange.newBuilder();
                        stGameStateChangeBuilder.setStGameRoomInfo(getSecureGameRoomInfo(stGameRoomInfo, 2));
                        stGameStateChangeBuilder.setUuid(fromUuid);
                        if (cards != null) {
                            for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
                                int cardNum = cards.get(cardIndex);
                                stGameStateChangeBuilder.addVGivenCards(cardNum);
                            }
                        }

                        sendGameStateChange(uuid, stGameStateChangeBuilder.build());
                    }
                }
            }
        }
    }

    @Override
    public boolean sendGameStateChange(long toUuid, SCGameRoomStateChange stGameStateChange) {
        if (stGameStateChange != null) {
            SCGameRoomStateChange.Builder stSCGameRoomStateChangeBuilder = stGameStateChange.toBuilder();
            GameRoomInfo.Builder stGameRoomInfoBuilder = stSCGameRoomStateChangeBuilder.getStGameRoomInfo().toBuilder();
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != toUuid) {

                    if (bAllAllin == true) {
                        if (stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                                && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_DEAD
                                && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                            continue;
                        }
                    }

                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
                        continue;
                    }

                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
                            continue;
                        }

                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                            stUserGameInfoNetBuilder.clearVCurrentHands();
                            stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                        } else {
                            if (bFlodResult == true) {
                                stUserGameInfoNetBuilder.clearVCurrentHands();
                                stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                            }
                        }
                    } else {
                        stUserGameInfoNetBuilder.clearVCurrentHands();
                        stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                    }
                } /*else {
                    if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD && stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && !bAllAllin){
                        if (handInfo != null && handInfo.getvSecureCards().size() > 0  && !handInfo.getvSecureCards().get(0).isEmpty()) {
                            stUserGameInfoNetBuilder.clearVCurrentHands();
                            stUserGameInfoNetBuilder.clearVCurrentHandsByte();
                            stUserGameInfoNetBuilder.addVCurrentHandsByte(handInfo.getvSecureCards().get(0));
                        }
                    }

                }*/


                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
            }

            stSCGameRoomStateChangeBuilder.setStGameRoomInfo(stGameRoomInfoBuilder);

            if (stSCGameRoomStateChangeBuilder.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId() % 2 == 0){
                GameRoomMsgManager.getInstance().sendGameRoomStateChangeMsg(toUuid, stSCGameRoomStateChangeBuilder.build());
            } else {
                GameRoomStateMsgManager.getInstance().sendGameRoomStateChangeMsg(toUuid, stSCGameRoomStateChangeBuilder.build());
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void SidePot() {
        Map<Integer, UserGameInfoNet.Builder> mapExistUsers = new ConcurrentHashMap<Integer, UserGameInfoNet.Builder>();
        ArrayList<PotInfo> mTempPotList = new ArrayList<PotInfo>();

        long lOtherPosts = 0;

        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            Long uuid = mHandPos.get(i);
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();

            if ((uuid != null && uuid > 0 &&
                    (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT))
                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                mapExistUsers.put(i, stUserGameInfoBuilder);
            }
        }

        while (mapExistUsers.size() > 0) {
            int potPlayerSize = 0;
            long lMinStacks = 0;

            Iterator iter = mapExistUsers.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder) entry.getValue();
                if (stUserGameInfoBuilder.getLBetStacks() > 0) {
                    potPlayerSize++;
                } else {
                    iter.remove();
                    continue;
                }

                if (lMinStacks == 0 && stUserGameInfoBuilder.getLBetStacks() > 0) {
                    lMinStacks = stUserGameInfoBuilder.getLBetStacks();
                } else if (lMinStacks > stUserGameInfoBuilder.getLBetStacks() && stUserGameInfoBuilder.getLBetStacks() > 0) {
                    lMinStacks = stUserGameInfoBuilder.getLBetStacks();
                }
            }

            if (lMinStacks == 0) {
                break;
            }

            PotInfo stPotInfo = new PotInfo();

            stPotInfo.potSize = lMinStacks * potPlayerSize;
            stPotInfo.allinSize = 0;
            iter = mapExistUsers.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Integer pos = (Integer) entry.getKey();
                UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder) entry.getValue();

                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLBetStacks() - lMinStacks);

                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                    stPotInfo.playerList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    stPotInfo.playerListpos.add(pos);

                    if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        stPotInfo.playerNotFoldList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }
                } else {
                    if (mHandPos.get(pos) != null) {
                        stPotInfo.playerList.add(mHandPos.get(pos));
                        stPotInfo.playerListpos.add(pos);
                    }

                    if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        stPotInfo.playerNotFoldList.add(mHandPos.get(pos));
                    }
                }

                if (stUserGameInfoBuilder.getLRemainStacks() == 0) {
                    stPotInfo.allinSize++;
                }

                if (stUserGameInfoBuilder.getLBetStacks() == 0) {

                    iter.remove();
                }
            }

            mTempPotList.add(stPotInfo);
            PotInfo stTempPotInfo = new PotInfo();
            stTempPotInfo.potSize = stPotInfo.potSize;
            stTempPotInfo.playerList.addAll(stPotInfo.playerList);
            stTempPotInfo.playerNotFoldList.addAll(stPotInfo.playerNotFoldList);
            stTempPotInfo.allinSize = stPotInfo.allinSize;
            stTempPotInfo.playerListpos.addAll(stPotInfo.playerListpos);

            //记录每一轮池子并合并同一个池子 弃牌模式下底池抽水使用
            mTempPotDrawList.add(stTempPotInfo);
            if (!CollectionUtils.isEmpty(mTempPotDrawList) && mTempPotDrawList.size() >= 2){
                PotInfo stLastPot = mTempPotDrawList.get(mTempPotDrawList.size() - 1);
                PotInfo stBeforeLastPot = mTempPotDrawList.get(mTempPotDrawList.size() - 2);

                Boolean bool = true; //默认是同一个池子
                if (stLastPot.playerList.size() == stBeforeLastPot.playerList.size()){
                    for (Long uuid : stBeforeLastPot.playerList){
                        if (!stLastPot.playerList.contains(uuid)){
                            bool = false;
                            break;
                        }
                    }

                    if (bool){
                        //两个池子应该是一个池子
                        PotInfo stPotInfoNew = new PotInfo();
                        stPotInfoNew.potSize = stBeforeLastPot.potSize + stLastPot.potSize;
                        stPotInfoNew.allinSize = stBeforeLastPot.allinSize;
                        stPotInfoNew.playerList.addAll(stBeforeLastPot.playerList);
                        stPotInfoNew.playerListpos.addAll(stBeforeLastPot.playerListpos);
                        stPotInfoNew.playerNotFoldList.addAll(stBeforeLastPot.playerNotFoldList);

                        mTempPotDrawList.remove(mTempPotDrawList.size() - 1);
                        mTempPotDrawList.remove(mTempPotDrawList.size() - 1);
                        mTempPotDrawList.add(stPotInfoNew);
                    }
                }
            }

            //如果有需要，合池子
            if (mTempPotList.size() >= 2) {
                PotInfo stLastPot = mTempPotList.get(mTempPotList.size() - 1);
                PotInfo stBeforeLastPot = mTempPotList.get(mTempPotList.size() - 2);
                if (stLastPot.playerNotFoldList.size() == stBeforeLastPot.playerNotFoldList.size()) {
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

        if (mTempPotList.size() > 0) {
            PotInfo stFristPot = mTempPotList.get(0);
            stFristPot.potSize += lOtherPosts;
        } else {
            if (lOtherPosts > 0) {
                PotInfo stPotInfo = new PotInfo();
                stPotInfo.potSize = lOtherPosts;
                stPotInfo.allinSize = 0;

                mTempPotList.add(stPotInfo);
            }
        }

        if (mTempPotList.size() > 0) {
            if (mPotList.size() == 0) {
                mPotList = mTempPotList;
            } else {
                PotInfo tempPot = mTempPotList.get(0);
                PotInfo stLastPot = mPotList.get(mPotList.size() - 1);

                if (stLastPot.allinSize == 0) {

                    if (stLastPot != null) {
                        stLastPot.potSize += tempPot.potSize;
                        stLastPot.allinSize = tempPot.allinSize;
                        ArrayList<Integer> stLastPos = new ArrayList<Integer>(stLastPot.playerListpos);

                        for (int i = 0; i < tempPot.playerListpos.size(); i++) {
                            int tempPos = tempPot.playerListpos.get(i);
                            long tempUuid = tempPot.playerList.get(i);
                            for (int j = 0; j < stLastPos.size(); j++) {
                                int lastPos = stLastPos.get(j);
                                if (lastPos != tempPos) {
                                    stLastPot.playerListpos.add(tempPos);
                                    stLastPot.playerList.add(tempUuid);
                                }
                            }
                        }
                    }
                } else {
                    mPotList.add(tempPot);
                }

                for (int i = 1; i < mTempPotList.size(); i++) {
                    PotInfo tempPot1 = mTempPotList.get(i);
                    mPotList.add(tempPot1);
                }
            }
        }

        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            Long uuid = mHandPos.get(i);
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();

            if ((uuid != null && uuid > 0 &&
                    (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT))
                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                mapExistUsers.put(i, stUserGameInfoBuilder);
            }
        }

        boolean bAllFold = true;
        int iAllFoldStacks = 0;
        for (int i = mPotList.size() - 1; i >= 0; i--) {
            bAllFold = true;
            PotInfo stPotInfo = mPotList.get(i);

            for (int pos : stPotInfo.playerListpos) {
                UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapExistUsers.get(pos);

                if (stUserGameInfoNetBuilder != null) {
                    if (stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD) {

                        bAllFold = false;
                        break;
                    }
                }
            }
            if (bAllFold == true) {
                iAllFoldStacks += stPotInfo.potSize;
                mPotList.remove(i);
            } else {
                stPotInfo.potSize += iAllFoldStacks;
                break;
            }
        }

        if (mPotList.size() == 0 && iAllFoldStacks > 0) {
            int iLengths = 0;
            int iInPos = -1;
            long uuid = 0;
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                if (stTempUserGameInfoNet.getStUserInfo().getUuid() > 0
                        && stTempUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                        && stTempUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                        && stTempUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                    uuid = stTempUserGameInfoNet.getStUserInfo().getUuid();
                    iLengths++;
                    iInPos = i;
                }
            }

            if (uuid > 0 && iLengths == 1 && iLengths != -1) {
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
        for (int i = 0; i < mPotList.size(); i++) {
            PotInfo stPotInfo = mPotList.get(i);

            //设置Pot相关
            stGameRoomInfoBuilder.addVCurrentPots(stPotInfo.potSize);

            //设置新pot结构
            TexasPoker.PotInfo.Builder stPotInfoBuilder = TexasPoker.PotInfo.newBuilder();
            stPotInfoBuilder.setLPotStacks(stPotInfo.potSize);
            for (int j = 0; j < stPotInfo.playerList.size(); j++) {
                long uuid = stPotInfo.playerList.get(j);
                stPotInfoBuilder.addVUuids(uuid);
            }
            stGameRoomInfoBuilder.addVPotInfos(stPotInfoBuilder);
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    /****************************
     * 比牌相关函数
     *******************************/
    @Override
    public int CompareSameLevelUser(GameUserResultInfo user1, GameUserResultInfo user2) {
        switch (user1.mCardStrength) {
            case 1: {
                for (int i = 0; i < user1.singleList.size(); i++) {
                    int user1CardNum = user1.singleList.get(i);
                    int user2CardNum = user2.singleList.get(i);

                    if (user1CardNum == 1) {
                        user1CardNum = 14;
                    }

                    if (user2CardNum == 1) {
                        user2CardNum = 14;
                    }

                    if (user1CardNum != user2CardNum) {
                        return user1CardNum - user2CardNum;
                    }
                }
                return 0;
            }
            case 2: {
                int user1pair = user1.pairList.get(0);
                int user2pair = user2.pairList.get(0);


                if (user1pair == user2pair) {
                    for (int i = 0; i < user1.singleList.size(); i++) {
                        int user1CardNum = user1.singleList.get(i);
                        int user2CardNum = user2.singleList.get(i);

                        if (user1CardNum == 1) {
                            user1CardNum = 14;
                        }

                        if (user2CardNum == 1) {
                            user2CardNum = 14;
                        }

                        if (user1CardNum != user2CardNum) {
                            return user1CardNum - user2CardNum;
                        }
                    }
                    return 0;
                } else {
                    if (user1pair == 1) {
                        user1pair = 14;
                    }

                    if (user2pair == 1) {
                        user2pair = 14;
                    }
                    return user1pair - user2pair;
                }
            }
            case 3: {

                for (int i = 0; i < user1.pairList.size(); i++) {
                    int user1CardNum = user1.pairList.get(i);
                    int user2CardNum = user2.pairList.get(i);

                    if (user1CardNum == 1) {
                        user1CardNum = 14;
                    }

                    if (user2CardNum == 1) {
                        user2CardNum = 14;
                    }

                    if (user1CardNum != user2CardNum) {
                        return user1CardNum - user2CardNum;
                    }
                }
                int user1single = user1.singleList.get(0);
                int user2single = user2.singleList.get(0);

                if (user1single == 1) {
                    user1single = 14;
                }

                if (user2single == 1) {
                    user2single = 14;
                }

                return user1single - user2single;
            }
            case 4: {
                int user1tri = user1.triList.get(0);
                int user2tri = user2.triList.get(0);


                if (user1tri == user2tri) {
                    for (int i = 0; i < user1.singleList.size(); i++) {
                        int user1CardNum = user1.singleList.get(i);
                        int user2CardNum = user2.singleList.get(i);

                        if (user1CardNum == 1) {
                            user1CardNum = 14;
                        }

                        if (user2CardNum == 1) {
                            user2CardNum = 14;
                        }

                        if (user1CardNum != user2CardNum) {
                            return user1CardNum - user2CardNum;
                        }
                    }
                    return 0;
                } else {
                    if (user1tri == 1) {
                        user1tri = 14;
                    }

                    if (user2tri == 1) {
                        user2tri = 14;
                    }

                    return user1tri - user2tri;
                }
            }
            case 5: {
                return user1.iStraightCard - user2.iStraightCard;
            }
            case 6: {
                for (int i = 0; i < 5; i++) {
                    int flushMaxHandCard1 = user1.finalCards.get(i) % 13 + 1;
                    int flushMaxHandCard2 = user2.finalCards.get(i) % 13 + 1;

                    if (flushMaxHandCard1 == 1) {
                        flushMaxHandCard1 = 14;
                    }

                    if (flushMaxHandCard2 == 1) {
                        flushMaxHandCard2 = 14;
                    }

                    if (flushMaxHandCard1 != flushMaxHandCard2) {
                        return flushMaxHandCard1 - flushMaxHandCard2;
                    }
                }
                return 0;
            }
            case 7: {
                int user1tri = user1.triList.get(0);
                int user2tri = user2.triList.get(0);
                if (user1tri == user2tri) {
                    int user1CardNum = user1.pairList.get(0);
                    int user2CardNum = user2.pairList.get(0);

                    if (user1CardNum == 1) {
                        user1CardNum = 14;
                    }

                    if (user1CardNum == 1) {
                        user1CardNum = 14;
                    }

                    return user1CardNum - user2CardNum;
                } else {
                    if (user1tri == 1) {
                        user1tri = 14;
                    }

                    if (user2tri == 1) {
                        user2tri = 14;
                    }

                    return user1tri - user2tri;
                }
            }
            case 8:

                int four1 = user1.four;
                int four2 = user2.four;

                if (four1 == 1) {
                    four1 = 14;
                }

                if (four2 == 1) {
                    four2 = 14;
                }
                if (four1 != four2) {
                    return four1 - four2;
                } else {

                    int cardNum1 = 0;
//				System.out.println("four1four1four1four11 : " + user1.singleList.size());
//				System.out.println("four1four1four1four12 : " + user1.pairList.size());
//				System.out.println("four1four1four1four13 : " + user1.triList.size());
                    if (user1.singleList.size() > 0) {
                        cardNum1 = user1.singleList.get(0) % 13 + 1;
                        if (cardNum1 == 1) {
                            cardNum1 = 14;
                        }
                    }

                    if (user1.pairList.size() > 0) {
                        int tempCardNum1 = user1.pairList.get(0) % 13 + 1;
                        if (tempCardNum1 == 1) {
                            tempCardNum1 = 14;
                        }

                        if (tempCardNum1 > cardNum1) {
                            cardNum1 = tempCardNum1;
                        }

                    }

                    if (user1.triList.size() > 0) {
                        int tempCardNum1 = user1.triList.get(0) % 13 + 1;
                        if (tempCardNum1 == 1) {
                            tempCardNum1 = 14;
                        }

                        if (tempCardNum1 > cardNum1) {
                            cardNum1 = tempCardNum1;
                        }
                    }

                    int cardNum2 = 0;
                    if (user2.singleList.size() > 0) {
                        cardNum2 = user2.singleList.get(0) % 13 + 1;
                        if (cardNum2 == 1) {
                            cardNum2 = 14;
                        }
                    }

                    if (user2.pairList.size() > 0) {
                        int tempCardNum2 = user2.pairList.get(0) % 13 + 1;
                        if (tempCardNum2 == 1) {
                            tempCardNum2 = 14;
                        }

                        if (tempCardNum2 > cardNum2) {
                            cardNum2 = tempCardNum2;
                        }
                    }

                    if (user2.triList.size() > 0) {
                        int tempCardNum2 = user2.triList.get(0) % 13 + 1;
                        if (tempCardNum2 == 1) {
                            tempCardNum2 = 14;
                        }

                        if (tempCardNum2 > cardNum2) {
                            cardNum2 = tempCardNum2;
                        }
                    }

                    if (cardNum1 == 1) {
                        cardNum1 = 14;
                    }

                    if (cardNum2 == 1) {
                        cardNum2 = 14;
                    }
                    return cardNum1 - cardNum2;
                }
            case 9:
                if (user1.finalCards.size() >= 4 && user2.finalCards.size() >= 4) {
                    int flushMaxHandCard1 = user1.iStraightCard;
                    int flushMaxHandCard2 = user2.iStraightCard;
                    if (flushMaxHandCard1 != flushMaxHandCard2) {
                        return flushMaxHandCard1 - flushMaxHandCard2;
                    }
                }
        }

        return 0;
    }

    /****************************
     * 牌局逻辑辅助函数
     *******************************/
    @Override
    public boolean checkAllAllin() {
        int iAllAllin = 0;
        long onlyBetStacks = 0;
        long allinStacks = 0;
        int iNoFold = 0;

        if (bAllAllin != true) {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 &&
                        stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                        && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                        && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                    iNoFold++;
                    if (stUserGameInfoNet.getLRemainStacks() > 0) {
                        iAllAllin++;
                        onlyBetStacks = stUserGameInfoNet.getLBetStacks();
                    } else {
                        if (stUserGameInfoNet.getLBetStacks() > allinStacks) {
                            allinStacks = stUserGameInfoNet.getLBetStacks();
                        }
                    }
                }
            }

            if (iAllAllin > 1) {
                bAllAllin = false;
            } else if (iAllAllin == 1) {
                if (allinStacks > onlyBetStacks)
                    bAllAllin = false;
                else {
                    if (iNoFold > 1) {
                        bAllAllin = true;
                    } else {
                        bAllAllin = false;
                    }
                }
            } else {
                if (iNoFold > 1) {
                    bAllAllin = true;
                } else {
                    bAllAllin = false;
                }

            }
        }

        return bAllAllin;
    }

    /****************************发牌辅助函数*******************************/
    /**
     * 发牌函数
     *
     * @return 返回一张手牌
     */
    @Override
    public int getTableCard(int cardNo) {
//		TableLogService.getInstance().error1(lFromRoomID
//				+"," + lGameRoomID
//				+"," + stGameRoomInfo.getLGameHandID()
//				+",getTableCard begin " + cardNo);

        if (bAllAllin == true) {
            int drawCard = mCards[iDrawCardNum++];

            //然后查看保险信息
            com.texaspoker.dubbo.game.insurance.InsuranceInfo stInsuranceInfo = InsuranceManager.getInstance().getInsurance(this.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());

            //如果有保险信息，则继续查看
            if (stInsuranceInfo != null) {
                Set<Integer> vAllOuts = new HashSet<Integer>();

                List<OutsInfo> outs = stInsuranceInfo.getAllOuts();
                for (OutsInfo stOutsInfo : outs) {
                    for (int i = 0; i < stOutsInfo.outs.length; i++) {
                        int out_card = stOutsInfo.outs[i];
                        vAllOuts.add(out_card);
                    }
                }

                long lPotSize = 0;
                for (long iSidePotSize : stGameRoomInfo.getVCurrentPotsList()) {
                    lPotSize += iSidePotSize;
                }

                //如果池子大于200倍bb，则需要50%再发一张
                if (lPotSize >= 200 * stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds() && stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds() >= 50) {
                    while (checkOutsCard(drawCard, vAllOuts, 40, cardNo, iDrawCardNum)) {
                        //则70%再发下一张
                        drawCard = mCards[iDrawCardNum++];
                    }
                } else if (lPotSize >= 100 * stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds() && stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds() >= 50) {
                    while (checkOutsCard(drawCard, vAllOuts, 35, cardNo, iDrawCardNum)) {
                        //则70%再发下一张
                        drawCard = mCards[iDrawCardNum++];
                    }
                } else {
//					if(stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds() >= 50){
//						System.out.println(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + ","+stGameRoomInfo.getLGameHandID()+" checkOutsCard lPotSize1111111111111111 : " + lPotSize);
//					}
                    while (checkOutsCard(drawCard, vAllOuts, 30, cardNo, iDrawCardNum)) {
                        //则50%再发下一张
                        drawCard = mCards[iDrawCardNum++];
                    }
                }
            }

//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",getTableCard card " + drawCard);
//
//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",getTableCard end " + cardNo);

            vTableCards.add(drawCard);
            return drawCard;
        } else {
            int drawCard = mCards[iDrawCardNum++];

//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",getTableCard card " + drawCard);

//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",getTableCard end " + cardNo);
            vTableCards.add(drawCard);
            return drawCard;
        }
    }

    @Override
    public int getHandCard() {
        return mCards[iDrawCardNum++];
    }

    //检查是否需要再发一张牌，如果随机数大于百分比的数据,则需要再发一张牌，否则不发牌，iPercent为再发一张的可能性
    //cardNo 要发的第几张牌
    //drawCardNum 要抓的第几张牌
    public boolean checkOutsCard(int drawCard, Set<Integer> vAllOuts, int iPercent, int cardNo, int drawCardNum) {
        if (vAllOuts.contains(drawCard)) {
            //检查是否还有牌可以接着发
            if (drawCardNum + (5 - cardNo) >= 52) {
                return false;
            } else {
                int precentCard = ShuffleAlgorithm.getRandomCard();
                if (precentCard < iPercent) {
//					System.out.println(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + ","+stGameRoomInfo.getLGameHandID()+" checkOutsCard drawCard : " + drawCard + " vAllOuts " + vAllOuts + " true " + precentCard+"/"+iPercent);
                    return true;
                } else {
//					System.out.println(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + ","+stGameRoomInfo.getLGameHandID()+" checkOutsCard drawCard : " + drawCard + " vAllOuts " + vAllOuts + " false "+ precentCard+"/"+iPercent);
                    return false;
                }
            }
        } else {
//			System.out.println(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + "checkOutsCard drawCard : " + drawCard + " vAllOuts " + vAllOuts);
            return false;
        }
    }

    /**
     * 发牌函数，测试用，发固定牌
     *
     * @param uuid   发牌的用户
     * @param time   第几张牌
     * @param bTable 是桌牌还是手牌
     * @return
     */
    @Override
    public int getCard(long uuid, int time, boolean bTable) {
        if (bTable) {
            return vTestTableCards.get(time);
        } else {
            if (uuid == 342342) {
                return vTestHandCards1.get(time);
            } else if (uuid == 203330) {
                return vTestHandCards2.get(time);
            } else if (uuid == 342270){
                return vTestHandCards3.get(time);
            } else {
                return vTestHandCards4.get(time);
            }
        }
    }

    /**
     * 洗牌相关算法
     */
    @Override
    public void resetCard() {
        //洗牌
        mCards = new int[52];

        ShuffleCard.initShuffle(mCards);

        // 安全洗牌一次
        ShuffleCard.secureShuffle(mCards);

        //重置获取牌的数量
        iDrawCardNum = 0;

        vTableCards.clear();
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
            UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
            stUserGameInfoNetBuilder.clearVPayTableCards();
            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
        }
        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    /****************************玩家超时辅助函数*******************************/
    /**
     * 重置玩家超时
     *
     * @param uuid 设置超时的玩家
     */
    @Override
    public void setTimeoutUserCurrent(long uuid) {
        mTimeoutGameUsers.setCurrent(uuid);
        mTimeoutGameUsers.setTimeout(uuid, (long) (2 + stGameRoomInfo.getStGameRoomBaseInfo().getIThinkingInterval()));
    }

    /**
     * 获取超时的玩家
     *
     * @return
     */
    @Override
    public ArrayList<Long> getTimeoutUsers() {
        return mTimeoutGameUsers.timeout();
    }

    /**
     * 延长超时，主要是给延时道具用
     *
     * @param uuid  延时的用户
     * @param delay 延时的时间
     * @return
     */
    @Override
    public boolean delayTimeout(long uuid, long delay) {
        return mTimeoutGameUsers.addTimeout(uuid, delay);
    }

    /**
     * 删除所有timeout相关
     */
    @Override
    public void removeAllTimeOutUser() {
        mTimeoutGameUsers.removeAllKey();
    }

    /****************************
     * 玩家超时次数辅助函数
     *******************************/
    @Override
    public void addUserTimeoutTimes(long uuid) {
        Integer times = mapTimeoutTimesUser.get(uuid);
        if (times == null) {
            mapTimeoutTimesUser.put(uuid, 1);
        } else {
            mapTimeoutTimesUser.put(uuid, times + 1);
        }
    }

    @Override
    public void resetUserTimeoutTimes(long uuid) {
        Integer times = mapTimeoutTimesUser.get(uuid);
        if (times != null) {
            mapTimeoutTimesUser.put(uuid, 0);
        }
    }

    @Override
    public void removeTimeoutTimesUser(long uuid) {
        try {
            mapTimeoutTimesUser.remove(uuid);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void removeAllTimeOutTimesUser() {
        mapTimeoutTimesUser.clear();
    }

    @Override
    public int getTimeoutTimesUser(long uuid) {
        Integer times = mapTimeoutTimesUser.get(uuid);
        if (times == null) {
            return 0;
        } else {
            return times;
        }
    }

    @Override
    public void flodWinProcess() {
        //AOF模式下依次看牌下发手牌保护,如果玩家在没有下发flop3张牌时结算需要将玩家的所有手牌下发
        GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        if ((stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && bFlopStatus == false) || (stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && bFlopStatus == false)){
            for (int i = 0; i < tGameRoomInfoBuilder.getVUserGameInfosCount(); i++){
                UserGameInfoNet.Builder stUserGameInfoNetBuilder = tGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() > 0
                        && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                        && stUserGameInfoNetBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY){
                    stUserGameInfoNetBuilder.clearVCurrentHands();

                    ArrayList<Integer> cards = new ArrayList<Integer>();
                    if (mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()) != null && mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()).getvCards() != null){
                        cards.addAll(mHandMap.get(stUserGameInfoNetBuilder.getStUserInfo().getUuid()).getvCards());
                    } else {
                        cards.add(-1);
                        cards.add(-1);
                    }
                    stUserGameInfoNetBuilder.addAllVCurrentHands(cards);
                }
                tGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
            }
        }
        stGameRoomInfo = tGameRoomInfoBuilder.build();

        sendAllGameStateChange(0, null);  //下发push动画过度

        //分池
        SidePot();

        //计算结果
        calculateWinnerFlod();

        //设置牌局状态
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result);
        stGameRoomInfoBuilder.setBStateChange(1);
        setStageUpdateTime(System.currentTimeMillis());
        stGameRoomInfo = stGameRoomInfoBuilder.build();

        bShowTableCard = true;
        bFirstWaitShowTableCard = true;

        mCurrentTableCards.clear();
        mCurrentTableCards.addAll(stGameRoomInfo.getVCurrentCardsList());

    }

    /****************************
     * 牌局用户位置辅助函数
     *******************************/
    @Override
    public int getUserPos(long uuid) {
        Integer iPos = mapUserPos.get(uuid);
        if (iPos != null) {
            return iPos;
        } else {
            return 0;
        }
    }

    @Override
    public void setUserPos(long uuid, int pos) {
        mapUserPos.put(uuid, pos);
    }

    @Override
    public void removeUserPos(long uuid) {
        mapUserPos.remove(uuid);
    }

    @Override
    public boolean containUser(long uuid) {
        return mapUserPos.containsKey(uuid);
    }

    //用户计算弃牌赢牌似的情况
    @Override
    public void calculateWinnerFlod() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        GameResultInfo.Builder stGameResultInfoBuilder = GameResultInfo.newBuilder();
        long lSingleMaxPot = 0;

        long lWinUuid = 0l; //获胜玩家uuid 弃牌模式下就1个玩家获胜

        //统计抽水的池子(池子只有1个玩家获胜)
        Map<Integer,Long> drawMap = new HashMap<Integer,Long>();
        for (int index = 0; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++) {
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

            for (int i = 0; i < posArray.size(); i++) {

                int pos = posArray.get(i);
                long winuuid = stPotInfo.playerList.get(i);

                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(pos);

                if (stUserGameInfo.getStUserInfo().getUuid() > 0) {
                    if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CALL
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CHECK
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_RAISE
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY) {
                        if (!winnerUuidSet.contains(winuuid)) {
                            winnerIDSet.add(pos);
                            winnerIDArray.add(pos);
                            winnerUuidSet.add(winuuid);
                            winnerUuidArray.add(winuuid);
                        }
                    } else if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        if (mHandPos.get(pos) != null) {
                            if (!allFoldUuidSet.contains(mHandPos.get(pos))) {
                                allFoldUuids.add(mHandPos.get(pos));
                                allFoldPos.add(pos);

                                allFoldUuidSet.add(mHandPos.get(pos));
                                allFoldPosSet.add(pos);
                            }
                        }
                    }
                } else {
                    if (mHandPos.get(pos) != null) {
                        if (!allFoldUuidSet.contains(mHandPos.get(pos))) {
                            allFoldUuids.add(mHandPos.get(pos));
                            allFoldPos.add(pos);

                            allFoldUuidSet.add(mHandPos.get(pos));
                            allFoldPosSet.add(pos);
                        }
                    }
                }
            }

            // 记录本手所有池子信息 roomid handid pot
            StringBuilder potLog = new StringBuilder("")
                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //roomid
                    .append(" ").append(stGameRoomInfo.getLGameHandID()) //handid
                    .append(" ").append(index) //池子编号
                    .append(" ").append(stGameRoomInfoBuilder.getVCurrentPots(index)) //未抽水时底池大小
                    .append(" ").append(winnerUuidArray) //该底池获胜玩家
                    .append(" ").append(stPotInfo.potSize) //底池大小
                    .append(" ").append(stPotInfo.allinSize) //allin人数
                    .append(" ").append(stPotInfo.playerList) //改底池对应所有玩家
                    .append(" ").append(stPotInfo.playerNotFoldList); //改底池对应没有弃牌的玩家
            PotLogService.getInstance().error(potLog.toString());

            //若是玩家自己贡献一个池子则不抽水
            if (stPotInfo.playerList.size() > 1){
                drawMap.put(index,stGameRoomInfoBuilder.getVCurrentPots(index));
            }

            if (!CollectionUtils.isEmpty(winnerUuidArray)){
                lWinUuid = winnerUuidArray.get(0);
            }
        }

        //统计底池抽水的池子
        Map<Integer,PotInfo> mDrawPotMap = new HashMap<Integer,PotInfo>(); // 池子编号-池子信息
        if (!CollectionUtils.isEmpty(mTempPotDrawList)){
            for (int i = 0; i < mTempPotDrawList.size(); i++){

                // 记录本手所有池子信息 roomid handid pot
                StringBuilder potLog = new StringBuilder("")
                        .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //roomid
                        .append(" ").append(stGameRoomInfo.getLGameHandID()) //handid
                        .append(" ").append(i) //池子编号
                        .append(" ").append("flodpot")
                        .append(" ").append(mTempPotDrawList.get(i).potSize) //未抽水时底池大小
                        .append(" ").append(mTempPotDrawList.get(i).allinSize) //allin人数
                        .append(" ").append(mTempPotDrawList.get(i).playerList) //该底池对应所有玩家
                        .append(" ").append(mTempPotDrawList.get(i).playerNotFoldList); //该底池对应没有弃牌的玩家
                PotLogService.getInstance().error(potLog.toString());

                if (mTempPotDrawList.get(i).playerList.size() > 1){
                    drawMap.put(i,mTempPotDrawList.get(i).potSize);
                    mDrawPotMap.put(i,mTempPotDrawList.get(i));
                }
            }
        }

        //进行底池抽水 条件 底池抽水,发了Flop3张牌
        Map<Integer,Long> userDrawStacks = new HashMap<Integer,Long>();
        if (drawMap != null && drawMap.size() > 0 && bCredit && iDrawType == 2 && bFlopStatus){//授信局抽水
            userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                    stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
        } else if (drawMap != null && drawMap.size() > 0 && !bCredit && iDrawType == 2 && bFlopStatus){//快速局抽水
            if (stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.QUICK_GAME_ROOM){
                userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
            } else {//普通联盟 普通俱乐部抽水
                userDrawStacks = GameFeeUtil.getBottomPump(drawMap,stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds(),stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB(),iUserNum,stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum());
            }

        }

        //抽水后计算玩家贡献抽水
        long totalDrawStacks = 0l;
        if (userDrawStacks != null){
            for (Map.Entry<Integer,Long> map : userDrawStacks.entrySet()){
                totalDrawStacks = totalDrawStacks + map.getValue();

                //记录玩家原始贡献抽水
                int j = 1; //记录玩家数
                long drawStacks = map.getValue() / mDrawPotMap.get(map.getKey()).playerList.size(); //记录玩家原始贡献抽水值
                long drawLeftStacks = map.getValue() % mDrawPotMap.get(map.getKey()).playerList.size(); //记录剩余玩家原始贡献抽水值

                //获胜玩家贡献的抽水
                for (Long winUuid : mDrawPotMap.get(map.getKey()).playerList){
                    //记录抽水日志
                    StringBuilder drawLog = new StringBuilder("")
                            .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                            .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                            .append(" ").append(map.getKey()) //底池编号
                            .append(" ").append(mDrawPotMap.get(map.getKey()).potSize) //该底池大小
                            .append(" ").append(map.getValue()) //该底池总抽水值
                            .append(" ").append(1) //该底池获胜人数
                            .append(" ").append(mDrawPotMap.get(map.getKey()).playerList.size()) //该底池贡献抽水的所有玩家数
                            .append(" ").append(3) //玩家类型 1 获胜玩家 2 未获胜玩家 3 弃牌模式下
                            .append(" ").append(winUuid); //玩家uuid

                    if (mUserDrawStacks.get(winUuid) == null){
                        if (j == mDrawPotMap.get(map.getKey()).playerList.size()){
                            mUserDrawStacks.put(winUuid, drawStacks + drawLeftStacks);
                            drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                        } else {
                            mUserDrawStacks.put(winUuid, drawStacks);
                            drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                        }
                    } else {
                        if (j == mDrawPotMap.get(map.getKey()).playerList.size()){
                            mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks + drawLeftStacks);
                            drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                        } else {
                            mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks);
                            drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                        }
                    }

                    drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                            .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                            .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                            .append(" ").append(iUserNum); //实际参人数
                    PotDrawLogService.getInstance().error(drawLog.toString());

                    j++;
                }
            }
        }

        //记录本手总抽水值
        PotLogService.getInstance().error("roomid: " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " handid " + stGameRoomInfo.getLGameHandID() + " totalDrawStacks " + totalDrawStacks);

        //弃牌模式下只有给一个玩家抽水,所有的抽水都是获胜玩家贡献的
        //获胜玩家原始贡献的抽水
        if (lWinUuid != 0){
            if (mUserOriginDrawStacks.get(lWinUuid) == null){
                mUserOriginDrawStacks.put(lWinUuid, totalDrawStacks);
            } else {
                mUserOriginDrawStacks.put(lWinUuid, mUserOriginDrawStacks.get(lWinUuid) + totalDrawStacks);
            }
        }


        for (int index = 0; index < stGameRoomInfoBuilder.getVCurrentPotsCount(); index++) {

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

            for (int i = 0; i < posArray.size(); i++) {

                int pos = posArray.get(i);
                long winuuid = stPotInfo.playerList.get(i);

                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(pos);

                if (stUserGameInfo.getStUserInfo().getUuid() > 0) {
                    if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CALL
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_CHECK
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_RAISE
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY) {
                        if (!winnerUuidSet.contains(winuuid)) {
                            winnerIDSet.add(pos);
                            winnerIDArray.add(pos);
                            winnerUuidSet.add(winuuid);
                            winnerUuidArray.add(winuuid);
                        }
                    } else if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        if (mHandPos.get(pos) != null) {
                            if (!allFoldUuidSet.contains(mHandPos.get(pos))) {
                                allFoldUuids.add(mHandPos.get(pos));
                                allFoldPos.add(pos);

                                allFoldUuidSet.add(mHandPos.get(pos));
                                allFoldPosSet.add(pos);
                            }
                        }
                    }
                } else {
                    if (mHandPos.get(pos) != null) {
                        if (!allFoldUuidSet.contains(mHandPos.get(pos))) {
                            allFoldUuids.add(mHandPos.get(pos));
                            allFoldPos.add(pos);

                            allFoldUuidSet.add(mHandPos.get(pos));
                            allFoldPosSet.add(pos);
                        }
                    }
                }
            }

            //本池子未获胜玩家
            List<Long> failUuidArray = new ArrayList<Long>();
            for (Long uuid : stPotInfo.playerList){
                if (!failUuidArray.contains(uuid) && !winnerUuidArray.contains(uuid)){
                    failUuidArray.add(uuid);
                }
            }

            //本池子所有弃牌玩家
            List<Long> flopUuidArray = new ArrayList<Long>();
            for (Long uuid : stPotInfo.playerList){
                if (!flopUuidArray.contains(uuid) && !allFoldUuids.contains(uuid)){
                    flopUuidArray.add(uuid);
                }
            }

            if (winnerIDArray.size() != 0) {
                GamePotResultInfo.Builder stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();

                long winStacks = 0l;

                //授信牌局 底池抽水且池子有抽水 暂时先屏蔽这种处理
                if (bCredit && iDrawType == 2 && bFlopStatus && stPotInfo.playerList.size() > 1 && false){
                    winStacks = (stGameRoomInfo.getVCurrentPots(index) - userDrawStacks.get(index)) / winnerIDArray.size();

                    //记录玩家原始贡献抽水
                    int j = 1; //记录玩家数
                    long drawOriginStacks = userDrawStacks.get(index) / winnerIDArray.size(); //记录玩家原始贡献抽水值
                    long drawOriginLeftStacks = userDrawStacks.get(index) % winnerIDArray.size(); //记录剩余玩家原始贡献抽水值

                    //获胜玩家原始贡献的抽水
                    for (Long winUuid : winnerUuidArray){
                        if (mUserOriginDrawStacks.get(winUuid) == null){
                            if (j == winnerUuidArray.size()){
                                mUserOriginDrawStacks.put(winUuid, drawOriginStacks + drawOriginLeftStacks);
                            } else {
                                mUserOriginDrawStacks.put(winUuid, drawOriginStacks);
                            }
                        } else {
                            if (j == winnerUuidArray.size()){
                                mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks + drawOriginLeftStacks);
                            } else {
                                mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks);
                            }
                        }

                        j++;
                    }

                    //记录玩家贡献的抽水
                    int i = 1; //记录玩家数
                    long drawStacks = userDrawStacks.get(index) / (winnerIDArray.size() + failUuidArray.size()); //记录玩家贡献抽水值
                    long drawLeftStacks = userDrawStacks.get(index) % (winnerIDArray.size() + failUuidArray.size()); //记录剩余玩家贡献抽水值

                    //获胜玩家贡献的抽水
                    for (Long winUuid : winnerUuidArray){
                        //记录抽水日志
                        StringBuilder drawLog = new StringBuilder("")
                                .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                .append(" ").append(index) //底池编号
                                .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                .append(" ").append(winnerIDArray.size()) //该底池获胜人数
                                .append(" ").append(winnerIDArray.size() + failUuidArray.size()) //该底池贡献抽水的所有玩家数
                                .append(" ").append(1) //玩家类型 1 获胜玩家 2 未获胜玩家
                                .append(" ").append(winUuid); //玩家uuid

                        if (mUserDrawStacks.get(winUuid) == null){
                            if (i == winnerUuidArray.size() + failUuidArray.size()){
                                mUserDrawStacks.put(winUuid, drawStacks + drawLeftStacks);
                                drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                            } else {
                                mUserDrawStacks.put(winUuid, drawStacks);
                                drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                            }
                        } else {
                            if (i == winnerUuidArray.size() + failUuidArray.size()){
                                mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks + drawLeftStacks);
                                drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                            } else {
                                mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks);
                                drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                            }
                        }

                        drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                                .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                                .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                                .append(" ").append(iUserNum); //实际参人数
                        PotDrawLogService.getInstance().error(drawLog.toString());

                        i++;
                    }

                    //未获胜玩家贡献的抽水
                    if (!CollectionUtils.isEmpty(failUuidArray)){
                        for (Long failUuid : failUuidArray){
                            //记录抽水日志
                            StringBuilder drawLog = new StringBuilder("")
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                    .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                    .append(" ").append(index) //底池编号
                                    .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                    .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                    .append(" ").append(winnerIDArray.size()) //该底池获胜人数
                                    .append(" ").append(winnerIDArray.size() + failUuidArray.size()) //该底池贡献抽水的所有玩家数
                                    .append(" ").append(2) //玩家类型 1 获胜玩家 2 未获胜玩家
                                    .append(" ").append(failUuid); //玩家uuid

                            if (mUserDrawStacks.get(failUuid) == null){
                                if (i == winnerUuidArray.size() + failUuidArray.size()){
                                    mUserDrawStacks.put(failUuid, drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(failUuid, drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                }
                            } else {
                                if (i == winnerUuidArray.size() + failUuidArray.size()){
                                    mUserDrawStacks.put(failUuid, mUserDrawStacks.get(failUuid) + drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(failUuid, mUserDrawStacks.get(failUuid) + drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                }
                            }

                            drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                                    .append(" ").append(iUserNum); //实际参人数
                            PotDrawLogService.getInstance().error(drawLog.toString());

                            i++;
                        }
                    }

                } else {
                    //弃牌模式下其实就是1个底池 底池减去总抽水值
                    winStacks = (stGameRoomInfo.getVCurrentPots(index) - totalDrawStacks) / winnerIDArray.size();
                }

                lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);
                for (int i = 0; i < winnerIDArray.size(); i++) {
                    int userPos = winnerIDArray.get(i);
                    long winUuid = winnerUuidArray.get(i);
                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        if (j == userPos) {
                            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();

                            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid() == winUuid) {
                                stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
                                stGamePotResultInfoBuilder.addLUserPots(winStacks / winnerIDArray.size());

                                stGameRoomInfo = stGameRoomInfoBuilder.build();

                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() + winStacks);

                                stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());
                            } else {
                                UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
                                if (stUserStandByGameInfoBuilder != null) {
                                    stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
                                    stGamePotResultInfoBuilder.addLUserPots(winStacks / winnerIDArray.size());

                                    stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks() + winStacks);

                                    mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);

                                    setBillCurrentStacksInRoom(winUuid,
                                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            , stUserStandByGameInfoBuilder.getLRemainStacks(), stUserStandByGameInfoBuilder.getLUnloadStacks());
                                }
                            }
                        }
                    }
                }
                stGameResultInfoBuilder.addStGamePotResultInfo(stGamePotResultInfoBuilder);
            } else {
                if (allFoldPos.size() > 0) {

                    GamePotResultInfo.Builder stGamePotResultInfoBuilder = GamePotResultInfo.newBuilder();

                    long winStacks = 0l;

                    //授信牌局 底池抽水且池子有抽水 暂时先屏蔽这种方式
                    if (bCredit && iDrawType == 2 && bFlopStatus && stPotInfo.playerList.size() > 1 && false){
                        winStacks = (stGameRoomInfo.getVCurrentPots(index) - userDrawStacks.get(index)) / allFoldPos.size();

                        //记录玩家原始贡献抽水
                        int j = 1; //记录玩家数
                        long drawOriginStacks = userDrawStacks.get(index) / allFoldPos.size(); //记录玩家原始贡献抽水值
                        long drawOriginLeftStacks = userDrawStacks.get(index) % allFoldPos.size(); //记录剩余玩家原始贡献抽水值

                        //获胜玩家原始贡献的抽水
                        for (Long winUuid : allFoldUuids){
                            if (mUserOriginDrawStacks.get(winUuid) == null){
                                if (j == allFoldUuids.size()){
                                    mUserOriginDrawStacks.put(winUuid, drawOriginStacks + drawOriginLeftStacks);
                                } else {
                                    mUserOriginDrawStacks.put(winUuid, drawOriginStacks);
                                }
                            } else {
                                if (j == allFoldUuids.size()){
                                    mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks + drawOriginLeftStacks);
                                } else {
                                    mUserOriginDrawStacks.put(winUuid, mUserOriginDrawStacks.get(winUuid) + drawOriginStacks);
                                }
                            }

                            j++;
                        }

                        //记录玩家贡献的抽水
                        int i = 1; //记录玩家数
                        long drawStacks = userDrawStacks.get(index) / (allFoldPos.size() + flopUuidArray.size()); //记录玩家贡献抽水值
                        long drawLeftStacks = userDrawStacks.get(index) % (allFoldPos.size() + flopUuidArray.size()); //记录剩余玩家贡献抽水值

                        //获胜玩家贡献的抽水
                        for (Long winUuid : allFoldUuids){
                            //记录抽水日志
                            StringBuilder drawLog = new StringBuilder("")
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                    .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                    .append(" ").append(index) //底池编号
                                    .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                    .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                    .append(" ").append(allFoldPos.size()) //该底池获胜人数
                                    .append(" ").append(allFoldPos.size() + flopUuidArray.size()) //该底池贡献抽水的所有玩家数
                                    .append(" ").append(1) //玩家类型 1 获胜玩家 2 未获胜玩家
                                    .append(" ").append(winUuid); //玩家uuid

                            if (mUserDrawStacks.get(winUuid) == null){
                                if (i == allFoldUuids.size() + flopUuidArray.size()){
                                    mUserDrawStacks.put(winUuid, drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(winUuid, drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                }
                            } else {
                                if (i == allFoldUuids.size() + flopUuidArray.size()){
                                    mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks + drawLeftStacks);
                                    drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                } else {
                                    mUserDrawStacks.put(winUuid, mUserDrawStacks.get(winUuid) + drawStacks);
                                    drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                }
                            }

                            drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                                    .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                                    .append(" ").append(iUserNum); //实际参人数
                            PotDrawLogService.getInstance().error(drawLog.toString());

                            i++;
                        }

                        //未获胜玩家贡献抽水
                        if (!CollectionUtils.isEmpty(flopUuidArray)){
                            for (Long flopUuid : flopUuidArray){
                                //记录抽水日志
                                StringBuilder drawLog = new StringBuilder("")
                                        .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()) //牌局id
                                        .append(" ").append(stGameRoomInfo.getLGameHandID()) //手数
                                        .append(" ").append(index) //底池编号
                                        .append(" ").append(stGameRoomInfo.getVCurrentPots(index)) //该底池大小
                                        .append(" ").append(userDrawStacks.get(index)) //该底池总抽水值
                                        .append(" ").append(allFoldPos.size()) //该底池获胜人数
                                        .append(" ").append(allFoldPos.size() + flopUuidArray.size()) //该底池贡献抽水的所有玩家数
                                        .append(" ").append(2) //玩家类型 1 获胜玩家 2 未获胜玩家
                                        .append(" ").append(flopUuid); //玩家uuid

                                if (mUserDrawStacks.get(flopUuid) == null){
                                    if (i == allFoldUuids.size() + flopUuidArray.size()){
                                        mUserDrawStacks.put(flopUuid, drawStacks + drawLeftStacks);
                                        drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                    } else {
                                        mUserDrawStacks.put(flopUuid, drawStacks);
                                        drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                    }
                                } else {
                                    if (i == allFoldUuids.size() + flopUuidArray.size()){
                                        mUserDrawStacks.put(flopUuid, mUserDrawStacks.get(flopUuid) + drawStacks + drawLeftStacks);
                                        drawLog.append(" ").append(drawStacks + drawLeftStacks); //玩家本底池贡献抽水
                                    } else {
                                        mUserDrawStacks.put(flopUuid, mUserDrawStacks.get(flopUuid) + drawStacks);
                                        drawLog.append(" ").append(drawStacks); //玩家本底池贡献抽水
                                    }
                                }

                                drawLog.append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getIPotDrawFee()) //抽水比例 *10000倍
                                        .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getILimitBB()) //抽水上限 *100倍
                                        .append(" ").append(stGameRoomInfo.getStGameRoomBaseInfo().getBChangeByNum()) //是否影响人数上限
                                        .append(" ").append(iUserNum); //实际参人数
                                PotDrawLogService.getInstance().error(drawLog.toString());

                                i++;
                            }
                        }

                    } else {
                        winStacks = (stGameRoomInfo.getVCurrentPots(index) - totalDrawStacks) / allFoldPos.size();
                    }

                    lSingleMaxPot += stGameRoomInfo.getVCurrentPots(index);

                    for (int i = 0; i < allFoldPos.size(); i++) {

                        int pos = allFoldPos.get(i);
                        long winUuid = 0;

                        winUuid = allFoldUuids.get(i);
                        if (pos >= 0) {
                            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(pos).toBuilder();
                            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getStUserInfo().getUuid() == winUuid) {
                                stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
                                stGamePotResultInfoBuilder.addLUserPots(winStacks);

                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() + winStacks);

                                stGameRoomInfoBuilder.setVUserGameInfos(pos, stUserGameInfoBuilder.build());
                            } else {
//								boolean bSettled = false;
                                UserGameInfoNet.Builder stUserStandByGameInfoBuilder = mapStandByUserGameInfos.get(winUuid);
                                if (stUserStandByGameInfoBuilder != null) {
                                    stGamePotResultInfoBuilder.addStUserWiner(stUserStandByGameInfoBuilder.getStUserInfo());
                                    stGamePotResultInfoBuilder.addLUserPots(winStacks);

                                    stUserStandByGameInfoBuilder.setLRemainStacks(stUserStandByGameInfoBuilder.getLRemainStacks() + winStacks);

                                    mapStandByUserGameInfos.put(winUuid, stUserStandByGameInfoBuilder);

                                    setBillCurrentStacksInRoom(winUuid,
                                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            , stUserStandByGameInfoBuilder.getLRemainStacks(), stUserStandByGameInfoBuilder.getLUnloadStacks());
//									bSettled=true;
                                }
//								if(bSettled == false){
                                for (int chgangeIndex = 0; chgangeIndex < stGameRoomInfo.getVUserGameInfosCount(); chgangeIndex++) {

                                    UserGameInfoNet.Builder stUserGameInfoBuilderChange = stGameRoomInfoBuilder.getVUserGameInfos(chgangeIndex).toBuilder();

                                    if (stUserGameInfoBuilderChange != null && stUserGameInfoBuilderChange.getStUserInfo().getUuid() == winUuid) {
                                        stGamePotResultInfoBuilder.addStUserWiner(stUserGameInfoBuilder.getStUserInfo());
                                        stGamePotResultInfoBuilder.addLUserPots(winStacks);

                                        stUserGameInfoBuilderChange.setLRemainStacks(stUserGameInfoBuilderChange.getLRemainStacks() + winStacks);

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

        if (lSingleMaxPot > lMaxPot) {
            lMaxPot = lSingleMaxPot;
        }


        for (int i = 0; i < vOriginalStacks.size(); i++) {
            long stack = vOriginalStacks.get(i);
            stGameResultInfoBuilder.addVlOriginalUserStacks(stack);
        }
        stGameRoomInfoBuilder.setStGameResultInfo(stGameResultInfoBuilder);


        Map<Long, Long> mapWinStacks = new ConcurrentHashMap<Long, Long>();
        for (GamePotResultInfo stGamePotResultInfo : stGameResultInfoBuilder.getStGamePotResultInfoList()) {
            for (int p = 0; p < stGamePotResultInfo.getStUserWinerCount(); p++) {
                long uuid = stGamePotResultInfo.getStUserWiner(p).getUuid();
                long winStacks = stGamePotResultInfo.getLUserPots(p);

                Long stacks = mapWinStacks.get(uuid);
                if (stacks == null) {
                    mapWinStacks.put(uuid, winStacks);
                } else {
                    mapWinStacks.put(uuid, stacks + winStacks);
                }
            }
        }

        long potStacks = 0l;

        //记录获胜玩家uuid
        List<Long> winUuidList = new ArrayList<Long>();

        //���������������������������������������
        Iterator iter = mapWinStacks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Long uuid = (Long) entry.getKey();
            Long winStacks = (Long) entry.getValue();
            potStacks = potStacks + winStacks;
            winUuidList.add(uuid);

            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                    + "," + stGameRoomInfo.getLGameHandID()
                    + "," + uuid
                    + "," + "showdown"
                    + "," + "noshow"
                    + "," + winStacks);

            stGameRoomInfo = stGameRoomInfoBuilder.build();
        }

        long diffStacks = 0l;

        //检测本手战绩平不平
        if (iDrawType != 2 && potStacks != getUserTotalBetStacks()){

            diffStacks = getUserTotalBetStacks() - potStacks;

            //本手战绩不平数据
            RecordLogService.getInstance().error("roomid: " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " handid: " + stGameRoomInfo.getLGameHandID() + " " + stGameRoomInfo);
        }

        for (long uuid : vHandList) {
            HandInfo stHandInfo = mHandMap.get(uuid);
            if (stHandInfo != null) {
                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                        + "," + stGameRoomInfo.getLGameHandID()
                        + "," + uuid
                        + "," + "hand"
                        + "," + stHandInfo.getvCards().get(0)
                        + "," + stHandInfo.getvCards().get(1)
                        + "," + stHandInfo.getlRemainStacks());
            }
        }

        long diffUuid = 0l;
        if (diffStacks != 0){
            if (!CollectionUtils.isEmpty(winUuidList) && winUuidList.size() == 1){
                diffUuid = winUuidList.get(0);
            }
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
        for (int k = 0; k < iGameRoomUserMaxNums; k++) {
            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder
                    .getVUserGameInfos(k)
                    .toBuilder();

            if (diffUuid != 0 && diffUuid == stResetUserGameInfoBuilder.getStUserInfo().getUuid()){
                stResetUserGameInfoBuilder.setLRemainStacks(stResetUserGameInfoBuilder.getLRemainStacks() + diffStacks);
            }

            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                UserBaseInfoNet.Builder stUserBaseInfo = stResetUserGameInfoBuilder.getStUserInfoBuilder();
                stUserBaseInfo.setUuid(0);
                stResetUserGameInfoBuilder.setStUserInfo(stUserBaseInfo);
            }

            stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
            stResetUserGameInfoBuilder.setLBetStacks(0);
            stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);

        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
//		stGameRoomInfo = InsuranceApi.getInsuranceBill(stGameRoomInfo,stGameRoomInfo.getLGameHandID());
        ArrayList<Long> vSitdownUsers = new ArrayList<Long>();
        //设置结果,实时账单信息
        for (int index = 0; index < stGameRoomInfo.getVUserGameInfosCount(); index++) {
            UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(index);
            if (stUserGameInfo.getStUserInfo().getUuid() > 0) {
                System.out.println("结账113 : " + stUserGameInfo.getStUserInfo().getUuid());
                //设置账单
                setBillCurrentStacksInRoom(stUserGameInfo.getStUserInfo().getUuid(),
                        stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                        , stUserGameInfo.getLRemainStacks(), stUserGameInfo.getLUnloadStacks());
                vSitdownUsers.add(stUserGameInfo.getStUserInfo().getUuid());
            }
        }

        Iterator iter1 = mapStandByUserGameInfos.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry entry = (Map.Entry) iter1.next();
            UserGameInfoNet.Builder stUserGameInfoBuilder = (UserGameInfoNet.Builder) entry.getValue();
            if (stUserGameInfoBuilder != null) {
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && !vSitdownUsers.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                    System.out.println("结账114 : " + stUserGameInfoBuilder.getStUserInfo().getUuid());
                    //设置账单
                    setBillCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            , stUserGameInfoBuilder.getLRemainStacks(), stUserGameInfoBuilder.getLUnloadStacks());
                }
            }
        }

        bFlodResult = true;
    }

    @Override
    public boolean getGameStarted() {
        return bStarted;
    }

    @Override
    public void saveOriginalStacks() {
        vOriginalStacks.clear();

        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
            vOriginalStacks.add(stUserGameInfoNet.getLRemainStacks());
            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                mOriginalStacks.put(stUserGameInfoNet.getStUserInfo().getUuid(), stUserGameInfoNet.getLRemainStacks());
            }
        }
    }

    @Override
    public ERROR_CODE_TYPE showcard(long lHandID, long uuid, int bShowCardOne, int bShowCardTwo) {
        synchronized (this) {
            UserBaseInfoNet.Builder stUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

            if (stUserBaseInfoBuilder != null) {
                if (stGameRoomInfo != null) {
                    //亮牌的状态位
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                    if (stGameRoomInfoBuilder.getLGameHandID() == lHandID) {
                        for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {

                            UserGameInfoNet stUserGameInfoNet = stGameRoomInfoBuilder.getVUserGameInfos(i);

                            if (stUserGameInfoNet.getStUserInfo().getUuid() == uuid && stUserGameInfoNet.getVCurrentHandsCount() > 1) {
                                //如果是该用户，则设置亮牌状态
                                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                                stUserGameInfoNetBuilder.setBShowCardOne(bShowCardOne);
                                stUserGameInfoNetBuilder.setBShowCardTwo(bShowCardTwo);
                                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);

                                String sShowCardLog = "";
                                if (bShowCardOne != 0) {
                                    sShowCardLog += stUserGameInfoNetBuilder.getVCurrentHands(0);
                                    sShowCardLog += ",";
                                }

                                if (bShowCardTwo != 0) {
                                    sShowCardLog += stUserGameInfoNetBuilder.getVCurrentHands(1);
                                }
                                if (sShowCardLog.length() == 0) {
                                    sShowCardLog = "none";
                                }
                                TableLogService.getInstance().error(lFromRoomID
                                        + "," + lGameRoomID
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoNetBuilder.getStUserInfo().getUuid()
                                        + "," + "showcard"
                                        + "," + sShowCardLog);
                            }
                        }
                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    } else {
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                    }
                } else {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
                }
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
            }
        }
    }

    @Override
    public ERROR_CODE_TYPE useItem(long uuid, long toUuid, long lMsgID, int itemID, String sExtra) {
        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
        if (stUserBaseInfoNetBuilder != null) {
            long lCheckPopularity = 10;

            if (sExtra.equals("animateItem#item15") || sExtra.equals("animateItem#item12")) {
                lCheckPopularity = 40;
            }

            switch (itemID) {
                case 10010:
                    if (UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), lCheckPopularity)) {
                        //组织push包
                        UseItemPushMsg.Builder stUseItemPushMsgBuilder = UseItemPushMsg.newBuilder();
                        stUseItemPushMsgBuilder.setLItemID(itemID);
                        stUseItemPushMsgBuilder.setLToUuid(toUuid);
                        stUseItemPushMsgBuilder.setLMsgID(System.currentTimeMillis());
                        stUseItemPushMsgBuilder.setLFromUuid(uuid);
                        stUseItemPushMsgBuilder.setLRoomID(lGameRoomID);
                        stUseItemPushMsgBuilder.setSExtra(sExtra);

                        stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -lCheckPopularity);
                        TranscationLogService.getInstance().error(stUserBaseInfoNetBuilder.getUuid() + " " + lGameRoomID + " " + 430001 + " " + sExtra + " 0 " + "0 " + lCheckPopularity + " " + stUserBaseInfoNetBuilder.getLPopularity());

                        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != stUserBaseInfoNetBuilder.getUuid()) {
                                GameRoomMsgManager.getInstance().sendUseItemMsg(stUserGameInfoNet.getStUserInfo().getUuid(), stUseItemPushMsgBuilder.build());
                            }
                        }

                        for (long standUuid : getStandByUuids()) {
                            UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(standUuid).build();

                            if (stUserBaseInfo != null && stUserBaseInfo.getUuid() > 0 && stUserBaseInfo.getUuid() != stUserBaseInfoNetBuilder.getUuid()) {
                                GameRoomMsgManager.getInstance().sendUseItemMsg(stUserBaseInfo.getUuid(), stUseItemPushMsgBuilder.build());
                            }
                        }
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    } else {
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
                    }
                case 20001:
                    if (UserManager.getInstance().checkUserCoin(stUserBaseInfoNetBuilder.getUuid(), 6)) {
                        //组织push包
                        UseItemPushMsg.Builder stUseItemPushMsgBuilder = UseItemPushMsg.newBuilder();
                        stUseItemPushMsgBuilder.setLItemID(itemID);
                        stUseItemPushMsgBuilder.setLToUuid(toUuid);
                        stUseItemPushMsgBuilder.setLMsgID(System.currentTimeMillis());
                        stUseItemPushMsgBuilder.setLFromUuid(uuid);
                        stUseItemPushMsgBuilder.setLRoomID(lGameRoomID);
                        stUseItemPushMsgBuilder.setSExtra(sExtra);

                        stUserBaseInfoNetBuilder = UserManager.getInstance().addUserCoin(stUserBaseInfoNetBuilder.getUuid(), -6);

                        //打印日志
                        TranscationLogService.getInstance().error(stUserBaseInfoNetBuilder.getUuid() + " " + 0 + " " + 430002 + " " + sExtra + " 0 " + "0 6 " + stUserBaseInfoNetBuilder.getLCoin());

                        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != stUserBaseInfoNetBuilder.getUuid()) {
                                GameRoomMsgManager.getInstance().sendUseItemMsg(stUserGameInfoNet.getStUserInfo().getUuid(), stUseItemPushMsgBuilder.build());
                            }
                        }

                        for (long standUuid : getStandByUuids()) {
                            UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(standUuid).build();

                            if (stUserBaseInfo != null && stUserBaseInfo.getUuid() > 0 && stUserBaseInfo.getUuid() != stUserBaseInfoNetBuilder.getUuid()) {
                                GameRoomMsgManager.getInstance().sendUseItemMsg(stUserBaseInfo.getUuid(), stUseItemPushMsgBuilder.build());
                            }
                        }
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    } else {
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN;
                    }
            }
        } else {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
        }
        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
    }

    public void setStandByUserGameInfos(long uuid, UserGameInfoNet.Builder stUserGameInfoNetBuilder) {
        mapStandByUserGameInfos.put(uuid, stUserGameInfoNetBuilder);
    }

    public UserGameInfoNet.Builder getStandByUserGameInfos(long uuid) {
        return mapStandByUserGameInfos.get(uuid);
    }

    public void removeStandByUserGameInfos(long uuid) {
        mapStandByUserGameInfos.remove(uuid);
    }

    public int getiGameRoomUserMaxNums() {
        return iGameRoomUserMaxNums;
    }

    public void setiGameRoomUserMaxNums(int iGameRoomUserMaxNums) {
        this.iGameRoomUserMaxNums = iGameRoomUserMaxNums;
    }

    //代理操作
    public void userDelegate(long uuid) {
        synchronized (this) {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

                //找到离开房间的用户
                if (stUserGameInfo.getStUserInfo().getUuid() == uuid) {
                    //获取牌局对象
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                    //获取托管用户信息
                    TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();

                    //设置为托管状态
                    userGameInfoBuilder.setBTemporarilyLeave(0);
                    userGameInfoBuilder.setLTemporarilyLeaveTime(0);
                    mTemporarilyLeaveUsersTimeout.removeKey(uuid);
                    mTemporarilyLeaveUsers.remove(uuid);
                    userGameInfoBuilder.setBDelegate(1);
                    userGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                    //将用户设置回房间
                    stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);

                    //通知保险用户进入代理状态
                    InsuranceApi.doGameDelegateOpen(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    userGameInfoBuilder.clearVCurrentHands();

                    //给牌桌上其他用户push托管信息
                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);

                        if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                            pushGameRoomUserMsgs(userGameInfoBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELEGATE, i);
                        }
                    }

                    //给旁观的人发送托管消息
                    for (long push_uuid : mStandBys) {
                        pushGameRoomUserMsgs(userGameInfoBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELEGATE, i);
                    }
                }
            }
        }
    }

    @Override
    public void pushGameRoomUserMsgsResetThinkingInterval(UserGameInfoNet stFromUserGamenfo,
                                                          long stToUserUuid, int iThinkingInterval) {
        //给被邀请用户发通知
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();

        //获取时间
        long curTime = System.currentTimeMillis();

        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);

        // 设置发送人的userbaseinfo
        if (stFromUserGamenfo != null) {
            stGameRoomPushMsgBuilder.setStFromUser(stFromUserGamenfo);
        }

        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");

        stGameRoomPushMsgBuilder.setIPos(0);

        // 设置 好友类型
        stGameRoomPushMsgBuilder.setEGameMessageType(GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESET_THINKING_INTERVAL);

        stGameRoomPushMsgBuilder.setIThinkingInterval(iThinkingInterval);

        checkAllAllin();

        if (stGameRoomInfo != null) {
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
//                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
//                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {
//
//                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
//                        continue;
//                    }
//
//                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
//                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
//                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
//                            continue;
//                        }
//
//                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
//                            stUserGameInfoNetBuilder.clearVCurrentHands();
//                        } else {
//                            if (bFlodResult == true) {
//                                stUserGameInfoNetBuilder.clearVCurrentHands();
//                            }
//                        }
//                    } else {
//                        stUserGameInfoNetBuilder.clearVCurrentHands();
//                    }
//                }
//                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
//            }

            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }

        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
                .build();

//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    //取消代理
    public void userReturnGame(long uuid) {
        synchronized (this) {
            for (int i = 0; i < this.stGameRoomInfo.getVUserGameInfosCount(); i++) {
                //获取炮狙用户信息
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                //找到要取消托管状态的用户
                if (stUserGameInfoNet != null
                        && stUserGameInfoNet.getStUserInfo().getUuid() > 0
                        && stUserGameInfoNet.getStUserInfo().getUuid() == uuid
                        && stUserGameInfoNet.getBDelegate() == 1) {
                    //重新设置用户的托管信息
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();

                    //通知保险用户进入代理状态
                    InsuranceApi.doGameDelegateClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                    //解除用户代理状态
                    stUserGameInfoNetBuilder.setBDelegate(0);
                    stUserGameInfoNetBuilder.setBTemporarilyLeave(0);
                    stUserGameInfoNetBuilder.setLTemporarilyLeaveTime(0);
                    mTemporarilyLeaveUsersTimeout.removeKey(uuid);
                    mTemporarilyLeaveUsers.remove(uuid);
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    stUserGameInfoNetBuilder.clearVCurrentHands();

                    //给牌桌上的用户发送返回牌桌的消息
                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);

                        if (stUserGameInfoNetIn.getStUserInfo().getUuid() > 0) {
                            pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), stUserGameInfoNetIn.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RETURNGAME, i);
                        }
                    }
                    //给旁观的人发返回牌桌消息
                    for (long push_uuid : mStandBys) {
                        pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RETURNGAME, i);
                    }
                    break;
                }
            }
        }
    }

    //留座离桌操作
    public long userTemporarilyLeave(long uuid) {
        synchronized (this) {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

                //找到离开房间的用户
                if (stUserGameInfo.getStUserInfo().getUuid() == uuid) {


                    if (stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                            || stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_STANDBY) {

                        //生成留座离桌时间
                        long lLeaveTime = System.currentTimeMillis();
                        //获取牌局对象
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                        //获取托管用户信息
                        TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();

                        //设置为托管状态
                        userGameInfoBuilder.setBTemporarilyLeave(1);
                        userGameInfoBuilder.setLTemporarilyLeaveTime(lLeaveTime);
                        userGameInfoBuilder.setBDelegate(0);
                        userGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                        //声纹信息
                        if (bVoice && mUserIsCheck.containsKey(uuid) && mUserIsCheck.get(uuid) == true){

                            long verifyStartTime = System.currentTimeMillis();
                            userGameInfoBuilder.setLVerificationStartTime(verifyStartTime);
                            userGameInfoBuilder.setLVerificationContinueTime(ConfigaturationManager.verifyTime);
                            userGameInfoBuilder.setPhrase(VoiceManager.getInstance().getOnePhrase(uuid));
                            userGameInfoBuilder.setLVoiceVerificationStatus(3);

                            List<Long> uuidList = new ArrayList<Long>();

                            for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);
                                if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid) {
                                    uuidList.add(stUserGameInfoNet.getStUserInfo().getUuid());
                                }
                            }

                            //玩家验证信息同步声纹服务
                            VoiceManager.getInstance().receiveCurrentTime(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),uuid,verifyStartTime,ConfigaturationManager.verifyTime,userGameInfoBuilder.getLBuyinClubID(),uuidList);

                            mUserIsCheck.put(uuid,false);

                            mUserVerifiyTime.put(uuid,verifyStartTime);

                            mUserVerifiying.put(uuid, true);
                        }

                        //将用户设置回房间
                        stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);

                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        userGameInfoBuilder.clearVCurrentHands();

                        //给牌桌上其他用户push托管信息
                        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);

                            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                                pushGameRoomUserMsgs(userGameInfoBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                        GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                            }
                        }

                        //给旁观的人发送托管消息
                        for (long push_uuid : mStandBys) {
                            pushGameRoomUserMsgs(userGameInfoBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                        }
                        mTemporarilyLeaveUsersTimeout.setCurrent(uuid, uuid);
                        return lLeaveTime;
                    } else if (iCouldPlayNum == 1 && stUserGameInfo.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY) {
                        //生成留座离桌时间
                        long lLeaveTime = System.currentTimeMillis();
                        //获取牌局对象
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                        //获取托管用户信息
                        TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();

                        //设置为托管状态
                        userGameInfoBuilder.setBTemporarilyLeave(1);
                        userGameInfoBuilder.setLTemporarilyLeaveTime(lLeaveTime);
                        userGameInfoBuilder.setBDelegate(0);
                        userGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                        //声纹信息
                        if (bVoice && mUserIsCheck.containsKey(uuid) && mUserIsCheck.get(uuid) == true){

                            long verifyStartTime = System.currentTimeMillis();
                            userGameInfoBuilder.setLVerificationStartTime(verifyStartTime);
                            userGameInfoBuilder.setLVerificationContinueTime(ConfigaturationManager.verifyTime);
                            userGameInfoBuilder.setPhrase(VoiceManager.getInstance().getOnePhrase(uuid));
                            userGameInfoBuilder.setLVoiceVerificationStatus(3);

                            List<Long> uuidList = new ArrayList<Long>();

                            for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);
                                if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid) {
                                    uuidList.add(stUserGameInfoNet.getStUserInfo().getUuid());
                                }
                            }

                            //玩家验证信息同步声纹服务
                            VoiceManager.getInstance().receiveCurrentTime(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),uuid,verifyStartTime,ConfigaturationManager.verifyTime,userGameInfoBuilder.getLBuyinClubID(),uuidList);

                            mUserIsCheck.put(uuid,false);

                            mUserVerifiyTime.put(uuid,verifyStartTime);

                            mUserVerifiying.put(uuid, true);
                        }

                        //将用户设置回房间
                        stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);

                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        userGameInfoBuilder.clearVCurrentHands();

                        //给牌桌上其他用户push托管信息
                        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);

                            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                                pushGameRoomUserMsgs(userGameInfoBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                        GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                            }
                        }

                        //给旁观的人发送托管消息
                        for (long push_uuid : mStandBys) {
                            pushGameRoomUserMsgs(userGameInfoBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                        }
                        mTemporarilyLeaveUsersTimeout.setCurrent(uuid, uuid);
                        return lLeaveTime;
                    } else {
                        //本手结束后变换状态
                        mTemporarilyLeaveUsers.put(uuid, uuid);
                        return 0;
                    }

                }
            }
            //说明用户不在牌桌上
            return -1;
        }
    }

    //取消留座离桌操作
    public boolean userReturnTemporarilyLeave(long uuid) {
        synchronized (this) {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

                //找到离开房间的用户
                if (stUserGameInfo.getStUserInfo().getUuid() == uuid) {
                    //获取牌局对象
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                    //获取托管用户信息
                    TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();

                    //设置为托管状态
                    userGameInfoBuilder.setBTemporarilyLeave(0);
                    userGameInfoBuilder.setLTemporarilyLeaveTime(0);
                    mTemporarilyLeaveUsersTimeout.removeKey(uuid);
                    mTemporarilyLeaveUsers.remove(uuid);

                    if (bVoice && userGameInfoBuilder.getLVoiceVerificationStatus() == 3){
                        userGameInfoBuilder.setLVoiceVerificationStatus(1);
                        userGameInfoBuilder.setLVerificationStartTime(0);
                    }

                    userGameInfoBuilder.setBDelegate(0);
                    //通知保险用户进入代理状态，留座离桌的bug
                    InsuranceApi.doGameDelegateClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                    if (stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1) {
                        userGameInfoBuilder.setBJustSit(1);
                    }

                    //将用户设置回房间
                    stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);

                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    userGameInfoBuilder.clearVCurrentHands();

                    //给牌桌上其他用户push托管信息
                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);

                        if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                            pushGameRoomUserMsgs(userGameInfoBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE_RETURN, i);
                        }
                    }

                    //给旁观的人发送托管消息
                    for (long push_uuid : mStandBys) {
                        pushGameRoomUserMsgs(userGameInfoBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE_RETURN, i);
                    }
                    mTemporarilyLeaveUsersTimeout.removeKey(uuid);
                    mTemporarilyLeaveUsers.remove(uuid);
                    return true;
                }

            }
            //说明用户不在牌桌上
            return false;
        }
    }

    /*
     * 伙牌嫌疑的站起，只发给站起本人
	 * (non-Javadoc)
	 * @see com.texaspoker.moment.room.GameRoomAbstractInterface#pushGameRoomUserMsgs(com.texaspoker.moment.TexasPoker.UserGameInfoNet, long, com.texaspoker.moment.TexasPoker.GAME_ROOM_MESSAGE_TYPE, int, int)
	 */
    @Override
    public void pushGameRoomUserMsgs(UserGameInfoNet stFromUserGamenfo, long stToUserUuid,
                                     GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType, int pos, int bCheat) {
        UserGameInfoNet secureUserGameInfoNet = getSecureUserGameInfoNet(stFromUserGamenfo, 2);
        //给被邀请用户发通知
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder();
        //标记伙牌嫌疑
        stGameRoomPushMsgBuilder.setBCheat(bCheat);
        //获取时间
        long curTime = System.currentTimeMillis();
        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);
        // 设置发送人的userbaseinfo
        if (secureUserGameInfoNet != null) {
            stGameRoomPushMsgBuilder.setStFromUser(secureUserGameInfoNet);
        }
        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");
        stGameRoomPushMsgBuilder.setIPos(pos);
        // 设置 好友类型
        stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);
        checkAllAllin();
        if (stGameRoomInfo != null) {
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
//            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
//                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
//                if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() != stToUserUuid) {
//                    if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN && bAllAllin == true) {
//                        continue;
//                    }
//                    if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result
//                            || (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD)) {
//                        if (stUserGameInfoNetBuilder.getBShowCardOne() == 1 || stUserGameInfoNetBuilder.getBShowCardTwo() == 1) {
//                            continue;
//                        }
//                        if (stUserGameInfoNetBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
//                            stUserGameInfoNetBuilder.clearVCurrentHands();
//                        } else {
//                            if (bFlodResult == true) {
//                                stUserGameInfoNetBuilder.clearVCurrentHands();
//                            }
//                        }
//                    } else {
//                        stUserGameInfoNetBuilder.clearVCurrentHands();
//                    }
//                }
//                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
//            }
            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }
        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder
                .build();
//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    public void pushUnloadStacksMsgs(long stToUserUuid,
                                     GAME_ROOM_MESSAGE_TYPE eGameRoomMessageType, long lUnloadStacks) {
        //push
        TexasPoker.GameRoomPushMsg.Builder stGameRoomPushMsgBuilder = TexasPoker.GameRoomPushMsg.newBuilder() ;

        //获取时间
        long curTime = System.currentTimeMillis();

        // 设置msg id
        stGameRoomPushMsgBuilder.setLMsgID(curTime);
        // 设置发送时间
        stGameRoomPushMsgBuilder.setLCreateTime(curTime);

        // 设置消息内容，暂时没有什么具体用处
        stGameRoomPushMsgBuilder.setSMsg("");

        stGameRoomPushMsgBuilder.setIPos(0);

        // 设置push类型
        stGameRoomPushMsgBuilder.setEGameMessageType(eGameRoomMessageType);
        stGameRoomPushMsgBuilder.setLUnloadStacks(lUnloadStacks);

        if(stGameRoomInfo!=null){
//            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            stGameRoomPushMsgBuilder.setStGameRoomInfo(dealPushGameRoom(stGameRoomInfo.toBuilder(),stToUserUuid));
        }

        TexasPoker.GameRoomPushMsg stGameRoomPushMsg = stGameRoomPushMsgBuilder.build();

//        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stToUserUuid);
        if (stToUserUuid > 0) {
            //发送push
            GameRoomMsgManager.getInstance().sendGameRoomMsg(stToUserUuid, stGameRoomPushMsg);
        }
    }

    /**
     * 手动设置触发声纹验证
     * @param uuid
     */
    public void verificationVoicePrint(long uuid){
        synchronized (this) {
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

            if (stGameRoomInfoBuilder.getStGameRoomBaseInfo().getBVoice() == 0){
                return;
            }

            for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                //在桌玩家
                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {

                    if (stResetUserGameInfoBuilder.getLVoiceVerificationStatus() == 3){
                        break;
                    }

                    //限制条件 1.本桌验证次数不超过2次 2.本天验证不超过5次
                    int verifyCount = mUserCheckCount.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()) == null ? 0 : mUserCheckCount.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid());

                    if (verifyCount >= ConfigaturationManager.verifyCount || VoiceManager.getInstance().getVerifyCount(stResetUserGameInfoBuilder.getStUserInfo().getUuid()) >= ConfigaturationManager.verifyTotalCount){
                        //此时不进行验证
                    } else {
                        //验证次数+1
                        if (mUserCheckCount.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()) == null){
                            mUserCheckCount.put(stResetUserGameInfoBuilder.getStUserInfo().getUuid(),1);
                        } else {
                            mUserCheckCount.put(stResetUserGameInfoBuilder.getStUserInfo().getUuid(),mUserCheckCount.get(stResetUserGameInfoBuilder.getStUserInfo().getUuid()) + 1);
                        }

                        //玩家本手需要检测记录
                        mUserIsCheck.put(stResetUserGameInfoBuilder.getStUserInfo().getUuid(),true);

                        //触发检测并留作离桌
                        userTemporarilyLeave(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    break;
                }
            }
        }
    }

    /**
     * 设置玩家声纹状态
     * @param uuid
     */
    public void setUserVoiceStatus(long uuid, int bPass){
        synchronized (this) {
            for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);

                if (stUserGameInfo.getStUserInfo().getUuid() == uuid){

                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                    //获取玩家信息
                    TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = stUserGameInfo.toBuilder();
                    userGameInfoBuilder.setLVoiceVerificationStatus(bPass + 1);
                    userGameInfoBuilder.setLVerificationStartTime(0);

                    mUserVerifiyTime.put(uuid,0l);
                    mUserVerifiying.put(uuid,false);

                    //将用户设置回房间
                    stGameRoomInfoBuilder.setVUserGameInfos(i, userGameInfoBuilder);

                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    //恢复留作离桌状态
                    userReturnTemporarilyLeave(uuid);

                    break;
                }
            }
        }
    }

    /**
     * 检测当前所有玩家是否都弃牌
     * @return
     */
    public boolean checkIsFoldAll(){
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                    && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_READY) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测是否还有betting玩家
     * @return
     */
    public boolean checkIsBetting(){
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测当前allin人数
     * @return
     */
    public int checkAllInNum(){
        int num = 0;
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN || stUserGameInfoBuilder.getLRemainStacks() == 0)) {
                num = num + 1;
            }
        }
        return num;
    }

    /**
     * 检测当前不是flod玩家 此时只能是一个玩家
     * @return
     */
    public long checkNotFlodUser(){
        long uuid = 0;
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 &&
                    (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                            && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD)) {
                uuid = stUserGameInfoBuilder.getStUserInfo().getUuid();
                break;
            }
        }
        return uuid;
    }

    /**
     * 获取所有allin玩家的uuid
     * @return
     */
    public List<Long> getAllInUuid(){
        List<Long> uuidList = new ArrayList<Long>();

        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            //获取所有牌局上的用户
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(j).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN || stUserGameInfoBuilder.getLRemainStacks() == 0) && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT) {
                uuidList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
            }
        }

        return uuidList;
    }

    /**
     * 获取场上最大下注玩家
     * @return
     */
    public List<Long> getMaxBetUuid(){
        List<Long> uuidList = new ArrayList<Long>();

        if (mSimUserBetStacks != null){
            for (Map.Entry<Long,Long> map : mSimUserBetStacks.entrySet()){
                if (lSimBetMaxStacks == map.getValue()){
                    uuidList.add(map.getKey());
                }
            }
        }

        return uuidList;
    }

    /**
     * 获取最后行动玩家
     * @return
     */
    public long getLastActionUuid(){
        //获取最大下注玩家
        List<Long> uuidList = getMaxBetUuid();
        long userid = 0l;
        //最大也就9人桌
        int posit = 10;

        if (uuidList.size() == 1){
            return uuidList.get(0);
        } else {
            for (Long uuid : uuidList){
                if (mSimUserPosit.get(uuid) < posit){
                    userid = uuid;
                    posit = mSimUserPosit.get(uuid);
                }
            }
            return userid;
        }
    }

    /**
     * 检测所有allin的玩家是否都是被迫allin的
     * @return
     */
    public Boolean checkAllBeForceAllin(){
        List<Long> uuidList = getAllInUuid();
        for (Long uuid : uuidList){
            if (mSimUserAllin.get(uuid) == null){
                return false;
            }
        }

        return true;
    }

    /**
     * 设置玩家下注筹码量
     * @param uuid
     * @param stacks
     */
    public void setSimUserBetStacks(long uuid, long stacks){
        //设置下注筹码量
        if (mSimUserBetStacks.get(uuid) == null){
            mSimUserBetStacks.put(uuid,stacks);
        } else {
            mSimUserBetStacks.put(uuid,mSimUserBetStacks.get(uuid) + stacks);
        }
        //设置下注最大值
        if (lSimBetMaxStacks < mSimUserBetStacks.get(uuid)){
            lSimBetMaxStacks = mSimUserBetStacks.get(uuid);
        }
    }

    /**
     * 获取本手玩家下注总筹码量
     * @return
     */
    public long getUserTotalBetStacks(){
        long totalBetStacks = 0l;

        if (mSimUserBetStacks != null){
            for (Map.Entry<Long,Long> map : mSimUserBetStacks.entrySet()){
                totalBetStacks = totalBetStacks + map.getValue();
            }
        }

        return totalBetStacks;
    }
}
