package com.texaspoker.moment.room;

import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.floplotto.FlopLottoManager;
import com.texaspoker.league.LeagueManager;
import com.texaspoker.logger.TranscationLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.*;
import com.texaspoker.moment.TexasPokerCommon;
import com.texaspoker.moment.TexasPokerCommon.*;
import com.texaspoker.moment.TexasPokerHttpLeague;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueInfo;
import com.texaspoker.user.UserManager;
import com.texaspoker.voice.VoiceManager;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameRoomManager {
    public TimeoutHashMap<Long, GameRoomDetailInfo> mGameRoomDetailInfos;
    private static GameRoomManager instance;

    ArrayList<GameRoomDetailInfo> getTimeOutGameRoom() {
        return mGameRoomDetailInfos.timeout();
    }

    public TimeoutHashMap<Long, Long> mGameRoomDraw; // 房间抽水计时map lGameRoomID-lGameRoomID

    ArrayList<Long> getTimeOutGameRoomDraw() {
        return mGameRoomDraw.timeout();
    }

    private GameRoomManager() {
        mGameRoomDetailInfos = new TimeoutHashMap<>();
        mGameRoomDetailInfos.setTimeout(800);

        mGameRoomDraw = new TimeoutHashMap<>();
        mGameRoomDraw.setTimeout(ConfigaturationManager.iDrawTime * 60 * 60 * 1000); // 设置房间抽水时间间隔
    }

    public static GameRoomManager getInstance() {
        if (instance == null) {
            synchronized (GameRoomManager.class) {
                instance = new GameRoomManager();
                ConnectDatabase.getInstance().loadAllGameRooms(instance.mGameRoomDetailInfos);

                instance.init();
            }
        }
        return instance;
    }

    private void init() {
        GameRoomService stGameRoomService = new GameRoomService();
        ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
        service1.scheduleAtFixedRate(stGameRoomService, 100, 1500, TimeUnit.MILLISECONDS);

        int iTotal = 3;
        for (int i = 0; i < iTotal; i++){
            GameRoomServiceWait stGameRoomServiceWait = new GameRoomServiceWait(i,iTotal);
            ScheduledExecutorService service2 = Executors.newSingleThreadScheduledExecutor();
            service2.scheduleAtFixedRate(stGameRoomServiceWait, 100, 1500, TimeUnit.MILLISECONDS);
        }

        GameRoomDrawService stGameRoomDrawService = new GameRoomDrawService();
        ScheduledExecutorService service3 = Executors.newSingleThreadScheduledExecutor();
        service3.scheduleAtFixedRate(stGameRoomDrawService, 100, 1500, TimeUnit.MILLISECONDS);
    }

    public GameRoomDetailInfo getGameRoomDetailInfo(long lGameRoomID) {
        return mGameRoomDetailInfos.get(lGameRoomID);
    }

    public void setGameRoomDetailInfo(GameRoomDetailInfo stGameRoomDetailInfo) {
        mGameRoomDetailInfos.setCurrent(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomDetailInfo);
    }

    public void setmGameRoomDraw(Long lGameRoomID){
        mGameRoomDraw.setCurrent(lGameRoomID, lGameRoomID);
    }

    /**
     * 检查是否具有创建俱乐部普通局的权限
     *
     * @param stGameRoomBaseInfo 牌桌信息
     * @param uuid               玩家 ID
     * @return 是否具有权限
     */
    private boolean checkAuthClubGame(GameRoomBaseInfo stGameRoomBaseInfo, long uuid) {
        if (stGameRoomBaseInfo.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
            if (stGameRoomBaseInfo.getLLeagueID() == 0) {
                ClubBaseInfo.Builder stClubBaseInfoBuilder = ClubManager.getInstance().getClubBaseInfo(stGameRoomBaseInfo.getLChatRoomID());
                if (stClubBaseInfoBuilder != null) {
                    // 检查俱乐部设置
                    if (stClubBaseInfoBuilder.getIManagerCreate() == 1) {
                        return ClubManager.getInstance().checkClubManager(stGameRoomBaseInfo.getLChatRoomID(), uuid);
                    }
                }
            }
        }
        return true;
    }

    public GameRoomIDResultInfo createGameRoom(long uuid, GameRoomBaseInfo stGameRoomBaseInfo) {
        GameRoomIDResultInfo stGameRoomIDResultInfo = new GameRoomIDResultInfo();

        if (stGameRoomBaseInfo.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
            boolean bInClub = ClubManager.getInstance().inClub(stGameRoomBaseInfo.getLChatRoomID(), uuid);
            if (!bInClub) {
                stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_CLUB_NO_USER);
                return stGameRoomIDResultInfo;
            }
        }

        if (!checkAuthClubGame(stGameRoomBaseInfo, uuid)) {
            stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_CLUB_NO_AUTH_MANAGE);
            return stGameRoomIDResultInfo;
        }

        UserBaseInfoNet.Builder stUserBaseInfoNet = UserManager.getInstance().getUserBaseInfo(uuid);

        if (stGameRoomBaseInfo.getLLeagueID() <= 0 && stUserBaseInfoNet.getICreateGameLimit() == 1 && stGameRoomBaseInfo.getBPaidStart() == 0) {
            stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_GAMEROOM_OVERFLOW);
            return stGameRoomIDResultInfo;
        } else {
            long coinCost;
            boolean isLimitPaidStart = false;
            if (stGameRoomBaseInfo.hasLLeagueID() && stGameRoomBaseInfo.getLLeagueID() > 0) {// 联盟局的情况处理
                coinCost = GameFeeUtil.getLeagueGameFee((int) stGameRoomBaseInfo.getLSmallBlinds(), stGameRoomBaseInfo.getIGameRoomUserMaxNums(), (stGameRoomBaseInfo.hasBCredit() && stGameRoomBaseInfo.getBCredit() == 1) ? 1 : 0);

                if (coinCost < 0/* 交易数量安全性保护*/ || !UserManager.getInstance().checkUserCoin(uuid, coinCost)) {// 游戏币不足
                    stGameRoomIDResultInfo.seteErrorCodeType(TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);

                    return stGameRoomIDResultInfo;
                } else if (!(stGameRoomBaseInfo.getECreateRoomType().equals(TexasPokerCommon.CREATE_ROOM_TYPE.CLUB_ROOM)
                        && ClubManager.getInstance().checkClubManager(stGameRoomBaseInfo.getLChatRoomID(), uuid))) {// 非管理员权限
                    stGameRoomIDResultInfo.seteErrorCodeType(TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_LEAGUE_NO_AUTH_CREATE);

                    return stGameRoomIDResultInfo;
                } else {// 允许创建
                    if (stGameRoomBaseInfo.getBCredit() == 0){
                        stUserBaseInfoNet = UserManager.getInstance().addUserCoin(stUserBaseInfoNet.getUuid(), -coinCost);
                        TranscationLogService.getInstance().error(stUserBaseInfoNet.getUuid() + " " + stGameRoomBaseInfo.getLGameRoomId() + " " + 110001 + " " + stGameRoomBaseInfo.getLDuration()
                                + " " + stGameRoomBaseInfo.getIMinBuyinRatio()
                                + " " + stGameRoomBaseInfo.getIMaxBuyinRatio() + " " + coinCost + " " + stUserBaseInfoNet.getLCoin());
                    }
                }
            } else {
                if (stUserBaseInfoNet.getICreateGameLimit() == 1) {
                    coinCost = GameFeeUtil.getLeagueGameFee((int) stGameRoomBaseInfo.getLSmallBlinds(), stGameRoomBaseInfo.getIGameRoomUserMaxNums(), (stGameRoomBaseInfo.hasBCredit() && stGameRoomBaseInfo.getBCredit() == 1) ? 1 : 0) / 2;

                    if (coinCost < 0/* 交易数量安全性保护*/ || !UserManager.getInstance().checkUserCoin(uuid, coinCost)) {// 游戏币不足
                        stGameRoomIDResultInfo.seteErrorCodeType(TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);

                        return stGameRoomIDResultInfo;
                    } else {// 允许创建
                        isLimitPaidStart = true;
                        if (stGameRoomBaseInfo.getBCredit() == 0){
                            stUserBaseInfoNet = UserManager.getInstance().addUserCoin(stUserBaseInfoNet.getUuid(), -coinCost);
                            TranscationLogService.getInstance().error(stUserBaseInfoNet.getUuid() + " " + stGameRoomBaseInfo.getLGameRoomId() + " " + 110011 + " " + stGameRoomBaseInfo.getLDuration()
                                    + " " + stGameRoomBaseInfo.getIMinBuyinRatio()
                                    + " " + stGameRoomBaseInfo.getIMaxBuyinRatio() + " " + coinCost + " " + stUserBaseInfoNet.getLCoin());
                        }
                    }
                }
            }

            synchronized (RoomIDManager.class) {
                GameRoomBaseInfo createdGameRoomBaseInfo = GameRoomBaseInfo.newBuilder(stGameRoomBaseInfo)
                        .setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()))
                        .setBBuyIn(1)
                        .setLCreateTime(System.currentTimeMillis())
                        .setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                        .setStCreateUserBaseInfo(stUserBaseInfoNet)
                        .setBPaidStart(isLimitPaidStart ? 1 : 0)
                        .setBUnloadable(stGameRoomBaseInfo.getBUnloadable()) //AOF模式下具有卸码功能
                        .build();

                GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();

                // 添加房间至俱乐部或聊天室
                switch (createdGameRoomBaseInfo.getECreateRoomType()) {
                    case CAHT_ROOM:
                        TexasPoker.ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(createdGameRoomBaseInfo.getLChatRoomID());
                        if (stChatRoomInfo != null)
                            ChatRoomManager.getInstance().addGameRooms(createdGameRoomBaseInfo.getLChatRoomID(), createdGameRoomBaseInfo.getLGameRoomId());
                        break;
                    case CLUB_ROOM:
                        if (createdGameRoomBaseInfo.hasBCredit() && createdGameRoomBaseInfo.getBCredit() == 1) {
                            pushAppCreateMsg(createdGameRoomBaseInfo.getLLeagueID(),// App Push
                                    createdGameRoomBaseInfo.getLChatRoomID(),
                                    createdGameRoomBaseInfo.getStrRoomName(),
                                    createdGameRoomBaseInfo.getLDuration(),
                                    createdGameRoomBaseInfo.getLSmallBlinds()/100,
                                    createdGameRoomBaseInfo.getIAnte()/100);
                        } else {
                            pushAppCreateMsg(createdGameRoomBaseInfo.getLLeagueID(),// App Push
                                    createdGameRoomBaseInfo.getLChatRoomID(),
                                    createdGameRoomBaseInfo.getStrRoomName(),
                                    createdGameRoomBaseInfo.getLDuration(),
                                    createdGameRoomBaseInfo.getLSmallBlinds(),
                                    createdGameRoomBaseInfo.getIAnte());
                        }

                        if (createdGameRoomBaseInfo.getLLeagueID() > 0) {// 俱乐部联盟局
                            TexasPokerHttpLeague.LeagueInfo stLeagueInfo = LeagueManager.getInstance().getLeagueInfo(createdGameRoomBaseInfo.getLLeagueID());
                            stGameRoomDetailInfo.setStLeagueInfo(stLeagueInfo);

                            if (stLeagueInfo != null) {// 添加房间至联盟
                                for (TexasPokerCommon.ClubBaseInfo clubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                                    ClubManager.getInstance().addGameRooms(clubBaseInfo.getLClubID(), createdGameRoomBaseInfo.getLGameRoomId(), createdGameRoomBaseInfo.getLLeagueID());
                                }
                            }
                        } else {// 俱乐部非联盟局
                            ClubManager.getInstance().addGameRooms(createdGameRoomBaseInfo.getLChatRoomID(), createdGameRoomBaseInfo.getLGameRoomId(), 0);
                        }
                        break;
                }

                GameRoomInfo.Builder gameRoomInfo = GameRoomInfo.newBuilder()
                        .setStGameRoomBaseInfo(createdGameRoomBaseInfo)
                        .setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                        .setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);

                for (int i = 0; i < createdGameRoomBaseInfo.getIGameRoomUserMaxNums(); i++) {
                    gameRoomInfo.addVUserGameInfos(UserGameInfoNet.newBuilder()
                            .setStUserInfo(TexasPokerCommon.UserBaseInfoNet.newBuilder()
                                    .setUuid(0)
                                    .setEType(TexasPokerCommon.ID_TYPE.ID_TYPE_NA)
                                    .setStrID("")
                                    .setStrCover("")
                                    .setStrNick("")
                                    .setStrPhoneNumber("")
                                    .setIGender(0)
                                    .setEVipType(TexasPokerCommon.VIP_TYPE.BLUE_CARD)
                                    .setLLoginTime(0))
                            .setIDelaySingleTime(20)
                            .setLRoomId(0)
                            .setLRemainStacks(0)
                            .setLBetStacks(0)
                            .setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL)
                            .setBGameDealer(0)
                            .setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY));
                }

                stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfo.build());
                stGameRoomDetailInfo.setiGameRoomUserMaxNums(createdGameRoomBaseInfo.getIGameRoomUserMaxNums());
                stGameRoomDetailInfo.lGameRoomID = createdGameRoomBaseInfo.getLGameRoomId();
                stGameRoomDetailInfo.lFromRoomID = createdGameRoomBaseInfo.getLChatRoomID();

                // 判断当前牌局是否为授信牌局
                if (stGameRoomBaseInfo.hasBCredit() && stGameRoomBaseInfo.getBCredit() == 1 && ClubManager.getInstance().getClubBaseInfo(stGameRoomBaseInfo.getLChatRoomID()).getLCreditLeagueID() > 0 ){

                    // 01.添加牌局授信标识
                    stGameRoomDetailInfo.bCredit = true;

                    // 02.设置牌局抽水方式
                    if (stGameRoomBaseInfo.hasBPotDrawCredit() && stGameRoomBaseInfo.getBPotDrawCredit() == 0){
                        stGameRoomDetailInfo.iDrawType = 1;
                    }
                    if (stGameRoomBaseInfo.hasBPotDrawCredit() && stGameRoomBaseInfo.getBPotDrawCredit() == 1){
                        stGameRoomDetailInfo.iDrawType = 2;
                    }

                }

                System.out.println(stGameRoomBaseInfo.getBCredit()+"=========金币局====="+stGameRoomBaseInfo.hasBCredit());
                //非授信局
                if (stGameRoomBaseInfo.getBCredit() == 0) {
                    // 01.添加牌局授信标识
                    stGameRoomDetailInfo.bCredit = false;


                    // 02.设置牌局抽水方式 如果有服务费则使用底池抽水
                    if (stGameRoomBaseInfo.getIPotDrawFee() > 0 && stGameRoomBaseInfo.getBPotDrawCredit() == 1) {
                        stGameRoomDetailInfo.iDrawType = 2;
                    } else {
                        stGameRoomDetailInfo.iDrawType = 0;
                    }
                }

                // 判断当前牌局是否为声纹牌局
                if (stGameRoomBaseInfo.hasBVoice() && stGameRoomBaseInfo.getBVoice() == 1){
                    stGameRoomDetailInfo.bVoice = true;

                    //创建声纹组
                    VoiceManager.getInstance().createGroup(stGameRoomDetailInfo.lGameRoomID,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID());
                }

                // GameRoomManager 调用
                setGameRoomDetailInfo(stGameRoomDetailInfo);

                try {
                    ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
                    ConnectDatabase.getInstance().insertInGameUser(createdGameRoomBaseInfo.getLGameRoomId(), uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                BillManager.getInstance().createBillInfo(0, createdGameRoomBaseInfo);

                UserManager.getInstance().addUserCreateGameRooms(uuid, createdGameRoomBaseInfo.getLGameRoomId());

                //创建野保险房间
                FlopLottoManager.getInstance().createFlopLotto(stGameRoomDetailInfo.lGameRoomID,stGameRoomBaseInfo.getLLeagueID(),stGameRoomBaseInfo.getBCredit(),stGameRoomBaseInfo.getBLeagueCurrencyPattern(),stGameRoomBaseInfo.getLBigBlinds(),stGameRoomBaseInfo.getStrRoomName(),stGameRoomBaseInfo.getLLeagueID() > 0 ? 3 : 2);

                stGameRoomIDResultInfo.seteErrorCodeType(TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
                stGameRoomIDResultInfo.setlGameRoomID(createdGameRoomBaseInfo.getLGameRoomId());

                return stGameRoomIDResultInfo;
            }
        }
    }

    private void pushAppCreateMsg(long lLeagueID, long lClubID, String sGameName, long lDuration, long sb, int ante) {
        if (lLeagueID > 0) {
            LeagueInfo stLeagueInfo = LeagueManager.getInstance().getLeagueInfo(lLeagueID);
            if (stLeagueInfo != null) {
                for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                    float fHour = (float) lDuration / (float) 3600;
                    APNManager.getInstance().createGameRoomPush(stClubBaseInfo.getLClubID(), sGameName, "" + fHour, sb, ante, 1);
                }
            }
        } else {
            float fHour = (float) lDuration / (float) 3600;
            APNManager.getInstance().createGameRoomPush(lClubID, sGameName, "" + fHour, sb, ante, 1);
        }
    }
}
