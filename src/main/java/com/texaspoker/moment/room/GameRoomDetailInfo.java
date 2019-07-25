package com.texaspoker.moment.room;

import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.AdvancedTimeoutHashMap;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.comm.LocationManager;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.credit.CreditManager;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.dubbo.game.insurance.InsuranceApi;
import com.texaspoker.floplotto.FlopLottoManager;
import com.texaspoker.lbs.LBSManager;
import com.texaspoker.league.LeagueManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.logger.PlayLogService;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.logger.TranscationLogService;
import com.texaspoker.moment.*;
import com.texaspoker.moment.TexasPoker.*;
import com.texaspoker.moment.TexasPokerCommon.*;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueInfo;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.user.UserManager;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_SITTED;

/**
 * @author afei
 */
public class GameRoomDetailInfo extends GameRoomAbstractInfo {
    private Map<Long, Long> mDelayPlayer = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mBuyinControlInfo = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mBuyinControlTimes = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mBuyinControlClubIDs = new ConcurrentHashMap<Long, Long>();

    private TimeoutHashMap<Long, Long> mBuyinControlTimeOut; //房间相关的数据

    private Map<Long, Long> mUserForceStandupUser = new ConcurrentHashMap<Long, Long>();

    private Map<Long, Long> mUserKickingUser = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mUserKickedUser = new ConcurrentHashMap<Long, Long>();

    private Map<Long, Long> mUserBuyinStacks = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mUserBuyinClubIDs = new ConcurrentHashMap<Long, Long>();
    private Map<Long, Long> mUserBuyining = new ConcurrentHashMap<Long, Long>();
    private Map<Long,Long> mUserUnload = new ConcurrentHashMap<Long,Long>();
    private Map<Long,Long> mUserWaitUnload = new ConcurrentHashMap<Long,Long>();

    //如果是联盟局，保存联盟信息
    private boolean bStartGamePush = false;

    //如果是联盟局，保存联盟信息
    private LeagueInfo stLeagueInfo;

    //重新设置的思考时间间隔
    int iThinkingInterval = 0;

    //是否已经结算用户数据结构
    private ConcurrentHashMap<Long, Long> mapUserNoCharge = new ConcurrentHashMap<Long, Long>();

    //结账保护
    public boolean bOver = false;

    //买入弹框倒计时map 首次坐下弹框不进入倒计时
    private TimeoutHashMap<Long, Long> mBuyinTimeOut;

    //入座玩家记录 辅助弹框倒计时用来判断是否为首次弹框
    private List<Long> sitDownUserList = new ArrayList<Long>();

