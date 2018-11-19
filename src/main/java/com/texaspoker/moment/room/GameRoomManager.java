package com.texaspoker.moment.room;

import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.league.LeagueManager;
import com.texaspoker.logger.TranscationLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.*;
import com.texaspoker.moment.TexasPokerCommon.*;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueInfo;
import com.texaspoker.user.UserManager;

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

    private GameRoomManager() {
        mGameRoomDetailInfos = new TimeoutHashMap<>();
        mGameRoomDetailInfos.setTimeout(800);
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

        GameRoomServiceWait stGameRoomServiceWait = new GameRoomServiceWait();
        ScheduledExecutorService service2 = Executors.newSingleThreadScheduledExecutor();
        service2.scheduleAtFixedRate(stGameRoomServiceWait, 100, 1500, TimeUnit.MILLISECONDS);
    }

    public GameRoomDetailInfo getGameRoomDetailInfo(long lGameRoomID) {
        return mGameRoomDetailInfos.get(lGameRoomID);
    }

    public void setGameRoomDetailInfo(GameRoomDetailInfo stGameRoomDetailInfo) {
        mGameRoomDetailInfos.setCurrent(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(), stGameRoomDetailInfo);
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
            if (stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NO) {
                synchronized (RoomIDManager.class) {
                    TexasPoker.GameRoomBaseInfo.Builder gameRoomBaseInfoBuilder = stGameRoomBaseInfo.toBuilder();
                    gameRoomBaseInfoBuilder.setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()));
                    gameRoomBaseInfoBuilder.setBBuyIn(1);
                    gameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
                    gameRoomBaseInfoBuilder.setIMaxBuyinRatio(stGameRoomBaseInfo.getIMaxBuyinRatio());
                    gameRoomBaseInfoBuilder.setIMinBuyinRatio(stGameRoomBaseInfo.getIMinBuyinRatio());
                    gameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                    gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));


                    TexasPoker.GameRoomInfo.Builder gameRoomInfo = TexasPoker.GameRoomInfo.newBuilder();
                    gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                    GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();


                    if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CAHT_ROOM) {
                        ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
                        if (stChatRoomInfo != null) {
                            ChatRoomManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                        }
                    } else if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
                        if (stGameRoomBaseInfo.getLLeagueID() > 0) {
                            LeagueInfo stLeagueInfo = LeagueManager.getInstance().getLeagueInfo(stGameRoomBaseInfo.getLLeagueID());

                            stGameRoomDetailInfo.setStLeagueInfo(stLeagueInfo);

                            if (stLeagueInfo != null) {
                                for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                                    ClubManager.getInstance().addGameRooms(stClubBaseInfo.getLClubID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                                }
                            }

                        } else {
                            ClubManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                        }
                    }

                    gameRoomInfo.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
                    gameRoomInfo.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);

                    for (int i = 0; i < gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums(); i++) {
                        TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();

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

                    stGameRoomDetailInfo.iGameRoomUserMaxNums = gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();

                    GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);

                    ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);

                    ConnectDatabase.getInstance().insertInGameUser(stGameRoomDetailInfo.getStGameRoomInfo()
                            .getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                    BillManager.getInstance().createBillInfo(0, gameRoomBaseInfoBuilder.build());

                    UserManager.getInstance().addUserCreateGameRooms(uuid, gameRoomBaseInfoBuilder.getLGameRoomId());

                    if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
                        pushAppCreateMsg(gameRoomBaseInfoBuilder.getLLeagueID()
                                , gameRoomBaseInfoBuilder.getLChatRoomID()
                                , gameRoomBaseInfoBuilder.getStrRoomName()
                                , gameRoomBaseInfoBuilder.getLDuration()
                                , gameRoomBaseInfoBuilder.getLSmallBlinds()
                                , gameRoomBaseInfoBuilder.getIAnte());
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

                    stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
                    stGameRoomIDResultInfo.setlGameRoomID(gameRoomBaseInfoBuilder.getLGameRoomId());
                    return stGameRoomIDResultInfo;
                }
            } else if (stGameRoomBaseInfo.getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL) {
                long lCoin = GameFeeUtil.getLeagueGameFee((int) stGameRoomBaseInfo.getLSmallBlinds(), stGameRoomBaseInfo.getIGameRoomUserMaxNums());
                if (UserManager.getInstance().checkUserCoin(stUserBaseInfoNet.getUuid(), lCoin)) {
                    synchronized (RoomIDManager.class) {
                        TexasPoker.GameRoomBaseInfo.Builder gameRoomBaseInfoBuilder = stGameRoomBaseInfo.toBuilder();
                        gameRoomBaseInfoBuilder.setLGameRoomId(RoomIDManager.getInstance().getNewRoomID(stUserBaseInfoNet.getUuid()));
                        gameRoomBaseInfoBuilder.setBBuyIn(1);
                        gameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
                        gameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                        gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));

                        TexasPoker.GameRoomInfo.Builder gameRoomInfo = TexasPoker.GameRoomInfo.newBuilder();
                        gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                        stUserBaseInfoNet = UserManager.getInstance().addUserCoin(stUserBaseInfoNet.getUuid(), -lCoin);

                        gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stUserBaseInfoNet);
                        GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();

                        if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CAHT_ROOM) {
                            ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(gameRoomBaseInfoBuilder.getLChatRoomID());
                            if (stChatRoomInfo != null) {
                                ChatRoomManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                            }
                        } else if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
                            if (stGameRoomBaseInfo.getLLeagueID() > 0) {
                                LeagueInfo stLeagueInfo = LeagueManager.getInstance().getLeagueInfo(stGameRoomBaseInfo.getLLeagueID());

                                stGameRoomDetailInfo.setStLeagueInfo(stLeagueInfo);

                                if (stLeagueInfo != null) {
                                    for (ClubBaseInfo stClubBaseInfo : stLeagueInfo.getVClubBaseInfosList()) {
                                        ClubManager.getInstance().addGameRooms(stClubBaseInfo.getLClubID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                                    }
                                }

                            } else {
                                ClubManager.getInstance().addGameRooms(gameRoomBaseInfoBuilder.getLChatRoomID(), gameRoomBaseInfoBuilder.getLGameRoomId());
                            }
                        }

                        gameRoomInfo.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
                        gameRoomInfo.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);
                        gameRoomInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                        for (int i = 0; i < gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums(); i++) {
                            TexasPoker.UserGameInfoNet.Builder userGameInfoBuilder = TexasPoker.UserGameInfoNet.newBuilder();

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

                            userGameInfoBuilder.setLRoomId(0);
                            userGameInfoBuilder.setLRemainStacks(0);
                            userGameInfoBuilder.setLBetStacks(0);

                            userGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
                            userGameInfoBuilder.setBGameDealer(0);
                            userGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);

                            gameRoomInfo.addVUserGameInfos(userGameInfoBuilder);
                        }

                        stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfo.build());

                        stGameRoomDetailInfo.iCouldPlayNum++;
                        stGameRoomDetailInfo.lGameRoomID = gameRoomBaseInfoBuilder.getLGameRoomId();
                        stGameRoomDetailInfo.lFromRoomID = gameRoomBaseInfoBuilder.getLChatRoomID();
                        stGameRoomDetailInfo.iGameRoomUserMaxNums = gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums();
                        stGameRoomDetailInfo.lLeagueCoin = lCoin;

                        GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
                        ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
                        ConnectDatabase.getInstance().insertInGameUser(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                        BillManager.getInstance().createBillInfo(0, gameRoomBaseInfoBuilder.build());

                        UserManager.getInstance().addUserCreateGameRooms(uuid, gameRoomBaseInfoBuilder.getLGameRoomId());

                        TranscationLogService.getInstance().error(stUserBaseInfoNet.getUuid() + " " + gameRoomBaseInfoBuilder.getLGameRoomId() + " " + 110001 + " " + gameRoomBaseInfoBuilder.getLDuration()
                                + " " + gameRoomBaseInfoBuilder.getIMinBuyinRatio()
                                + " " + gameRoomBaseInfoBuilder.getIMaxBuyinRatio() + " " + lCoin + " " + stUserBaseInfoNet.getLCoin());

                        if (gameRoomBaseInfoBuilder.getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM) {
                            pushAppCreateMsg(gameRoomBaseInfoBuilder.getLLeagueID()
                                    , gameRoomBaseInfoBuilder.getLChatRoomID()
                                    , gameRoomBaseInfoBuilder.getStrRoomName()
                                    , gameRoomBaseInfoBuilder.getLDuration()
                                    , gameRoomBaseInfoBuilder.getLSmallBlinds()
                                    , gameRoomBaseInfoBuilder.getIAnte());
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

                        stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
                        stGameRoomIDResultInfo.setlGameRoomID(gameRoomBaseInfoBuilder.getLGameRoomId());
                        return stGameRoomIDResultInfo;
                    }
                } else {
                    stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);
                    return stGameRoomIDResultInfo;
                }
            }
        }
        stGameRoomIDResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
        return stGameRoomIDResultInfo;
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
