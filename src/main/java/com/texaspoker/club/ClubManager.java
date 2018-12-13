package com.texaspoker.club;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.logger.PlayLogService;
import com.texaspoker.moment.TexasPokerClubService;
import com.texaspoker.moment.TexasPokerClubService.*;
import com.texaspoker.moment.TexasPokerCommon;
import com.texaspoker.moment.TexasPokerCommon.ClubBaseInfo;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerHttpClub.*;
import com.texaspoker.moment.TexasPokerHttpClub.CLUB_USER_LEVEL;
import com.texaspoker.moment.TexasPokerHttpClub.ClubInfo;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

import java.util.ArrayList;
import java.util.List;

public class ClubManager {
    private static ClubManager instance;

    //RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    private TexasPokerClubService.ClubService.BlockingInterface blockingClubService;


    private ClubManager() {
        host = ConfigaturationManager.sClubServiceHost;
        port = ConfigaturationManager.iClubServicePost;

        //RPC初始化流程
        client = new RpcClient();
        channel = new RpcChannel(client);
        try {
            channel.connect(host, port);
        } catch (Exception e) {
            NormalLogService.getInstance().error("client connect error:" + e.getMessage());
            return;
        }

        blockingClubService = TexasPokerClubService.ClubService.newBlockingStub(channel);
        NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
    }

    //初始化
    public static ClubManager getInstance() {
        if (instance == null) {
            synchronized (ClubManager.class) {
                instance = new ClubManager();
            }
        }
        return instance;
    }

