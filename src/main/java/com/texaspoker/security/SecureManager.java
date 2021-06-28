package com.texaspoker.security;

import com.google.protobuf.ByteString;
import com.texaspoker.moment.TexasPokerSecureProtocolService;
import com.texaspoker.moment.room.HandInfo;
import com.texaspoker.rpc.client.RpcChannel;
import com.texaspoker.rpc.client.RpcClient;
import com.texaspoker.rpc.client.RpcController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：piness
 * @date ：Created in 2021-06-15 19:20
 * @description：
 * @modified By：
 * @version: $
 */
public class SecureManager {
    private static SecureManager instance;
    //RPC相关变量
    private RpcClient client;
    private RpcChannel channel;
    private String host = "";
    private int port = 0;
    private static RpcController controller = new RpcController();
    TexasPokerSecureProtocolService.SecureProtocolService.BlockingInterface blockingSecureService ;

    //私有的构造函数
    private SecureManager(){
        host = "172.16.32.4";
        port = 9238;
        System.out.println("SecureManager hosthost : " + host);
        System.out.println("SecureManager portport : " + port);

        client = new RpcClient();
        channel = new RpcChannel(client);
        try {
            channel.connect(host, port);
        } catch (Exception e) {
//	    	NormalLogService.getInstance().error("client connect error:" + e.getMessage());
            return;
        }

        blockingSecureService = TexasPokerSecureProtocolService.SecureProtocolService.newBlockingStub(channel);
        System.out.println("client connect " + host + ":" + port + " ok");
//	    NormalLogService.getInstance().error("client connect " + host + ":" + port + " ok");
    }

    //初始化
    public static SecureManager getInstance() {
        if (instance == null) {
            synchronized (SecureManager.class) {
                instance = new SecureManager();
            }
        }
        return instance;
    }

    //加密数据
    public TexasPokerSecureProtocolService.EncryptDataResponse encryptData(long roomID, long uuid, List data) {
        TexasPokerSecureProtocolService.EncryptDataRequest.Builder stEncryptDataRequestBuilder = TexasPokerSecureProtocolService.EncryptDataRequest.newBuilder();
        TexasPokerSecureProtocolService.PlainDataMessage plainDataMessage = TexasPokerSecureProtocolService.PlainDataMessage.newBuilder()
                .setLRoomid(roomID)
                .setLuuid(uuid)
                .addAllVData(data)
                .build();
        stEncryptDataRequestBuilder.setPlanData(plainDataMessage);
        try {
            TexasPokerSecureProtocolService.EncryptDataResponse response = blockingSecureService.encryptData(controller, stEncryptDataRequestBuilder.build());
            if (response != null && response.getIErrCode() == 0) {
                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //批量加密数据
    public TexasPokerSecureProtocolService.EncryptDataListResponse encryptDataList(List<TexasPokerSecureProtocolService.PlainDataMessage> plainDataMessageList) {
        TexasPokerSecureProtocolService.EncryptDataListRequest.Builder stEncryptDataListRequestBuilder = TexasPokerSecureProtocolService.EncryptDataListRequest.newBuilder();

        stEncryptDataListRequestBuilder.addAllVPlainDatas(plainDataMessageList);
        try {
            TexasPokerSecureProtocolService.EncryptDataListResponse response = blockingSecureService.encryptDataList(controller, stEncryptDataListRequestBuilder.build());
            if (response != null && response.getIErrCode() == 0) {
                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //解密数据
    public TexasPokerSecureProtocolService.DecryptDataResponse decryptData(TexasPokerSecureProtocolService.CipherDataMessage cipherData) {
        TexasPokerSecureProtocolService.DecryptDataRequest.Builder stDecryptDataRequestBuilder = TexasPokerSecureProtocolService.DecryptDataRequest.newBuilder();

        stDecryptDataRequestBuilder.setCipherData(cipherData);
        try {
            TexasPokerSecureProtocolService.DecryptDataResponse response = blockingSecureService.decryptData(controller, stDecryptDataRequestBuilder.build());
            if (response != null && response.getIErrCode() == 0) {
                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //批量解密数据
    public TexasPokerSecureProtocolService.DecryptDataListResponse decryptDataList(List<TexasPokerSecureProtocolService.CipherDataMessage> cipherDataMessageList) {
        TexasPokerSecureProtocolService.DecryptDataListRequest.Builder stDecryptDataListRequestBuilder = TexasPokerSecureProtocolService.DecryptDataListRequest.newBuilder();

        stDecryptDataListRequestBuilder.addAllVCipherData(cipherDataMessageList);
        try {
            TexasPokerSecureProtocolService.DecryptDataListResponse response = blockingSecureService.decryptDataList(controller, stDecryptDataListRequestBuilder.build());
            if (response != null && response.getIErrCode() == 0) {
                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