    /****************************
     * 牌局操作函数
     *******************************/
    public ERROR_CODE_TYPE startGame(long uuid, int type) {
        synchronized (this) {
            if (stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid) {

                //赋初值
                bPause = false;
                bStarted = true;
                lFromRoomID = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                lGameRoomID = stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId();
                iGameRoomUserMaxNums = stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums();


                if (stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 0) {
                    TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                            + "," + "open"
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds() + "/" + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()
                            + "," + stGameRoomInfo.getEGameRoomType().getNumber()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());

                    if (bCredit) {
                        TableLogService.getInstance().error(lFromRoomID
                                + "," + lGameRoomID
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                                + "," + "opentype"
                                + "," + 1
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn());
                    } else {
                        TableLogService.getInstance().error(lFromRoomID
                                + "," + lGameRoomID
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                                + "," + "opentype"
                                + "," + 0
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn());
                    }

                    if (type == 1) {
                        //自动开局就打日志，尽量不要影响其他牌局
                        TableLogService.getInstance().error(lFromRoomID
                                + "," + lGameRoomID
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                                + "," + "autostart"
                                + "," + 0
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getIAutoStartNum());
                    }
                }

                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();

                stGameRoomBaseInfoBuilder.setBStarted(1);
                stGameRoomBaseInfoBuilder.setLStartTime(System.currentTimeMillis());
                stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);

                stGameRoomInfo = stGameRoomInfoBuilder.build();
                iGameRoomUserMaxNums = stGameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();

                mDealPos = iGameRoomUserMaxNums - 2;

                pushAppPushStartGame();
                pushAllGameRoomUserMsgs(null, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_START, stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_START_GAMEROOM;
            }
        }
    }

    /***
     * 站起操作
     *
     * @param uuid  要站起的用户
     * @param bAuto 是否为系统自动站起
     * @return
     */
    public ERROR_CODE_TYPE doStandUpAction(long uuid, boolean bAuto, int type) {
        synchronized (this) {

            //授信牌局,牌局结束玩家不能站起
            if (bCredit){
                if (bOver){
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }

            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                boolean bExist = false;
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                    if (stUserGameInfoBuilderTemp.getStUserInfo().getUuid() == uuid) {
                        if (stUserGameInfoBuilderTemp.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                                && stUserGameInfoBuilderTemp.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_READY) {
                            bExist = true;
                            break;
                        }
                    }
                }
                if (bExist) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
                }
            } else {
                if (bAuto == false && stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1) {
                    boolean bPlaying = true;
                    long lRemainStacks = 0;
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                        if (stUserGameInfoBuilderTemp.getStUserInfo().getUuid() == uuid) {
                            if (stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                                    || stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    || stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_STANDBY) {
                                bPlaying = false;
                                break;
                            } else {
                                lRemainStacks = stUserGameInfoBuilderTemp.getLRemainStacks();
                            }
                        }
                    }
                    if (bPlaying) {
                        if (checkAllAllin() == true)
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ALLOW_STANDUP;

                        if (lRemainStacks == 0)
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
            if (this.containUser(uuid)) {
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();

                    if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                        //获取用户副本
                        stUserMyGameInfoNetBuilder = stUserGameInfoBuilder.clone();

                        //保存弃牌状态
                        pos = i;

                        //设置牌局状态

                        removeUserPos(uuid);
                        addStandByUuid(uuid);

                        //通知保险用户进入代理状态
                        InsuranceApi.doGameDelegateClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                        //解除用户代理状态
                        stUserGameInfoBuilder.setBDelegate(0);
                        stUserGameInfoBuilder.setBTemporarilyLeave(0);
                        stUserGameInfoBuilder.setLTemporarilyLeaveTime(0);
                        mTemporarilyLeaveUsersTimeout.removeKey(uuid);
                        mTemporarilyLeaveUsers.remove(uuid);
                        stUserGameInfoBuilder.setBKickedInGame(0);
                        //带出的申请记录清空
                        stUserGameInfoBuilder.setBUnloadingStacks(0);
                        mUserUnload.remove(uuid);

                        mapStandByUserGameInfos.put(uuid, stUserGameInfoBuilder.clone());
                        mPlayers.remove(uuid);

                        //设置牌局中用户状态，站起操作
                        UserBaseInfoNet.Builder stUserBaseInfoBuilder = stUserGameInfoBuilder.getStUserInfoBuilder();
                        stUserBaseInfoBuilder.setUuid(-1);
                        stUserGameInfoBuilder.setStUserInfo(stUserBaseInfoBuilder);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                        bRemoved = true;

                        //如果用户正在打牌，则需要要先完成弃牌操作
                        if (stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1
                                && stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE
                                && stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result_VALUE
                                && stGameRoomInfo.getEGameState().getNumber() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE
                                && stUserMyGameInfoNetBuilder.getEGameState().getNumber() != USER_GAME_STATE.USER_GAME_STATE_WAIT_VALUE) {
                            if (bAllAllin == false) {
                                if (checkAllAllin() != true) {
                                    if (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING) {
                                        doFlodAction(uuid, pos, false);
                                    } else {
                                        //如果用户还参与牌局，并且没有轮到他打牌，则默认直接设置为弃牌状态即可
                                        if (stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY
                                                && stUserGameInfoBuilder.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                                                && stUserGameInfoBuilder.getLRemainStacks() != 0) {
                                            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_FOLD);
                                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                                            //主动弃牌日志
                                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + "," + stGameRoomInfo.getLGameHandID()
                                                    + "," + uuid
                                                    + "," + "fold"
                                                    + "," + 0);

                                            int victoryUser = iGameOver();
                                            if (victoryUser != -1) {
                                                //如果已经决出胜负，则进入结算阶段
                                                flodWinProcess();

                                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                                        + "," + stGameRoomInfo.getLGameHandID()
                                                        + "," + uuid
                                                        + "," + "standup"
                                                        + "," + 0);

                                                sendAllGameStateChange(uuid, null);
                                            }
                                        } else {

                                        }
                                    }
                                } else {
                                    if (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING) {
                                        doFlodAction(uuid, pos, false);
                                    } else {
                                        SidePot();
                                        resetStepStatus();
                                    }
                                }
                            }
                        }

                        //重新计算牌局参与人数
                        if (stUserGameInfoBuilder.getLRemainStacks() != 0) {
                            if (stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart
                                    && stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop
                                    && stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop
                                    && stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn
                                    && stGameRoomInfoBuilder.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River) {
                                iCouldPlayNum--;
                                if (iCouldPlayNum < 0) {
                                    iCouldPlayNum = 0;
                                }
                            }
                        }
                        break;
                    }
                }

                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (bRemoved == false) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;
                }
                //各种站起push
                if (stUserMyGameInfoNetBuilder != null) {

                    // 授信牌局直接调玩家计时map,并通知bill服锁水,非授信牌局返还玩家金币
                    if (this.bCredit){

                        if (iDrawType == 1) {
                            // 01.将玩家写入计时map
                            mUserLeaveTime.setCurrent(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());
                            mUserLeaveTimeMap.setCurrent(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());
                        }

                        String msgKey = System.currentTimeMillis()+""+stUserMyGameInfoNetBuilder.getStUserInfo().getUuid()+(RandomUtils.nextInt(9000) + 1000)+4;

                        // 02.通知bill服锁水
                        BillManager.getInstance().standUpToBill(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks(), stGameRoomInfo.getLGameHandID(), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), msgKey);

                        // 03.日志打印
                        TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = TexasPokerHttpClub.ClubUserCrditInfo.newBuilder();
                        if (stUserMyGameInfoNetBuilder.getLBuyinClubID() > 0){
                            stClubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(stUserMyGameInfoNetBuilder.getLBuyinClubID(), uuid);
                        }

                        String sLog = uuid + " " + 0 + " " + 910004 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                + " " + msgKey + " " + " 0 " + stUserMyGameInfoNetBuilder.getLRemainStacks()+stUserMyGameInfoNetBuilder.getLUnloadStacks() + " " + (stClubUserCrditInfo != null ? stClubUserCrditInfo.getLCreditBalance() : -1) + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                        TranscationLogService.getInstance().error(sLog);

                    } else {
                        //返还玩家金币
                        if (stUserMyGameInfoNetBuilder.getLRemainStacks()+stUserMyGameInfoNetBuilder.getLUnloadStacks() >= 0) {
                            UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks());
                            String sLog = uuid + " " + 0 + " " + 110004 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                    + " 0 " + " 0 " + stUserMyGameInfoNetBuilder.getLRemainStacks()+stUserMyGameInfoNetBuilder.getLUnloadStacks() + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                            TranscationLogService.getInstance().error(sLog);
                            if (ConfigaturationManager.bClubCredit) {
                                try {
                                    if (CreditManager.getInstance().checkBuyInToCredit(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stUserMyGameInfoNetBuilder.getLBuyinClubID()) == 1) {
                                        CreditManager.getInstance().standUpToCredit(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stUserMyGameInfoNetBuilder.getLBuyinClubID(),
                                                stUserMyGameInfoNetBuilder.getLRemainStacks(), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), getBuyStacks(uuid));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                        if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid) {
                            pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP, pos);
                        }
                    }

                    for (long standUuid : getStandByUuids()) {
                        if (standUuid > 0) {
                            pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), standUuid,
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP, pos);
                        }
                    }

                    if (bAuto && type == 0) {
                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_STANDUP, pos);
                    }
                    if (bAuto && type == 1) {
                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_STANDUP, pos, type);
                    }
                    if (bAuto && type == 2) {
                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_STANDUP, pos, type);
                    }
                    if (bAuto && type == 3) {
                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_STANDUP, pos, type);
                    }

                }

                //APP push如果当时是满桌的话，就push，已经站起一个用户，所以说明如果只有1个空位，则push
                int iUserNums = 0;
                for (UserGameInfoNet stUserGameInfoNet : stGameRoomInfo.getVUserGameInfosList()) {
                    if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                        iUserNums++;
                    }
                }

                if (iUserNums == (stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums() - 1)) {
                    pushAppPushGameSeatEmpty();
                }
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_SITTED;
            }
        }
    }

    public ERROR_CODE_TYPE pauseGame(long uuid) {
        synchronized (this) {
            if (getGameStarted() == true) {
                if (uuid == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) {

                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();
                    stGameRoomBaseInfoBuilder.setLGamePauseTime(System.currentTimeMillis());
                    stGameRoomBaseInfoBuilder.setBGamePause(1);
                    stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    bPause = true;
                    pushAllGameRoomUserMsgs(null, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_PAUSE, stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
                    InsuranceApi.setRoomPause(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                } else {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_OWNER;
                }
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_START;
            }
        }
    }

    //检查buyin请求的超时
    public void checkTemporarilyLeaveTimeout() {
        ArrayList<Long> timeoutArray = mTemporarilyLeaveUsersTimeout.timeout();
        if (timeoutArray != null) {
            for (int i = 0; i < timeoutArray.size(); i++) {
                Long uuid = (Long) timeoutArray.get(i);
                doStandUpAction(uuid, true, 0);
                mTemporarilyLeaveUsersTimeout.removeKey(uuid);
            }
        }
    }

    public ERROR_CODE_TYPE resumeGame(long uuid) {
        synchronized (this) {
            if (getGameStarted() == true) {
                if (uuid == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) {
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfo.getStGameRoomBaseInfo().toBuilder();

                    //重新设置时间
                    long lEclipseTime = (System.currentTimeMillis() - stGameRoomBaseInfoBuilder.getLGamePauseTime()) / 1000;
                    //正在动作的用户延时
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet.Builder stUserGameInfoBuilderTemp = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                        if (stUserGameInfoBuilderTemp.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING) {
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
                    pushAllGameRoomUserMsgs(null, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESUME, 0);

                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                } else {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_OWNER;
                }

            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_START;
            }
        }
    }

    public boolean leaveGameRoom(long uuid) {
        synchronized (this) {
            if (mStandingBys.contains(uuid)) {
                removeStandByUuid(uuid);
                return true;
            } else {
                return false;
            }
        }
    }

    public ERROR_CODE_TYPE resetBuyinState(int bBuyinControl) {
        synchronized (this) {
            if (bBuyinControl == 1) {
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                stGameRoomBaseInfoBuilder.setBBuyinControl(1);
                stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                setStGameRoomInfo(stGameRoomInfoBuilder.build());
                enableBuyinControl();
            } else if (bBuyinControl == 0) {
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

    public ERROR_CODE_TYPE buyBuyinControl(long uuid, long clubID, long lStacks) {
        synchronized (this) {
            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            if (mBuyinControlInfo.containsKey(uuid)) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_ALREADY_INVITE_BUYIN;
            }

            long popularity = (100 + GameFeeUtil.getServiceFeeRat((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds())) * lStacks / 100;

            //首先获取用户的基本信息
            UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

            if (stUserBaseInfoNetBuilder != null) {
                //检查用户是否存在买入俱乐部不相同的问题
                if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() > 0) {
                    for (UserGameInfoNet stUserGameInfoNet : stGameRoomInfo.getVUserGameInfosList()) {
                        if (stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                            //如果用户过，则查看买入过的俱乐部和这次买入的俱乐部是否为1个俱乐部
                            if (stUserGameInfoNet.getLBuyinClubID() != 0) {
                                if (stUserGameInfoNet.getLBuyinClubID() != clubID) {
                                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_LEAGUE_BUYIN_NOT_SAME_CLUB;
                                }
                            }
                        }
                    }
                }

                if (bCredit||UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), popularity)) {
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                    for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                        //找到购买的筹码
                        UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
                        if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {

                            if(bCredit){
                                //授信买入流程
                                ERROR_CODE_TYPE errCode = creditBuyinControl(uuid, clubID, lStacks, stUserBaseInfoNetBuilder,GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                                //授信买入检测失败
                                if (errCode != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) return errCode;

                            }


                            long lCreateTime = System.currentTimeMillis();
                            stUserGameInfoBuilder.setBWaitingBuyinConfirmation(1);
                            stUserGameInfoBuilder.setLBuyinTime(lCreateTime);
                            stUserGameInfoBuilder.setLBuyinClubID(clubID);
                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);

                            stGameRoomInfo = stGameRoomInfoBuilder.build();
                            mBuyinControlInfo.put(uuid, lStacks);
                            mBuyinControlTimes.put(uuid, lCreateTime);
                            mBuyinControlTimeOut.setCurrent(uuid, uuid);
                            mBuyinControlClubIDs.put(uuid, clubID);


                            if(!bCredit){
                                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                                stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                                stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_INVITE);
                                stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                                stBuyinControlMsgBuilder.setLActionUuid(uuid);
                                stBuyinControlMsgBuilder.setLClubID(clubID);
                                stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                                ClubBaseInfo.Builder stClubBaseInfoBuilder = ClubManager.getInstance().getClubBaseInfo(clubID);
                                if (stClubBaseInfoBuilder != null) {
                                    stBuyinControlMsgBuilder.setSClubName(stClubBaseInfoBuilder.getSClubName());
                                }

                                //首先push给房主
                                sendAllBuyinControls(stBuyinControlMsgBuilder.build(), clubID, 0);

                                pushAppPushBuyinInvite(stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID(), clubID, stUserBaseInfoNetBuilder.getStrNick());
                            }
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                        }
                    }

                    UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);
                    if (stUserGameInfoBuilder != null) {

                        if(bCredit){
                            //授信买入流程
                            ERROR_CODE_TYPE errCode = creditBuyinControl(uuid, clubID, lStacks, stUserBaseInfoNetBuilder,GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                            //授信买入检测失败
                            if (errCode != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) return errCode;
                        }


                        long lCreateTime = System.currentTimeMillis();
                        stUserGameInfoBuilder.setBWaitingBuyinConfirmation(1);
                        stUserGameInfoBuilder.setLBuyinTime(lCreateTime);
                        stUserGameInfoBuilder.setLBuyinClubID(clubID);
                        mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
                        mBuyinControlInfo.put(uuid, lStacks);
                        mBuyinControlTimes.put(uuid, lCreateTime);
                        mBuyinControlTimeOut.setCurrent(uuid, uuid);
                        mBuyinControlClubIDs.put(uuid, clubID);

                        if(!bCredit){
                            BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                            stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                            stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                            stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_INVITE);
                            stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                            stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                            stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                            stBuyinControlMsgBuilder.setLActionUuid(uuid);
                            stBuyinControlMsgBuilder.setLClubID(clubID);
                            stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                            ClubBaseInfo.Builder stClubBaseInfoBuilder = ClubManager.getInstance().getClubBaseInfo(clubID);
                            if (stClubBaseInfoBuilder != null) {
                                stBuyinControlMsgBuilder.setSClubName(stClubBaseInfoBuilder.getSClubName());
                            }

                            sendAllBuyinControls(stBuyinControlMsgBuilder.build(), clubID, 0);

                            pushAppPushBuyinInvite(stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID(), clubID, stUserBaseInfoNetBuilder.getStrNick());
                        }
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    }

                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
                } else {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
                }

            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
            }
        }
    }

    private ERROR_CODE_TYPE creditBuyinControl(long uuid, long clubID, long lStacks, UserBaseInfoNet.Builder stUserBaseInfoNetBuilder,GAME_ROOM_TYPE gameRoomType) {
//检测用户信息
        UserBaseInfoNet.Builder stApplyUser = stUserBaseInfoNetBuilder;


        //检测俱乐部信息
        ClubBaseInfo.Builder stClubBase = ClubManager.getInstance().getClubBaseInfo(clubID);
        if(stClubBase==null)  return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_CLUB;
        if(stClubBase.getLCreditLeagueID()<=0)return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_CLUB_AUTH;

        //检测联盟
        TexasPokerHttpLeague.LeagueBaseInfo stLeagueBaseInfo = LeagueManager.getInstance().getLeagueBaseInfo(stClubBase.getLCreditLeagueID());
        if(stLeagueBaseInfo == null) return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_LEAGUE;


        //检测是否已经申请过联盟币
        if(ConnectDatabase.getInstance().judgeHasApplayCreditMsg(uuid, clubID))return ERROR_CODE_TYPE.ERROR_CODE_TYPE_WAIT_LAST_APPLY;
        //如果玩家申请回收校验玩家余额


        //开始组包，写入申请
        TexasPokerHttpClub.ApplayCreditMsg.Builder stApplayCreditMsg = TexasPokerHttpClub.ApplayCreditMsg.newBuilder();
        stApplayCreditMsg.setLApplicantUuid(stApplyUser.getUuid())
                .setSApplicantNickname(stApplyUser.getStrNick())
                .setSApplicantShowid(stApplyUser.getSShowID())
                .setIApplicantGender(stApplyUser.getIGender())
                .setSApplicantIcon(stApplyUser.getStrSmallCover())
                .setLApplicantClubID(stClubBase.getLClubID())
                .setSApplicantClubName(stClubBase.getSClubName())
                .setLLeagueID(stLeagueBaseInfo.getLLeagueID())
                .setSLeagueCreditCoinName(stLeagueBaseInfo.getSLeagueName())
                .setLManagerUuid(stClubBase.getStCreateUserBaseInfo().getUuid())
                .setEApplyCreditStatus(TexasPokerHttpClub.APPLY_CREDIT_STATUS.APPLY_CREDIT_STATUS_BUYING)
                .setLCreditCoid(lStacks)
                .setLCreateTime(System.currentTimeMillis())
                .setLGameRoomID(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId())
                .setIGameType(gameRoomType.getNumber())
                .setLMsgID(0);
        ConnectDatabase.getInstance().insertCreditApplyMsgs(stApplayCreditMsg.build());

        //发送push
        for(Long managerUuid:ClubManager.getInstance().getClubManagerUuids(stClubBase.getLClubID())){
            pushLeagueCoinApplyMsg(stApplyUser, managerUuid, stApplayCreditMsg.getLCreditCoid(), stApplayCreditMsg.getLCreateTime());
        }
        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;


    }



    private void pushLeagueCoinApplyMsg(UserBaseInfoNet.Builder stApplyUser, long toUuid,long applyCoin,long createTime) {
        TexasPoker.BuyinControlMsg.Builder stBuyinControlMsgBuilder = TexasPoker.BuyinControlMsg.newBuilder();
        stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
        stBuyinControlMsgBuilder.setLBuyStacks(applyCoin);
        stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(TexasPoker.BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_LEAGUECOIN_APPLY);
        stBuyinControlMsgBuilder.setStFromUser(stApplyUser);
        stBuyinControlMsgBuilder.setLActionUuid(toUuid);
        stBuyinControlMsgBuilder.setLBuyinTime(createTime);
        GameRoomMsgManager.getInstance().pushLeagueCoinApply(toUuid, stBuyinControlMsgBuilder.build());
    }
    public ERROR_CODE_TYPE buyinControlActionAccept(long uuid, long lActionedUuid) {
        synchronized (this) {
            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            //如果不存在请求，则显示错误
            if (!mBuyinControlInfo.containsKey(lActionedUuid)) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_INVITE_BUYIN;
            }

            long lStacks = mBuyinControlInfo.get(lActionedUuid);
            long lMsgID = mBuyinControlTimes.get(lActionedUuid);
            mBuyinControlInfo.remove(lActionedUuid);
            mBuyinControlTimes.remove(lActionedUuid);
            mBuyinControlTimeOut.removeKey(lActionedUuid);
            mBuyinControlClubIDs.remove(lActionedUuid);
            //发送成功的push
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

            UserBaseInfoNet.Builder stUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                //找到购买的筹码
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() == lActionedUuid) {

                    long lCreateTime = System.currentTimeMillis();
                    stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                    stUserGameInfoBuilder.setLBuyinTime(0);
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    ERROR_CODE_TYPE eErrorCodeType = buyBuyin(lActionedUuid, stUserGameInfoBuilder.getLBuyinClubID(), lStacks, false);

                    if(((!bCredit)&&(eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY ||eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS))
                            ||(bCredit&&eErrorCodeType!=ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS&&eErrorCodeType!=ERROR_CODE_TYPE_NOT_SITTED)) {
                        eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYINCONTROL_NO_MONEY;
                        BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                        stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                        stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                        stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                        stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
                        stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                        stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                        stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                        stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                        stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                        if (stUserBaseInfoBuilder != null) {
                            stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                        } else {
                            stBuyinControlMsgBuilder.setSActionNickname("");
                        }

                        GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);

                        sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);

                        if (stUserGameInfoBuilder.getLRemainStacks() == 0 &&
                                (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                            this.doStandUpAction(lActionedUuid, true, 0);
                        }
                    } else {
                        BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                        stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                        stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                        stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                        stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ACCEPT);
                        stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                        stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                        stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                        stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                        stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                        if (stUserBaseInfoBuilder != null) {
                            stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                        } else {
                            stBuyinControlMsgBuilder.setSActionNickname("");
                        }


                        GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);

                        sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);
                    }


                    return eErrorCodeType;
                }
            }
            UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(lActionedUuid);
            if (stUserGameInfoBuilder != null) {
                long lCreateTime = System.currentTimeMillis();
                stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                stUserGameInfoBuilder.setLBuyinTime(0);
                mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);

                ERROR_CODE_TYPE eErrorCodeType = buyBuyin(lActionedUuid, stUserGameInfoBuilder.getLBuyinClubID(), lStacks, false);

                if(((!bCredit)&&(eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY ||eErrorCodeType  == ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS))
                        ||(bCredit&&eErrorCodeType!=ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS&&eErrorCodeType!=ERROR_CODE_TYPE_NOT_SITTED)){
                    eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYINCONTROL_NO_MONEY;
                    BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                    stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                    stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                    stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                    stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
                    stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                    stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                    stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                    stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                    stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                    if (stUserBaseInfoBuilder != null) {
                        stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                    } else {
                        stBuyinControlMsgBuilder.setSActionNickname("");
                    }


                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);

                    sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);
                } else {
                    BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                    stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                    stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                    stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ACCEPT);
                    stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                    stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                    stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                    stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                    stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                    stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                    if (stUserBaseInfoBuilder != null) {
                        stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                    } else {
                        stBuyinControlMsgBuilder.setSActionNickname("");
                    }


                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);
                    sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);
                }

                return eErrorCodeType;
            }

            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
        }
    }

    public ERROR_CODE_TYPE buyinControlActionDeny(long uuid, long lActionedUuid, boolean auto) {
        synchronized (this) {
            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            //如果不存在请求，则显示错误
            if (!mBuyinControlInfo.containsKey(lActionedUuid)) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_INVITE_BUYIN;
            }

            long lStacks = mBuyinControlInfo.get(lActionedUuid);
            long lMsgID = mBuyinControlTimes.get(lActionedUuid);

            mBuyinControlInfo.remove(lActionedUuid);
            mBuyinControlTimes.remove(lActionedUuid);
            mBuyinControlTimeOut.removeKey(lActionedUuid);
            mBuyinControlClubIDs.remove(lActionedUuid);

            //发送成功的push
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            UserBaseInfoNet.Builder stUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                //找到购买的筹码
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() == lActionedUuid) {
                    long lCreateTime = System.currentTimeMillis();
                    stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                    stUserGameInfoBuilder.setLBuyinTime(0);
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                    stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                    stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                    stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
                    stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                    stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                    stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                    stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                    stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                    stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                    if (stUserBaseInfoBuilder != null) {
                        stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                    } else {
                        stBuyinControlMsgBuilder.setSActionNickname("");
                    }

                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);

                    sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);

                    if (stUserGameInfoBuilder.getLRemainStacks() == 0 &&
                            (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                        this.doStandUpAction(lActionedUuid, true, 0);
                    }

                    if (auto == true) {

                        //授信不修改联盟币申请
                        if(bCredit){
                            //ConnectDatabase.getInstance().updateCreditApplyMsgs(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, stUserBaseInfoBuilder.getStrNick(), lActionedUuid, 2);
                        }else{
                            ConnectDatabase.getInstance().dealBuyInInfoV2(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    , lActionedUuid, 1000, "", BUYIN_CONTROL_STATUS.BUYIN_CONTROL_STATUS_BUYIN_DENY, System.currentTimeMillis());
                        }
                    }
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                }
            }

            UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(lActionedUuid);
            if (stUserGameInfoBuilder != null) {
                long lCreateTime = System.currentTimeMillis();
                stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                stUserGameInfoBuilder.setLBuyinTime(0);
                mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);

                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                stBuyinControlMsgBuilder.setLBuyinActionID(lCreateTime);
                stBuyinControlMsgBuilder.setLBuyStacks(lStacks);
                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                stBuyinControlMsgBuilder.setLActionUuid(lActionedUuid);
                stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder.getStUserInfo());
                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                stBuyinControlMsgBuilder.setLMsgID(lMsgID);
                stBuyinControlMsgBuilder.setLBuyinTime(stUserGameInfoBuilder.getLBuyinTime());
                if (stUserBaseInfoBuilder != null) {
                    stBuyinControlMsgBuilder.setSActionNickname(stUserBaseInfoBuilder.getStrNick());
                } else {
                    stBuyinControlMsgBuilder.setSActionNickname("");
                }


                //给申请人发push消息
                GameRoomMsgManager.getInstance().sendBuyinControlMsg(lActionedUuid, stBuyinControlMsgBuilder.build(), 0);

                //给房主发push消息
                sendAllBuyinControls(stBuyinControlMsgBuilder.build(), stUserGameInfoBuilder.getLBuyinClubID(), lActionedUuid);

                if (auto == true) {
                    if(bCredit){
                        //ConnectDatabase.getInstance().updateCreditApplyMsgs(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, stUserBaseInfoBuilder.getStrNick(), lActionedUuid, 2);
                    }else{
                        ConnectDatabase.getInstance().dealBuyInInfoV2(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                , lActionedUuid, 1000, "", BUYIN_CONTROL_STATUS.BUYIN_CONTROL_STATUS_BUYIN_DENY, System.currentTimeMillis());
                    }
                }

                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            }

            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_GAME;
        }
    }

    public ERROR_CODE_TYPE disableBuyinControl() {
        synchronized (this) {
            //如果不存在请求，则显示错误
            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            Iterator iter = mBuyinControlInfo.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Long uuid = (Long) entry.getKey();

                //发送成功的push
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                    //找到购买的筹码
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
                    if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                        stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                        stUserGameInfoBuilder.setLBuyinTime(0);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                    }
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);
                if (stUserGameInfoBuilder != null) {
                    stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                    stUserGameInfoBuilder.setLBuyinTime(0);
                    mapStandByUserGameInfos.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stUserGameInfoBuilder);
                }


                stGameRoomInfo = stGameRoomInfoBuilder.build();

                mBuyinControlInfo.remove(uuid);
                mBuyinControlTimes.remove(uuid);
                mBuyinControlTimeOut.removeKey(uuid);
                mBuyinControlClubIDs.remove(uuid);
            }

            //发送成功的push
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                //找到购买的筹码
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();

                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DISABLE);
                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(stUserGameInfoBuilder.getStUserInfo().getUuid()
                            , stBuyinControlMsgBuilder.build(), 0);

                    if (stUserGameInfoBuilder.getLRemainStacks() == 0 &&
                            (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                        this.doStandUpAction(stUserGameInfoBuilder.getStUserInfo().getUuid(), true, 0);
                    }
                }
            }


            for (long push_uuid : getStandByUuids()) {
                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DISABLE);
                //							stBuyinControlMsgBuilder.setStFromUser(stUserGameInfoBuilder);
                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                GameRoomMsgManager.getInstance().sendBuyinControlMsg(push_uuid
                        , stBuyinControlMsgBuilder.build(), 0);
            }

            ConnectDatabase.getInstance().gameoverBuyInControls(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        }
    }

    public ERROR_CODE_TYPE enableBuyinControl() {
        synchronized (this) {

            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }
            //发送成功的push
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                //找到购买的筹码
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();

                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ENABLE);
                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0)
                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(stUserGameInfoBuilder.getStUserInfo().getUuid()
                            , stBuyinControlMsgBuilder.build(), 0);
            }


            for (long push_uuid : getStandByUuids()) {
                BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
                stBuyinControlMsgBuilder.setLBuyinActionID(System.currentTimeMillis());
                stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_ENABLE);
                stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
                stBuyinControlMsgBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                GameRoomMsgManager.getInstance().sendBuyinControlMsg(push_uuid
                        , stBuyinControlMsgBuilder.build(), 0);
            }


            stGameRoomInfo = stGameRoomInfoBuilder.build();

            mBuyinControlInfo.clear();
            mBuyinControlTimes.clear();
            mBuyinControlTimeOut.clear();
            mBuyinControlClubIDs.clear();

            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        }
    }

    /**
     * 用户坐下
     *
     * @param uuid    坐下用户的uuid
     * @param lStacks 买入的金额
     */
    public ERROR_CODE_TYPE buyBuyin(long uuid, long clubID, long lStacks, boolean bCredit) {
        synchronized (this) {

            //授信牌局,牌局结束玩家不能买入
            if (this.bCredit){
                if (bOver){
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }

            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            // 生成银币扣除数据
            long popularity = 0;

            UserGameInfoNet.Builder stUserMyGameInfoNetBuilder = null;

            int iPos = 0;

            boolean bSetted = false;

            ERROR_CODE_TYPE eErrorCodeType;

            popularity = (100 + GameFeeUtil.getServiceFeeRat((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds())) * lStacks / 100;

            //首先获取用户的基本信息
            UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

            //检查用户是否存在
            if (stUserBaseInfoNetBuilder != null) {
                //检查用户是否存在买入俱乐部不相同的问题
                if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() > 0) {
                    for (UserGameInfoNet stUserGameInfoNet : stGameRoomInfo.getVUserGameInfosList()) {
                        if (stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                            //如果用户过，则查看买入过的俱乐部和这次买入的俱乐部是否为1个俱乐部
                            if (stUserGameInfoNet.getLBuyinClubID() != 0) {
                                if (stUserGameInfoNet.getLBuyinClubID() != clubID) {
                                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_LEAGUE_BUYIN_NOT_SAME_CLUB;
                                }
                            }
                        }
                    }
                }

                //授信牌局玩家买入俱乐部,需要判断当前俱乐部是否被冻结,若冻结玩家不能买入,玩家被冻结也不能买入
                if (this.bCredit){

                    //检测玩家是否被冻结
                    List<TexasPokerHttpClub.ClubUserCrditInfo> vClubUserCrditInfo = new ArrayList<TexasPokerHttpClub.ClubUserCrditInfo>();
                    TexasPokerHttpClub.ClubUserCrditInfo.Builder tClubUserCrditInfo = TexasPokerHttpClub.ClubUserCrditInfo.newBuilder();
                    tClubUserCrditInfo.setUuid(uuid);
                    tClubUserCrditInfo.setLClubID(clubID);
                    vClubUserCrditInfo.add(tClubUserCrditInfo.build());

                    ArrayList<Long> frozenUserList = ClubManager.getInstance().isCreditUserFrozen(stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID(),vClubUserCrditInfo);
                    if (!CollectionUtils.isEmpty(frozenUserList) && frozenUserList.get(0) == uuid){
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_USER_HAS_BEAN_FROZEN;
                    }

                    //检测俱乐部是否被冻结
                    eErrorCodeType = ClubManager.getInstance().isLimitCreditClubBuyIn(clubID);
                    if (eErrorCodeType != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                        //买入俱乐部冻结
                        return eErrorCodeType;
                    }

                    //玩家买入时必须在座位上检测,安全性保护
                    if (!containUser(uuid)){
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_SITTED;
                    }

                    //玩家入座收费检测德扑币是否足够
                    if (!UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), lStacks))){
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
                    }
                }

                //区分授信牌局和非授信牌局,如果是授信牌局扣除玩家在俱乐部内的联盟币,非授信牌局扣除玩家自身的德扑币
                //检查用户德扑币是否充足或联盟币是否充足
                if ((UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), popularity) && !this.bCredit)
                        || (this.bCredit && ClubManager.getInstance().checkClubUserCredit(clubID, uuid, lStacks))) {
                    if (bCredit) {
                        int iOnTable = 0;
                        long remainstack = 0;
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                            if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                                iOnTable = 1;
                                remainstack = stUserGameInfoBuilder.getLRemainStacks();
                            }
                        }
                        if (iOnTable == 0) {
                            UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);
                            if (stUserGameInfoBuilder != null) {
                                remainstack = stUserGameInfoBuilder.getLRemainStacks();
                            }
                        }
                        int result = CreditManager.getInstance().buyInToCredit(uuid, stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), clubID,
                                lStacks, getBuyStacks(uuid), iOnTable, stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStrRoomName(), remainstack);
                        if (result != 0) {
                            CreditDenyPush(lStacks, uuid);
                            if (remainstack == 0) {
                                doStandUpAction(uuid, true, 0);
                            }
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                        }
                    }

                    eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    //获取要增加筹码的用户
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        //找到购买的筹码
                        UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                        if (stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                            stUserMyGameInfoNetBuilder = stUserGameInfoBuilder;
                            iPos = i;
                            //如果这个是弃牌或者不在牌局里面，则直接加盲注
                            if (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_FOLD
                                    || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                                    || (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY && getGameStarted() == false)
                                    || (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY && this.iCouldPlayNum == 1)) {

                                //玩家购买的筹码
                                if ((stUserGameInfoBuilder.getLRemainStacks() + lStacks) < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio() / 10 + 1) * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()) {
                                    //解决连续大忙问题，暂时注释下个版本启用
//									if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
//										stUserGameInfoBuilder.setBJustSit(1);
//									}

                                    String msgKey = System.currentTimeMillis()+""+uuid+(RandomUtils.nextInt(9000) + 1000)+0;
                                    //授信牌局在写入账单之前先做联盟币更新,防止最大买入和最大允许亏损
                                    if (this.bCredit){

                                        eErrorCodeType = ClubManager.getInstance().updateUserCreditInGame(clubID, uuid, lStacks, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), 1, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 0, msgKey);
                                        if (eErrorCodeType != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                                            return eErrorCodeType;
                                        } else {
                                            //扣除入座服务费
                                            stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), lStacks));
                                            String sLog = uuid + " " + clubID + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID() + " " + 910005 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + " 0 " + " 0 " + lStacks + " " + stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate() + " " + GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), lStacks) + " " + stUserBaseInfoNetBuilder.getLPopularity();
                                            TranscationLogService.getInstance().error(sLog);
                                        }
                                    }

                                    long buyStacks = lStacks;

                                    stUserGameInfoBuilder.setLRemainStacks(buyStacks + stUserGameInfoBuilder.getLRemainStacks());
                                    stUserGameInfoBuilder.setLBuyinClubID(clubID);

                                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                                    //修改账单信息
                                    buyStakcsInRoom(uuid, buyStacks, clubID, 1);

                                    if (stUserGameInfoBuilder != null) {
                                        setBillCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
                                                stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                                , stUserGameInfoBuilder.getLRemainStacks(), stUserGameInfoBuilder.getLUnloadStacks());
                                    }
                                    bSetted = true;

                                    //授信局减去玩家联盟币,非授信局减去用户的银币
                                    if (this.bCredit){
                                        //用户联盟币已减完,只做日志打印
                                        TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(clubID, uuid);
                                        String sLog = uuid + " " + clubID + " " + 910000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                + " " + msgKey + " " + " 0 " + lStacks + " " + stClubUserCrditInfo.getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);

                                    } else {
                                        //减去用户的银币
                                        stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);
                                        String sLog = uuid + " " + clubID + " " + 110000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                + " 0 " + " 0 " + lStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    }

                                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                                    if (GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE == stGameRoomInfoBuilder.getEGameState().getNumber()) {
                                        resetGameRoom();
                                        stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
                                    }
                                } else {
                                    eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS;
                                    bSetted = true;
                                }
                            } else {
                                //把好友状态从游戏玩家列表中删除
                                stUserGameInfoBuilder.setBBuyin(1);
                                stUserGameInfoBuilder.setLBuyinClubID(clubID);
                                stUserGameInfoBuilder.setLBuyinStacks((int) lStacks);
                                stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
//								System.out.println("buyBuyin clubID 2 : " + clubID);
                                stGameRoomInfo = stGameRoomInfoBuilder.build();
                                if (stGameRoomInfoBuilder.getEGameState() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result) {
                                    stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
                                }
                                bSetted = true;
                            }
                            break;
                        }
                    }

                    if (bSetted == false) {
                        //查看所有在旁观队列
                        UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(uuid);
                        if (stUserGameInfoBuilder != null) {
                            stUserMyGameInfoNetBuilder = stUserGameInfoBuilder;
                            if ((stUserGameInfoBuilder.getLRemainStacks() + lStacks) < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio() / 10 + 1) * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()) {
                                //解决连续大忙问题，暂时注释下个版本启用
//									if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
//										stUserGameInfoBuilder.setBJustSit(1);
//									}

                                //玩家购买的筹码
                                long buyStacks = lStacks;
                                stUserGameInfoBuilder.setLRemainStacks(buyStacks + stUserGameInfoBuilder.getLRemainStacks());
                                stUserGameInfoBuilder.setBWaitingBuyinConfirmation(0);
                                stUserGameInfoBuilder.setLBuyinClubID(clubID);

                                //修改账单信息
                                buyStakcsInRoom(uuid, buyStacks, clubID, 2);

                                if (stUserGameInfoBuilder != null) {
                                    setBillCurrentStacksInRoom(stUserGameInfoBuilder.getStUserInfo().getUuid(),
                                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            , stUserGameInfoBuilder.getLRemainStacks(), stUserGameInfoBuilder.getLUnloadStacks());
                                }
                                //玩家不在桌只扣除服务费
                                long fee = (GameFeeUtil.getServiceFeeRat((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds())) * lStacks / 100;
                                //减去用户的银币,只扣除服务费
                                stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -fee);
                                String sLog = uuid + " " + clubID + " " + 110000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                        + " 0 " + " 0 " + lStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                TranscationLogService.getInstance().error(sLog);

                            } else {
                                eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_BUYIN_MAX_STACKS;
                            }
                        }
                    }

                    //如果房间没有坐下的话，会push消息
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet stUserGameInfoSendNet = stGameRoomInfo.getVUserGameInfos(i);

                        if (stUserGameInfoSendNet.getStUserInfo().getUuid() == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) {
                            //首先找到离开房间的用户基本信息
                            pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserGameInfoSendNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUYIN, iPos);
                            break;
                        }
                    }

                    if (mapStandByUserGameInfos.get(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) != null) {
                        //首先找到离开房间的用户基本信息
                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUYIN, iPos);
                    }
                } else {
                    if (this.bCredit){
                        eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_LEAGUE_CREDIT_NOT_ENOUGH_BUYIN;
                    } else {
                        eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
                    }
                }
            } else {
                eErrorCodeType = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
            }
            return eErrorCodeType;
        }
    }

    public ERROR_CODE_TYPE checkLBSInfo(long uuid, int bIPLimit, int bGPSLimit) {
//		System.out.println("checkLBSInfocheckLBSInfo111");
        ArrayList<Long> vOtherUuids = new ArrayList<Long>();
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
//			System.out.println("checkLBSInfocheckLBSInfo113");
            if (stUserGameInfoNet != null && stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                vOtherUuids.add(stUserGameInfoNet.getStUserInfo().getUuid());
            }
//			System.out.println("checkLBSInfocheckLBSInfo114");
        }
