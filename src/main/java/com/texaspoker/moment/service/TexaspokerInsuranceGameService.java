package com.texaspoker.moment.service;

import com.texaspoker.apns.APNManager;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPokerGameService;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.moment.room.QuickGameRoomManager;
import com.texaspoker.rpc.server.RpcServer;
import com.texaspoker.user.UserManager;

public class TexaspokerInsuranceGameService {

	  private static RpcServer server = new RpcServer();

	  public static void main(String[] args) throws Exception {
	     String host = "0.0.0.0";
	     int port = 9162;
	     
	     ConfigaturationManager.getInstance();
	     APNManager.getInstance();
	     UserManager.getInstance();

	     server.config(host, port);

	     server.registerBlockingService(TexasPokerGameService.GameService.newReflectiveBlockingService(new GameServiceImpl()));

	     server.run();
	     System.out.println("server start... ");
	     
	     QuickGameRoomManager.getInstance();
	     GameRoomManager.getInstance();
	     
	  }
	}
