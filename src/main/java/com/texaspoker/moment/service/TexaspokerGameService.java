package com.texaspoker.moment.service;

import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.moment.TexasPokerGameService;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.moment.room.QuickGameRoomManager;
import com.texaspoker.rpc.server.RpcServer;
import com.texaspoker.user.UserManager;

public class TexaspokerGameService  {

	  private static RpcServer server = new RpcServer();

	  public static void main(String[] args) throws Exception {
	     String host = "0.0.0.0";
	     int port = 9160;
	     
	     ConfigaturationManager.getInstance();
	     UserManager.getInstance();
	     QuickGameRoomManager.getInstance();
	     GameRoomManager.getInstance();
	     
	     server.config(host, port);

	     server.registerService(TexasPokerGameService.GameService.newReflectiveService(new GameServiceImpl()));

	     server.registerBlockingService(TexasPokerGameService.GameService.newReflectiveBlockingService(new GameServiceImpl()));

	     server.run();
	     System.out.println("server start... ");
	  }
	}