//		System.out.println("checkLBSInfocheckLBSInfo112");

        boolean bPass = true;
        //IP检测
        if (bIPLimit != 0) {
            bPass = checkIPLimit(uuid, vOtherUuids);
        }

        //如果IP检测不通过，直接返回不通过
        if (bPass == false) {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_IP_CHECK_ERROR;
        }

        //GPS检测
        if (bGPSLimit != 0) {
            bPass = checkGPSLimit(uuid, vOtherUuids);
        }

        //如果GPS检测不通过，直接返回不通过
        if (bPass == false) {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_GPS_CHECK_ERROR;
        }

        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
    }

    public boolean checkIPLimit(long uuid, ArrayList<Long> vOtherUuids) {
        return LBSManager.getInstance().verifyIPInfoToOthers(uuid, vOtherUuids, getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
    }

    public boolean checkGPSLimit(long uuid, ArrayList<Long> vOtherUuids) {
        return LBSManager.getInstance().verifyLocationToOthers(uuid, vOtherUuids, getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
    }

    private boolean checkTableFull() {
        boolean bTableFull = true;
        for (UserGameInfoNet stUserGameInfoNet : this.stGameRoomInfo.getVUserGameInfosList()) {
            if (stUserGameInfoNet.getStUserInfo().getUuid() == 0) {
                bTableFull = false;
                break;
            }
        }

        return bTableFull;
    }

    private void tableFullPush(long toUuid) {
        pushFullGameRoomUserMsgs(null, toUuid,
                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FULL_TABLE, 0);
        APNManager.getInstance().fullGameRoomPush(toUuid, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
    }

    public ERROR_CODE_TYPE sitdown(long uuid, int iPos) {
        synchronized (this) {
            //授信牌局,牌局结束玩家不能坐下
            if (bCredit){
                if (bOver){
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                }
            }

            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_PAUSED;
            }

            if (containUser(uuid)) {
                //如果用户已经存在房间了，就直接返回
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
            }

            //定义errorcode变量
            ERROR_CODE_TYPE eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;

            //检测LBS信息
            eErrorCode = checkLBSInfo(uuid, stGameRoomInfo.getStGameRoomBaseInfo()
                    .getBIPLimit(), stGameRoomInfo.getStGameRoomBaseInfo().getBGPSLimit());

            if (eErrorCode != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                return eErrorCode;
            }

            eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM;

            //坐下用户的信息
            UserGameInfoNet.Builder stUserMyGameInfoNetBuilder = null;

            if (iPos >= 0 && iPos < iGameRoomUserMaxNums) {
                //获取牌局信息
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                //找到的需要坐下的位置
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(iPos).toBuilder();

                //该位置可以坐下
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() == 0 || stUserGameInfoBuilder.getStUserInfo().getUuid() == -1) {
                    //获取该玩家站起前的游戏信息
                    stUserMyGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);

                    //如果玩家在旁观的玩家中，则开始设置坐下玩家信息
                    if (stUserMyGameInfoNetBuilder != null) {

                        String msgKey = System.currentTimeMillis()+""+uuid+(RandomUtils.nextInt(9000) + 1000)+3;

                        //授信牌局,检查玩家买入俱乐部是否冻结,玩家是否被冻结
                        if (bCredit) {

                            eErrorCode = ClubManager.getInstance().isLimitCreditClubBuyIn(stUserMyGameInfoNetBuilder.getLBuyinClubID());

                            //检测玩家是否被冻结
                            List<TexasPokerHttpClub.ClubUserCrditInfo> vClubUserCrditInfo = new ArrayList<TexasPokerHttpClub.ClubUserCrditInfo>();
                            TexasPokerHttpClub.ClubUserCrditInfo.Builder tClubUserCrditInfo = TexasPokerHttpClub.ClubUserCrditInfo.newBuilder();
                            tClubUserCrditInfo.setUuid(uuid);
                            tClubUserCrditInfo.setLClubID(stUserMyGameInfoNetBuilder.getLBuyinClubID());
                            vClubUserCrditInfo.add(tClubUserCrditInfo.build());

                            ArrayList<Long> frozenUserList = ClubManager.getInstance().isCreditUserFrozen(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID(),vClubUserCrditInfo);
                            if (!CollectionUtils.isEmpty(frozenUserList) && frozenUserList.get(0) == uuid){
                                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_USER_HAS_BEAN_FROZEN;
                            }

                            if (eErrorCode != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                                //买入俱乐部冻结
                                return eErrorCode;
                            } else {
                                //进行解除锁水操作 会检测玩家是否够上桌条件
                                ERROR_CODE_TYPE errorCodeType = BillManager.getInstance().sitDownToBill(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), msgKey);
                                if (errorCodeType != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                                    return errorCodeType;
                                }
                                if (iDrawType == 1){
                                    //检测玩家是否超时
                                    mUserLeaveTime.removeKey(uuid);
                                    mUserLeaveTimeMap.removeKey(uuid);
                                }
                            }
                        }

                        //获取人气值，查看是否还有足够的人气值
                        UserBaseInfoNet.Builder stUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());


                        //授信牌局检查玩家联盟币,非授信牌局检查玩家德扑币
                        if ((stUserBaseInfoBuilder.getLPopularity() >= stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks() && !bCredit) || (bCredit)) {
                            int result = 5;
                            try {
                                if (ConfigaturationManager.bClubCredit) {
                                    if (CreditManager.getInstance().checkBuyInToCredit(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stUserMyGameInfoNetBuilder.getLBuyinClubID()) == 1) {
                                        result = CreditManager.getInstance().sitDownToCredit(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stUserMyGameInfoNetBuilder.getLBuyinClubID(),
                                                getBuyStacks(uuid), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), stUserMyGameInfoNetBuilder.getLRemainStacks());
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (result == 5 || result == 0) {

                                if (bCredit){
                                    if (stUserMyGameInfoNetBuilder.getLBuyinClubID() == 0){
                                        //玩家首次坐下,此时没有买入俱乐部不能获取当前玩家在俱乐部的联盟币 标识为0
                                        String sLog = stUserBaseInfoBuilder.getUuid() + " " + stUserMyGameInfoNetBuilder.getLBuyinClubID() + " " + 910003 + " "
                                                + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " " + msgKey + " " + " 0 "
                                                + (stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks()) + " " + 0 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    } else {
                                        TexasPokerHttpClub.ClubUserCrditInfo.Builder clubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(stUserMyGameInfoNetBuilder.getLBuyinClubID(), uuid);
                                        String sLog = stUserBaseInfoBuilder.getUuid() + " " + stUserMyGameInfoNetBuilder.getLBuyinClubID() + " " + 910003 + " "
                                                + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " " + msgKey + " " + " 0 "
                                                + (stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks()) + " " + clubUserCrditInfo.getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    }

                                } else {
                                    //减去玩家金币
                                    stUserBaseInfoBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoBuilder.getUuid(), -(stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks()));

                                    String sLog = stUserBaseInfoBuilder.getUuid() + " " + stUserMyGameInfoNetBuilder.getLBuyinClubID() + " " + 110003 + " "
                                            + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " 0 " + " 0 "
                                            + (stUserMyGameInfoNetBuilder.getLRemainStacks() + stUserMyGameInfoNetBuilder.getLUnloadStacks()) + " " + stUserBaseInfoBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                    TranscationLogService.getInstance().error(sLog);
                                }

                                if (inNoChargeList(stUserBaseInfoBuilder.getUuid()) || stUserBaseInfoBuilder.getUuid() == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) {
                                    //先检查是否在别的游戏中

                                    //不需要付费
                                    stUserMyGameInfoNetBuilder.clearVCurrentHands();
                                    stUserMyGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                                    stUserMyGameInfoNetBuilder.setBGameDealer(0);
                                    if (stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1) {
                                        stUserMyGameInfoNetBuilder.setBJustSit(1);
                                    }

                                    UserGameInfoNet.Builder stUserMyTempGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(iPos);
                                    if (stUserMyTempGameInfoNetBuilder != null) {
                                        stUserMyGameInfoNetBuilder.setEGameState(stUserMyTempGameInfoNetBuilder.getEGameState());
                                        stUserMyGameInfoNetBuilder.setLBetStacks(stUserMyTempGameInfoNetBuilder.getLBetStacks());
                                    }
                                    //在该位置下坐下
                                    stGameRoomInfoBuilder.setVUserGameInfos(iPos, stUserMyGameInfoNetBuilder);

                                    //设置座位和用户的对应关系
                                    mPlayers.add(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());
                                    setUserPos(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), iPos);


                                    //从旁观人中删除该用户
                                    removeAllStandByStatusUuid(uuid);

                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    UserManager.getInstance().setUserInTable(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                                    if (getGameStarted() == false && bStartGamePush == false && checkTableFull() == true) {
                                        //发送满桌提示给房主
                                        tableFullPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
                                        bStartGamePush = true;
                                    }

                                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                                } else {
                                    stUserMyGameInfoNetBuilder.clearVCurrentHands();
                                    stUserMyGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                                    stUserMyGameInfoNetBuilder.setBGameDealer(0);
                                    stUserMyGameInfoNetBuilder.setStUserInfo(stUserBaseInfoBuilder);
                                    if (stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1) {
                                        stUserMyGameInfoNetBuilder.setBJustSit(1);
                                    }

                                    UserGameInfoNet.Builder stUserMyTempGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(iPos);
                                    if (stUserMyTempGameInfoNetBuilder != null) {
                                        stUserMyGameInfoNetBuilder.setEGameState(stUserMyTempGameInfoNetBuilder.getEGameState());
                                        stUserMyGameInfoNetBuilder.setLBetStacks(stUserMyTempGameInfoNetBuilder.getLBetStacks());
                                    }

                                    //在该位置下坐下
                                    stGameRoomInfoBuilder.setVUserGameInfos(iPos, stUserMyGameInfoNetBuilder);

                                    //设置座位和用户的对应关系
                                    mPlayers.add(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid());

                                    setUserPos(stUserMyGameInfoNetBuilder.getStUserInfo().getUuid(), iPos);

                                    //从旁观人中删除该用户
                                    removeAllStandByStatusUuid(uuid);

                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    //把用户放到nocahrge队列里面
                                    addNorChargeList(stUserBaseInfoBuilder.getUuid());

                                    UserManager.getInstance().setUserInTable(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                                    if (getGameStarted() == false && bStartGamePush == false && checkTableFull() == true) {
                                        //发送满桌提示给房主
                                        tableFullPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
                                        bStartGamePush = true;
                                    }
                                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                                }
                            } else {
                                //发送拒绝申请买入的push
                                CreditDenyPush(stUserMyGameInfoNetBuilder.getLRemainStacks(), uuid);
                                if (stUserMyGameInfoNetBuilder.getLRemainStacks() <= 0) {
                                    doStandUpAction(uuid, true, 0);
                                }
                                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
                            }
                        } else {
                            eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY;
                        }
                    } else {
                        eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
                    }
                } else {
                    eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS;
                }
            } else {
                eErrorCode = ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS;
            }

            if (stUserMyGameInfoNetBuilder != null) {
                //通知房间的其他人有人离开房间了
                if (iPos >= 0 && iPos < iGameRoomUserMaxNums && eErrorCode == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                    //给在牌局的人push
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                        if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() != uuid) {
                            //首先找到离开房间的用户基本信息
                            pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SITDOWN, iPos);
                        }
                    }

                    //给旁观的人push
                    for (long push_uuid : getStandByUuids()) {
                        UserBaseInfoNet stUserBaseInfoNet = mapStandByUserGameInfos.get(push_uuid).getStUserInfo();

                        pushGameRoomUserMsgs(stUserMyGameInfoNetBuilder.build(), stUserBaseInfoNet.getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SITDOWN, iPos);
                    }
                }
            }

            //如果玩家入座成功就会弹买入框
            if (eErrorCode == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){

                //检测玩家是否首次入座,首次入座添加进玩家入座队列内
                if (sitDownUserList.contains(uuid)){
                    //玩家不是首次入座,需检测玩家的筹码是否<=0,如果<=0则进入弹框倒计时map
                    if (stUserMyGameInfoNetBuilder.getLRemainStacks() <= 0){
                        mBuyinTimeOut.setCurrent(uuid,uuid);
                    }
                } else {
                    sitDownUserList.add(uuid);
                }
            }

            return eErrorCode;
        }
    }

    //检查buyin请求的超时
    public void checkBuyinControlTimeout() {
        ArrayList<Long> timeoutArray = mBuyinControlTimeOut.timeout();
        if (timeoutArray != null) {
            for (int i = 0; i < timeoutArray.size(); i++) {
                Long uuid = (Long) timeoutArray.get(i);
                if (uuid != null) {
                    buyinControlActionDeny(0, uuid, true);
                }
            }
        }
    }

    //检查buyin弹框请求的超时
    public void checkoutBuyinTimeout(){
        ArrayList<Long> timeoutArray = mBuyinTimeOut.timeout();
        if (timeoutArray != null) {
            for (int i = 0; i < timeoutArray.size(); i++) {
                Long uuid = (Long) timeoutArray.get(i);
                if (uuid != null) {
                    forceStandup(uuid);
                    mBuyinTimeOut.removeKey(uuid);
                }
            }
        }
    }

    //玩家有买入命令时移除超时
    public void removeBuyinTimeOut(long uuid){
        mBuyinTimeOut.removeKey(uuid);
    }

    //检查buyin请求的超时
    private void checkStandByTimeout() {
        ArrayList<Long> timeoutArray = mStandByTimeOut.timeout();
        if (timeoutArray != null) {
            for (int i = 0; i < timeoutArray.size(); i++) {
                Long uuid = (Long) timeoutArray.get(i);
                removeStandByUuid(uuid);
            }
        }
    }

    private void robotCheck() {
        preActionCheck();
        delegateCheck();
        robotFlodCheck();
    }

    //托管检测及处理
    private void delegateCheck() {
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
            if (stUserGameInfoNet != null
                    && stUserGameInfoNet.getStUserInfo().getUuid() > 0
                    && stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                    && stUserGameInfoNet.getBDelegate() == 1) {
                //如果正在动作的用户
                if (lStepMaxStacks == stUserGameInfoNet.getLBetStacks()) {
                    doCheckAction(stUserGameInfoNet.getStUserInfo().getUuid(), 0);
                } else {
                    doFlodAction(stUserGameInfoNet.getStUserInfo().getUuid(), (long) -1, true);
                }
                return;
            }
        }
    }

    //检查牌局是否结束
    private boolean checkGameOver() {
        if (bGameCanceled == true) {
            return true;
        }
        if ((bStarted == true) &&
                (System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLStartTime() > stGameRoomInfo.getStGameRoomBaseInfo().getLDuration() * 1000)) {
            return true;
        }
        if ((bStarted == false) && System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime() > 7200 * 1000) {
            return true;
        }
        return false;
    }

    public void gameRoomTimeoutHandle(TimeoutHashMap<Long, GameRoomDetailInfo> mGameRoomDetailInfos) {
        synchronized (this) {

            //如果为授信牌局需要检测玩家离桌是否超时,如果超时则通知bill玩家阶段性结束
            if (bCredit && iDrawType == 1){
                ArrayList<Long> uuidList = mUserLeaveTimeMap.timeout();
                    for (Long uuid : uuidList){

                        // 01.通知账单服,玩家阶段性结束
                        BillManager.getInstance().checkOutBill(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                        // 02.清除玩家在牌局的数据
                        TexasPoker.UserGameInfoNet.Builder userGameInfoNet = mapStandByUserGameInfos.get(uuid);
                        userGameInfoNet.clearLBetStacks();
                        userGameInfoNet.clearEGameRole();
                        userGameInfoNet.clearLBuyinClubID();
                        userGameInfoNet.clearLBuyinStacks();
                        userGameInfoNet.clearLRemainStacks();
                        userGameInfoNet.clearBUnloadingStacks();
                        userGameInfoNet.clearLUnloadStacks();
                        userGameInfoNet.clearBGameDealer();
                        mapStandByUserGameInfos.put(uuid, userGameInfoNet);

                        // 03.清除掉玩家所有在桌数据
                        mUserBuyinStacks.remove(uuid);
                        mUserBuyining.remove(uuid);
                        mUserOutStacks.remove(uuid);
                        mUserBuyinStacks.remove(uuid);
                        mUserBuyinClubIDs.remove(uuid);

                        // 04.mUserLeaveTimeMap清除超时玩家
                        mUserLeaveTimeMap.removeKey(uuid);

                    }
            }


            if (stGameRoomInfo.getStGameRoomBaseInfo().getBGamePause() == 1) {
                if ((System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLGamePauseTime()) / 1000 > 600) {
                    this.resumeGame(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
                }
                return;
            }

            //先检查买入超时
            checkBuyinControlTimeout();

            //检查买入弹框超时
            checkoutBuyinTimeout();

            checkStandByTimeout();

            checkTemporarilyLeaveTimeout();

            //首先判断每个房间的游戏状态
//			TableLogService.getInstance().error1(lFromRoomID
//					+"," + lGameRoomID
//					+"," + stGameRoomInfo.getLGameHandID()
//					+",gameRoomTimeoutHandle begin" + stGameRoomInfo.getEGameState());

            switch (stGameRoomInfo.getEGameState().getNumber()) {
                //如果游戏状态是等待，则需要当进入房间人数大于等于2是，开始牌局
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE: {
                    long interval = System.currentTimeMillis() - stGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime();

                    boolean bGameOver = checkGameOver();
                    if (bGameOver) {
                        PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1001");
                        if (bOver == true) {
                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1003");
                            bOver = true;
                            mGameRoomDetailInfos.removeKey(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                        } else {

                            //牌局结束通知野保险结束
                            FlopLottoManager.getInstance().overFlopLotto(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            //牌局结束 AOF模式下检测卸码
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1){
                                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                                for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
                                    UserGameInfoNet.Builder stUserGameInfoNetBuilder =  stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                                    if(stUserGameInfoNetBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoNetBuilder.getBUnloadingStacks() > 0){
                                        //判断是否满足最少在卓筹码的条件
                                        if(stUserGameInfoNetBuilder.getLRemainStacks() - stUserGameInfoNetBuilder.getBUnloadingStacks() < stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIUnloadRatio() * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()){
                                            long lUnloadStacks = stUserGameInfoNetBuilder.getBUnloadingStacks();
                                            //重置状态
                                            stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                            mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                            //带出失败发送push
                                            pushUnloadStacksMsgs(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_UNLOAD_STACK_NOT_ENOUGH, lUnloadStacks);
                                        }else{
                                            long lUnloadStacks = stUserGameInfoNetBuilder.getBUnloadingStacks();
                                            //直接减少玩家牌桌上的筹码
                                            stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks() - stUserGameInfoNetBuilder.getBUnloadingStacks());
                                            //保存用户卸码的值
                                            stUserGameInfoNetBuilder.setLUnloadStacks(stUserGameInfoNetBuilder.getLUnloadStacks() + stUserGameInfoNetBuilder.getBUnloadingStacks());
                                            //重置状态
                                            stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                            mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                            //修改筹码状态
                                            setBillCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
                                                    stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),stUserGameInfoNetBuilder.getLRemainStacks(),stUserGameInfoNetBuilder.getLUnloadStacks());
                                            mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                            //带出成功发送push
                                            pushUnloadStacksMsgs(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_UNLOAD_SUCCESS, lUnloadStacks);

                                            Long uuid = stUserGameInfoNetBuilder.getStUserInfo().getUuid();
                                            Long clubid = mUserBuyinClubIDs.get(uuid);
                                            if(clubid == null){
                                                clubid = (long)0;
                                            }
                                            String sLog = uuid + " " + clubid + " " + 110003 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + " 0 " + " 0 " + lUnloadStacks + " " + 0 +" " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                            TranscationLogService.getInstance().error(sLog);
                                        }
                                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                                        sendAllGameStateChange(0, null);
                                    }
                                }

                                ArrayList<Long> vUnloadingUuids = new ArrayList<Long>(mUserUnload.keySet());
                                for(long uuid:vUnloadingUuids){
                                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
                                    if(stUserGameInfoNetBuilder != null){
                                        long lUnloadStacks = stUserGameInfoNetBuilder.getBUnloadingStacks();
                                        //重置状态
                                        stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                        mapStandByUserGameInfos.put(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),stUserGameInfoNetBuilder);
                                        mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                        //带出失败发送push
                                        pushUnloadStacksMsgs(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_UNLOAD_FAILD, lUnloadStacks);
                                    }
                                    mUserUnload.remove(uuid);
                                }
                            }

//						BillManager.getInstance().finishGameRoomAndReturn(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                            //如果为授信牌局,在牌局结束时在抽一次水
                            if (bCredit && iDrawType == 1) {

                                // 01.抽水操作
                                List<TexasPokerBillService.UserRemainstackAfterDraw> userRemainstackAfterDrawList = BillManager.getInstance().drawBill(1001L,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                                ArrayList<Long> vSitdownUsers = new ArrayList<Long>();

                                // 02.玩家抽水后余额同步玩家lRemainStacks字段
                                for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                                    UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                    // 在桌游戏的玩家
                                    if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                        for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList) {

                                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == userRemainstackAfterDraw.getUuid()) {
                                                stResetUserGameInfoBuilder.setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                                                vSitdownUsers.add(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                                break;
                                            }
                                        }
                                    }

                                    stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                                }

                                //旁观玩家
                                for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList){
                                    if (mapStandByUserGameInfos.containsKey(userRemainstackAfterDraw.getUuid()) && !vSitdownUsers.contains(userRemainstackAfterDraw.getUuid())){
                                        mapStandByUserGameInfos.get(userRemainstackAfterDraw.getUuid()).setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                                    }
                                }
                            }

                            //如果为授信牌局且底池抽水将所有玩家贡献抽水发送至账单服
                            if (bCredit && iDrawType == 2){
                                List<TexasPokerBillService.UserPotDrawInfo> userPotDrawInfoList = new ArrayList<TexasPokerBillService.UserPotDrawInfo>();
                                if (mUserDrawStacks != null){
                                    for (Map.Entry<Long,Long> drawMap : mUserDrawStacks.entrySet()){
                                        TexasPokerBillService.UserPotDrawInfo.Builder stUserPotDrawInfo = TexasPokerBillService.UserPotDrawInfo.newBuilder();
                                        stUserPotDrawInfo.setUuid(drawMap.getKey());
                                        stUserPotDrawInfo.setLDrawNum(drawMap.getValue());
                                        stUserPotDrawInfo.setLGameRoomID(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                                        stUserPotDrawInfo.setLPotContribution(mUserOriginDrawStacks.get(drawMap.getKey()) == null ? 0 : mUserOriginDrawStacks.get(drawMap.getKey()));

                                        userPotDrawInfoList.add(stUserPotDrawInfo.build());
                                    }
                                }
                                BillManager.getInstance().setPotDrawCredit(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), userPotDrawInfoList);
                            }

                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1005");
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfo.getLGameHandID()
                                    + "," + 0
                                    + "," + "closetable");

                            //只返还在桌玩家金币
                            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                                PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1007");
                                UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                                if (ConfigaturationManager.bClubCredit && stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() > 0) {
                                    PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1009");
                                    try {
                                        if (CreditManager.getInstance().checkBuyInToCredit(stUserGameInfoNetBuilder.getStUserInfo().getUuid()
                                                , stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stUserGameInfoNetBuilder.getLBuyinClubID()) == 1) {
                                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 10011");
                                            CreditManager.getInstance().finishToCredit(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),
                                                    stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getLRemainStacks(), stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(),
                                                    getBuyStacks(stUserGameInfoNetBuilder.getStUserInfo().getUuid()));
                                        }
                                    } catch (Exception e) {
                                        PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 10013");
                                        e.printStackTrace();
                                    }
                                }
                                if (stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() > 0 && (stUserGameInfoNetBuilder.getLRemainStacks() + stUserGameInfoNetBuilder.getLUnloadStacks()) > 0) {
                                    PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 10015");

                                    // 授信牌桌返还联盟币,普通牌桌返还德扑币
                                    if (bCredit) {

                                        String msgKey = System.currentTimeMillis()+""+stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid()+(RandomUtils.nextInt(9000) + 1000)+4;

                                        ClubManager.getInstance().updateUserCreditInGame(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid(), -(stUserGameInfoNetBuilder.getLRemainStacks() + stUserGameInfoNetBuilder.getLUnloadStacks()), stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), 5, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 0, msgKey);
                                        String sLog = stUserGameInfoNetBuilder.getStUserInfo().getUuid() + " " + 0 + " " + 910004 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                + " " + msgKey + " " + " 0 " + stUserGameInfoNetBuilder.getLRemainStacks()+stUserGameInfoNetBuilder.getLUnloadStacks() + " " + ClubManager.getInstance().getClubUserCreditInfo(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid()).getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    } else {
                                        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stUserGameInfoNetBuilder.getLRemainStacks() + stUserGameInfoNetBuilder.getLUnloadStacks());
                                        String sLog = stUserGameInfoNetBuilder.getStUserInfo().getUuid() + " " + 0 + " " + 110004 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                + " 0 " + " 0 " + stUserGameInfoNetBuilder.getLRemainStacks()+stUserGameInfoNetBuilder.getLUnloadStacks() + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    }
                                }
                            }

                            pushAllGameRoomUserMsgs(null, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_OVER);
                            bOver = true;
                            //房间到时，删除房间
                            ConnectDatabase.getInstance().removeGameRoomInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            ConnectDatabase.getInstance().removeGameRoomMember(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            UserManager.getInstance().removeUserCreateGameRooms(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                                    , stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            ChatRoomManager.getInstance().removeChatRoomGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID(), stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            ClubManager.getInstance().removeChatRoomGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID(), stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID());

                            long coinCost = 0;
                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 10017");
                            // 联盟相关操作
                            if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() > 0) {
                                PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 10019");
                                if (stLeagueInfo != null) {
                                    PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100111");
                                    for (TexasPokerCommon.ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                                        PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100113");
                                        ClubManager.getInstance().removeChatRoomGameRoom(stClubBaseInfo.getLClubID(),
                                                stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),
                                                stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID());
                                    }
                                }

                                if (!bStarted) {// 如果牌局没有开始，则返还钻石
                                    PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100115");
                                    coinCost = GameFeeUtil.getLeagueGameFee((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds(), stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums(), bCredit ? 1 : 0);
                                }
                            } else if (stGameRoomInfo.getStGameRoomBaseInfo().hasBPaidStart() && stGameRoomInfo.getStGameRoomBaseInfo().getBPaidStart() == 1) {
                                PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100117");
                                // 此时说明非联盟局、并且创建时收取过钻石，应返还扣除数额
                                if (!bStarted) {// 如果牌局没有开始，则返还钻石
                                    PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100119");
                                    coinCost = GameFeeUtil.getLeagueGameFee((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds(), stGameRoomInfo.getStGameRoomBaseInfo().getIGameRoomUserMaxNums(), bCredit ? 1 : 0) / 2;
                                }
                            }
                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100121 coinCost: >" + coinCost + "<");

                            if (coinCost > 0 && coinCost <= 10000) {// 交易数量安全性保护
                                PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100123");
                                if (!bCredit){
                                    UserBaseInfoNet.Builder userBaseInfoNetBuilder = UserManager.getInstance().addUserCoin(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), coinCost);
                                    TranscationLogService.getInstance().error(userBaseInfoNetBuilder.getUuid() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            + " " + 110022 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLDuration() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getIMinBuyinRatio()
                                            + " " + stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio() + " " + coinCost + " " + userBaseInfoNetBuilder.getLCoin());
                                }
                            }
                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 100125");

                            BillManager.getInstance().setGameOverTag(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lMaxPot, mGameHandID);
                            saveBuyinStacks();

                            QuickGameRoomManager.getInstance().finishQuickGameRoom(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID());
                            mGameRoomDetailInfos.removeKey(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            List<Long> vPushUsers = BillManager.getInstance().getBillInfoUsers(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            for (long uuid : vPushUsers) {
//							pushAppPushGameFinish(uuid);
                            }

                            Iterator iter = mUserOutStacks.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                Long uuid = (Long) entry.getKey();
                                Long lRemainStacks = (Long) entry.getValue();
                                Long clubid = mUserBuyinClubIDs.get(uuid);
                                if (clubid == null) {
                                    clubid = (long) 0;
                                }

                                // 授信牌局和非授信transcation日志
                                if (bCredit) {
                                    String sLog = uuid + " " + clubid + " " + 910002 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            + " 0 " + " 0 " + lRemainStacks + " " + 0 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                    TranscationLogService.getInstance().error(sLog);
                                } else {
                                    String sLog = uuid + " " + clubid + " " + 110002 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            + " 0 " + " 0 " + lRemainStacks + " " + 0 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                    TranscationLogService.getInstance().error(sLog);
                                }
                            }

                            //把所有没有审批的买入请求，全部设置为拒绝
                            ConnectDatabase.getInstance().gameoverBuyInControls(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            //把实时买入记录拷贝到历史买入记录数据库中
                            ConnectDatabase.getInstance().copyToHistoryBuyInControls(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            //把买入记录从实时买入记录中删除
                            ConnectDatabase.getInstance().removeBuyInControls(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            PlayLogService.getInstance().debug("GameRoomDetailInfo.gameRoomTimeoutHandle - 1001000");
                        }

                        break;
                    } else {

                        //授信牌局需要检测当前是否需要抽水
                        if (bCredit && bDraw && iDrawType == 1){

                            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                            // 01.抽水操作
                            List<TexasPokerBillService.UserRemainstackAfterDraw> userRemainstackAfterDrawList = BillManager.getInstance().drawBill(1001L,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                            ArrayList<Long> vSitdownUsers = new ArrayList<Long>();

                            // 02.玩家抽水后余额同步玩家lRemainStacks字段
                            for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                                UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                //在桌玩家
                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                    for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList) {

                                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == userRemainstackAfterDraw.getUuid()) {
                                            stResetUserGameInfoBuilder.setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                                            vSitdownUsers.add(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                            break;
                                        }
                                    }
                                }

                                stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                            }

                            //旁观玩家
                            for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList){
                                if (mapStandByUserGameInfos.containsKey(userRemainstackAfterDraw.getUuid()) && !vSitdownUsers.contains(userRemainstackAfterDraw.getUuid())){
                                    mapStandByUserGameInfos.get(userRemainstackAfterDraw.getUuid()).setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                                }
                            }

                            // 03.修改抽水标志位为false
                            bDraw = false;

                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            // 04.检查玩家买入俱乐部是否冻结,若冻结强制站起玩家
                            for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                                UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                                // 只考虑当前在桌进行游戏的玩家
                                if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                    if (ClubManager.getInstance().isCreditClubFrozen(stResetUserGameInfoBuilder.getLBuyinClubID())){
                                        //玩家买入俱乐部冻结,强制站起
                                        GameRoomDetailInfo gameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                                        gameRoomDetailInfo.doStandUpAction(stResetUserGameInfoBuilder.getStUserInfo().getUuid(), true, 2);
                                    }
                                }
                            }

                            sendAllGameStateChange(0, null);
                        }

                        //房间没有到时见，继续游戏
                        //等待10秒钟以后，如果牌局里面大于两个人，需要把游戏状态改成start，并通知改牌局的所有玩家
                        if (bFirstWaitShowTableCard == false && bShowTableCard == true) {
                            bShowTableCard = false;
                            mCurrentTableCards.clear();
                        }

                        if (bFirstWaitShowTableCard == true) {
                            bFirstWaitShowTableCard = false;
                        }

                        resetGameRoom();
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        if (iCouldPlayNum >= 2 && getGameStarted() == true) {
                            //首先设置牌局的状态
                            stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart);
                            stGameRoomInfoBuilder.setBStateChange(1);


                            //最后把最新的牌局信息push给每一个进入牌局的用户
                            stGameRoomInfo = stGameRoomInfoBuilder.build();
                            sendAllGameStateChange(0, null);
                        }
                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                        mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
                        //判断是否满足自动开启的条件。1、开启自动开启2、牌局未开启 3、当前在桌人数大于设定自动开桌人数
                        if (stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIAutoStartNum() > 0
                                && getGameStarted() == false
                                && iCouldPlayNum >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIAutoStartNum()) {
                            //自动开启游戏
                            startGame(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), 1);

                            //授信牌局在自动开局时加入抽水计时机制
                            if (bCredit && iDrawType == 1) {
                                GameRoomManager.getInstance().setmGameRoomDraw(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                            }
                        }
                    }
                }
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameStart_VALUE: {
                    resetGameRoom();
                /*
				 * 检测IP-GPS
				 */
                    checkIpAndGps();
                    resetCard();
                    reCalculateCouldPlayNum();
                    bShowTableCard = false;
                    mCurrentTableCards.clear();

                    if (iCouldPlayNum >= 2) {
                        //增加handid
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        mGameHandID++;
                        stGameRoomInfoBuilder.setLGameHandID(mGameHandID);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                + "," + stGameRoomInfo.getLGameHandID()
                                + ",startround"
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());

                        saveOriginalStacks();

                        //重置Dealer位置
                        rsetetDealerPos();

                        //重新可以进行牌局的用户，去掉刚坐在小盲位的用户
                        couldPlayNumAfterSetDealer();

                        //刷新野保险下注超时重新计时
                        FlopLottoManager.getInstance().refreshBetTimeOut(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                        if (iCouldPlayNum >= 2) {
                            //每个选手发两张牌,并把游戏的状态设置为FirstRound
                            if (stGameRoomInfo.getStGameRoomBaseInfo().getBStraddle() == 1) {
                                beforeTheGameStraddle();
                            } else {
                                beforeTheGame();
                            }

                            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getIAnte() > 0) {
                                gotoAnteStatus();
                            }

                            if (stGameRoomInfo.getStGameRoomBaseInfo().getBStraddle() == 1) {
                                gotoStraddleStartStatus();
                            } else {
                                gotoStartStatus();
                            }
                        } else {
                            //重置刚坐下的状态
                            rsetetJustSitTag();

                            //重新可以进行牌局的用户，去掉刚坐在小盲位的用户
                            couldPlayNumAfterSetDealer();

                            //每个选手发两张牌,并把游戏的状态设置为FirstRound
                            beforeTheGame();
                            if (this.stGameRoomInfo.getStGameRoomBaseInfo().getIAnte() > 0) {
                                gotoAnteStatus();
                            }

                            gotoStartStatus();
                        }
                    }
                }
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result_VALUE: {
                    //从result状态需要编导wait状态，并且清空整个房间
                    //重置整个房间的状态
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    removeAllTimeOutUser();
                    mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);

                    sendAllGameStateChange(0, null);
                }

                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_SHOWCARD_VALUE:
                    resetGameRoom();

                    TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + 0
                            + "," + "cleartable");
                    removeAllTimeOutUser();

                    // 一手结束后,如果为授信牌局且这手结束抽水,进行抽水操作,同步玩家LRemainStacks字段,检查玩家买入俱乐部是否冻结,若冻结强制站起玩家
                    if (bCredit && bDraw && iDrawType == 1){

                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                        // 01.抽水操作
                        List<TexasPokerBillService.UserRemainstackAfterDraw> userRemainstackAfterDrawList = BillManager.getInstance().drawBill(1001L,stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());

                        ArrayList<Long> vdownUsers = new ArrayList<Long>();

                        // 02.玩家抽水后余额同步玩家lRemainStacks字段
                        for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                            // 在桌玩家
                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList) {

                                    if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == userRemainstackAfterDraw.getUuid()) {
                                        stResetUserGameInfoBuilder.setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                                        vdownUsers.add(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                        break;
                                    }
                                }
                            }

                            stGameRoomInfoBuilder.setVUserGameInfos(k, stResetUserGameInfoBuilder);
                        }

                        //旁观玩家
                        for (TexasPokerBillService.UserRemainstackAfterDraw userRemainstackAfterDraw : userRemainstackAfterDrawList){
                            if (mapStandByUserGameInfos.containsKey(userRemainstackAfterDraw.getUuid()) && !vdownUsers.contains(userRemainstackAfterDraw.getUuid())){
                                mapStandByUserGameInfos.get(userRemainstackAfterDraw.getUuid()).setLRemainStacks(userRemainstackAfterDraw.getLRemainstacks());
                            }
                        }

                        // 03.修改抽水标志位为false
                        bDraw = false;

                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        // 04.检查玩家买入俱乐部是否冻结,若冻结强制站起玩家
                        for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();

                            // 只考虑当前在桌进行游戏的玩家
                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                if (ClubManager.getInstance().isCreditClubFrozen(stResetUserGameInfoBuilder.getLBuyinClubID())){
                                    //玩家买入俱乐部冻结,强制站起
                                    GameRoomDetailInfo gameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                                    gameRoomDetailInfo.doStandUpAction(stResetUserGameInfoBuilder.getStUserInfo().getUuid(), true, 2);
                                }
                            }
                        }

                        sendAllGameStateChange(0, null);

                    }

                    //授信牌局每手结束检测玩家是否冻结,冻结则强制站起
                    if (bCredit){
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        List<TexasPokerHttpClub.ClubUserCrditInfo> vClubUserCrditInfo = new ArrayList<TexasPokerHttpClub.ClubUserCrditInfo>();

                        //只考虑在桌玩家
                        for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                            UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(k).toBuilder();
                            if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                                TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = TexasPokerHttpClub.ClubUserCrditInfo.newBuilder();
                                stClubUserCrditInfo.setUuid(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                stClubUserCrditInfo.setLClubID(stResetUserGameInfoBuilder.getLBuyinClubID());

                                vClubUserCrditInfo.add(stClubUserCrditInfo.build());
                            }
                        }

                        if (!CollectionUtils.isEmpty(vClubUserCrditInfo)){
                            ArrayList<Long> frozenUserList = ClubManager.getInstance().isCreditUserFrozen(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID(),vClubUserCrditInfo);

                            if (!CollectionUtils.isEmpty(frozenUserList)){
                                for (Long uuid : frozenUserList){
                                    //玩家被冻结,强制站起
                                    GameRoomDetailInfo gameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                                    gameRoomDetailInfo.doStandUpAction(uuid, true, 3);
                                }
                            }
                        }
                    }

                    //每手牌局结束检测玩家剩余筹码是否<=0,如果<=0弹出买入弹框,玩家进入买入弹框倒计时队列
                    for (int k = 0; k < this.iGameRoomUserMaxNums; k++) {
                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(k).toBuilder();
                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stResetUserGameInfoBuilder.getLRemainStacks() <= 0){
                            mBuyinTimeOut.setCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid(),stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                        }
                    }

                    mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);

                    break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop_VALUE: {
                    //检测机器人自动弃牌
                    robotCheck();

                    //如果allin状态，则发牌跳转到下一个状态
                    if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",gotoFlopAllStatus enter1" + stGameRoomInfo.getEGameState());

                        gotoFlopAllStatus(0);
                    }

                }
                mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One_VALUE:
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two_VALUE:
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three_VALUE: {
                    mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
                }
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
                    if (checkAllAllin()) {
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",checkGotoTurnStatus enter6" + stGameRoomInfo.getEGameState());

                        checkGotoTurnStatus(0);
                    } else {
                        //检测机器人自动弃牌
                        robotCheck();
                    }

                    mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
                }
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE: {
                    if (checkAllAllin()) {
                        checkGotoRiverStatus(0);
                    } else {
                        //检测机器人自动弃牌
                        robotCheck();
                    }

                    mGameRoomDetailInfos.setCurrent(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), this);
                }
                break;
                case GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE: {
                    if (checkAllAllin()) {
                        gotoResultStatus(0);
                    } else {
                        //检测机器人自动弃牌
                        robotCheck();
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
    private void resetGameRoom() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        iCouldPlayNum = 0;
        bAllAllin = false;
        lStepMaxStacks = 0;
        stGameRoomInfoBuilder.setLEffectiveRaise(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
        mHandMap.clear();
        vHandList.clear();
        mHandPos.clear();
        bFlopStatus = false;

        //���������������������������������������������
        stGameRoomInfoBuilder.clearVCurrentCards();
        stGameRoomInfoBuilder.clearVCurrentPots();
        stGameRoomInfoBuilder.clearVPotInfos();
        stGameRoomInfoBuilder.clearStGameResultInfo();

        stGameRoomInfoBuilder.setBAllAllin(0);
        //���������������������������������
        for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
            UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
            stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
            stUserGameInfoNetBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
            stUserGameInfoNetBuilder.setBGameDealer(0);
            stUserGameInfoNetBuilder.clearVCurrentHands();
            stUserGameInfoNetBuilder.setLBetStacks(0);
            stUserGameInfoNetBuilder.setIDelayLong(0);
            stUserGameInfoNetBuilder.setIDelaySingleTime(20);
            stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

            //用户筹码非0保护
            if (stUserGameInfoNetBuilder.getLRemainStacks() < 0) {
                stUserGameInfoNetBuilder.setLRemainStacks(0);
            }

            if (stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() == -1) {
                UserBaseInfoNet.Builder stUserBaseInfoBuilder = stUserGameInfoNetBuilder.getStUserInfoBuilder();
                stUserBaseInfoBuilder.setUuid(0);
                stUserGameInfoNetBuilder.setStUserInfo(stUserBaseInfoBuilder);
            } else if (stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid() > 0) {
                //如果用户被踢
                if (mUserKickingUser.containsKey(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid())) {
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    //踢人逻辑，首先把用户站起
                    doStandUpAction(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), true, 0);

                    //踢人消息
                    kickUserPush(stUserGameInfoNetBuilder, i, 1);

                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                } else if (mUserForceStandupUser.containsKey(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid())) {
                    //踢人逻辑，首先把用户站起
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    doStandUpAction(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), true, 0);

                    //踢人消息
                    forceStandupPush(stUserGameInfoNetBuilder, i);
                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                } else if (mTemporarilyLeaveUsers.containsKey(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid())) {
                    //如果需要补充筹码，则进行补充筹码的工作
                    //重置用户状态
                    stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                    if (stUserGameInfoNetBuilder.getBBuyin() == 1) {
                        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                        long popularity = 0;

                        popularity = (100 + GameFeeUtil.getServiceFeeRat((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds())) * stUserGameInfoNetBuilder.getLBuyinStacks() / 100;

                        if (stUserBaseInfoNetBuilder != null) {
                            if ((UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), popularity) && !this.bCredit)
                                    || (this.bCredit && ClubManager.getInstance().checkClubUserCredit(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stUserGameInfoNetBuilder.getLBuyinStacks())
                                        && UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(),stUserGameInfoNetBuilder.getLBuyinStacks())))) {

                                //zhangp

                                long buyStacks = stUserGameInfoNetBuilder.getLBuyinStacks();

                                if (stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio() / 10 + 1) * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()) {
                                    //解决连续大忙问题，暂时注释下个版本启用
//										if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1 && stUserGameInfoNetBuilder.getLRemainStacks() == 0){
//											stUserGameInfoNetBuilder.setBJustSit(1);
//										}

                                    String msgKey = System.currentTimeMillis()+""+stUserGameInfoNetBuilder.getStUserInfo().getUuid()+(RandomUtils.nextInt(9000) + 1000)+0;
                                    //授信牌局在写入账单之前先做联盟币更新,防止最大买入和最大允许亏损
                                    if (this.bCredit){
                                        ERROR_CODE_TYPE eErrorCodeType = ClubManager.getInstance().updateUserCreditInGame(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid(), buyStacks, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), 1, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 0, msgKey);
                                        if (eErrorCodeType != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                                            buyStacks = 0;
                                        } else {
                                            //扣除入座服务费
                                            stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), buyStacks));
                                            String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID() + " " + 910005 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + " 0 " + " 0 " + buyStacks + " " + stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate() + " " + GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), buyStacks) + " " + stUserBaseInfoNetBuilder.getLPopularity();
                                            TranscationLogService.getInstance().error(sLog);
                                        }
                                    }

                                    stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks);

                                    buyStakcsInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), buyStacks, stUserGameInfoNetBuilder.getLBuyinClubID(), 3);


                                    if (stUserGameInfoNetBuilder != null) {
                                        setBillCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
                                                stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                , stUserGameInfoNetBuilder.getLRemainStacks(), stUserGameInfoNetBuilder.getLUnloadStacks());
                                    }

                                    //授信局减去玩家联盟币,非授信局减去用户的银币
                                    if (this.bCredit) {
                                        //用户联盟币已减完,只做日志打印
                                        TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                                        String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + 910000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                + " " + msgKey + " " + " 0 " + buyStacks + " " + stClubUserCrditInfo.getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);

                                    } else {
                                        stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);

                                        String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + 110000 + " "
                                                + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " 0 " + " 0 "
                                                + buyStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                        TranscationLogService.getInstance().error(sLog);
                                    }

                                }
                            }
                        }

                        stUserGameInfoNetBuilder.setBBuyin(0);
                    }

                    //生成留座离桌时间
                    long lLeaveTime = System.currentTimeMillis();

                    //进入留座离桌状态
                    stUserGameInfoNetBuilder.setBTemporarilyLeave(1);
                    stUserGameInfoNetBuilder.setLTemporarilyLeaveTime(lLeaveTime);
                    stUserGameInfoNetBuilder.setBDelegate(0);
                    stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

                    //将用户设置回房间
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);


                    stUserGameInfoNetBuilder.clearVCurrentHands();

                    //给牌桌上其他用户push托管信息
                    for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                        UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);

                        if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                            pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), stUserGameInfoNet.getStUserInfo().getUuid(),
                                    GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                        }
                    }

                    //给旁观的人发送托管消息
                    for (long push_uuid : mStandBys) {
                        pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_TEMPORARILY_LEAVE, i);
                    }
                    mTemporarilyLeaveUsersTimeout.setCurrent(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                } else {

                    stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);

                    //重置延时道具
                    if (isDelayPlayer(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid())) {
                        stUserGameInfoNetBuilder.setIDelayTotalTimes(1);
                        stUserGameInfoNetBuilder.setIDelayCost(getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
                    } else {
                        stUserGameInfoNetBuilder.setIDelayTotalTimes(0);
                        stUserGameInfoNetBuilder.setIDelayCost(getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
                    }
                    stUserGameInfoNetBuilder.setIDelayLong(0);
                    stUserGameInfoNetBuilder.setIDelaySingleTime(20);
                    stUserGameInfoNetBuilder.setBShowCardOne(0);
                    stUserGameInfoNetBuilder.setBShowCardTwo(0);

                    if (stUserGameInfoNetBuilder.getBBuyin() == 1) {
                        iCouldPlayNum++;
                        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stUserGameInfoNetBuilder.getStUserInfo().getUuid());

                        long popularity = 0;

                        popularity = (100 + GameFeeUtil.getServiceFeeRat((int) stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds())) * stUserGameInfoNetBuilder.getLBuyinStacks() / 100;

                        if (stUserBaseInfoNetBuilder != null) {
                            if ((UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), popularity) && !this.bCredit)
                                    || (this.bCredit && ClubManager.getInstance().checkClubUserCredit(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stUserGameInfoNetBuilder.getLBuyinStacks())
                                        && UserManager.getInstance().checkUserPopularity(stUserBaseInfoNetBuilder.getUuid(), GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(),stUserGameInfoNetBuilder.getLBuyinStacks())))) {

                                long buyStacks = stUserGameInfoNetBuilder.getLBuyinStacks();

                                if (stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks < (this.stGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio() / 10 + 1) * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBuyIn()) {
                                    //解决连续大忙问题，暂时注释下个版本启用
//										if(stGameRoomInfo.getStGameRoomBaseInfo().getBStarted() == 1 && stUserGameInfoNetBuilder.getLRemainStacks() == 0){
//											stUserGameInfoNetBuilder.setBJustSit(1);
//										}

                                    String msgKey = System.currentTimeMillis()+""+stUserGameInfoNetBuilder.getStUserInfo().getUuid()+(RandomUtils.nextInt(9000) + 1000)+0;

                                    //授信牌局在写入账单之前先做联盟币更新,防止最大买入和最大允许亏损
                                    if (this.bCredit){
                                        ERROR_CODE_TYPE eErrorCodeType = ClubManager.getInstance().updateUserCreditInGame(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid(), buyStacks, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomInfo.getLGameHandID(), 1, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 0, msgKey);
                                        if (eErrorCodeType != ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                                            buyStacks = 0;
                                        } else {
                                            //扣除入座服务费
                                            stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), buyStacks));
                                            String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID() + " " + 910005 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + " 0 " + " 0 " + buyStacks + " " + stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate() + " " + GameFeeUtil.getSeatFee(stGameRoomInfo.getStGameRoomBaseInfo().getISeatFeeRate(), buyStacks) + " " + stUserBaseInfoNetBuilder.getLPopularity();
                                            TranscationLogService.getInstance().error(sLog);
                                        }
                                    }

                                    // zhangp 授信
                                    int state = 0;
                                    if (ConfigaturationManager.bClubCredit) {
                                        state = CreditManager.getInstance().checkBuyInToCredit(stUserBaseInfoNetBuilder.getUuid(),
                                                stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), stUserGameInfoNetBuilder.getLBuyinClubID());
                                    }
                                    //非授信玩家或者 已经有申请未走授信的买入 继续走原来的一套
                                    if ((state == 2 && usedInGame(stUserBaseInfoNetBuilder.getUuid())) || state == 0) {
                                        stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks);

                                        buyStakcsInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), buyStacks, stUserGameInfoNetBuilder.getLBuyinClubID(), 4);

                                        if (stUserGameInfoNetBuilder != null) {
                                            setBillCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
                                                    stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                    , stUserGameInfoNetBuilder.getLRemainStacks(), stUserGameInfoNetBuilder.getLUnloadStacks());
                                        }

                                        //授信局减去玩家联盟币,非授信局减去用户的银币
                                        if (this.bCredit) {
                                            //用户联盟币已减完,只做日志打印
                                            TexasPokerHttpClub.ClubUserCrditInfo.Builder stClubUserCrditInfo = ClubManager.getInstance().getClubUserCreditInfo(stUserGameInfoNetBuilder.getLBuyinClubID(), stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                                            String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + 910000 + " " + stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                    + " " + msgKey + " " + " 0 " + buyStacks + " " + stClubUserCrditInfo.getLCreditBalance() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                            TranscationLogService.getInstance().error(sLog);

                                        } else {
                                            //扣除用户德扑币
                                            stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);

                                            //打印日志
                                            String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + 110000 + " "
                                                    + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " 0 " + " 0 "
                                                    + buyStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                            TranscationLogService.getInstance().error(sLog);
                                        }

                                    } else {
                                        int iOnTable = 0;
                                        long remainstack = 0;
                                        stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                                            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(i).toBuilder();
                                            if (stUserGameInfoBuilder.getStUserInfo().getUuid() == stUserBaseInfoNetBuilder.getUuid()) {
                                                iOnTable = 1;
                                                remainstack = stUserGameInfoBuilder.getLRemainStacks();
                                            }
                                        }
                                        if (iOnTable == 1) {
                                            UserGameInfoNet.Builder stUserGameInfoBuilder = mapStandByUserGameInfos.get(stUserBaseInfoNetBuilder.getUuid());
                                            if (stUserGameInfoBuilder != null) {
                                                remainstack = stUserGameInfoBuilder.getLRemainStacks();
                                            }
                                        }
                                        int result = CreditManager.getInstance().buyInToCredit(stUserBaseInfoNetBuilder.getUuid(), stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(), stUserGameInfoNetBuilder.getLBuyinClubID(),
                                                buyStacks, getBuyStacks(stUserBaseInfoNetBuilder.getUuid()), iOnTable, stGameRoomInfoBuilder.getStGameRoomBaseInfo().getStrRoomName(), remainstack);
                                        if (result == 0) {
                                            stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks() + buyStacks);

                                            buyStakcsInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), buyStacks, stUserGameInfoNetBuilder.getLBuyinClubID(), 5);

                                            if (stUserGameInfoNetBuilder != null) {
                                                setBillCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
                                                        stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId()
                                                        , stUserGameInfoNetBuilder.getLRemainStacks(), stUserGameInfoNetBuilder.getLUnloadStacks());
                                            }

                                            //扣除用户德扑币
                                            stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(stUserBaseInfoNetBuilder.getUuid(), -popularity);

                                            //打印日志
                                            String sLog = stUserBaseInfoNetBuilder.getUuid() + " " + stUserGameInfoNetBuilder.getLBuyinClubID() + " " + 110000 + " "
                                                    + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + " 0 " + " 0 "
                                                    + buyStacks + " " + stUserBaseInfoNetBuilder.getLPopularity() + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                            TranscationLogService.getInstance().error(sLog);
                                        } else {
                                            CreditDenyPush(stUserGameInfoNetBuilder.getLBuyinStacks(), stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                                        }
                                    }

                                }
                            }
                        }

                        stUserGameInfoNetBuilder.setBBuyin(0);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    } else if (stUserGameInfoNetBuilder.getLRemainStacks() == 0) {
                        if (stUserGameInfoNetBuilder.getBDelegate() == 1) {
                            stGameRoomInfo = stGameRoomInfoBuilder.build();
                            //踢人逻辑，首先把用户站起
                            doStandUpAction(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), true, 0);

                            stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        } else {
                            stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                        }
                    } else {
                        iCouldPlayNum++;
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    }

                    if (stUserGameInfoNetBuilder.getBTemporarilyLeave() == 1) {
                        stUserGameInfoNetBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    }

                }
            }
        }

        //AOF模式下牌局开始才会触发卸码
        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1){
            if((iCouldPlayNum>= 2 && getGameStarted() == true) || mGameHandID > 0){
                if(!mUserUnload.isEmpty()){
                    Map<Long,Long> userUnloadNotDoing = new HashMap<>();
                    //带出逻辑
                    for(int i = 0 ; i < stGameRoomInfoBuilder.getVUserGameInfosCount();i++){
                        UserGameInfoNet.Builder stUserGameInfoNetBuilder =  stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                        //最后进入卸码逻辑
                        if(stUserGameInfoNetBuilder.getBUnloadingStacks() > 0){

                            //如果当前游戏状态是wait，带出请求又在wait里面，则不做处理
                            if(stGameRoomInfo.getEGameState().getNumber() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE && mUserWaitUnload.containsKey(stUserGameInfoNetBuilder.getStUserInfo().getUuid())){
                                userUnloadNotDoing.put(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), stUserGameInfoNetBuilder.getStUserInfo().getUuid());
                            }else{
                                //判断是否满足最少在卓筹码的条件
                                if(stUserGameInfoNetBuilder.getLRemainStacks() - stUserGameInfoNetBuilder.getBUnloadingStacks() < stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIUnloadRatio() * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()){
                                    long lUnloadStacks = stUserGameInfoNetBuilder.getBUnloadingStacks();
                                    //重置状态
                                    stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                    mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                    //带出失败发送push
                                    pushUnloadStacksMsgs(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_UNLOAD_STACK_NOT_ENOUGH, lUnloadStacks);
                                }else{
                                    long lUnloadStacks = stUserGameInfoNetBuilder.getBUnloadingStacks();
                                    //直接减少玩家牌桌上的筹码
                                    stUserGameInfoNetBuilder.setLRemainStacks(stUserGameInfoNetBuilder.getLRemainStacks() - stUserGameInfoNetBuilder.getBUnloadingStacks());
                                    //保存用户卸码的值
                                    stUserGameInfoNetBuilder.setLUnloadStacks(stUserGameInfoNetBuilder.getLUnloadStacks() + stUserGameInfoNetBuilder.getBUnloadingStacks());
                                    //重置状态
                                    stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                    mUserUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                    mUserWaitUnload.remove(stUserGameInfoNetBuilder.getStUserInfoBuilder().getUuid());
                                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                    //修改筹码状态
                                    setBillCurrentStacksInRoom(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),
                                            stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),stUserGameInfoNetBuilder.getLRemainStacks(),stUserGameInfoNetBuilder.getLUnloadStacks());
                                    //带出成功发送push
                                    pushUnloadStacksMsgs(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_UNLOAD_SUCCESS, lUnloadStacks);

                                    Long uuid = stUserGameInfoNetBuilder.getStUserInfo().getUuid();
                                    Long clubid = mUserBuyinClubIDs.get(uuid);
                                    if(clubid == null){
                                        clubid = (long)0;
                                    }
                                    String sLog = uuid + " " + clubid + " " + 110003 + " " + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                            + " 0 " + " 0 " + lUnloadStacks + " " + 0 +" " + stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
                                    TranscationLogService.getInstance().error(sLog);
                                }
                            }
                        }
                    }
                    //重置离开牌桌玩家的筹码,按照需求，站起或者退出牌局，不在桌时卸码取消，按照需求不用push
                    ArrayList<Long> vUnloadingUuids = new ArrayList<Long>(mUserUnload.keySet());
                    for(long uuid:vUnloadingUuids){
                        //未做处理的带出，这里不做处理
                        if(!userUnloadNotDoing.containsKey(uuid)){
                            UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
                            if(stUserGameInfoNetBuilder != null){
                                //重置状态
                                stUserGameInfoNetBuilder.setBUnloadingStacks(0);
                                mapStandByUserGameInfos.put(stUserGameInfoNetBuilder.getStUserInfo().getUuid(),stUserGameInfoNetBuilder);
                            }
                            mUserUnload.remove(uuid);
                        }
                    }
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    sendAllGameStateChange(0,null);
                    stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                }
            }
        }

        if (iThinkingInterval > 0) {
            GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
            stGameRoomBaseInfoBuilder.setIThinkingInterval(iThinkingInterval);
            stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
            iThinkingInterval = 0;
        }

        //踢出其站起的用户
        ArrayList<Long> vKickingUuids = new ArrayList<Long>(mUserKickingUser.keySet());
        for (long uuid : vKickingUuids) {
            UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
            if (stUserGameInfoNetBuilder != null) {
                //踢人消息
                kickUserPush(stUserGameInfoNetBuilder, 0, 0);
            }
        }
        mUserForceStandupUser.clear();
        mTemporarilyLeaveUsers.clear();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);

        mPotList.clear();

        saveBuyinStacks();
        //������������������������
        stGameRoomInfo = stGameRoomInfoBuilder.build();

    }

    private void resetStepStatus() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

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
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stResetUserGameInfoBuilder.setIDelayLong(0);
                                stResetUserGameInfoBuilder.setIDelaySingleTime(20);

                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                            }
                        } else {
                            if (bAllAllin == true) {
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                            }
                        }
                    } else {
                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

                    }

                    stResetUserGameInfoBuilder.setLBetStacks(0);
                    stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

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
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stResetUserGameInfoBuilder.setIDelayLong(0);
                                stResetUserGameInfoBuilder.setIDelaySingleTime(20);

                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                iBetting = 1;
                            }
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

        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    private void resetGameUserStatus() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        if (iCouldPlayNum == 2) {
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
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stResetUserGameInfoBuilder.setIDelayLong(0);
                                stResetUserGameInfoBuilder.setIDelaySingleTime(20);


                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                            }

                        } else {
                            if (bAllAllin == true) {
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);
                            }
                        }
                    } else {
                        stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_ALLIN);

                    }

                    stResetUserGameInfoBuilder.setLBetStacks(0);
                    stResetUserGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);

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
            }
        } else {
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
                                stResetUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stResetUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());

                                stResetUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stResetUserGameInfoBuilder.setIDelayLong(0);
                                stResetUserGameInfoBuilder.setIDelaySingleTime(20);

                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stResetUserGameInfoBuilder.getStUserInfo().getUuid());
                                iBetting = 1;
                            }
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

        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    //预动作检查处理
    private void preActionCheck() {
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

            if (stUserGameInfoNet != null
                    && stUserGameInfoNet.getStUserInfo().getUuid() > 0
                    && stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING) {

                //如果由于动作，执行相关与动作操作
                if (stUserGameInfoNet.getEPreAction() == USER_GAME_STATE.USER_GAME_STATE_CALL) {
                    //恢复与动作状态
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                    stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    long betstacks = lStepMaxStacks - stUserGameInfoNet.getLBetStacks();
                    if (betstacks > 0) {
                        if (betstacks <= stUserGameInfoNet.getLRemainStacks()) {
                            doCallAction(stUserGameInfoNet.getStUserInfo().getUuid(), betstacks, 0);
                        } else {
                            doCallAction(stUserGameInfoNet.getStUserInfo().getUuid(), stUserGameInfoNet.getLRemainStacks(), 0);
                        }
                    }
                } else if (stUserGameInfoNet.getEPreAction() == USER_GAME_STATE.USER_GAME_STATE_CHECK) {
                    //恢复与动作状态
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                    stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    //如果正在动作的用户
                    if (lStepMaxStacks == stUserGameInfoNet.getLBetStacks()) {
                        doCheckAction(stUserGameInfoNet.getStUserInfo().getUuid(), 0);
                    } else {
                        //不操作，并且取消用户的状态
                    }
                } else if (stUserGameInfoNet.getEPreAction() == USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                    //恢复与动作状态
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                    stUserGameInfoNetBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    //如果正在动作的用户
                    if (lStepMaxStacks == stUserGameInfoNet.getLBetStacks()) {
                        doCheckAction(stUserGameInfoNet.getStUserInfo().getUuid(), 0);
                    } else {
                        doFlodAction(stUserGameInfoNet.getStUserInfo().getUuid(), (long) -1, true);
                    }
                }

                return;
            }
        }
    }

    private void robotFlodCheck() {
        ArrayList<Long> timeoutUser = getTimeoutUsers();

        if (timeoutUser != null) {
            if (timeoutUser.size() > 0) {
                long uuid = timeoutUser.get(0);

                if (getTimeoutTimesUser(uuid) > 1) {
                    removeTimeoutTimesUser(uuid);
                    doStandUpAction(uuid, true, 0);
                } else {


                    UserGameInfoNet.Builder stUserGameInfoBuilder = null;
                    for (int k = 0; k < iGameRoomUserMaxNums; k++) {
                        UserGameInfoNet.Builder stResetUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(k).toBuilder();

                        if (stResetUserGameInfoBuilder.getStUserInfo().getUuid() == uuid) {
                            stUserGameInfoBuilder = stResetUserGameInfoBuilder;
                            break;
                        }
                    }

                    if (stUserGameInfoBuilder != null) {
                        if (lStepMaxStacks == stUserGameInfoBuilder.getLBetStacks()) {
                            addUserTimeoutTimes(uuid);
                            doCheckAction(uuid, 0);
                        } else {
                            addUserTimeoutTimes(uuid);
                            doFlodAction(uuid, (long) -1, false);
                        }
                    } else {
                        addUserTimeoutTimes(uuid);
                        doFlodAction(uuid, (long) -1, false);
                    }
                }
            }
        }
    }

    private void beforeTheGameStraddle() {

        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        if (iCouldPlayNum > 3) {

            //大于两个人的时候牌局出事状态的设置
            //然后设置牌局中玩家的状态
            //用户的较色
            int userRole = 0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
            //循环次数
            int iLoop = 0;

            //首先设置纽扣，小盲，大盲位
            for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                    //如果是小盲，并且用户刚刚坐下，则用户不能参加牌局

                    if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                            || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);
                    } else {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);

                        //设置用户的较色状态
                        if (0 == userRole) {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_DEAL);
                            stUserGameInfoBuilder.setBGameDealer(1);
                            //设置dealer的index
                            mDealPos = userIndex;
                            //寻找下一个小盲注
                            userRole++;
                        } else if (1 == userRole) {
                            //设置小盲的index
                            mSmallBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_SMALL_BLIND);
                            //寻找下一个大盲
                            userRole++;

                        } else if (2 == userRole) {
                            //设置大盲的index
                            mBigBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
                            //大盲为第一轮最后一个说话的人
                            mEndPos = userIndex;
                            //寻找普通玩家
                            userRole++;

                        } else if (3 == userRole) {
                            //设置大盲的index
                            mBigBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_STRADDLE);
                            //大盲为第一轮最后一个说话的人
                            mEndPos = userIndex;
                            //寻找普通玩家
                            userRole++;

                        } else {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                            stUserGameInfoBuilder.setBGameDealer(0);
                        }
                        stUserGameInfoBuilder.setBJustSit(0);
                    }

                } else {
                    stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                    stUserGameInfoBuilder.setBGameDealer(0);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }


            int startIndex = mBigBlindPos + 1;

            if (startIndex >= iGameRoomUserMaxNums) {
                startIndex -= iGameRoomUserMaxNums;
            }
            iLoop = 0;
            //寻找第一轮的理论结束位的后一位，枪口位
            for (int j = startIndex; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                    //找到大盲位后面的第一个用户的位置
                    mEndPos = userIndex;
                    break;
                }
            }

            //最后把最新的牌局信息push给每一个进入牌局的用户
            stGameRoomInfo = stGameRoomInfoBuilder.build();


            //把牌局的信息push给每个人
            sendAllGameStateChange(0, null);
        } else if (iCouldPlayNum == 3) {

            //大于两个人的时候牌局出事状态的设置
            //然后设置牌局中玩家的状态
            //用户的较色
            int userRole = 0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
            //循环次数
            int iLoop = 0;

            //首先设置纽扣，小盲，大盲位
            for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                    //如果是小盲，并且用户刚刚坐下，则用户不能参加牌局

                    if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                            || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);
                    } else {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);

                        //设置用户的较色状态
                        if (0 == userRole) {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_DEAL);
                            stUserGameInfoBuilder.setBGameDealer(1);
                            //设置dealer的index
                            mDealPos = userIndex;
                            //寻找下一个小盲注
                            userRole++;
                        } else if (1 == userRole) {
                            //设置小盲的index
                            mSmallBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_SMALL_BLIND);
                            //寻找下一个大盲
                            userRole++;

                        } else if (2 == userRole) {
                            //设置大盲的index
                            mBigBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
                            //大盲为第一轮最后一个说话的人
                            mEndPos = userIndex;
                            //寻找普通玩家
                            userRole++;

                        } else {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                            stUserGameInfoBuilder.setBGameDealer(0);
                        }
                        stUserGameInfoBuilder.setBJustSit(0);
                    }

                } else {
                    stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                    stUserGameInfoBuilder.setBGameDealer(0);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }


            int startIndex = mBigBlindPos + 1;

            if (startIndex >= iGameRoomUserMaxNums) {
                startIndex -= iGameRoomUserMaxNums;
            }
            iLoop = 0;
            //寻找第一轮的理论结束位的后一位，枪口位
            for (int j = startIndex; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                    //找到大盲位后面的第一个用户的位置
                    mEndPos = userIndex;
                    break;
                }
            }

            //最后把最新的牌局信息push给每一个进入牌局的用户
            stGameRoomInfo = stGameRoomInfoBuilder.build();


            //把牌局的信息push给每个人
            sendAllGameStateChange(0, null);
        } else {
            //如果两个人的时候，牌局初始状态的设置
            int userRole = 0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
            //循环次数
            int iLoop = 0;

            //首先设置纽扣，小盲，大盲位
            for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                    //设置游戏的用户状态
                    stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                    stUserGameInfoBuilder.setBGameDealer(0);
                    stUserGameInfoBuilder.setIDelayLong(0);
                    stUserGameInfoBuilder.setIDelaySingleTime(20);


                    if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                            || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {
                        //直接跳过，不发牌
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                    }
                    //设置用户的较色状态
                    else if (0 == userRole) {
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
                    } else if (1 == userRole) {
                        //寻找下一个大盲（因为是两个人的牌局，所以大盲就是dealer）
                        mBigBlindPos = userIndex;
                        stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
                        userRole++;
                        stUserGameInfoBuilder.setBJustSit(0);
                    } else if (2 == userRole) {
                        //设置大盲的index
                        mBigBlindPos = userIndex;
                        //寻找普通玩家
                        userRole++;
                    } else {
                        stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                        stUserGameInfoBuilder.setBGameDealer(0);
                    }

                } else {
                    stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                    stUserGameInfoBuilder.setBGameDealer(0);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }


            //最后把最新的牌局信息push给每一个进入牌局的用户
            stGameRoomInfo = stGameRoomInfoBuilder.build();
        }
    }

    private void beforeTheGame() {

        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

        if (iCouldPlayNum > 2) {

            //大于两个人的时候牌局出事状态的设置
            //然后设置牌局中玩家的状态
            //用户的较色
            int userRole = 0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
            //循环次数
            int iLoop = 0;

            //首先设置纽扣，小盲，大盲位
            for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                    //如果是小盲，并且用户刚刚坐下，则用户不能参加牌局

                    if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                            || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);
                    } else {
                        //设置游戏的用户状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                        stUserGameInfoBuilder.setBGameDealer(0);
                        stUserGameInfoBuilder.setIDelayLong(0);
                        stUserGameInfoBuilder.setIDelaySingleTime(20);

                        //设置用户的较色状态
                        if (0 == userRole) {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_DEAL);
                            stUserGameInfoBuilder.setBGameDealer(1);
                            //设置dealer的index
                            mDealPos = userIndex;
                            //寻找下一个小盲注
                            userRole++;
                        } else if (1 == userRole) {
                            //设置小盲的index
                            mSmallBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_SMALL_BLIND);
                            //寻找下一个大盲
                            userRole++;

                        } else if (2 == userRole) {
                            //设置大盲的index
                            mBigBlindPos = userIndex;
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
                            //大盲为第一轮最后一个说话的人
                            mEndPos = userIndex;
                            //寻找普通玩家
                            userRole++;

                        } else {
                            stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                            stUserGameInfoBuilder.setBGameDealer(0);
                        }
                        stUserGameInfoBuilder.setBJustSit(0);
                    }

                } else {
                    stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                    stUserGameInfoBuilder.setBGameDealer(0);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }


            int startIndex = mBigBlindPos + 1;

            if (startIndex >= iGameRoomUserMaxNums) {
                startIndex -= iGameRoomUserMaxNums;
            }
            iLoop = 0;
            //寻找第一轮的理论结束位的后一位，枪口位
            for (int j = startIndex; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0) {
                    //找到大盲位后面的第一个用户的位置
                    mEndPos = userIndex;
                    break;
                }
            }

            //最后把最新的牌局信息push给每一个进入牌局的用户
            stGameRoomInfo = stGameRoomInfoBuilder.build();


            //把牌局的信息push给每个人
            sendAllGameStateChange(0, null);
        } else {
            //如果两个人的时候，牌局初始状态的设置
            int userRole = 0;//0 代表deal 1代表小盲 2代表大盲 3代表普通
            //循环次数
            int iLoop = 0;

            //首先设置纽扣，小盲，大盲位
            for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                    //设置游戏的用户状态
                    stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_READY);
                    stUserGameInfoBuilder.setBGameDealer(0);
                    stUserGameInfoBuilder.setIDelayLong(0);
                    stUserGameInfoBuilder.setIDelaySingleTime(20);


                    if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                            || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {
                        //直接跳过，不发牌
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                    }
                    //设置用户的较色状态
                    else if (0 == userRole) {
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
                    } else if (1 == userRole) {
                        //寻找下一个大盲（因为是两个人的牌局，所以大盲就是dealer）
                        mBigBlindPos = userIndex;
                        stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_BIG_BLIND);
                        userRole++;
                        stUserGameInfoBuilder.setBJustSit(0);
                    } else if (2 == userRole) {
                        //设置大盲的index
                        mBigBlindPos = userIndex;
                        //寻找普通玩家
                        userRole++;
                    } else {
                        stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                        stUserGameInfoBuilder.setBGameDealer(0);
                    }

                } else {
                    stUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                    stUserGameInfoBuilder.setBGameDealer(0);
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }


            //最后把最新的牌局信息push给每一个进入牌局的用户
            stGameRoomInfo = stGameRoomInfoBuilder.build();
        }
    }

    /**
     * 初始化函数
     */
    public GameRoomDetailInfo() {
        mTimeoutGameUsers = new AdvancedTimeoutHashMap<Long, Long>();

        mBuyinControlTimeOut = new TimeoutHashMap<Long, Long>();
        mBuyinControlTimeOut.setTimeout(150000);

        mStandByTimeOut = new TimeoutHashMap<Long, Long>();
        mStandByTimeOut.setTimeout(12000);

        mBuyinTimeOut = new TimeoutHashMap<Long, Long>();
        mBuyinTimeOut.setTimeout(300000);

        vTestHandCards1.add(2);
        vTestHandCards1.add(3);

        vTestHandCards2.add(26);
        vTestHandCards2.add(51);

        vTestTableCards.add(0);
        vTestTableCards.add(9);
        vTestTableCards.add(10);
        vTestTableCards.add(11);
        vTestTableCards.add(12);
    }

    public static int getDelayCost(int times) {
        switch (times) {
            case 0:
                return 0;
            case 1:
                return 20;
        }
        return 50 * (times - 1);
    }

    /****************************
     * 延时相关函数
     *******************************/
    public void addDelayPlayer(long uuid) {
        mDelayPlayer.put(uuid, uuid);
    }

    public boolean isDelayPlayer(long uuid) {
        if (mDelayPlayer.get(uuid) == null) {
            return false;
        } else {
            return true;
        }
    }

    /****************************
     * 结算辅助函数
     *******************************/
    public boolean inNoChargeList(long uuid) {
        if (mapUserNoCharge.containsKey(uuid)) {
            return true;
        } else {
            return false;
        }
    }

    public void addNorChargeList(long uuid) {
        mapUserNoCharge.put(uuid, uuid);
    }

    public void removeNorChargeList(long uuid) {
        mapUserNoCharge.remove(uuid);
    }

    //牌局结束push
    void pushAppPushGameFinish(long touuid) {
        APNManager.getInstance().gameOverPush(touuid, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 1);
    }

    void pushAppPushBuyinInvite(long leagueID, long clubid, String strNickname) {
        if (leagueID == 0) {
            APNManager.getInstance().buyinPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), strNickname, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 1);
        } else {
            if (stLeagueInfo != null) {
                for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                    if (stClubBaseInfo.getLClubID() == clubid) {
                        APNManager.getInstance().buyinPush(stClubBaseInfo.getStCreateUserBaseInfo().getUuid(), strNickname, stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName(), 1);
                    }
                }
            }
        }
    }

    //牌局进入push
    void pushAppPushEnterGame(long fromuuid, int iGameRoomUsers) {
        UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(fromuuid);
        //获取push相关信息
        if (stFromUserBaseInfoNetBuilder != null) {
            APNManager.getInstance().enterGameRoomPush(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                    , stFromUserBaseInfoNetBuilder.getStrNick()
                    , getStGameRoomInfo().getStGameRoomBaseInfo().getStrRoomName()
                    , iGameRoomUsers
                    , 1);
        }
    }

    void pushAppPushStartGame() {
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0) {
                APNManager.getInstance().StartGameRoomPush(stUserGameInfoNet.getStUserInfo().getUuid()
                        , stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
                        , 1);

            }
        }

        for (long standUuid : getStandByUuids()) {
            if (standUuid > 0) {
                APNManager.getInstance().StartGameRoomPush(standUuid
                        , stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
                        , 1);
            }
        }
    }

    void pushAppPushGameSeatEmpty() {
//		if(stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
//			long sb = stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds();
//			int ante = stGameRoomInfo.getStGameRoomBaseInfo().getIAnte();
//			float fHour =  (float)stGameRoomInfo.getStGameRoomBaseInfo().getLDuration()/(float)3600;
//			 APNManager.getInstance().emptyGameRoomPush(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
//					 , stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName()
//					 , ""+fHour , sb, ante, 1);
//		}
    }

