package com.texaspoker.moment.room;

import com.texaspoker.bill.BillManager;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.database.ConnectDatabase;
import com.texaspoker.logger.TranscationLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.*;
import com.texaspoker.moment.TexasPokerCommon;
import com.texaspoker.moment.TexasPokerCommon.*;
import com.texaspoker.msg.GameRoomMsgManager;
import com.texaspoker.user.UserManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuickGameRoomManager {

    private ConcurrentHashMap<Long, QuickGameRoomInfo.Builder> mQuickGameRoomInfos;
    private ConcurrentHashMap<Long, HashSet<Long>> mUserQuickGameRooms;
    private ConcurrentHashMap<String, Long> mCodeQuickGameRooms;

    private static QuickGameRoomManager instance;

    private QuickGameRoomManager() {
        mQuickGameRoomInfos = new ConcurrentHashMap<>();
        mUserQuickGameRooms = new ConcurrentHashMap<>();
        mCodeQuickGameRooms = new ConcurrentHashMap<>();
        TimerTask task = new TimerTask() {
            public void run() {
                // 检查是否有超时的房间
                Iterator iter = mQuickGameRoomInfos.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = (QuickGameRoomInfo.Builder) entry.getValue();

                    if (stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART) {
                        if (System.currentTimeMillis() - stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLCreateTime()
                                > stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLDuration() * 1000) {
                            finishQuickGameRoom(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
                        }
                    }
                }
            }
        };
        Timer timer = new Timer(true);
        timer.schedule(task, 1000, 60000);
    }

    public static QuickGameRoomManager getInstance() {
        synchronized (QuickGameRoomManager.class) {
            if (instance == null) {
                instance = new QuickGameRoomManager();
                ConnectDatabase.getInstance().loadAllQuickGameRooms(instance.mQuickGameRoomInfos, instance.mCodeQuickGameRooms, instance.mUserQuickGameRooms);
            }
        }
        return instance;
    }

    public void setQuickGameRoomInfos(QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder) {
        mQuickGameRoomInfos.put(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId(), stQuickGameRoomInfoBuilder);
        mCodeQuickGameRooms.put(stQuickGameRoomInfoBuilder.getSCryptCode(), stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
        ConnectDatabase.getInstance().insertQuickRoomInfo(stQuickGameRoomInfoBuilder.build());
    }

    public void addUserCreateQuickGameRoom(long uuid, long lQuickGameRoomID) {
        UserManager.getInstance().setUserQuickGameRoom(uuid, lQuickGameRoomID);
    }

    public void removeUserCreateQuickGameRoom(long uuid, long lQuickGameRoomID) {
        UserManager.getInstance().removeUserQuickGameRoom(uuid);
        UserManager.getInstance().removeUserCreateGameRooms(uuid, lQuickGameRoomID);
    }

    public Long getUserCreateQuickGameRoom(long uuid) {
        return UserManager.getInstance().getUserQuickGameRoom(uuid);
    }

    public QuickGameRoomInfo.Builder getQuickGameRoomInfoByRoomID(long lRoomID) {
        return mQuickGameRoomInfos.get(lRoomID);
    }

    public void removeQuickGameRoomInfos(long lQuickGameRoomID) {
        mQuickGameRoomInfos.remove(lQuickGameRoomID);
    }

    public void setUserQuickGameRoomInfos(long uuid, long lQuickGameRoomID) {
        HashSet<Long> vQuickGameRoomIDs = mUserQuickGameRooms.get(uuid);
        if (vQuickGameRoomIDs == null) {
            vQuickGameRoomIDs = new HashSet<>();
        }
        vQuickGameRoomIDs.add(lQuickGameRoomID);
        mUserQuickGameRooms.put(uuid, vQuickGameRoomIDs);
    }

    public void removeUserQuickGameRoomInfos(long uuid, long lQuickGameRoomID) {
        HashSet<Long> vQuickGameRoomIDs = mUserQuickGameRooms.get(uuid);
        if (vQuickGameRoomIDs == null) {
            vQuickGameRoomIDs = new HashSet<>();
        }
        vQuickGameRoomIDs.remove(lQuickGameRoomID);
        mUserQuickGameRooms.put(uuid, vQuickGameRoomIDs);
    }

    public HashSet<Long> getUserQuickGameRoomInfos(long uuid) {
        return mUserQuickGameRooms.get(uuid);
    }

    public Long getQuickGameRoomInfoByCode(String code) {
        return mCodeQuickGameRooms.get(code);
    }

    public void finishQuickGameRoom(long lQuickGameRoomID) {
        QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
        if (stQuickGameRoomInfoBuilder != null) {
            stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH);
            removeQuickGameRoomInfos(lQuickGameRoomID);

            ConnectDatabase.getInstance().insertQuickRoomInfo(stQuickGameRoomInfoBuilder.build());

            removeUserCreateQuickGameRoom(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLCreateUser(), lQuickGameRoomID);

            for (int i = 0; i < stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfosCount(); i++) {
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
        }
    }

    public QuickGameRoomResultInfo createQuickGameRoom(long uuid, GameRoomBaseInfo stGameRoomBaseInfo) {
        UserBaseInfoNet.Builder stFromUserBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

        QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();

        if (stFromUserBuilder == null) {
            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
            return stQuickGameRoomResultInfo;
        } else {
            if (stFromUserBuilder.getICreateGameLimit() == 1 && stGameRoomBaseInfo.getBPaidStart() == 0) {
                stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_GAMEROOM_OVERFLOW);
                return stQuickGameRoomResultInfo;
            } else {
                // 近似非快速局创建，此时需处理期望扣钻的情况
                long coinCost;
                boolean isLimitPaidStart = false;
                if (stFromUserBuilder.getICreateGameLimit() == 1) {
                    coinCost = GameFeeUtil.getLeagueGameFee((int) stGameRoomBaseInfo.getLSmallBlinds(), stGameRoomBaseInfo.getIGameRoomUserMaxNums(), 0) / 2;

                    if (coinCost < 0/* 交易数量安全性保护*/ || !UserManager.getInstance().checkUserCoin(uuid, coinCost)) {// 游戏币不足
                        stQuickGameRoomResultInfo.seteErrorCodeType(TexasPokerCommon.ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_COIN);

                        return stQuickGameRoomResultInfo;
                    } else {// 允许创建
                        isLimitPaidStart = true;
                        stFromUserBuilder = UserManager.getInstance().addUserCoin(stFromUserBuilder.getUuid(), -coinCost);
                        TranscationLogService.getInstance().error(stFromUserBuilder.getUuid() + " " + stGameRoomBaseInfo.getLGameRoomId() + " " + 110012 + " " + stGameRoomBaseInfo.getLDuration()
                                + " " + stGameRoomBaseInfo.getIMinBuyinRatio() + " " + stGameRoomBaseInfo.getIMaxBuyinRatio() + " " + coinCost + " " + stFromUserBuilder.getLCoin());
                    }
                }

                synchronized (RoomIDManager.class) {
                    // 创建房间流程
                    long lRoomId = RoomIDManager.getInstance().getNewRoomID(uuid);

                    // 生成聊天室
                    ChatRoomInfo stChatRoomInfo = ChatRoomInfo.newBuilder()
                            .setStChatRoomBaseInfo(ChatRoomBaseInfo.newBuilder()
                                    .setLChatRoomId(lRoomId)
                                    .setLCreateUser(uuid)
                                    .setNMaxMembers(20)
                                    .setNCurMembers(1)
                                    .setIRoomType(3)
                                    .setNOwnerCreate(1)
                                    .setNOwnerInvite(0)
                                    .setIMute(1)
                                    .setStrRoomIcon("")
                                    .setStrRoomName(stGameRoomBaseInfo.getStrRoomName())
                                    .setStrRoomTempName(""))
                            .addVUserBaseInfos(stFromUserBuilder).build();

                    // 构造房间
                    GameRoomBaseInfo transferredInfo = GameRoomBaseInfo.newBuilder(stGameRoomBaseInfo)
                            .setLCreateTime(System.currentTimeMillis())
                            .setBPrivateRoom(0)
                            .setLChatRoomID(lRoomId)
                            .setLGameRoomId(0)
                            .setStCreateUserBaseInfo(stFromUserBuilder)
                            .setBStarted(0)
                            .setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                            .setIThinkingInterval(15)
                            .setBPaidStart(isLimitPaidStart ? 1 : 0)
                            .build();

                    String sCode = RoomIDManager.getInstance().getRandomCode();// 快速码

                    // 生成快速房间
                    QuickGameRoomInfo stQuickGameRoomInfo = QuickGameRoomInfo.newBuilder()
                            .setStGameRoomBaseInfo(transferredInfo)
                            .setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                            .setStChatRoomInfo(stChatRoomInfo)
                            .setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART)
                            .setSCryptCode(sCode)
                            .build();

                    // ChatRoomManager 调用
                    ChatRoomManager.getInstance().setChatRoomInfo(lRoomId, stChatRoomInfo);

                    // DB 调用
                    ConnectDatabase.getInstance().insertRoomInfo(stChatRoomInfo);

                    // 本地调用
                    setQuickGameRoomInfos(stQuickGameRoomInfo.toBuilder());// 把房间放到管理器里面，用于管理
                    setUserQuickGameRoomInfos(uuid, lRoomId);// 加入玩家id-房间id列表

                    // UserManager 调用
                    addUserCreateQuickGameRoom(uuid, lRoomId);// 加入玩家参加的房间列表
                    UserManager.getInstance().addUserCreateGameRooms(uuid, lRoomId);// 把房间放到房主的房间列表里面

                    // 构造回包
                    stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfo);
                    stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);

                    return stQuickGameRoomResultInfo;
                }
            }
        }
    }

    public GameRoomResultInfo startQuickGameRoom(long uuid, long lQuickGameRoomID) {
        UserBaseInfoNet.Builder stUserBaseInfoNet = UserManager.getInstance().getUserBaseInfo(uuid);

        GameRoomResultInfo stGameRoomResultInfo = new GameRoomResultInfo();

        QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
        if (stQuickGameRoomInfoBuilder != null) {
            if (stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() != QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART) {
                stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_QUICK_GAMEROOM_ALREADY_START);
            } else {
                ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
                if (stChatRoomInfoBuilder != null) {
                    GameRoomBaseInfo.Builder stSettingGameRoomBaseInfoBuilder = stQuickGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
                    if (stSettingGameRoomBaseInfoBuilder != null) {
                        synchronized (RoomIDManager.class) {
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
                            gameRoomBaseInfoBuilder.setBIPLimit(stSettingGameRoomBaseInfoBuilder.getBIPLimit());
                            gameRoomBaseInfoBuilder.setBGPSLimit(stSettingGameRoomBaseInfoBuilder.getBGPSLimit());
                            gameRoomBaseInfoBuilder.setIThinkingInterval(15);
                            gameRoomBaseInfoBuilder.setBStraddle(stSettingGameRoomBaseInfoBuilder.getBStraddle());
                            gameRoomBaseInfoBuilder.setIAutoStartNum(stSettingGameRoomBaseInfoBuilder.getIAutoStartNum());
                            gameRoomBaseInfoBuilder.setBPaidStart(stSettingGameRoomBaseInfoBuilder.getBPaidStart());
                            gameRoomBaseInfoBuilder.setBFullAllIn(stSettingGameRoomBaseInfoBuilder.getBFullAllIn());
                            gameRoomBaseInfoBuilder.setIVPIPlimit(stSettingGameRoomBaseInfoBuilder.getIVPIPlimit());

                            gameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(uuid));
                            gameRoomBaseInfoBuilder.setLChatRoomID(lQuickGameRoomID);

                            TexasPoker.GameRoomInfo.Builder gameRoomInfoBuilder = TexasPoker.GameRoomInfo.newBuilder();
                            gameRoomInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

                            GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();

                            List<UserBaseInfoNet> userList = new ArrayList<>();

                            ChatRoomInfo stChatRoomInfo = stQuickGameRoomInfoBuilder.getStChatRoomInfo();
                            if (stChatRoomInfo != null) {
                                userList = stChatRoomInfo.getVUserBaseInfosList();
                                ChatRoomManager.getInstance().addGameRooms(stChatRoomInfo.getStChatRoomBaseInfo().getLChatRoomId(), gameRoomBaseInfoBuilder.getLGameRoomId());
                            }
                            gameRoomInfoBuilder.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
                            gameRoomInfoBuilder.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_GameWait);

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

                                gameRoomInfoBuilder.addVUserGameInfos(userGameInfoBuilder);
                            }
                            stGameRoomDetailInfo.setStGameRoomInfo(gameRoomInfoBuilder.build());
                            stGameRoomDetailInfo.setiGameRoomUserMaxNums(gameRoomBaseInfoBuilder.getIGameRoomUserMaxNums());
                            stGameRoomDetailInfo.lGameRoomID = gameRoomBaseInfoBuilder.getLGameRoomId();
                            stGameRoomDetailInfo.lFromRoomID = gameRoomBaseInfoBuilder.getLChatRoomID();
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

                            GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
                            ConnectDatabase.getInstance().insertGameRoomInfo(stGameRoomDetailInfo);
                            ConnectDatabase.getInstance().insertInGameUser(
                                    stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(), uuid);

                            BillManager.getInstance().createBillInfo(0, gameRoomBaseInfoBuilder.build());

                            stGameRoomDetailInfo.buyStakcsInRoom(stUserBaseInfoNet.getUuid(), 0, gameRoomBaseInfoBuilder.getLChatRoomID());

                            stSettingGameRoomBaseInfoBuilder.setLGameRoomId(gameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
                            stChatRoomInfoBuilder.addVGameRoomBaseInfos(stSettingGameRoomBaseInfoBuilder);

                            stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
                            stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(gameRoomBaseInfoBuilder);
                            stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_START);

                            QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);

                            for (int i = 0; i < userList.size(); i++) {
                                UserBaseInfoNet stUserBaseInfo = userList.get(i);
                                if (stUserBaseInfo.getUuid() != uuid) {
                                    // 发送邀请通知
                                    pushQuickGameRoomUserMsgs(stUserBaseInfo,
                                            stUserBaseInfo.getUuid(),
                                            QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_STARTGAME,
                                            stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID(),
                                            stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
                                }
                            }
                            stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
                            stGameRoomResultInfo.setStGameRoomInfo(gameRoomInfoBuilder.build());
                        }
                    }

                } else {
                    stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_CHATROOM);
                }

            }
        } else {
            stGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
        }

        return stGameRoomResultInfo;
    }

    public QuickGameRoomResultInfo enterQuickGameRoom(long uuid, String sCryptCode) {
        Long lQuickGameRoomID = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByCode(sCryptCode);

        QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();
        if (lQuickGameRoomID != null) {
            //如果快速房间存在，则读取房间基本信息

            QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);

            if (stQuickGameRoomInfoBuilder != null && stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() != QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH) {
                //首先把用户加到聊天室中
                UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(uuid);
                if (stUserBaseInfoNetBuilder != null) {
                    ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
                    //把用户加入聊天室
                    stChatRoomInfoBuilder.addVUserBaseInfos(stUserBaseInfoNetBuilder);
                    ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());

                    stQuickGameRoomInfoBuilder.setStChatRoomInfo(stChatRoomInfoBuilder);
                    //网数据库里存放聊天室用户数据
                    ConnectDatabase.getInstance().insertChatRoomMembers(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), uuid);

                    QuickGameRoomManager.getInstance().setQuickGameRoomInfos(stQuickGameRoomInfoBuilder);

                    //push消息
                    for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
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
                    if (stGameRoomDetailInfo != null && stGameRoomDetailInfo.getStGameRoomInfo() != null) {
                        stQuickGameRoomResultInfo.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
                    }

                    return stQuickGameRoomResultInfo;
                } else {
                    stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
                    return stQuickGameRoomResultInfo;
                }
            } else {
                stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
                return stQuickGameRoomResultInfo;
            }
        } else {
            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
            return stQuickGameRoomResultInfo;
        }
    }

    public QuickGameRoomResultInfo quitQuickGameRoom(long uuid, long lQuickGameRoomID) {
        //获取房间基本信息
        QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lQuickGameRoomID);
        QuickGameRoomResultInfo stQuickGameRoomResultInfo = new QuickGameRoomResultInfo();
        if (stQuickGameRoomInfoBuilder != null) {

            ChatRoomInfo.Builder stChatRoomInfoBuilder = stQuickGameRoomInfoBuilder.getStChatRoomInfoBuilder();
            if (stChatRoomInfoBuilder != null) {

                if (stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLCreateUser() != uuid) {
                    int kickIndex = -1;
                    long removeUuid = 0;
                    int removeCount = 0;
                    UserBaseInfoNet.Builder stRemoveUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

                    if (stRemoveUserBaseInfoBuilder != null) {
                        //检查房间的每一个用户，如果被删除用户在房间中，则把用户删除
                        for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                            UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
                            if (stUserBaseInfoNet.getUuid() == stRemoveUserBaseInfoBuilder.getUuid()) {
                                //把人踢出房间
                                kickIndex = i;
                                removeUuid = stUserBaseInfoNet.getUuid();
                                break;
                            }
                        }

                        //把人踢出房间
                        if (kickIndex != -1 && removeUuid != 0) {
                            removeCount++;
                            stChatRoomInfoBuilder.removeVUserBaseInfos(kickIndex);

                            //重新设置房间的名称
                            String sChatRoomName = "";
                            for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                                UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);

                                if (stUserBaseInfoNet != null) {
                                    if (stUserBaseInfoNet.getStrNick() != null) {
                                        sChatRoomName += stUserBaseInfoNet.getStrNick();
                                    }
                                }
                            }

                            //重新设置chatroom的值
                            ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());

                            //重新设置数据库数据
                            ConnectDatabase.getInstance().removeFromChatRoomUser(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), removeUuid);

                            UserManager.getInstance().removeUserChatRooms(removeUuid, stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId());

                            //然后给屋子中剩下的人发送
                            for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                                UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
                                pushQuickGameRoomUserMsg(stRemoveUserBaseInfoBuilder.build(), stRemoveUserBaseInfoBuilder.build()
                                        , stUserBaseInfoNet.getUuid(), QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_QUIT, stChatRoomInfoBuilder.build()
                                        , stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
                            }
                        }

                        //把人踢出房间
                        if (removeCount != 0) {
                            //然后退出对应牌局
                            if (stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() > 0) {
                                GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
                                if (stGameRoomDetailInfo != null) {
                                    stGameRoomDetailInfo.leaveGameRoom(uuid);
                                }
                            }

                            //最后删除快速房间
                            QuickGameRoomManager.getInstance().removeUserQuickGameRoomInfos(uuid, lQuickGameRoomID);
                            stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
                            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
                            return stQuickGameRoomResultInfo;
                        } else {
                            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_QUICK_GAMEROOM);
                            return stQuickGameRoomResultInfo;
                        }
                    } else {
                        stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
                        return stQuickGameRoomResultInfo;
                    }
                } else {
                    int kickIndex = -1;
                    long removeUuid = 0;
                    int removeCount = 0;
                    UserBaseInfoNet.Builder stRemoveUserBaseInfoBuilder = UserManager.getInstance().getUserBaseInfo(uuid);

                    if (stRemoveUserBaseInfoBuilder != null) {
                        //检查房间的每一个用户，如果被删除用户在房间中，则把用户删除
                        for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                            UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
                            if (stUserBaseInfoNet.getUuid() == stRemoveUserBaseInfoBuilder.getUuid()) {
                                //把人踢出房间
                                kickIndex = i;
                                removeUuid = stUserBaseInfoNet.getUuid();
                                break;
                            }
                        }

                        //把人踢出房间
                        if (kickIndex != -1 && removeUuid != 0) {
                            removeCount++;
                            stChatRoomInfoBuilder.removeVUserBaseInfos(kickIndex);

                            //重新设置房间的名称
                            String sChatRoomName = "";
                            for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                                UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);

                                if (stUserBaseInfoNet != null) {
                                    if (stUserBaseInfoNet.getStrNick() != null) {
                                        sChatRoomName += stUserBaseInfoNet.getStrNick();
                                    }
                                }
                            }

                            //重新设置chatroom的值
                            ChatRoomManager.getInstance().setChatRoomInfo(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), stChatRoomInfoBuilder.build());

                            //重新设置数据库数据
                            ConnectDatabase.getInstance().removeFromChatRoomUser(stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId(), removeUuid);
                            UserManager.getInstance().removeUserChatRooms(removeUuid, stChatRoomInfoBuilder.getStChatRoomBaseInfo().getLChatRoomId());

                            //然后给屋子中剩下的人发送
                            for (int i = 0; i < stChatRoomInfoBuilder.getVUserBaseInfosCount(); i++) {
                                UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfoBuilder.getVUserBaseInfos(i);
                                pushQuickGameRoomUserMsg(stRemoveUserBaseInfoBuilder.build(), stRemoveUserBaseInfoBuilder.build()
                                        , stUserBaseInfoNet.getUuid(), QUICK_GAME_ROOM_MESSAGE_TYPE.QUICK_GAME_ROOM_MESSAGE_REMOVEROOM, stChatRoomInfoBuilder.build(),
                                        stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
                            }
                        }


                        //把人踢出房间
                        if (removeCount != 0) {
                            //然后退出对应牌局
                            if (stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId() > 0) {
                                GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getLGameRoomId());
                                if (stGameRoomDetailInfo != null) {
                                    stGameRoomDetailInfo.leaveGameRoom(uuid);
                                }
                            }

                            //最后删除快速房间
                            QuickGameRoomManager.getInstance().removeQuickGameRoomInfos(lQuickGameRoomID);
                            QuickGameRoomManager.getInstance().removeUserQuickGameRoomInfos(uuid, lQuickGameRoomID);
                            QuickGameRoomManager.getInstance().removeUserCreateQuickGameRoom(uuid, lQuickGameRoomID);

                            stQuickGameRoomResultInfo.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder.build());
                            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);

                            if (stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getEGameRoomSeniorType() == GAME_ROOM_SENIOR_TYPE.GAME_ROOM_SENIOR_TYPE_NORMAL) {
                                UserManager.getInstance().addUserCoin(stQuickGameRoomInfoBuilder.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), 200);
                            }
                            return stQuickGameRoomResultInfo;

                        } else {
                            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER_IN_QUICK_GAMEROOM);
                            return stQuickGameRoomResultInfo;
                        }
                    } else {
                        stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_USER);
                        return stQuickGameRoomResultInfo;
                    }
                }

            } else {
                stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
                return stQuickGameRoomResultInfo;
            }
        } else {
            stQuickGameRoomResultInfo.seteErrorCodeType(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
            return stQuickGameRoomResultInfo;
        }
    }

    void pushQuickGameRoomUserMsgs(UserBaseInfoNet stFromUserInfo/* 邀请人 */,
                                   long stToUserUuid, QUICK_GAME_ROOM_MESSAGE_TYPE eQuickGameRoomMessageType,
                                   long lRoomChatRoomID, long lRoomGameRoomID) {
        //给被邀请用户发通知
        TexasPoker.QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = TexasPoker.QuickGameRoomPushMsg.newBuilder();

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

    void pushQuickGameRoomUserMsg(UserBaseInfoNet stFromUserBaseInfo, UserBaseInfoNet stRemoveUserBaseInfo, long stToUserUuid,
                                  QUICK_GAME_ROOM_MESSAGE_TYPE eQUICK_GAME_ROOM_MESSAGE_TYPE, ChatRoomInfo stChatRoomInfo, long lGameRoomID) {
        //给被邀请用户发通知
        TexasPoker.QuickGameRoomPushMsg.Builder stQuickGameRoomPushMsgBuilder = TexasPoker.QuickGameRoomPushMsg.newBuilder();

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
