package com.texaspoker.apns;

import java.io.File;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import com.alibaba.dubbo.rpc.protocol.protobuf.ProtobufController;
import com.texaspoker.moment.TexasPokerAPNDubbo.BuyinPushRequest;
import com.texaspoker.moment.TexasPokerAPNDubbo.CreateGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNDubbo.EmptyGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNDubbo.EnterGameRoomPushRequest;
import com.texaspoker.moment.TexasPokerAPNDubbo.APNService;
import com.texaspoker.moment.TexasPokerAPNDubbo.GameOverPushRequest;
import com.texaspoker.moment.TexasPokerAPNDubbo.StartGameRoomPushRequest;

public class APNManager  {
	private static APNManager instance;

	//RPC相关变量
	private APNService.BlockingInterface stAPNService;
	
	private ProtobufController pbController;
    
	//私有的构造函数
	private APNManager (){
		File directory = new File("");//设定为当前文件夹
	    String dirName = directory.getAbsolutePath();
	    
	    //获取配置文件
	    String fileName = "/" + dirName + "/src/res/consumer.xml";
	    
	    System.out.println("APNManager fileName : " + fileName);
	    
	    FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(new String[] {fileName});
	    context.start();
	    
	    stAPNService = (APNService.BlockingInterface)context.getBean("apnService");
	        if(stAPNService!=null){
	        	System.out.println("stPokerHistoryService not null");
	        }else{
	        	System.out.println("stPokerHistoryService null");
	        }
	        
	        pbController = (ProtobufController)context.getBean("pbController");
	      
	      if(pbController!=null){
	      	System.out.println("pbController not null");
	      }else{
	      	System.out.println("pbController null");
	      }
	}
	
	//单例模式
	public static  APNManager getInstance() {          
		if (instance == null) {  
			synchronized(APNManager.class){
				instance = new APNManager();  
			}
		}  
		return instance;  
	} 

	public void enterGameRoomPush(long uuid,String strToken,String strNick,String strGameRoomName,int badge){
		try{
			
			EnterGameRoomPushRequest.Builder stEnterGameRoomPushRequestBuilder = EnterGameRoomPushRequest.newBuilder();
			stEnterGameRoomPushRequestBuilder.setUuid(uuid);
			stEnterGameRoomPushRequestBuilder.setSToken(strToken);
			stEnterGameRoomPushRequestBuilder.setStrNick(strNick);
			stEnterGameRoomPushRequestBuilder.setIBadge(""+badge);
			stEnterGameRoomPushRequestBuilder.setStrRoomName(strGameRoomName);
			stEnterGameRoomPushRequestBuilder.setIGameRoomUsers(0);

			stAPNService.enterGameRoomPush(pbController, stEnterGameRoomPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void buyinPush(long uuid,String strToken,String strNick,String strGameRoomName,int badge){
		try{
			
			BuyinPushRequest.Builder stBuyinPushRequestBuilder = BuyinPushRequest.newBuilder();
			stBuyinPushRequestBuilder.setUuid(uuid);
			stBuyinPushRequestBuilder.setSToken(strToken);
			stBuyinPushRequestBuilder.setStrNick(strNick);
			stBuyinPushRequestBuilder.setIBadge(""+badge);
			stBuyinPushRequestBuilder.setStrRoomName(strGameRoomName);

			stAPNService.buyinPush(pbController, stBuyinPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void StartGameRoomPush(long uuid,String strToken,String strGameRoomName,int badge){
		try{
			
			StartGameRoomPushRequest.Builder stStartGameRoomPushRequestBuilder = StartGameRoomPushRequest.newBuilder();
			stStartGameRoomPushRequestBuilder.setUuid(uuid);
			stStartGameRoomPushRequestBuilder.setSToken(strToken);
			stStartGameRoomPushRequestBuilder.setSGameRoomName(strGameRoomName);

			stAPNService.startGameRoomPush(pbController, stStartGameRoomPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void gameOverPush(long uuid,String strToken,String strGameRoomName,int badge){
		try{
			
			System.out.println("gameOverPushgameOverPushgameOverPush!!!!!!!!!!~~~~~~~~~~~~~~~~~~~~");
			GameOverPushRequest.Builder stGameOverPushRequestBuilder = GameOverPushRequest.newBuilder();
			stGameOverPushRequestBuilder.setUuid(uuid);
			stGameOverPushRequestBuilder.setSToken(strToken);
			stGameOverPushRequestBuilder.setIBadge(""+badge);
			stGameOverPushRequestBuilder.setStrRoomName(strGameRoomName);

			stAPNService.gameOverPush(pbController, stGameOverPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void createGameRoomPush(long uuid,long lClubID,String strGameRoomName,String sHour,long lSmallBlinds,int iAnte,int badge){
		try{
			
			CreateGameRoomPushRequest.Builder stCreateGameRoomPushRequestBuilder = CreateGameRoomPushRequest.newBuilder();
			stCreateGameRoomPushRequestBuilder.setUuid(uuid);
			stCreateGameRoomPushRequestBuilder.setLClubID(lClubID);
			stCreateGameRoomPushRequestBuilder.setSHour(sHour);
			stCreateGameRoomPushRequestBuilder.setLSmallBlind(lSmallBlinds);
			stCreateGameRoomPushRequestBuilder.setIAnte(iAnte);
			stCreateGameRoomPushRequestBuilder.setSGameRoomName(strGameRoomName);
			
			stAPNService.createGameRoomPush(pbController, stCreateGameRoomPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void emptyGameRoomPush(long uuid,long lClubID,String strGameRoomName,String sHour,long lSmallBlinds,int iAnte,int badge){
		try{
			
			EmptyGameRoomPushRequest.Builder stEmptyGameRoomPushRequestBuilder = EmptyGameRoomPushRequest.newBuilder();
			stEmptyGameRoomPushRequestBuilder.setUuid(uuid);
			stEmptyGameRoomPushRequestBuilder.setLClubID(lClubID);
			stEmptyGameRoomPushRequestBuilder.setSHour(sHour);
			stEmptyGameRoomPushRequestBuilder.setLSmallBlind(lSmallBlinds);
			stEmptyGameRoomPushRequestBuilder.setIAnte(iAnte);

			stAPNService.emptyGameRoomPush(pbController, stEmptyGameRoomPushRequestBuilder.build());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
}