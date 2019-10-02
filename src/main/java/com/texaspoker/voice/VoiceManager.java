package com.texaspoker.voice;

import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerCommon;
import com.texaspoker.moment.TexasPokerVoicePrintService;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

/**
 * Created by zhangmeng on 2019/9/23.
 */
public class VoiceManager {

    private static RpcController controller = new RpcController();
    private TexasPokerVoicePrintService.VoicePrintService.Stub voiceStub;
    private TexasPokerVoicePrintService.VoicePrintService.BlockingInterface voiceBlocking;

    private static VoiceManager instance;

    private VoiceManager(){
        String host = ConfigaturationManager.sVoiceServiceHost;
        int port = ConfigaturationManager.iVoiceServicePost;

        // RPC Client 初始化
        RpcClient client = new RpcClient();
        RpcChannel channel = new RpcChannel(client);

        try {
            channel.connect(host, port);
        } catch (Exception e) {
            NormalLogService.getInstance().error("VoiceManager.VoiceManager - channel.connect() caused exception, e.getMessage(): >" + e.getMessage() + "<");
            return;
        }

        voiceStub = TexasPokerVoicePrintService.VoicePrintService.newStub(channel);
        voiceBlocking = TexasPokerVoicePrintService.VoicePrintService.newBlockingStub(channel);
        NormalLogService.getInstance().error("VoiceManager.VoiceManager - channel.connect() is UP, host: >" + host + "< port: >" + port + "<");
    }

    public static VoiceManager getInstance() {
        if (instance == null) {
            synchronized (VoiceManager.class) {
                instance = new VoiceManager();
            }
        }
        return instance;
    }

    /**
     * 创建组
     * @param roomid
     */
    public void createGroup(long roomid){
        try {
            TexasPokerVoicePrintService.CreateGroupRequest.Builder request = TexasPokerVoicePrintService.CreateGroupRequest.newBuilder();
            request.setRoomId(roomid);

            voiceStub.createGroup(controller, request.build(), null);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家今日验证次数
     * @param uuid
     * @return
     */
    public int getVerifyCount(long uuid){
        try {
            TexasPokerVoicePrintService.GetVerifyCountRequest.Builder request = TexasPokerVoicePrintService.GetVerifyCountRequest.newBuilder();
            request.setUuid(uuid);

            TexasPokerVoicePrintService.GetVerifyCountResponse response = voiceBlocking.getVerifyCount(controller, request.build());
            if (response != null && response.getVErrCode() == 1){
                return response.getTotalCount();
            } else {
                return 0;
            }

        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取一个短语
     * @param uuid
     * @return
     */
    public String getOnePhrase(long uuid){
        try {
            TexasPokerVoicePrintService.GetOnePhraseRequest.Builder request = TexasPokerVoicePrintService.GetOnePhraseRequest.newBuilder();
            request.setUuid(uuid);

            TexasPokerVoicePrintService.GetOnePhraseResponse response = voiceBlocking.getOnePhrase(controller, request.build());
            if (response != null && response.getVErrCode() == 1){
                return response.getPhrase();
            } else {
                return "";
            }
        } catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 通知声纹服玩家验证的基本信息
     * @param roomid
     * @param uuid
     * @param currentTime
     * @param time
     */
    public void receiveCurrentTime(long roomid, long uuid, long currentTime, int time){
        try {
            TexasPokerVoicePrintService.ReceiveCurrentTimeRequest.Builder request = TexasPokerVoicePrintService.ReceiveCurrentTimeRequest.newBuilder();
            request.setUuid(uuid);
            request.setCurrentTime(currentTime);
            request.setRoomid(roomid);
            request.setTimeTaken(time);
            request.setEGameRoomType(TexasPokerCommon.GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);

            voiceStub.receiveCurrentTime(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 删除组
     * @param roomid
     */
    public void deleteGroup(long roomid){
        try {
            TexasPokerVoicePrintService.DeleteGroupRequest.Builder request = TexasPokerVoicePrintService.DeleteGroupRequest.newBuilder();
            request.setRoomId(roomid);

            voiceStub.deleteGroup(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