//	public ArrayList<BuyinControl> getAllBuyinControls(long clubid){
//		ArrayList<BuyinControl> vBuyinControls = new ArrayList<BuyinControl>();
//		Iterator iter = mBuyinControlInfo.entrySet().iterator();
//
//		while (iter.hasNext()) {
//			Map.Entry entry = (Map.Entry) iter.next();
//			Long uuid = (Long) entry.getKey();
//
//			UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
//			if(stUserBaseInfoNetBuilder!=null){
//				BuyinControl.Builder stBuyinControlBuilder = BuyinControl.newBuilder();
//				long lStacks = mBuyinControlInfo.get(uuid);
//				long lCreateTime = mBuyinControlTimes.get(uuid);
//				long lClubID = mBuyinControlClubIDs.get(uuid);
//
//
//				if(lClubID==clubid || clubid==0){
//					stBuyinControlBuilder.setStUserBaseInfo(stUserBaseInfoNetBuilder);
//					stBuyinControlBuilder.setLGameRoomID(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
//					stBuyinControlBuilder.setSGameRoomName(stGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
//					stBuyinControlBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL);
//					stBuyinControlBuilder.setLStacks(lStacks);
//					stBuyinControlBuilder.setLBuyinCreateTime(lCreateTime);
//					ClubBaseInfo.Builder stClubBaseInfoBuilder = ClubManager.getInstance().getClubBaseInfo(lClubID);
//					if(stClubBaseInfoBuilder!=null){
//						stBuyinControlBuilder.setSClubName(stClubBaseInfoBuilder.getSClubName());
//					}
//					stBuyinControlBuilder.setLClubID(lClubID);
//					vBuyinControls.add(stBuyinControlBuilder.build());
//				}else{
//
//				}
//			}
//		}
//		return vBuyinControls;
//	}

    //用户进入房间
    //用户进入房间
    public ERROR_CODE_TYPE enterGameRoom(long uuid) {
        synchronized (this) {
            try {
                if (mUserKickedUser.containsKey(uuid)) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_BLACKED;
                }
                //把用户加入房间
                //获取gameroom builder,这里要注意toBuilder的方式，看看toBuilder是否会有问题
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

                //开发游戏
                boolean bInClub = false;
                if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() == 0) {
                    if (stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
                        bInClub = ClubManager.getInstance().inClub(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLChatRoomID(), uuid);
                    } else {
                        bInClub = true;
                    }
                } else {
                    bInClub = true;
                }

                //如果不在俱乐部。则返回错误码
                if (bInClub == false) {
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_CLUB_NO_USER;
                }

                if (containUser(uuid)) {
                    //如果用户已经存在房间了，就直接返回
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet stUserGameInfo = stGameRoomInfo.getVUserGameInfos(i);
                        if (stUserGameInfo.getStUserInfo().getUuid() == uuid) {
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                        }
                    }
                } else if (mapStandByUserGameInfos.containsKey(uuid)) {
                    //如果用户已经存在房间了，就直接返回
                    addStandByUuid(uuid);
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                } else {
                    //生成新的游戏玩家，放入旁观队列,暂离游戏
                    TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();

                    UserBaseInfoNet stUserBaseInfo = UserManager.getInstance().getUserBaseInfo(uuid).build();
                    if (stUserBaseInfo != null) {
                        userGameInfoBuilder.setStUserInfo(stUserBaseInfo);
                        userGameInfoBuilder.setLRoomId(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
                        userGameInfoBuilder.setLRemainStacks(0);

                        userGameInfoBuilder.setLBetStacks(0);
                        userGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);
                        userGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                        userGameInfoBuilder.setBGameDealer(0);
                        userGameInfoBuilder.setEPreAction(USER_GAME_STATE.USER_GAME_STATE_WAIT);
                        //设置座位和用户的对应关系
                        mapStandByUserGameInfos.put(uuid, userGameInfoBuilder);
                        addStandByUuid(uuid);
                        setStGameRoomInfo(stGameRoomInfoBuilder.build());

                        ConnectDatabase.getInstance().insertInGameUser(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                , uuid);

                        //APP push
                        if (stGameRoomInfo != null && stGameRoomInfo.getStGameRoomBaseInfo() != null
                                && stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo() != null) {

                            int iTotal = 0;
                            for (int k = 0; k < stGameRoomInfo.getVUserGameInfosCount(); k++) {
                                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(k);
                                if (stUserGameInfoNet.getStUserInfo().getUuid() > 0)
                                    iTotal++;
                            }

                            pushAppPushEnterGame(uuid, iTotal);
                        }

                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                    } else {
                        return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
                    }
                }

                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
            } catch (Exception e) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
            }
        }
    }

    public int getDealyTimes(long uuid) {
        for (int i = 0; i < this.stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

            if (stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                return stUserGameInfoNet.getIDelayTotalTimes();
            }
        }
        return 0;
    }

    public void setDealyTimes(long uuid, int times) {
        synchronized (this) {
            GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();

            for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfoBuilder.getVUserGameInfos(i);
                if (stUserGameInfoNet != null) {
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                    if (stUserGameInfoNetBuilder.getStUserInfo().getUuid() == uuid) {
                        stUserGameInfoNetBuilder.setIDelayTotalTimes(times);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                        addDelayPlayer(uuid);
                    }
                }
            }

            stGameRoomInfo = stGameRoomInfoBuilder.build();
        }
    }

    public ERROR_CODE_TYPE useDelayItem(long uuid, long lBettingID) {
        synchronized (this) {
            for (int i = 0; i < this.stGameRoomInfo.getVUserGameInfosCount(); i++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                if (stUserGameInfoNet.getStUserInfo().getUuid() == uuid
                        && stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        && stUserGameInfoNet.getLBettingID() == lBettingID) {

                    if (stUserGameInfoNet.getIDelayTotalTimes() == 0) {
                        //直接使用延时道具
                        //延时功能
                        if (delayTimeout(stUserGameInfoNet.getStUserInfo().getUuid(), 20)) {
                            //打印日志
                            UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
                            if (stUserBaseInfoNetBuilder != null) {
                                TranscationLogService.getInstance().error(uuid + " " + 0 + " " + 430003 + " " + 1 + " 0 " + "0 0 " + stUserBaseInfoNetBuilder.getLCoin());
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
                    } else {
                        UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
                        if (stUserBaseInfoNetBuilder != null) {
                            if (UserManager.getInstance().checkUserCoin(stUserBaseInfoNetBuilder.getUuid(), stUserGameInfoNet.getIDelayCost())) {
                                //延时功能
                                if (delayTimeout(stUserGameInfoNet.getStUserInfo().getUuid(), 20)) {
                                    //首先设置回包
                                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                                    stUserBaseInfoNetBuilder = UserManager.getInstance().addUserCoin(stUserBaseInfoNetBuilder.getUuid(), -stUserGameInfoNet.getIDelayCost());
                                    stUserGameInfoNetBuilder.setIDelayTotalTimes(stUserGameInfoNetBuilder.getIDelayTotalTimes() + 1);
                                    stUserGameInfoNetBuilder.setIDelayLong(stUserGameInfoNetBuilder.getIDelayLong() + 20);
                                    stUserGameInfoNetBuilder.setIDelayCost(GameRoomDetailInfo.getDelayCost(stUserGameInfoNetBuilder.getIDelayTotalTimes()));
                                    stUserGameInfoNetBuilder.setStUserInfo(stUserBaseInfoNetBuilder);

                                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                                    stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                                    //打印日志
                                    TranscationLogService.getInstance().error(uuid + " " + 0 + " " + 430003 + " " + 1 + " 0 " + "0 " + stUserGameInfoNet.getIDelayCost() + " " + stUserBaseInfoNetBuilder.getLCoin());

                                    addDelayPlayer(uuid);

                                    pushAllGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_DELAY);
                                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                                }
                                break;
                            } else {
                                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN;
                            }
                        } else {
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER;
                        }
                    }
                }
            }

            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_INPOS;
        }
    }

    public ERROR_CODE_TYPE resetBuyinRatio(long uuid, int iMinBuyinRatio, int iMaxBuyinRatio) {
        synchronized (this) {
            if (stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid) {
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(iMaxBuyinRatio);
                stGameRoomBaseInfoBuilder.setIMinBuyinRatio(iMinBuyinRatio);
                stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                stGameRoomInfo = stGameRoomInfoBuilder.build();

                //发送push消息
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet stUserGameInfoSendNet = stGameRoomInfo.getVUserGameInfos(i);


                    if ((stUserGameInfoSendNet.getStUserInfo().getUuid() > 0) &&
                            (stUserGameInfoSendNet.getStUserInfo().getUuid() != stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid())) {
                        //首先找到离开房间的用户基本信息
                        pushGameRoomUserMsgs(null, stUserGameInfoSendNet.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESET_BUYIN_RATIO);
                    }
                }

                if (mapStandByUserGameInfos.get(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) != null) {
                    //首先找到离开房间的用户基本信息
                    pushGameRoomUserMsgs(null, stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(),
                            GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_RESET_BUYIN_RATIO);
                }

                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_GAME_ROOM_OWNER;
            }

        }
    }

    private void reCalculateCouldPlayNum() {
        iCouldPlayNum = 0;
        iUserNum = 0;
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0
                    && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_WAIT
                    && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD
                    && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_STANDBY) {
                iCouldPlayNum++;
            }
        }
        iUserNum = iCouldPlayNum;
    }

    private void gotoAnteStatus() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Ante);
        stGameRoomInfoBuilder.setBStateChange(1);

        int iLoop = 0;

        if (iCouldPlayNum >= 2) {
            //每个用户都下注
            for (int j = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                //从dealer开始，每次找到
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();

                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY) {

                    if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getIAnte()) {
                        TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                + "," + stGameRoomInfoBuilder.getLGameHandID()
                                + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                + "," + "ante"
                                + "," + stUserGameInfoBuilder.getLRemainStacks());
                    } else {
                        TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                + "," + stGameRoomInfoBuilder.getLGameHandID()
                                + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                + "," + "ante"
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
                    }

                    if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getIAnte()) {
                        //先下小盲注
                        stUserGameInfoBuilder.setLBetStacks(stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());

                        //设置剩余赌注数
                        stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                - stGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
                    } else {
                        //先下小盲注
                        stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                        //设置剩余赌注数
                        stUserGameInfoBuilder.setLRemainStacks(0);
                    }

                    mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());
                }

                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            sendAllGameStateChange(0, null);

            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {
                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();

                resetGameUserStatus();

                sendAllGameStateChange(0, null);
            } else {
                SidePot();
                resetGameUserStatus();
            }
        } else {
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            resetGameRoom();
            sendAllGameStateChange(0, null);
        }

    }

    public boolean usedInGame(long uuid) {
        UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
        if (stUserGameInfoNetBuilder != null && stUserGameInfoNetBuilder.getStUserInfo().getUuid() == uuid) {
            Long lBuyinStacks = mUserBuyinStacks.get(uuid);
            if (lBuyinStacks != null && lBuyinStacks > 0) {
                return true;
            }
        }
        return false;
    }

    public long getBuyStacks(long uuid) {
        UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
        if (stUserGameInfoNetBuilder != null && stUserGameInfoNetBuilder.getStUserInfo().getUuid() == uuid) {
            Long lBuyinStacks = mUserBuyinStacks.get(uuid);
            if (lBuyinStacks != null && lBuyinStacks > 0) {
                return lBuyinStacks;
            }
        }
        return 0;
    }

    private void gotoStartStatus() {
        //每个选手发两张牌,并把游戏的状态设置为FirstRound
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop);
        stGameRoomInfoBuilder.setBStateChange(1);

        int userRole = 1;//0 代表deal 1代表小盲 2代表大盲 3代表普通
        int iLoop = 0;

        vPosPriority.clear();

        if (iCouldPlayNum > 2) {
            for (int j = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                //从dealer开始，每次找到
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                    int card1 = 0;
                    int card2 = 0;

                    if (GameRoomDetailInfo.NOT_DEBUG) {
                        card1 = getHandCard();
                        card2 = getHandCard();
                    } else {
                        card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 0, false);
                        card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 1, false);
                    }

                    //首先给每个人发两张手牌 如果开启AOF模式且依次看牌手牌为-1
                    if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                    } else {
                        stUserGameInfoBuilder.addVCurrentHands(card1);
                        stUserGameInfoBuilder.addVCurrentHands(card2);
                    }

                    Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    if (lRemainStacks == null) {
                        lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
                    }
                    HandInfo stHandInfo = new HandInfo();
                    stHandInfo.addCards(card1, card2);
                    stHandInfo.setlRemainStacks(lRemainStacks);
                    mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stHandInfo);
                    vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());

                    //设置牌手位置优先级数据
                    if (!vPosPriority.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                        vPosPriority.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                            + "," + "hand"
                            + "," + card1
                            + "," + card2
                            + "," + lRemainStacks;

                    InsuranceApi.parseGameAction(content);

                    switch (userRole) {
                        case 1:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);

                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
                            }

                            //如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
                            } else {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }


                            userRole = 2;
                            break;
                        case 2:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                            }

                            //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                            } else {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }

                            userRole = 3;
                            break;
                        case 3:
                            //设置需要进行下注的人的状态
                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            if (stUserGameInfoBuilder.getLRemainStacks() > 0 && checkAllAllin() == false) {
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stUserGameInfoBuilder.setIDelayLong(0);
                                stUserGameInfoBuilder.setIDelaySingleTime(20);
                                //如果AOF开启下发玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }

                                //把玩家放到超时检测队列里面
                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                                userRole = 4;
                            }

                            //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            break;

                    }
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }

            if (lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
            }
            iLoop = 0;

            stGameRoomInfo = stGameRoomInfoBuilder.build();
            if (userRole == 3 && checkAllAllin() == false) {
                for (int p = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, p++) {
                    int userIndex_spec = 0;
                    if (p < iGameRoomUserMaxNums) {
                        userIndex_spec = p;
                    } else {
                        userIndex_spec = p - iGameRoomUserMaxNums;
                    }

                    //从dealer开始，每次找到
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex_spec).toBuilder();

                    if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                            && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND)) {
                        //设置需要进行下注的人的状态
                        if (stUserGameInfoBuilder.getLRemainStacks() > 0) {
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                            stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                            stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            //如果AOF开启下发玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            //把玩家放到超时检测队列里面
                            removeAllTimeOutUser();
                            setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                            userRole = 4;

                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex_spec, stUserGameInfoBuilder.build());
                        }

                        //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                            stUserGameInfoBuilder.clearVCurrentHands();
                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                        }

                        break;

                    }
                }
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();

            sendAllGameStateChange(0, null);
            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

                //AOF模式下依次看牌下发手牌保护,如果玩家都为allin状态了直接下发玩家手牌
                GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
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

                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                resetStepStatus();
                sendAllGameStateChange(0, null);
            }
        } else if (iCouldPlayNum == 2) {

            //把牌局的信息push给每个人，目前的实现还有一些问题。最后需要把push和设置用户状态分离
            //说先设置房间里面的各个成员的状态
            for (int j = 0; j < iGameRoomUserMaxNums; j++) {
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();

                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                    //生成两张手牌
                    int card1 = 0;
                    int card2 = 0;
                    if (GameRoomDetailInfo.NOT_DEBUG) {
                        card1 = getHandCard();
                        card2 = getHandCard();
                    } else {
                        card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 0, false);
                        card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 1, false);
                    }

                    Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    if (lRemainStacks == null) {
                        lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
                    }

                    //发牌到到用户手上 如果开启AOF模式手牌为-1
                    if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                    } else {
                        stUserGameInfoBuilder.addVCurrentHands(card1);
                        stUserGameInfoBuilder.addVCurrentHands(card2);
                    }

                    HandInfo stHandInfo = new HandInfo();
                    stHandInfo.addCards(card1, card2);
                    stHandInfo.setlRemainStacks(lRemainStacks);
                    mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stHandInfo);
                    vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    mHandPos.put(j, stUserGameInfoBuilder.getStUserInfo().getUuid());

                    //设置牌手位置优先级数据
                    if (!vPosPriority.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                        vPosPriority.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                            + "," + "hand"
                            + "," + card1
                            + "," + card2
                            + "," + lRemainStacks;

                    InsuranceApi.parseGameAction(content);

                    if (j == mBigBlindPos) {
                        //设置大盲的游戏状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);

                        if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "bb"
                                    + "," + stUserGameInfoBuilder.getLRemainStacks());
                        } else {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "bb"
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                        }

                        //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                        if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                            //先下大盲注
                            stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                            //设置剩余赌注数
                            stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                        } else {
                            //先下大盲注
                            stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                            //设置剩余赌注数
                            stUserGameInfoBuilder.setLRemainStacks(0);

                            //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }
                        }
                        if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                            lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                        }


                    } else {
                        if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()) {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "sb"
                                    + "," + stUserGameInfoBuilder.getLRemainStacks());
                        } else {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "sb"
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
                        }

                        //设置小盲的游戏状态
                        if (stUserGameInfoBuilder.getLRemainStacks() > 0) {
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                            stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                            stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            //如果AOF开启下发玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            //如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
                            } else {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }

                            //把要动作的用户设置到超时队列里面去
                            removeAllTimeOutUser();
                            setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                        }

                        //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                            stUserGameInfoBuilder.clearVCurrentHands();
                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                        }

                        if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                            lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                        }
                    }

                    //设置房间的游戏状态
                    stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());
                }
            }
            if (lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();

            sendAllGameStateChange(0, null);
            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

                //AOF模式下依次看牌下发手牌保护,如果玩家都为allin状态了直接下发玩家手牌
                GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
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

                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                resetStepStatus();
                sendAllGameStateChange(0, null);
            }
        } else {
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            resetGameRoom();
            sendAllGameStateChange(0, null);
        }
    }

    private void gotoStraddleStartStatus() {
        //每个选手发两张牌,并把游戏的状态设置为FirstRound
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop);
        stGameRoomInfoBuilder.setBStateChange(1);

        int userRole = 1;//0 代表deal 1代表小盲 2代表大盲 3代表普通
        int iLoop = 0;

        vPosPriority.clear();

        if (iCouldPlayNum > 3) {
            for (int j = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                //从dealer开始，每次找到
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                    int card1 = 0;
                    int card2 = 0;

                    if (GameRoomDetailInfo.NOT_DEBUG) {
                        card1 = getHandCard();
                        card2 = getHandCard();
                    } else {
                        card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 0, false);
                        card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 1, false);
                    }

                    //首先给每个人发两张手牌 如果AOF开启且依次看牌下发-1
                    if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                    } else {
                        stUserGameInfoBuilder.addVCurrentHands(card1);
                        stUserGameInfoBuilder.addVCurrentHands(card2);
                    }

                    Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    if (lRemainStacks == null) {
                        lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
                    }
                    HandInfo stHandInfo = new HandInfo();
                    stHandInfo.addCards(card1, card2);
                    stHandInfo.setlRemainStacks(lRemainStacks);
                    mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stHandInfo);
                    vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());

                    //设置牌手位置优先级数据
                    if (!vPosPriority.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                        vPosPriority.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                            + "," + "hand"
                            + "," + card1
                            + "," + card2
                            + "," + lRemainStacks;

                    InsuranceApi.parseGameAction(content);

                    switch (userRole) {
                        case 1:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);

                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
                            }

                            //如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
                            } else {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }


                            userRole = 2;
                            break;
                        case 2:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                            }

                            //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                            } else {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }

                            userRole = 3;
                            break;
                        case 3:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
                            if (stUserGameInfoBuilder.getLRemainStacks() < 2 * stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "straddle"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "straddle"
                                        + "," + 2 * stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                            }

                            //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= 2 * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(2 * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - 2 * stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                            } else {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }

                            userRole = 4;
                            break;
                        case 4:
                            //设置需要进行下注的人的状态
                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            if (stUserGameInfoBuilder.getLRemainStacks() > 0 && checkAllAllin() == false) {
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stUserGameInfoBuilder.setIDelayLong(0);
                                stUserGameInfoBuilder.setIDelaySingleTime(20);
                                //如果AOF开启下发玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }

                                //把玩家放到超时检测队列里面
                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                                userRole = 5;
                            }

                            //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            break;

                    }
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }

            if (lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
            }
            iLoop = 0;

            stGameRoomInfo = stGameRoomInfoBuilder.build();
            if (userRole == 4 && checkAllAllin() == false) {
                for (int p = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, p++) {
                    int userIndex_spec = 0;
                    if (p < iGameRoomUserMaxNums) {
                        userIndex_spec = p;
                    } else {
                        userIndex_spec = p - iGameRoomUserMaxNums;
                    }

                    //从dealer开始，每次找到
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex_spec).toBuilder();

                    if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                            && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND)) {
                        //设置需要进行下注的人的状态
                        if (stUserGameInfoBuilder.getLRemainStacks() > 0) {
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                            stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                            stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            //如果AOF开启下发玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            //把玩家放到超时检测队列里面
                            removeAllTimeOutUser();
                            setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                            userRole = 4;

                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex_spec, stUserGameInfoBuilder.build());
                        }

                        //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                            stUserGameInfoBuilder.clearVCurrentHands();
                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                        }

                        break;

                    }
                }
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();

            sendAllGameStateChange(0, null);
            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

                //AOF模式下依次看牌下发手牌保护,如果玩家都为allin状态了直接下发玩家手牌
                GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
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

                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                resetStepStatus();
                sendAllGameStateChange(0, null);
            }
        } else if (iCouldPlayNum == 3) {
            for (int j = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
                int userIndex = 0;
                if (j < iGameRoomUserMaxNums) {
                    userIndex = j;
                } else {
                    userIndex = j - iGameRoomUserMaxNums;
                }

                //从dealer开始，每次找到
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                    int card1 = 0;
                    int card2 = 0;

                    if (GameRoomDetailInfo.NOT_DEBUG) {
                        card1 = getHandCard();
                        card2 = getHandCard();
                    } else {
                        card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 0, false);
                        card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 1, false);
                    }

                    //首先给每个人发两张手牌 如果AOF开启且依次看牌下发-1
                    if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                    } else {
                        stUserGameInfoBuilder.addVCurrentHands(card1);
                        stUserGameInfoBuilder.addVCurrentHands(card2);
                    }

                    Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    if (lRemainStacks == null) {
                        lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
                    }
                    HandInfo stHandInfo = new HandInfo();
                    stHandInfo.addCards(card1, card2);
                    stHandInfo.setlRemainStacks(lRemainStacks);
                    mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stHandInfo);
                    vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    mHandPos.put(userIndex, stUserGameInfoBuilder.getStUserInfo().getUuid());

                    //设置牌手位置优先级数据
                    if (!vPosPriority.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                        vPosPriority.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                            + "," + "hand"
                            + "," + card1
                            + "," + card2
                            + "," + lRemainStacks;

                    InsuranceApi.parseGameAction(content);

                    switch (userRole) {
                        case 1:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);

                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "sb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
                            }

                            //如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
                            } else {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }


                            userRole = 2;
                            break;
                        case 2:
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);
                            if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stUserGameInfoBuilder.getLRemainStacks());
                            } else {
                                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                                        + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                        + "," + "bb"
                                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                            }

                            //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks()
                                        - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                            } else {
                                //先下大盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }
                            if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                                lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                            }

                            userRole = 3;
                            break;
                        case 3:
                            //设置需要进行下注的人的状态
                            stGameRoomInfo = stGameRoomInfoBuilder.build();

                            if (stUserGameInfoBuilder.getLRemainStacks() > 0 && checkAllAllin() == false) {
                                stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                                stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                                stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                                stUserGameInfoBuilder.setIDelayLong(0);
                                stUserGameInfoBuilder.setIDelaySingleTime(20);
                                //如果AOF开启下发玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }

                                //把玩家放到超时检测队列里面
                                removeAllTimeOutUser();
                                setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                                userRole = 4;
                            }

                            //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            break;

                    }
                }
                stGameRoomInfoBuilder.setVUserGameInfos(userIndex, stUserGameInfoBuilder.build());
            }

            if (lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
            }
            iLoop = 0;

            stGameRoomInfo = stGameRoomInfoBuilder.build();
            if (userRole == 3 && checkAllAllin() == false) {
                for (int p = mSmallBlindPos; iLoop < iGameRoomUserMaxNums; iLoop++, p++) {
                    int userIndex_spec = 0;
                    if (p < iGameRoomUserMaxNums) {
                        userIndex_spec = p;
                    } else {
                        userIndex_spec = p - iGameRoomUserMaxNums;
                    }

                    //从dealer开始，每次找到
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(userIndex_spec).toBuilder();

                    if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                            && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BLIND)) {
                        //设置需要进行下注的人的状态
                        if (stUserGameInfoBuilder.getLRemainStacks() > 0) {
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                            stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                            stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            //如果AOF开启下发玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            //把玩家放到超时检测队列里面
                            removeAllTimeOutUser();
                            setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());
                            userRole = 4;

                            stGameRoomInfoBuilder.setVUserGameInfos(userIndex_spec, stUserGameInfoBuilder.build());
                        }

                        //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                            stUserGameInfoBuilder.clearVCurrentHands();
                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                        }

                        break;

                    }
                }
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();

            sendAllGameStateChange(0, null);
            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

                //AOF模式下依次看牌下发手牌保护,如果玩家都为allin状态了直接下发玩家手牌
                GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
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

                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                resetStepStatus();
                sendAllGameStateChange(0, null);
            }
        } else if (iCouldPlayNum == 2) {

            //把牌局的信息push给每个人，目前的实现还有一些问题。最后需要把push和设置用户状态分离
            //说先设置房间里面的各个成员的状态
            for (int j = 0; j < iGameRoomUserMaxNums; j++) {
                UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(j).toBuilder();
                if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                        && (stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_BETTING
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_ALLIN
                        || stUserGameInfoBuilder.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_READY)) {
                    //生成两张手牌
                    int card1 = 0;
                    int card2 = 0;
                    if (GameRoomDetailInfo.NOT_DEBUG) {
                        card1 = getHandCard();
                        card2 = getHandCard();
                    } else {
                        card1 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 0, false);
                        card2 = getCard(stUserGameInfoBuilder.getStUserInfo().getUuid(), 1, false);
                    }

                    Long lRemainStacks = mOriginalStacks.get(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    if (lRemainStacks == null) {
                        lRemainStacks = stUserGameInfoBuilder.getLRemainStacks();
                    }

                    //发牌到到用户手上 如果AOF开启且依次看牌下发-1
                    if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                        stUserGameInfoBuilder.addVCurrentHands(-1);
                    } else {
                        stUserGameInfoBuilder.addVCurrentHands(card1);
                        stUserGameInfoBuilder.addVCurrentHands(card2);
                    }

                    HandInfo stHandInfo = new HandInfo();
                    stHandInfo.addCards(card1, card2);
                    stHandInfo.setlRemainStacks(lRemainStacks);
                    mHandMap.put(stUserGameInfoBuilder.getStUserInfo().getUuid(), stHandInfo);
                    vHandList.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    mHandPos.put(j, stUserGameInfoBuilder.getStUserInfo().getUuid());

                    //设置牌手位置优先级数据
                    if (!vPosPriority.contains(stUserGameInfoBuilder.getStUserInfo().getUuid())) {
                        vPosPriority.add(stUserGameInfoBuilder.getStUserInfo().getUuid());
                    }

                    String content = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                            + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                            + "," + stGameRoomInfo.getLGameHandID()
                            + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                            + "," + "hand"
                            + "," + card1
                            + "," + card2
                            + "," + lRemainStacks;

                    InsuranceApi.parseGameAction(content);

                    if (j == mBigBlindPos) {
                        //设置大盲的游戏状态
                        stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BLIND);

                        if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "bb"
                                    + "," + stUserGameInfoBuilder.getLRemainStacks());
                        } else {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "bb"
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
                        }

                        //如果剩余筹码量大于等于大盲注，则按照正常的流程下注,否则按照全下处理
                        if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds()) {
                            //先下大盲注
                            stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());

                            //设置剩余赌注数
                            stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLBigBlinds());
                        } else {
                            //先下大盲注
                            stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                            //设置剩余赌注数
                            stUserGameInfoBuilder.setLRemainStacks(0);

                            //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }
                        }
                        if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                            lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                        }


                    } else {
                        //设置小盲的游戏状态
                        if (stUserGameInfoBuilder.getLRemainStacks() < stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds()) {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "sb"
                                    + "," + stUserGameInfoBuilder.getLRemainStacks());
                        } else {
                            TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                    + "," + stGameRoomInfoBuilder.getLGameHandID()
                                    + "," + stUserGameInfoBuilder.getStUserInfo().getUuid()
                                    + "," + "sb"
                                    + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
                        }

                        if (stUserGameInfoBuilder.getLRemainStacks() > 0) {
                            stUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_BETTING);
                            stUserGameInfoBuilder.setLBettingID(System.currentTimeMillis());
                            stUserGameInfoBuilder.setLActTime(System.currentTimeMillis());
                            stUserGameInfoBuilder.setIDelayLong(0);
                            stUserGameInfoBuilder.setIDelaySingleTime(20);
                            //如果AOF开启下发玩家手牌
                            if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                stUserGameInfoBuilder.clearVCurrentHands();
                                stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                            }

                            //如果剩余筹码量大于等于小盲注，则按照正常的流程下注,否则按照全下处理
                            if (stUserGameInfoBuilder.getLRemainStacks() >= stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds()) {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(stUserGameInfoBuilder.getLRemainStacks() - stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLSmallBlinds());
                            } else {
                                //先下小盲注
                                stUserGameInfoBuilder.setLBetStacks(stUserGameInfoBuilder.getLRemainStacks());

                                //设置剩余赌注数
                                stUserGameInfoBuilder.setLRemainStacks(0);

                                //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
                                    stUserGameInfoBuilder.clearVCurrentHands();
                                    stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                                }
                            }

                            //把要动作的用户设置到超时队列里面去
                            removeAllTimeOutUser();
                            setTimeoutUserCurrent(stUserGameInfoBuilder.getStUserInfo().getUuid());

                        }

                        //AOF模式下依次看牌开启此时玩家是被迫allin需带玩家手牌
                        if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1 && stUserGameInfoBuilder.getLRemainStacks() == 0){
                            stUserGameInfoBuilder.clearVCurrentHands();
                            stUserGameInfoBuilder.addAllVCurrentHands(mHandMap.get(stUserGameInfoBuilder.getStUserInfo().getUuid()).getvCards());
                        }

                        if (lStepMaxStacks < stUserGameInfoBuilder.getLBetStacks()) {
                            lStepMaxStacks = stUserGameInfoBuilder.getLBetStacks();
                        }
                    }

                    //设置房间的游戏状态
                    stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoBuilder.build());
                }
            }
            if (lStepMaxStacks < stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds()) {
                lStepMaxStacks = stGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds();
            }
            stGameRoomInfo = stGameRoomInfoBuilder.build();

            sendAllGameStateChange(0, null);
            //设置房间的allin状态
            if (stGameRoomInfo.getEGameState() != GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Result && checkAllAllin() == true) {

                //AOF模式下依次看牌下发手牌保护,如果玩家都为allin状态了直接下发玩家手牌
                GameRoomInfo.Builder tGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                if (stGameRoomInfo.getStGameRoomBaseInfo().hasBFullAllIn() && stGameRoomInfo.getStGameRoomBaseInfo().getBFullAllIn() == 1 && stGameRoomInfo.getStGameRoomBaseInfo().getBLookCardTurn() == 1){
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

                SidePot();
                stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setBAllAllin(1);
                stGameRoomInfo = stGameRoomInfoBuilder.build();
                resetStepStatus();
                sendAllGameStateChange(0, null);
            }
        } else {
            stGameRoomInfo = stGameRoomInfoBuilder.build();
            resetGameRoom();
            sendAllGameStateChange(0, null);
        }
    }


    public void helloGame(long uuid) {
        addStandByUuid(uuid);
    }

    public void gotoNextStep() {


        synchronized (this) {
//				TableLogService.getInstance().error1(lFromRoomID
//						+"," + lGameRoomID
//						+"," + stGameRoomInfo.getLGameHandID()
//						+",gotoNextStep " + stGameRoomInfo.getEGameState());

            switch (stGameRoomInfo.getEGameState().getNumber()) {
                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_VALUE: {
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",gotoNextStep1 ROOM_GAME_STATE_Flop_VALUE");
                    gotoTurnStatus(0, true);
                }
                break;
                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn_VALUE: {
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",gotoNextStep1 ROOM_GAME_STATE_Turn_VALUE");
                    gotoRiverStatus(0, true);
                }
                break;
                case TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River_VALUE: {
//					TableLogService.getInstance().error1(lFromRoomID
//							+"," + lGameRoomID
//							+"," + stGameRoomInfo.getLGameHandID()
//							+",gotoNextStep1 ROOM_GAME_STATE_River_VALUE");
                    gotoResultStatus(0);
                }
                break;
            }


