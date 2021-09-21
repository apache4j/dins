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
        TexasPoker.GameRoomInfo stGameRoomInfo = stSCGameRoomStateChange.getStGameRoomInfo();
        System.out.print("t:"+System.currentTimeMillis()+",Gid:" + toUserId + ",roomid:" +
                stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId() + ",handid:" + stGameRoomInfo.getLGameHandID() + ",state:" + stGameRoomInfo.getEGameState());
        for (int i = 0; i < stGameRoomInfo.getVUserGameInfosCount(); i++) {
            TexasPoker.UserGameInfoNet userGameInfoNet = stGameRoomInfo.getVUserGameInfos(i);
            if (userGameInfoNet.getVCurrentHandsByteCount() > 0) {
                System.out.print(",uuid:" + userGameInfoNet.getStUserInfo().getUuid() + ",byteList:" +
                        userGameInfoNet.getVCurrentHandsByteList().get(0).toStringUtf8().length());
            }
            if (userGameInfoNet.getVCurrentHandsCount() > 0) {
                System.out.print(",uuid:" + userGameInfoNet.getStUserInfo().getUuid() + ",cardList:" +
                        userGameInfoNet.getVCurrentHandsList().toString());
            }
        }
        System.out.println();
        TexasPokerMsgService.GameRoomStateChangePushRequest.Builder stGameRoomStateChangePushRequestBuilder = TexasPokerMsgService.GameRoomStateChangePushRequest.newBuilder();
        stGameRoomStateChangePushRequestBuilder.setUuid(toUserId);
        stGameRoomStateChangePushRequestBuilder.setStSCGameRoomStateChange(stSCGameRoomStateChange);

        msgService.pushGameRoomStateChange(controller, stGameRoomStateChangePushRequestBuilder.build(),null);
    }

}
