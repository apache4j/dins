package com.texaspoker.floplotto;

import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.NormalLogService;
import com.texaspoker.moment.TexasPokerFlopLottoService;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

import java.util.List;

/**
 * Created by zhangmeng on 2019/5/6.
 */
public class FlopLottoManager {

    private static RpcController controller = new RpcController();
    private TexasPokerFlopLottoService.FlopLottoGameService.Stub flopLottoStub;

    private static FlopLottoManager instance;

    private FlopLottoManager(){
        String host = ConfigaturationManager.sFlopLottoServiceHost;
        int port = ConfigaturationManager.iFlopLottoServicePost;

        // RPC Client 初始化
        RpcClient client = new RpcClient();
        RpcChannel channel = new RpcChannel(client);

        try {
            channel.connect(host, port);
        } catch (Exception e) {
            NormalLogService.getInstance().error("FlopLottoManager.FlopLottoManager - channel.connect() caused exception, e.getMessage(): >" + e.getMessage() + "<");
            return;
        }

        flopLottoStub = TexasPokerFlopLottoService.FlopLottoGameService.newStub(channel);
        NormalLogService.getInstance().error("FlopLottoManager.FlopLottoManager - channel.connect() is UP, host: >" + host + "< port: >" + port + "<");
    }

    public static FlopLottoManager getInstance() {
        if (instance == null) {
            synchronized (FlopLottoManager.class) {
                instance = new FlopLottoManager();
            }
        }
        return instance;
    }

    /**
     * 创建野保险房间
     * @param lRoomId
     * @param lLeagueID
     * @param bCredit
     * @param bLeagueCurrencyPattern
     * @param lBlinds
     */
    public void createFlopLotto(long lRoomId, long lLeagueID, int bCredit, int bLeagueCurrencyPattern, long lBlinds, String strRoomName, int iType){
        try {
            TexasPokerFlopLottoService.CreateFlopLottoRequest.Builder request = TexasPokerFlopLottoService.CreateFlopLottoRequest.newBuilder();
            request.setLRoomId(lRoomId);
            request.setLLeagueID(lLeagueID);
            request.setBCredit(bCredit);
            request.setBLeagueCurrencyPattern(bLeagueCurrencyPattern);
            request.setLBlinds(lBlinds);
            request.setStrRoomName(strRoomName);
            request.setIType(iType);
            request.setIGameType(1);

            flopLottoStub.createFlopLotto(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 下发flop3张牌
     * @param lRoomId
     * @param cards
     * @param lTime
     * @param lGameHandID
     */
    public void pushFlopLottoCards(long lRoomId, List<Integer> cards, long lTime, long lGameHandID){
        try {
            TexasPokerFlopLottoService.PushFlopLottoCardsRequest.Builder request = TexasPokerFlopLottoService.PushFlopLottoCardsRequest.newBuilder();
            request.setLRoomId(lRoomId);
            request.addAllVCards(cards);
            request.setLTime(lTime);
            request.setLGameHandID(lGameHandID);

            flopLottoStub.pushFlopLottoCards(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 结束牌局
     * @param lRoomId
     */
    public void overFlopLotto(long lRoomId){
        try {
            TexasPokerFlopLottoService.OverFlopLottoRequest.Builder request = TexasPokerFlopLottoService.OverFlopLottoRequest.newBuilder();
            request.setLRoomId(lRoomId);

            flopLottoStub.overFlopLotto(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 刷新野保险服下注超时重新计时 游戏服在每次start状态时调用
     * @param lRoomId
     */
    public void refreshBetTimeOut(long lRoomId){
        try {
            TexasPokerFlopLottoService.RefreshBetTimeOutRequest.Builder request = TexasPokerFlopLottoService.RefreshBetTimeOutRequest.newBuilder();
            request.setLRoomId(lRoomId);

            flopLottoStub.refreshBetTimeOut(controller, request.build(), null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
