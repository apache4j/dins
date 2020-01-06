package com.texaspoker.bill;

import com.google.protobuf.ServiceException;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPokerBillService;
import com.texaspoker.moment.TexasPokerBillService.*;
import com.texaspoker.moment.TexasPokerCommon;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

import java.util.List;

public class BillManager {

    //RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerBillService.BillService.BlockingInterface blockingBillService;

    TexasPokerBillService.BillService.Stub billService;

    private static BillManager instance;

    //私有的构造函数
    private BillManager() {
        host = ConfigaturationManager.sBillGameServiceHost;
        port = ConfigaturationManager.iBillGameServicePost;

        client = new RpcClient();
        channel = new RpcChannel(client);
        try {
            channel.connect(host, port);
        } catch (Exception e) {
            NormalLogService.getInstance().error("client connect error:" + e.getMessage());
            return;
        }

        blockingBillService = TexasPokerBillService.BillService.newBlockingStub(channel);
        billService = TexasPokerBillService.BillService.newStub(channel);
        NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");

    }

    //单例模式
    public static BillManager getInstance() {
        if (instance == null) {
            synchronized (BillManager.class) {
                instance = new BillManager();
            }
        }
        return instance;
    }

    public boolean removeBill(long uuid, long roomid) {
        //设置用户
        RemoveBillInfoRequest.Builder stRemoveBillInfoRequestBuilder = RemoveBillInfoRequest.newBuilder();
        stRemoveBillInfoRequestBuilder.setUuid(uuid);
        stRemoveBillInfoRequestBuilder.setLBillID(roomid);
        RemoveBillInfoResponse stRemoveBillInfoResponse = null;
        try {
            stRemoveBillInfoResponse = blockingBillService.removeBillInfo(controller, stRemoveBillInfoRequestBuilder.build());
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (stRemoveBillInfoResponse != null && stRemoveBillInfoResponse.getIErrorCode() == 0) {
            return true;
        } else {
            return false;
        }
    }

    //改变某个房间的某个用户的购买筹码的值
    public boolean buyStackInRoom(long uuid, long roomid, long changeStacks, long clubid, String clubname) {
        GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);
        if (stGameRoomDetailInfo != null
                && stGameRoomDetailInfo.getStGameRoomInfo() != null
                && stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo() != null) {

            //设置用户
            BuyStackInRoomRequest.Builder stBuyStackInRoomRequestBuilder = BuyStackInRoomRequest.newBuilder();
            stBuyStackInRoomRequestBuilder.setUuid(uuid);
            stBuyStackInRoomRequestBuilder.setLBillID(roomid);
            stBuyStackInRoomRequestBuilder.setLChangeStacks(changeStacks);
            stBuyStackInRoomRequestBuilder.setLClubID(clubid);
            stBuyStackInRoomRequestBuilder.setSClubName(clubname);

            try {
                BuyStackInRoomResponse stBuyStackInRoomResponse = blockingBillService.buyStackInRoom(controller, stBuyStackInRoomRequestBuilder.build());
                if (stBuyStackInRoomResponse != null && stBuyStackInRoomResponse.getIErrorCode() == 0) {
                    return true;
                } else {
                    return false;
                }
            } catch (ServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    public void setCurrentStacksInRoom(long uuid, long roomid, long stacks, long lUnloadStacks, int handid) {
        GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);

        if (stGameRoomDetailInfo != null
                && stGameRoomDetailInfo.getStGameRoomInfo() != null
                && stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo() != null) {
            //设置用户
            SetCurrentStacksInRoomRequest.Builder stSetCurrentStacksInRoomRequestBuilder = SetCurrentStacksInRoomRequest.newBuilder();
            stSetCurrentStacksInRoomRequestBuilder.setUuid(uuid);
            stSetCurrentStacksInRoomRequestBuilder.setLBillID(roomid);
            stSetCurrentStacksInRoomRequestBuilder.setLStacks(stacks);
            stSetCurrentStacksInRoomRequestBuilder.setISNGRank(0);
            stSetCurrentStacksInRoomRequestBuilder.setLUnloadStacks(lUnloadStacks);
            stSetCurrentStacksInRoomRequestBuilder.setIPlayHandNum(handid);

            try {
                blockingBillService.setCurrentStacksInRoom(controller, stSetCurrentStacksInRoomRequestBuilder.build());
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
    }

    public void addInsuranceBuyin(long uuid, long roomid, long stacks) {
        GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);

        if (stGameRoomDetailInfo != null
                && stGameRoomDetailInfo.getStGameRoomInfo() != null
                && stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo() != null) {
            //设置用户
            AddInsuranceBuyinRequest.Builder stAddInsuranceBuyinRequestBuilder = AddInsuranceBuyinRequest.newBuilder();
            stAddInsuranceBuyinRequestBuilder.setUuid(uuid);
            stAddInsuranceBuyinRequestBuilder.setLBillID(roomid);
            stAddInsuranceBuyinRequestBuilder.setLInsuranceBuyin(stacks);

            billService.addInsuranceBuyin(controller, stAddInsuranceBuyinRequestBuilder.build(), null);
        }
    }

    public void addInsuranceGetStacks(long uuid, long roomid, long stacks, long lInsuranceBuyin) {
        GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);

        if (stGameRoomDetailInfo != null
                && stGameRoomDetailInfo.getStGameRoomInfo() != null
                && stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo() != null) {
            //设置用户
            AddInsuranceGetStackRequest.Builder stAddInsuranceGetStackRequestBuilder = AddInsuranceGetStackRequest.newBuilder();
            stAddInsuranceGetStackRequestBuilder.setUuid(uuid);
            stAddInsuranceGetStackRequestBuilder.setLBillID(roomid);
            stAddInsuranceGetStackRequestBuilder.setLGetStacks(stacks);
            stAddInsuranceGetStackRequestBuilder.setLInsuranceBuyin(lInsuranceBuyin);

            try {
                blockingBillService.addInsuranceGetStack(controller, stAddInsuranceGetStackRequestBuilder.build());
            } catch (ServiceException e) {
                e.printStackTrace();
            }

        }
    }

    public void setFinalInsuranceGetStacks(long uuid, long roomid, long stacks) {
        GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(roomid);

        if (stGameRoomDetailInfo != null
                && stGameRoomDetailInfo.getStGameRoomInfo() != null
                && stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo() != null) {
            //设置用户
            SetFinalInsuranceInfoRequest.Builder stSetFinalInsuranceInfoRequestBuilder = SetFinalInsuranceInfoRequest.newBuilder();
            stSetFinalInsuranceInfoRequestBuilder.setUuid(uuid);
            stSetFinalInsuranceInfoRequestBuilder.setLBillID(roomid);
            stSetFinalInsuranceInfoRequestBuilder.setLTotalGetStacks(stacks);
            stSetFinalInsuranceInfoRequestBuilder.setLTotalInsuranceBuyin(0);

            billService.setFinalInsuranceInfo(controller, stSetFinalInsuranceInfoRequestBuilder.build(), null);
        }
    }

    public void setGameOverTag(long roomid, long maxpot, long totalgamenum) {
        //设置用户
        SetGameOverTagRequest.Builder stSetGameOverTagRequestBuilder = SetGameOverTagRequest.newBuilder();
        stSetGameOverTagRequestBuilder.setLBillID(roomid);
        stSetGameOverTagRequestBuilder.setLMaxpot(maxpot);
        stSetGameOverTagRequestBuilder.setLTotalgamenum(totalgamenum);

        try {
            blockingBillService.setGameOverTag(controller, stSetGameOverTagRequestBuilder.build());
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void createBillInfo(long uuid, GameRoomBaseInfo stGameRoomBaseInfo) {
        //设置用户
        CreateBillInfoRequest.Builder stCreateBillInfoRequestBuilder = CreateBillInfoRequest.newBuilder();
        stCreateBillInfoRequestBuilder.setLBillID(stGameRoomBaseInfo.getLGameRoomId());
        stCreateBillInfoRequestBuilder.setUuid(uuid);
        stCreateBillInfoRequestBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL);
        stCreateBillInfoRequestBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfo);

        try {
            blockingBillService.createBillInfo(controller, stCreateBillInfoRequestBuilder.build());
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<Long> getBillInfoUsers(long lBillID) {
        GetAllBillUsersRequest.Builder stGetAllBillUsersRequestBuilder = GetAllBillUsersRequest.newBuilder();
        stGetAllBillUsersRequestBuilder.setLBillID(lBillID);

        GetAllBillUsersResponse stGetAllBillUsersResponse = null;
        try {
            stGetAllBillUsersResponse = blockingBillService.getAllBillUsers(controller, stGetAllBillUsersRequestBuilder.build());
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (stGetAllBillUsersResponse != null && stGetAllBillUsersResponse.getIErrorCode() == 0) {
            return stGetAllBillUsersResponse.getVUuidsList();
        } else {
            return null;
        }
    }

//	public void finishGameRoomAndReturn(long lBillID){
//		FinishGameRoomAndReturnRequest.Builder stFinishGameRoomAndReturnRequestBuilder = FinishGameRoomAndReturnRequest.newBuilder();
//		stFinishGameRoomAndReturnRequestBuilder.setLBillID(lBillID);
//		
//		try {
//			blockingBillService.finishGameRoomAndReturn(controller, stFinishGameRoomAndReturnRequestBuilder.build());
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

    /**
     * 站起锁水,玩家站起时调用
     * @param uuid
     * @param roomid
     */
    public void standUpToBill(long uuid, long roomid, long lBetStacks, long handid, String roomName, String msgKey){
        StandUpToBillRequest.Builder stStandUpToBillRequest = StandUpToBillRequest.newBuilder();
        stStandUpToBillRequest.setUuid(uuid);
        stStandUpToBillRequest.setLGameRoomID(roomid);
        stStandUpToBillRequest.setLBetStacks(lBetStacks);
        stStandUpToBillRequest.setEGameType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
        stStandUpToBillRequest.setLHandId(handid);
        stStandUpToBillRequest.setSRoomName(roomName);
        stStandUpToBillRequest.setMsgKey(msgKey);

        try {
            blockingBillService.standUpToBill(controller, stStandUpToBillRequest.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    /**
     * 到点抽水,一手结束后进行抽水
     * @param uuid
     * @param roomid
     * @return
     */
    public List<UserRemainstackAfterDraw> drawBill(long uuid, long roomid){
        DrawBillRequest.Builder stDrawBillRequest = DrawBillRequest.newBuilder();
        stDrawBillRequest.setUuid(uuid);
        stDrawBillRequest.setLGameRoomID(roomid);

        DrawBillResponse stDrawBillResponse = null;
        try {
            stDrawBillResponse = blockingBillService.drawBill(controller, stDrawBillRequest.build());
            if (stDrawBillResponse != null && stDrawBillResponse.getIErrorCode() == 0){
                return stDrawBillResponse.getVUserRemainstackAfterDrawsList();
            } else {
                return null;
            }
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 玩家离座时间超过限制,玩家阶段性结束
     * @param uuid
     * @param roomid
     */
    public void checkOutBill(long uuid, long roomid){
        CheckOutBillRequest.Builder stCheckOutBillRequest = CheckOutBillRequest.newBuilder();
        stCheckOutBillRequest.setUuid(uuid);
        stCheckOutBillRequest.setLGameRoomID(roomid);

        try {
            blockingBillService.checkOutBill(controller, stCheckOutBillRequest.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    /**
     * 玩家坐下未超时,解除锁水 会检查玩家联盟币是否够上桌
     * @param uuid
     * @param roomid
     */
    public TexasPokerCommon.ERROR_CODE_TYPE sitDownToBill(long uuid, long roomid, long handid, String roomName, String msgKey){
        SitDownToBillRequest.Builder stSitDownToBillRequest = SitDownToBillRequest.newBuilder();
        stSitDownToBillRequest.setLGameRoomID(roomid);
        stSitDownToBillRequest.setUuid(uuid);
        stSitDownToBillRequest.setEGameType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
        stSitDownToBillRequest.setLHandId(handid);
        stSitDownToBillRequest.setSRoomName(roomName);
        stSitDownToBillRequest.setMsgKey(msgKey);

        SitDownToBillResponse response = null;
        try {
            response = blockingBillService.sitDownToBill(controller, stSitDownToBillRequest.build());
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
     * 底池抽水将玩家贡献抽水同步至账单服
     * @param roomid
     * @param userPotDrawInfoList
     */
    public void setPotDrawCredit(long roomid, List<UserPotDrawInfo> userPotDrawInfoList){
        SetPotDrawCreditRequest.Builder stSetPotDrawCreditRequest = SetPotDrawCreditRequest.newBuilder();
        stSetPotDrawCreditRequest.setLGameRoomID(roomid);
        stSetPotDrawCreditRequest.addAllVUserPotDrawInfos(userPotDrawInfoList);

        try {
            blockingBillService.setPotDrawCredit(controller, stSetPotDrawCreditRequest.build());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

}