    //获取俱乐部信息
    public ClubInfo.Builder getClubInfo(long lClubID) {
        GetClubInfoRequest.Builder stGetClubInfoRequestBuilder = GetClubInfoRequest.newBuilder();
        stGetClubInfoRequestBuilder.setLClubID(lClubID);

        try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
            GetClubInfoResponse response = blockingClubService.getClubInfo(controller, stGetClubInfoRequestBuilder.build());

            if (response != null && response.getStClubInfo() != null && response.getStClubInfo().getStClubBaseInfo().getLClubID() > 0) {
                return response.getStClubInfo().toBuilder();
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    //删除俱乐部信息
    public void removeClubInfo(long lClubID) {
        ClubInfoClubID.Builder stClubInfoClubIDBuilder = ClubInfoClubID.newBuilder();
        stClubInfoClubIDBuilder.setLClubID(lClubID);

        try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
            blockingClubService.removeClubInfo(controller, stClubInfoClubIDBuilder.build());
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
        }
    }

    //获取俱乐部中的牌局
    public ArrayList<Long> getGameRooms(long clubID, long uuid) {
        GetClubGameRoomsRequest.Builder stGetClubGameRoomsRequestBuilder = GetClubGameRoomsRequest.newBuilder();
        stGetClubGameRoomsRequestBuilder.setLClubID(clubID);
        stGetClubGameRoomsRequestBuilder.setUuid(uuid);

        try {
            GetClubGameRoomsResponse stGetClubGameRoomsResponse = blockingClubService.getClubGameRooms(controller, stGetClubGameRoomsRequestBuilder.build());
            if (stGetClubGameRoomsResponse != null && stGetClubGameRoomsResponse.getVGameRoomIDsList() != null) {
                return new ArrayList<Long>(stGetClubGameRoomsResponse.getVGameRoomIDsList());
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
        }

        return null;
    }

    /**
     * 添加俱乐部中的游戏房间
     *
     * @param cid 俱乐部 ID
     * @param rid 游戏房间 ID
     * @param lid 联盟 ID
     */
    public void addGameRooms(long cid, long rid, long lid) {
        PlayLogService.getInstance().debug("ClubManager.addGameRooms - 1001 cid: >" + cid + "< rid: >" + rid + "< lid: >" + lid + "<");
        ClubGameRequest clubGameRequest = ClubGameRequest.newBuilder()
                .setLClubID(cid)
                .setLGameRoomID(rid)
                .setBLeagueGame(lid > 0 ? 1 : 0)
                .setEGAMEROOMTYPE(TexasPokerCommon.GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                .setLLeagueID(lid)
                .build();

        try {
            PlayLogService.getInstance().debug("clubGameRequest: >" + clubGameRequest + "<");
            blockingClubService.addClubGameRooms(controller, clubGameRequest);
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("ClubManager.addGameRooms - caused exception, e.getMessage(): >" + e.getMessage() + "<");
            e.printStackTrace();
        }
        PlayLogService.getInstance().debug("ClubManager.addGameRooms - 1001000");
    }

    /**
     * 移除俱乐部中的游戏房间
     *
     * @param cid 俱乐部 ID
     * @param rid 游戏房间 ID
     * @param lid 联盟 ID
     */
    public void removeChatRoomGameRoom(long cid, long rid, long lid) {
        PlayLogService.getInstance().debug("ClubManager.removeChatRoomGameRoom - 1001 cid: >" + cid + "< rid: >" + rid + "< lid: >" + lid + "<");
        ClubGameRequest clubGameRequest = ClubGameRequest.newBuilder()
                .setLClubID(cid)
                .setLGameRoomID(rid)
                .setBLeagueGame(lid > 0 ? 1 : 0)
                .setEGAMEROOMTYPE(TexasPokerCommon.GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE)
                .setLLeagueID(lid)
                .build();

        try {
            PlayLogService.getInstance().debug("clubGameRequest: >" + clubGameRequest + "<");
            blockingClubService.removeClubGameRooms(controller, clubGameRequest);
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("ClubManager.removeChatRoomGameRoom - caused exception, e.getMessage(): >" + e.getMessage() + "<");
            e.printStackTrace();
        }
        PlayLogService.getInstance().debug("ClubManager.removeChatRoomGameRoom - 1001000");
    }

    //检查俱乐部是否已经满员
    public boolean checkFull(long clubid, long uuid) {
        CheckFullByClubidAndLevelRequest.Builder stcheckFullByClubidAndLevelRequestBuilder = CheckFullByClubidAndLevelRequest.newBuilder();
        stcheckFullByClubidAndLevelRequestBuilder.setLClubID(clubid);
        stcheckFullByClubidAndLevelRequestBuilder.setILevel(0);
        stcheckFullByClubidAndLevelRequestBuilder.setUuid(uuid);

        try {
            CheckFullByClubidAndLevelResponse stcheckFullByClubidAndLevelResponse = blockingClubService.checkFullByClubidAndLevel(controller, stcheckFullByClubidAndLevelRequestBuilder.build());
            if (stcheckFullByClubidAndLevelResponse != null && stcheckFullByClubidAndLevelResponse.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (stcheckFullByClubidAndLevelResponse.getIStatus() == 1) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (ServiceException e) {
            System.out.println("syn echo error:" + e.getMessage());
        }
        return false;

    }

    //获取俱乐部信息
    public ClubBaseInfo.Builder getClubBaseInfo(long lClubID) {
        GetClubBaseInfoRequest.Builder stGetClubBaseInfoRequestBuilder = GetClubBaseInfoRequest.newBuilder();
        stGetClubBaseInfoRequestBuilder.setLClubID(lClubID);

        try {
//			System.out.println("getUserBaseInfogetUserBaseInforpc begin.");
            GetClubBaseInfoResponse response = blockingClubService.getClubBaseInfo(controller, stGetClubBaseInfoRequestBuilder.build());

            if (response != null && response.getStClubBaseInfo() != null && response.getStClubBaseInfo().getLClubID() > 0) {
                return response.getStClubBaseInfo().toBuilder();
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    //该用户是否在俱乐部中
    public boolean inClub(long clubID, long uuid) {
        CheckInClubRequest.Builder stCheckInClubRequestBuilder = CheckInClubRequest.newBuilder();
        stCheckInClubRequestBuilder.setLClubID(clubID);
        stCheckInClubRequestBuilder.setLClubMemberUuid(uuid);

        try {
            CheckInClubResponse stCheckInClubResponse = blockingClubService.checkInClub(controller, stCheckInClubRequestBuilder.build());
            if (stCheckInClubResponse != null && stCheckInClubResponse.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (stCheckInClubResponse.getBInClub() == 1) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (ServiceException e) {
            System.out.println("syn echo error:" + e.getMessage());
        }
        return false;
    }

    //添加俱乐部基金
    public ClubBaseInfo.Builder addClubfund(long lClubid, long lFund) {
        AddClubFundRequest.Builder stAddClubFundRequestBuilder = AddClubFundRequest.newBuilder();
        stAddClubFundRequestBuilder.setLClubID(lClubid);
        stAddClubFundRequestBuilder.setLFund(lFund);

        try {
            AddClubFundResponse response = blockingClubService.addClubFund(controller, stAddClubFundRequestBuilder.build());
            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    //添加俱乐部积分
    public ClubBaseInfo.Builder addClubScore(long lClubid, long lScore) {
        AddClubScoreRequest.Builder stAddClubScoreRequestBuilder = AddClubScoreRequest.newBuilder();
        stAddClubScoreRequestBuilder.setLClubID(lClubid);
        stAddClubScoreRequestBuilder.setLScore(lScore);

        try {
            AddClubScoreResponse response = blockingClubService.addClubScore(controller, stAddClubScoreRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubBaseInfo.Builder setClubBaseInfo(long clubid, String sClubName, String sClubDesc) {
        SetClubBaseInfoRequest.Builder stSetClubBaseInfoRequestBuilder = SetClubBaseInfoRequest.newBuilder();
        stSetClubBaseInfoRequestBuilder.setLClubID(clubid);
        stSetClubBaseInfoRequestBuilder.setSClubName(sClubName);
        stSetClubBaseInfoRequestBuilder.setSDesc(sClubDesc);

        try {
            SetClubBaseInfoResponse response = blockingClubService.setClubBaseInfo(controller, stSetClubBaseInfoRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubInfo.Builder setClubUserLevel(long clubid, long uuid, CLUB_USER_LEVEL eClubUserLevel) {
        SetClubUserLevelRequest.Builder stSetClubUserLevelRequestBuilder = SetClubUserLevelRequest.newBuilder();
        stSetClubUserLevelRequestBuilder.setLClubID(clubid);
        stSetClubUserLevelRequestBuilder.setUuid(uuid);
        stSetClubUserLevelRequestBuilder.setEClubUserLevel(eClubUserLevel);

        try {
            SetClubUserLevelResponse response = blockingClubService.setClubUserLevel(controller, stSetClubUserLevelRequestBuilder.build());

            if (response != null && response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (response.getStClubInfo() != null) {
                    return response.getStClubInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubBaseInfo.Builder uploadClubIcon(long clubid, String sBigIcon, String sSmallIcon) {
        UploadClubIconRequest.Builder stUploadClubIconRequestBuilder = UploadClubIconRequest.newBuilder();
        stUploadClubIconRequestBuilder.setLClubID(clubid);
        stUploadClubIconRequestBuilder.setSSmallClubIcon(sSmallIcon);
        stUploadClubIconRequestBuilder.setSBigClubIcon(sBigIcon);

        try {
            UploadClubIconResponse response = blockingClubService.uploadClubIcon(controller, stUploadClubIconRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubBaseInfo.Builder uploadClubCover(long clubid, String sBigCover, String sSmallCover, int iPos) {
        UploadClubCoverRequest.Builder stUploadClubCoverRequestBuilder = UploadClubCoverRequest.newBuilder();
        stUploadClubCoverRequestBuilder.setLClubID(clubid);
        stUploadClubCoverRequestBuilder.setSBigAlbums(sBigCover);
        stUploadClubCoverRequestBuilder.setSSmallAlbums(sSmallCover);
        stUploadClubCoverRequestBuilder.setIPos(iPos);
        try {
            UploadClubCoverResponse response = blockingClubService.uploadClubCover(controller, stUploadClubCoverRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubBaseInfo.Builder deleteClubCover(long clubid, int iPos) {
        DeleteClubCoverRequest.Builder stDeleteClubCoverRequestBuilder = DeleteClubCoverRequest.newBuilder();
        stDeleteClubCoverRequestBuilder.setLClubID(clubid);
        stDeleteClubCoverRequestBuilder.setIPos(iPos);

        try {
            DeleteClubCoverResponse response = blockingClubService.deleteClubCover(controller, stDeleteClubCoverRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubSettingInfo setClubSetting(long uuid, long clubID, int mute, int vibrate) {
        SetClubSettingRequest.Builder stSetClubSettingRequestBuilder = SetClubSettingRequest.newBuilder();
        stSetClubSettingRequestBuilder.setLClubID(clubID);
        stSetClubSettingRequestBuilder.setUuid(uuid);
        stSetClubSettingRequestBuilder.setIMute(mute);
        stSetClubSettingRequestBuilder.setIVibrate(vibrate);

        try {
            SetClubSettingResponse response = blockingClubService.setClubSetting(controller, stSetClubSettingRequestBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                ClubSettingInfo stClubSettingInfo = new ClubSettingInfo();
                stClubSettingInfo.iMute = response.getIMute();
                stClubSettingInfo.iVibrate = response.getIVibrate();
                return stClubSettingInfo;
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubSettingInfo getClubSetting(long uuid, long clubID) {
        GetClubSettingRequest.Builder stGetClubSettingBuilder = GetClubSettingRequest.newBuilder();
        stGetClubSettingBuilder.setLClubID(clubID);
        stGetClubSettingBuilder.setUuid(uuid);

        try {
            GetClubSettingResponse response = blockingClubService.getClubSetting(controller, stGetClubSettingBuilder.build());

            if (response != null && response.getIErrorCode() == 0) {
                ClubSettingInfo stClubSettingInfo = new ClubSettingInfo();
                stClubSettingInfo.iMute = response.getIMute();
                stClubSettingInfo.iVibrate = response.getIVibrate();
                return stClubSettingInfo;
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubInfo.Builder addClubMember(long clubid, long uuid, CLUB_USER_LEVEL eClubUserLevel) {
        AddClubMemberRequest.Builder stAddClubMemberRequestBuilder = AddClubMemberRequest.newBuilder();
        stAddClubMemberRequestBuilder.setLClubID(clubid);
        stAddClubMemberRequestBuilder.setUuid(uuid);
        stAddClubMemberRequestBuilder.setEClubUserLevel(eClubUserLevel);

        try {
            AddClubMemberResponse response = blockingClubService.addClubMember(controller, stAddClubMemberRequestBuilder.build());

            if (response != null && response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (response.getStClubInfo() != null) {
                    return response.getStClubInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubInfo.Builder deleteClubMember(long clubid, long uuid) {
        DeleteClubMemberRequest.Builder stDeleteClubMemberRequestBuilder = DeleteClubMemberRequest.newBuilder();
        stDeleteClubMemberRequestBuilder.setLClubID(clubid);
        stDeleteClubMemberRequestBuilder.setUuid(uuid);

        try {
            DeleteClubMemberResponse response = blockingClubService.deleteClubMember(controller, stDeleteClubMemberRequestBuilder.build());

            if (response != null && response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (response.getStClubInfo() != null) {
                    return response.getStClubInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public ClubBaseInfo.Builder addClubMax(long clubid, int iNum) {
        AddClubMaxRequest.Builder stAddClubMaxRequest = AddClubMaxRequest.newBuilder();
        stAddClubMaxRequest.setLClubID(clubid);
        stAddClubMaxRequest.setINum(iNum);

        try {
            AddClubMaxResponse response = blockingClubService.addClubMax(controller, stAddClubMaxRequest.build());

            if (response != null && response.getIErrorCode() == 0) {
                if (response.getStClubBaseInfo() != null) {
                    return response.getStClubBaseInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public List<Long> getClubManagerUuids(long clubid) {
        GetClubManagerUuidsRequest.Builder stGetClubManagerUuidsRequestBuilder = GetClubManagerUuidsRequest.newBuilder();
        stGetClubManagerUuidsRequestBuilder.setLClubID(clubid);
        stGetClubManagerUuidsRequestBuilder.setUuid(0);

        try {
            GetClubManagerUuidsResponse response = blockingClubService.getClubManagerUuids(controller, stGetClubManagerUuidsRequestBuilder.build());

            if (response != null && response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS) {
                if (response.getVClubManagerUuidsList() != null) {
                    return response.getVClubManagerUuidsList();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            NormalLogService.getInstance().error("syn echo error:" + e.getMessage());
            return null;
        }
    }

    public boolean checkClubManager(long lClubID, long uuid) {
        List<Long> vClubManagerUuids = ClubManager.getInstance().getClubManagerUuids(lClubID);
        if (vClubManagerUuids != null) {
            //检查房主是否是房间的管理员
            for (long lClubManagerUuid : vClubManagerUuids) {
                //检查房主是否是俱乐部的管理员
                if (lClubManagerUuid == uuid) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取俱乐部玩家信息
     * @param lClubID
     * @param uuid
     * @return
     */
    public ClubUserCrditInfo.Builder getClubUserCreditInfo(long lClubID, long uuid){
        GetClubUserCreditInfoRequest.Builder stGetClubUserCreditInfoRequest = GetClubUserCreditInfoRequest.newBuilder();
        stGetClubUserCreditInfoRequest.setUuid(uuid);
        stGetClubUserCreditInfoRequest.setLClubID(lClubID);

        try {
            GetClubUserCreditInfoResponse response = blockingClubService.getClubUserCreditInfo(controller, stGetClubUserCreditInfoRequest.build());

            if (response != null && response.getEErrorCode() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
                if (response.getStClubUserCrditInfo() != null){
                    return response.getStClubUserCrditInfo().toBuilder();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新玩家联盟币 买入为正值 带出为负值
     * @param lClubID
     * @param uuid
     * @param lDcredit
     * @return
     */
    public ERROR_CODE_TYPE updateUserCreditInGame(long lClubID, long uuid, long lDcredit){
        UpdateUserCreditInGameRequest.Builder stUpdateUserCreditInGameRequest = UpdateUserCreditInGameRequest.newBuilder();
        stUpdateUserCreditInGameRequest.setUuid(uuid);
        stUpdateUserCreditInGameRequest.setLClubID(lClubID);
        stUpdateUserCreditInGameRequest.setLDcredit(lDcredit);
        stUpdateUserCreditInGameRequest.setBUpdateBalance(0);

        UpdateUserCreditInGameResponse response = null;
        try {
            response = blockingClubService.updateUserCreditInGame(controller, stUpdateUserCreditInGameRequest.build());
            if (response != null){
                return response.getEErrorCode();
            } else {
                return null;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 授信俱乐部是否冻结 踢人时检测使用
     * @param lClubID
     * @return
     */
    public boolean isCreditClubFrozen(long lClubID){
        IsCreditClubFrozenRequest.Builder stIsCreditClubFrozenRequest = IsCreditClubFrozenRequest.newBuilder();
        stIsCreditClubFrozenRequest.setLClubID(lClubID);

        IsCreditClubFrozenResponse response = null;
        try {
            response = blockingClubService.isCreditClubFrozen(controller, stIsCreditClubFrozenRequest.build());
            if (response != null && response.getIErrorCode() == 0){
                return true;
            } else {
                return false;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 授信俱乐部是否冻结 坐下,买入时检测使用
     * @param lClubID
     * @return
     */
    public boolean isLimitCreditClubBuyIn(long lClubID){
        IsLimitCreditClubBuyInRequest.Builder stIsLimitCreditClubBuyInRequest = IsLimitCreditClubBuyInRequest.newBuilder();
        stIsLimitCreditClubBuyInRequest.setLClubID(lClubID);

        IsLimitCreditClubBuyInResponse response = null;
        try {
            response = blockingClubService.isLimitCreditClubBuyIn(controller, stIsLimitCreditClubBuyInRequest.build());
            if (response != null && response.getIErrorCode() == 0){
                return true;
            } else {
                return false;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查用户授信余额是否足够
     * @param lClubID
     * @param uuid
     * @param lCredit
     * @return
     */
    public boolean checkClubUserCredit(long lClubID, long uuid, long lCredit){
        CheckClubUserCreditRequest.Builder stCheckClubUserCreditRequest = CheckClubUserCreditRequest.newBuilder();
        stCheckClubUserCreditRequest.setUuid(uuid);
        stCheckClubUserCreditRequest.setLClubID(lClubID);
        stCheckClubUserCreditRequest.setLCredit(lCredit);

        CheckClubUserCreditResponse response = null;
        try {
            response = blockingClubService.checkClubUserCredit(controller, stCheckClubUserCreditRequest.build());
            if (response != null && response.getIErrorCode() == 1){
                return true;
            } else {
                return false;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return false;
        }
    }
}
