package com.texaspoker.msg;

import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPokerMsgService;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

/**
 * Created by grunt on 2020/4/19.
 */
public class GameRoomStateMsgManager {

    //RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();

    TexasPokerMsgService.MsgService.Stub msgService;
    private static GameRoomStateMsgManager instance;

    //私有的构造函数
    private GameRoomStateMsgManager (){
        host = ConfigaturationManager.sStateMsgServiceHost;
        port = ConfigaturationManager.iStateMsgServicePost;

        client = new RpcClient();
        channel = new RpcChannel(client);
        try {
            channel.connect(host, port);
        } catch (Exception e) {
            System.out.println("GameRoomStateMsgManager client connect error:" + e.getMessage());
            return;
        }

        //机器类型和Update的对应表
        msgService = TexasPokerMsgService.MsgService.newStub(channel);
    }

    //单例模式
    public static GameRoomStateMsgManager getInstance() {
        if (instance == null) {
            synchronized(GameRoomStateMsgManager.class){
                instance = new GameRoomStateMsgManager();
            }
        }
        return instance;
    }

    public void sendGameRoomStateChangeMsg(Long toUserId,TexasPoker.SCGameRoomStateChange stSCGameRoomStateChange){
        TexasPokerMsgService.GameRoomStateChangePushRequest.Builder stGameRoomStateChangePushRequestBuilder = TexasPokerMsgService.GameRoomStateChangePushRequest.newBuilder();
        System.out.println("roomid:" + stSCGameRoomStateChange.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId() + ",stateMsg GameState()====" + stSCGameRoomStateChange.getStGameRoomInfo().getEGameState());
        System.out.println(",stateMsg table====" + stSCGameRoomStateChange.getStGameRoomInfo().getVCurrentCardsList().toString());
        TexasPoker.GameRoomInfo stGameRoomInfo = stSCGameRoomStateChange.getStGameRoomInfo();
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            if (stGameRoomInfo.getVUserGameInfos(i).getVCurrentHandsCount() > 0) {
                System.out.println("card1:" + stGameRoomInfo.getVUserGameInfos(i).getVCurrentHands(0) + ",card2:" + stGameRoomInfo.getVUserGameInfos(i).getVCurrentHands(1));
            }
            if (stGameRoomInfo.getVUserGameInfos(i).getVCurrentHandsByteCount() > 0) {
                System.out.println("bytecard:" + stGameRoomInfo.getVUserGameInfos(i).getVCurrentHandsByte(0));
            }

        }
        stGameRoomStateChangePushRequestBuilder.setUuid(toUserId);
        stGameRoomStateChangePushRequestBuilder.setStSCGameRoomStateChange(stSCGameRoomStateChange);

        msgService.pushGameRoomStateChange(controller, stGameRoomStateChangePushRequestBuilder.build(),null);
    }

}