//				TableLogService.getInstance().error1(lFromRoomID
//						+"," + lGameRoomID
//						+"," + stGameRoomInfo.getLGameHandID()
//						+",gotoNextStep2 " + stGameRoomInfo.getEGameState());
        }


    }

    /**
     * 买入保险
     *
     * @param uuid              买保险的用户
     * @param lGameHandID       买入牌局的手ID
     * @param eGameRoomStatus   买入时候的牌局状态
     * @param vBuyInsuranceInfo 买入动作集合
     */
    public GameRoomInfo buyInsurance(long uuid, long lGameHandID, GAME_ROOM_GAME_STATE eGameRoomStatus, List<BuyInsuranceInfo> vBuyInsuranceInfo) {
        synchronized (this) {
            try {
                InsuranceInfo stInsuranceInfo = InsuranceApi.doInsuranceAction(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lGameHandID, eGameRoomStatus, uuid, vBuyInsuranceInfo);
                if (stInsuranceInfo != null) {
                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();

                    UserGameInfoNet stUserGameInfoNet = null;
                    for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                        UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                        if (stTempUserGameInfoNet.getStUserInfo().getUuid() > 0 && stTempUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                            stUserGameInfoNet = stTempUserGameInfoNet;
                        }
                    }
                    //push相关
                    pushAllGameRoomUserMsgsIncludeSelf(stUserGameInfoNet, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUY_INSURANCE);

                    return stGameRoomInfo;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * 房主设置的用户是否可以买入保险
     *
     * @param uuid          房主
     * @param lSettedUuid   被设置的用户
     * @param bCanInsurance 是否可以买入保险
     */
    public ERROR_CODE_TYPE creatorInsuranceSetting(long uuid, long lSettedUuid, int bCanInsurance) {
        synchronized (this) {
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
     *
     * @param uuid              设置的用户
     * @param bDefaultInsurance 是否可以买入保险
     */
    public ERROR_CODE_TYPE userInsuranceSetting(long uuid, int bDefaultInsurance) {
        synchronized (this) {
            boolean bInsurance = false;
            if (bDefaultInsurance > 0) {
                bInsurance = InsuranceApi.setUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, true);
            } else {
                bInsurance = InsuranceApi.setUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, false);
            }

            if (bInsurance == false) {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SET_INSURANCE;
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            }
        }
    }

    /**
     * 保险延时道具
     *
     * @param uuid           设置的用户
     * @param lGameHandID    手ID
     * @param eGameRoomState 牌局的状态
     */
    public InsuranceUserInfo buyInsuranceDealy(long uuid, long lGameHandID, GAME_ROOM_GAME_STATE eGameRoomState) {
        synchronized (this) {
            InsuranceInfo stInsuranceInfo = InsuranceApi.delayInsuranceAction(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lGameHandID, eGameRoomState, uuid);

            if (stInsuranceInfo != null) {
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
                stGameRoomInfo = stGameRoomInfoBuilder.build();

                UserGameInfoNet stUserGameInfoNet = null;
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet stTempUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
                    if (stTempUserGameInfoNet.getStUserInfo().getUuid() > 0 && stTempUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                        stUserGameInfoNet = stTempUserGameInfoNet;
                    }
                }

                //push相关
                pushAllGameRoomUserMsgsIncludeSelf(stUserGameInfoNet, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_INSURANCE_DELAY);

                for (InsuranceUserInfo stInsuranceUserInfo : stInsuranceInfo.getVInsuranceUserInfosList()) {

                    if (stInsuranceUserInfo.getUuid() == uuid) {
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
    private void rsetetDealerPos() {
        int startIndex = mDealPos + 1;
        if (startIndex >= iGameRoomUserMaxNums) {
            startIndex -= iGameRoomUserMaxNums;
        }
        int iLoop = 0;
        for (int j = startIndex; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
            int userIndex = 0;
            if (j < iGameRoomUserMaxNums) {
                userIndex = j;
            } else {
                userIndex = j - iGameRoomUserMaxNums;
            }

            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(userIndex).toBuilder();

            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0
                    && stUserGameInfoBuilder.getLRemainStacks() > 0
                    && stUserGameInfoBuilder.getBJustSit() == 0
                    && stUserGameInfoBuilder.getBTemporarilyLeave() == 0) {
                mDealPos = userIndex;
                return;
            }
        }
        mDealPos = startIndex;

    }

    /**
     * 保险超时
     */
    public void doInsuranceTimeout() {
        synchronized (this) {
            InsuranceApi.doInsuranceTimeout(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId());
        }
    }

    /*
	 * 检查重复购买
	 */
    public boolean checkUserRebuy(long uuid, List<BuyInsuranceInfo> vBuyInsuranceInfo) {
        synchronized (this) {
            return InsuranceApi.checkUserRebuy(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid, vBuyInsuranceInfo);
        }
    }

    /**
     * 获取保险状态
     */
    public int getGameSetting(long uuid) {
        boolean bNotBuyInsurance = InsuranceApi.getUserInsuranceClose(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), uuid);
        if (bNotBuyInsurance == true) {
            return 1;
        } else {
            return 0;
        }
    }

    private void couldPlayNumAfterSetDealer() {
        iCouldPlayNum = 0;
        //首先设置纽扣，小盲，大盲位
        int iLoop = 0;

        int userRole = 0;
        for (int j = mDealPos; iLoop < iGameRoomUserMaxNums; iLoop++, j++) {
            int userIndex = 0;
            if (j < iGameRoomUserMaxNums) {
                userIndex = j;
            } else {
                userIndex = j - iGameRoomUserMaxNums;
            }

            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfo.getVUserGameInfos(userIndex).toBuilder();
            //判断这个位置是否有用户坐下，没有用户坐下，则过掉这个用户
            if (stUserGameInfoBuilder.getStUserInfo().getUuid() > 0 && stUserGameInfoBuilder.getLRemainStacks() > 0) {
                //如果是小盲，并且用户刚刚坐下，则用户不能参加牌局
                if ((stUserGameInfoBuilder.getBJustSit() == 1 && userRole == 1)
                        || stUserGameInfoBuilder.getBTemporarilyLeave() == 1) {

                } else {
                    //设置用户的较色状态
                    if (0 == userRole) {
                        userRole++;
                    } else if (1 == userRole) {
                        //寻找下一个大盲
                        userRole++;
                    }
                    iCouldPlayNum++;
                }
            }
        }
    }

    //计算dealer位置
    private void rsetetJustSitTag() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        for (int i = 0; i < iGameRoomUserMaxNums; i++) {
            UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();

            stUserGameInfoBuilder.setBJustSit(0);

            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
        }

        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    public void pushInsuranceTimeout(long uuid, InsuranceInfo stInsuranceInfo) {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.setVInsuranceInfos(stInsuranceInfo);
        stGameRoomInfo = stGameRoomInfoBuilder.build();
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

            if (stUserGameInfoNet.getStUserInfo().getUuid() > 0 && stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                pushGameRoomUserMsgs(stUserGameInfoNet, uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_BUY_INSURANCE, i);
            }
        }
    }

    //踢人功能
    public void kickUserInGame(long uuid) {
        synchronized (this) {
            for (int i = 0; i < this.stGameRoomInfo.getVUserGameInfosCount(); i++) {
                //获取炮狙用户信息
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                //找到要取消托管状态的用户
                if (stUserGameInfoNet != null
                        && stUserGameInfoNet.getStUserInfo().getUuid() > 0
                        && stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {

                    //获取牌局中玩家的信息
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();

                    //判断踢出状态
                    if (stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                            || stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_STANDBY) {
                        //没在打牌中，直接踢出,相当于用户离开房间
                        doStandUpAction(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), true, 0);

                        //踢人消息
                        kickUserPush(stUserGameInfoNetBuilder, i, 1);
                    } else {
                        //这个时间，不能踢人，先记录，然后等到牌局结束再踢人
                        mUserKickingUser.put(uuid, uuid);
                        return;
                    }
                }
            }

            //如果被踢的人在旁观队列，踢出用户
            if (mStandBys.contains(uuid)) {
                UserGameInfoNet.Builder stUserGameInfoNetBuilder = mapStandByUserGameInfos.get(uuid);
                if (stUserGameInfoNetBuilder != null) {
                    //踢人消息
                    kickUserPush(stUserGameInfoNetBuilder, 0, 0);
                    return;
                }
            }
        }
    }

    /**
     * 强制站起
     * @param uuid
     */
    public void forceStandup(long uuid) {
        synchronized (this) {
            for (int i = 0; i < this.stGameRoomInfo.getVUserGameInfosCount(); i++) {
                //获取炮狙用户信息
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);

                //找到要取消托管状态的用户
                if (stUserGameInfoNet != null
                        && stUserGameInfoNet.getStUserInfo().getUuid() > 0
                        && stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {

                    //获取牌局中玩家的信息
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();

                    //判断踢出状态
                    if (stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_WAIT
                            || stUserGameInfoNet.getEGameState() == USER_GAME_STATE.USER_GAME_STATE_STANDBY) {
                        //没在打牌中，直接踢出,相当于用户离开房间
                        doStandUpAction(stUserGameInfoNetBuilder.getStUserInfo().getUuid(), true, -1);

                        //踢人消息
                        forceStandupPush(stUserGameInfoNetBuilder, i);
                    } else {
                        //这个时间，不能踢人，先记录，然后等到牌局结束再踢人
                        mUserForceStandupUser.put(uuid, uuid);
                        return;
                    }
                }
            }
        }
    }

    //牌局黑名单接口
    public void addBlackList(long uuid) {
        mUserKickedUser.put(uuid, uuid);
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        stGameRoomInfoBuilder.addVKickUserIDs(uuid);
        stGameRoomInfo = stGameRoomInfoBuilder.build();
    }

    //踢人接口
    public void kickUserPush(UserGameInfoNet.Builder stUserGameInfoNetBuilder, int pos, int bKickedInGame) {
        //加入牌局黑名单
        addBlackList(stUserGameInfoNetBuilder.getStUserInfo().getUuid());

        //给牌桌上的用户发送返回牌桌的消息
        stUserGameInfoNetBuilder.clearVCurrentHands();
        stUserGameInfoNetBuilder.setBKickedInGame(bKickedInGame);
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);

            if (stUserGameInfoNetIn.getStUserInfo().getUuid() > 0) {
                pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), stUserGameInfoNetIn.getStUserInfo().getUuid(),
                        GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_KICK, pos);
            }
        }

        //给旁观的人发返回牌桌消息
        for (long push_uuid : mStandBys) {
            pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_KICK, pos);
        }

        //从旁观列表里面删除(保险起见)
        removeStandByUuid(stUserGameInfoNetBuilder.getStUserInfo().getUuid());

        //把人从踢人列表里面删除
        mUserKickingUser.remove(stUserGameInfoNetBuilder.getStUserInfo().getUuid());
    }

    //游戏强制结束接口
    public void cancelGame() {
        //加入牌局黑名单
        bGameCanceled = true;

        //发送push，通知牌局要结束
        pushCancelGame();
    }

    private void pushCancelGame() {
        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);

            if (stUserGameInfoNetIn.getStUserInfo().getUuid() > 0) {
                pushGameRoomUserMsgs(null, stUserGameInfoNetIn.getStUserInfo().getUuid(),
                        GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_CANCEL, 0);
            }
        }

        //给旁观的人发返回牌桌消息
        for (long push_uuid : mStandBys) {
            pushGameRoomUserMsgs(null, push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_CANCEL, 0);
        }
    }

    public LeagueInfo getStLeagueInfo() {
        return stLeagueInfo;
    }

    public void setStLeagueInfo(LeagueInfo stLeagueInfo) {
        this.stLeagueInfo = stLeagueInfo;
    }

    public void buyStakcsInRoom(long uuid, long lBuyStacks, long clubid, int type) {
        Long lBuyinStacks = mUserBuyinStacks.get(uuid);
        if (lBuyinStacks == null) {
            lBuyinStacks = (long) 0;
        }

        String sClubName = "";
        //检查联盟信息,取出相关的俱乐部信息
        if (stLeagueInfo != null) {
            for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                if (stClubBaseInfo.getLClubID() == clubid && stClubBaseInfo.getSClubName() != null) {
                    sClubName = stClubBaseInfo.getSClubName();
                }
            }
        }

        lBuyinStacks += lBuyStacks;
        mUserBuyining.put(uuid, uuid);

        NormalLogService.getInstance().error("uuid:" + uuid + " roomid:" + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                + " lBuyinStacks:" + lBuyinStacks + " lBuyStacks:" + lBuyStacks + " clubid:" + clubid + " type:" + type);

        //修改账单信息
        if (BillManager.getInstance().buyStackInRoom(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lBuyinStacks, clubid, sClubName)) {
            mUserBuyining.remove(uuid);

            Long lRemainStacks = mUserOutStacks.get(uuid);
            if (lRemainStacks == null) {
                mUserOutStacks.put(uuid, lBuyinStacks);
            } else {
                mUserOutStacks.put(uuid, lBuyinStacks + lRemainStacks);
            }

        }
        mUserBuyinStacks.put(uuid, lBuyinStacks);
        mUserBuyinClubIDs.put(uuid, clubid);
    }

    public void saveBuyinStacks() {
        Iterator iter = mUserBuyining.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Long uuid = (Long) entry.getKey();

            long lBuyinStacks = mUserBuyinStacks.get(uuid);
            long clubid = mUserBuyinClubIDs.get(uuid);

            String sClubName = "";
            //检查联盟信息,取出相关的俱乐部信息
            if (stLeagueInfo != null) {
                for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                    if (stClubBaseInfo.getLClubID() == clubid && stClubBaseInfo.getSClubName() != null) {
                        sClubName = stClubBaseInfo.getSClubName();
                    }
                }
            }

            if (BillManager.getInstance().buyStackInRoom(uuid, stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(), lBuyinStacks, clubid, sClubName)) {
                mUserBuyining.remove(uuid);

                Long lRemainStacks = mUserOutStacks.get(uuid);
                if (lRemainStacks == null) {
                    mUserOutStacks.put(uuid, lBuyinStacks);
                } else {
                    mUserOutStacks.put(uuid, lBuyinStacks + lRemainStacks);

                }

            }
        }
    }

    //俱乐部买入控制push接口
    //俱乐部买入控制push接口
    private void sendAllBuyinControls(BuyinControlMsg stBuyinControlMsg, long clubID, long exceptionalUuid) {
        //如果是俱乐部牌局，则看创建牌局的人是否是俱乐部的管理员，如果是俱乐部的管理员，则给俱乐部里面的每个管理员都push买入控制请求
        if (this.stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
            long lClubID = clubID;
            //如果不是联盟局，则设置俱乐部ID为来源ID

            if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() == 0) {
                lClubID = stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID();
            }

            List<Long> vClubManagerUuids = ClubManager.getInstance().getClubManagerUuids(lClubID);
            if (vClubManagerUuids != null) {
                boolean bClubManager = false;
                //检查房主是否是房间的管理员
                if (stGameRoomInfo.getStGameRoomBaseInfo().getLLeagueID() > 0) {
                    bClubManager = true;
                } else {
                    for (long lClubManagerUuid : vClubManagerUuids) {
                        //检查房主是否是俱乐部的管理员
                        if (lClubManagerUuid == stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()) {
                            bClubManager = true;
                            break;
                        }
                    }
                }

                //如果房主在俱乐部的管理员中，则给所有俱乐部管理员都push买入控制信息
                if (bClubManager) {
                    for (long lClubManagerUuid : vClubManagerUuids) {
                        GameRoomMsgManager.getInstance().sendBuyinControlMsg(lClubManagerUuid, stBuyinControlMsg, exceptionalUuid);
                    }
                } else {
                    GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                            , stBuyinControlMsg, exceptionalUuid);
                }
            } else {
                GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                        , stBuyinControlMsg, exceptionalUuid);
            }

        } else {
            GameRoomMsgManager.getInstance().sendBuyinControlMsg(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
                    , stBuyinControlMsg, exceptionalUuid);
        }
    }

    public boolean checkClubManager(long uuid) {
        if (this.stGameRoomInfo.getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
            List<Long> vClubManagerUuids = ClubManager.getInstance().getClubManagerUuids(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID());
            if (vClubManagerUuids != null) {
                //检查房主是否是房间的管理员
                for (long lClubManagerUuid : vClubManagerUuids) {
                    //检查房主是否是俱乐部的管理员
                    if (lClubManagerUuid == uuid) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public ERROR_CODE_TYPE setStraddle(long uuid, int bStraddle) {
        synchronized (this) {
            if (stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid) {
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                stGameRoomBaseInfoBuilder.setBStraddle(bStraddle);
                stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                stGameRoomInfo = stGameRoomInfoBuilder.build();

                for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                    UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);

                    if (stUserGameInfoNetIn.getStUserInfo().getUuid() > 0) {
                        pushGameRoomUserMsgs(null, stUserGameInfoNetIn.getStUserInfo().getUuid(),
                                GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SET_STRADDLE, 0);
                    }
                }

                //给旁观的人发返回牌桌消息
                for (long push_uuid : mStandBys) {
                    pushGameRoomUserMsgs(null, push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_SET_STRADDLE, 0);
                }

                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
            } else {
                return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_GAME_ROOM_OWNER;
            }
        }
    }

    //强制站起
    private void forceStandupPush(UserGameInfoNet.Builder stUserGameInfoNetBuilder, int pos) {

        //给牌桌上的用户发送返回牌桌的消息
        stUserGameInfoNetBuilder.clearVCurrentHands();

        for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
            UserGameInfoNet stUserGameInfoNetIn = stGameRoomInfo.getVUserGameInfos(j);

            if (stUserGameInfoNetIn.getStUserInfo().getUuid() > 0) {
                pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), stUserGameInfoNetIn.getStUserInfo().getUuid(),
                        GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_STANDUP, pos);
            }
        }

        //给旁观的人发返回牌桌消息
        for (long push_uuid : mStandBys) {
            pushGameRoomUserMsgs(stUserGameInfoNetBuilder.build(), push_uuid, GAME_ROOM_MESSAGE_TYPE.GAME_ROOM_MESSAGE_FORCE_STANDUP, pos);
        }

        //把人从踢人列表里面删除
        mUserForceStandupUser.remove(stUserGameInfoNetBuilder.getStUserInfo().getUuid());
    }


    //重新设置思考时间
    public ERROR_CODE_TYPE resetPlayThinkingInterval(long uuid, int interval) {
        if (stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == uuid) {
            if (interval == 10 || interval == 12 || interval == 15) {

//					System.out.println("resetPlayThinkingInterval ： " + stGameRoomInfo.getEGameState());
                if (stGameRoomInfo.getEGameState().getNumber() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE) {

                    GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                    GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                    stGameRoomBaseInfoBuilder.setIThinkingInterval(interval);
                    stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
                    stGameRoomInfo = stGameRoomInfoBuilder.build();
                    iThinkingInterval = 0;
                } else {
                    iThinkingInterval = interval;
                }

                Map<Long, Long> mSendedUuids = new HashMap<Long, Long>();

                //发送push消息
                for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet stUserGameInfoSendNet = stGameRoomInfo.getVUserGameInfos(i);

                    if (stUserGameInfoSendNet.getStUserInfo().getUuid() > 0) {
                        if (mSendedUuids.get(stUserGameInfoSendNet.getStUserInfo().getUuid()) == null) {
                            mSendedUuids.put(stUserGameInfoSendNet.getStUserInfo().getUuid(), stUserGameInfoSendNet.getStUserInfo().getUuid());
                            //首先找到离开房间的用户基本信息
                            pushGameRoomUserMsgsResetThinkingInterval(null, stUserGameInfoSendNet.getStUserInfo().getUuid(), interval);
                        }
                    }
                }

                //给旁观的人发返回牌桌消息
                for (long push_uuid : mStandBys) {
                    if (mSendedUuids.get(push_uuid) == null) {
                        mSendedUuids.put(push_uuid, push_uuid);
                        pushGameRoomUserMsgsResetThinkingInterval(null, push_uuid, interval);
                    }
                }
            }
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        } else {
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_GAME_ROOM_OWNER;
        }
    }


    public List<Integer> showTableCards(long uuid, long lGameRoomHandID, long lFee) {
        synchronized (this) {

            if (stGameRoomInfo.getLGameHandID() == lGameRoomHandID
                    && (bShowTableCard == true)) {
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                //找到目前桌上的排
                Set<Integer> vTempTableCards = new HashSet<Integer>();
                if (mCurrentTableCards.size() > 0) {
                    vTempTableCards.addAll(mCurrentTableCards);
                }

                for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                    UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomInfoBuilder.getVUserGameInfosBuilder(i);
                    if (stUserGameInfoNetBuilder != null && stUserGameInfoNetBuilder.getStUserInfo().getUuid() == uuid) {

                        if (stUserGameInfoNetBuilder.getVPayTableCardsCount() > 0) {
                            //找到目前应翻看的牌
                            vTempTableCards.addAll(stUserGameInfoNetBuilder.getVPayTableCardsList());
                        }

                        if (vTempTableCards.size() >= 5) {
                            return vTableCards.subList(0, vTempTableCards.size());
                        } else {
                            UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().addUserPopularity(uuid, -lFee);

                            //打印日志
                            TranscationLogService.getInstance().error(uuid + " " + 0 + " " + 430004 + " 0 " + "0 " + lFee + " " + stUserBaseInfoNetBuilder.getLPopularity());

                            if (vTempTableCards.size() == 0) {
                                //获取三张牌
                                if (vTableCards.size() == 0) {
                                    //从新获取三张牌
                                    getTableCard(0);
                                    getTableCard(1);
                                    getTableCard(2);
                                }

                                for (int j = 0; j < 3; j++) {
                                    vTempTableCards.add(vTableCards.get(j));
                                }
                                //设置用户数据

                            } else if (vTempTableCards.size() == 3) {
                                //获取第四张牌
                                if (vTableCards.size() == 3) {
                                    //从新获取三张牌
                                    getTableCard(3);
                                }

                                //添加一张牌
                                vTempTableCards.add(vTableCards.get(3));
                            } else if (vTempTableCards.size() == 4) {
                                //获取第五张牌
                                //获取第四张牌
                                if (vTableCards.size() == 4) {
                                    //从新获取三张牌
                                    getTableCard(4);
                                }

                                //添加一张牌
                                vTempTableCards.add(vTableCards.get(4));
                            }

                            stUserGameInfoNetBuilder.clearVPayTableCards();
                            stUserGameInfoNetBuilder.addAllVPayTableCards(vTempTableCards);

                            stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoNetBuilder);
                        }
                    }
                }

                TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                        + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                        + "," + stGameRoomInfoBuilder.getLGameHandID()
                        + "," + uuid
                        + "," + "showtablecard"
                        + "," + vTempTableCards);

                stGameRoomInfo = stGameRoomInfoBuilder.build();
                return vTableCards.subList(0, vTempTableCards.size());
            } else {
                return null;
            }
        }
    }

    public boolean setPreAction(long uuid, USER_GAME_STATE eGameRoomType) {
        synchronized (this) {
            for (int j = 0; j < stGameRoomInfo.getVUserGameInfosCount(); j++) {
                UserGameInfoNet stUserGameInfoNet = stGameRoomInfo.getVUserGameInfos(j);
                if (stUserGameInfoNet != null && stUserGameInfoNet.getStUserInfo().getUuid() == uuid) {
                    //检查用户状态，如果用户已经allin，或者已经弃牌，就没有预动作了
                    if (stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_ALLIN
                            && stUserGameInfoNet.getEGameState() != USER_GAME_STATE.USER_GAME_STATE_FOLD) {
                        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                        UserGameInfoNet.Builder stUserGameInfoNetBuilder = stUserGameInfoNet.toBuilder();
                        stUserGameInfoNetBuilder.setEPreAction(eGameRoomType);
                        stGameRoomInfoBuilder.setVUserGameInfos(j, stUserGameInfoNetBuilder);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();

                        TableLogService.getInstance().error(stGameRoomInfo.getStGameRoomBaseInfo().getLChatRoomID()
                                + "," + stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId()
                                + "," + stGameRoomInfoBuilder.getLGameHandID()
                                + "," + uuid
                                + "," + "preaction"
                                + "," + eGameRoomType);
                        return true;
                    }
                }
            }
            return false;

        }
    }

    public void CreditDenyPush(long remainStacks, long toUuid) {
        long timeTamp = System.currentTimeMillis();
        BuyinControlMsg.Builder stBuyinControlMsgBuilder = BuyinControlMsg.newBuilder();
        stBuyinControlMsgBuilder.setLBuyinActionID(timeTamp);
        stBuyinControlMsgBuilder.setLBuyStacks(remainStacks);
        stBuyinControlMsgBuilder.setEBuyinControlMessageTypeDeny(BUYIN_CONTROL_MESSAGE_TYPE.BUYIN_CONTROL_MESSAGE_TYPE_DENY);
        stBuyinControlMsgBuilder.setStGameRoomBaseInfo(stGameRoomInfo.getStGameRoomBaseInfo());
        stBuyinControlMsgBuilder.setLActionUuid(toUuid);
        stBuyinControlMsgBuilder.setStFromUser(stGameRoomInfo.getStGameRoomBaseInfo().getStCreateUserBaseInfo());
        stBuyinControlMsgBuilder.setEGameRoomType(stGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomType());
        stBuyinControlMsgBuilder.setLMsgID(timeTamp);
        stBuyinControlMsgBuilder.setLBuyinTime(timeTamp);
        stBuyinControlMsgBuilder.setSActionNickname("");
        GameRoomMsgManager.getInstance().sendBuyinControlMsg(toUuid, stBuyinControlMsgBuilder.build(), 0);
    }

    /**
     * 每手开始检测IP-GPS 信息
     */
    private void checkIpAndGps() {
        GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
        if (stGameRoomInfoBuilder.getStGameRoomBaseInfo().getBIPLimit() != 0
                || stGameRoomInfoBuilder.getStGameRoomBaseInfo().getBGPSLimit() != 0) {
            //需要判断是否开启全服检测或者针对联盟的检测
            if (LocationManager.getInstance().checkOpen(stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLLeagueID())) {
                List<Long> tableUuids = new ArrayList<Long>();
                List<Long> cheatUuids = new ArrayList<Long>();
                for (int i = 0; i < stGameRoomInfoBuilder.getVUserGameInfosCount(); i++) {
                    if (stGameRoomInfoBuilder.getVUserGameInfosBuilder(i).getStUserInfo()
                            .getUuid() > 0) {
                        tableUuids.add(stGameRoomInfoBuilder.getVUserGameInfosBuilder(i).getStUserInfo()
                                .getUuid());
                    }
                }
                if (tableUuids != null && tableUuids.size() > 1) {
                    cheatUuids = LBSManager.getInstance().verifyTablesUuids(tableUuids,
                            stGameRoomInfoBuilder.getStGameRoomBaseInfo().getBIPLimit(),
                            stGameRoomInfoBuilder.getStGameRoomBaseInfo().getBGPSLimit(),
                            stGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId(),
                            stGameRoomInfoBuilder.getLGameHandID());
                }
                if (cheatUuids != null && cheatUuids.size() > 0) {
                    for (Long cheatUuid : cheatUuids) {
                        if (cheatUuid > 0) {
                            doStandUpAction(cheatUuid, true, 1);
                        }
                    }
                    resetGameRoom();
                }
            }
        }
    }

    public ERROR_CODE_TYPE userUnloadStacks(long uuid,	long lUnloadStacks){
        synchronized(this){
            try{
                GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
                GameRoomBaseInfo stGameRoomBaseInfo = stGameRoomInfoBuilder.getStGameRoomBaseInfo();
                //筹码小于1，直接返回成功
                if(lUnloadStacks < 1){
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
                }

                //检查卸码开关
                if(stGameRoomBaseInfo.getBUnloadable() == 0 || stGameRoomBaseInfo.getBFullAllIn() == 0){
                    //带出失败发送push
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNLOAD_STACKS_FAILD;
                }
                boolean  bExit = false;
                //目前牌桌上的用户的卸码
                for(int i = 0 ; i < stGameRoomInfo.getVUserGameInfosCount(); i++){
                    UserGameInfoNet.Builder stUserGameInfoBuilder = stGameRoomInfoBuilder.getVUserGameInfos(i).toBuilder();
                    if(stUserGameInfoBuilder.getStUserInfo().getUuid() == uuid){
                        bExit = true;
                        //检测玩家是否已经申请过一次
                        if(stUserGameInfoBuilder.getBUnloadingStacks() != 0){
                            //带出失败发送push
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_HAND_HAS_UNLOAD_STACKS;
                        }
                        long originStacks = stUserGameInfoBuilder.getLRemainStacks();

                        //撤码数量直接以最小带入的倍数计算即可
                        if(originStacks -lUnloadStacks < stGameRoomBaseInfo.getIUnloadRatio()*stGameRoomBaseInfo.getLBigBlinds()){
                            //带出失败发送push
                            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNLOAD_STACKS_NOT_ENOUGH;
                        }
                        //标记已经申请带出，赋值要带出的值
                        stUserGameInfoBuilder.setBUnloadingStacks((int)lUnloadStacks);
                        stGameRoomInfoBuilder.setVUserGameInfos(i, stUserGameInfoBuilder);
                        //防止玩家申请带出后又离开牌桌
                        if(stGameRoomInfo.getEGameState().getNumber() == GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait_VALUE){
                            mUserWaitUnload.put(uuid, lUnloadStacks);
                        }
                        mUserUnload.put(uuid, lUnloadStacks);
                        stGameRoomInfo = stGameRoomInfoBuilder.build();
                        break;
                    }
                }
                if(!bExit){
                    return ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNLOAD_STACKS_FAILD;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
            return ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
        }
    }
}
