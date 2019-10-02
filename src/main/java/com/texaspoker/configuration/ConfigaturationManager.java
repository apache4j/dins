package com.texaspoker.configuration;

import java.io.File;

public class ConfigaturationManager {
    public static boolean bClubCredit  = false; // 屏蔽老版本授信
    
	public static String gHttpUrl = "";
	public static String gDataBaseConfigFile="";

	public static String sUserServiceHost;
	public static int iUserServicePost;
	
	public static String sClubServiceHost;
	public static int iClubServicePost;
	
	public static String sMsgServiceHost;
	public static int iMsgServicePost;
	
	public static String sApnServiceHost;
	public static int iApnServicePost;
	
	public static String sRoomHallServiceHost;
	public static int iRoomHallServicePost;
	
	public static String sNormalGameServiceHost;
	public static int iNormalGameServicePost;
	
	public static String sLBSServiceHost;
	public static int iLBSServicePost;
	
	public static String sSNGGameServiceHost;
	public static int iSNGGameServicePost;
	
	public static String sBillGameServiceHost;
	public static int iBillGameServicePost;
	
	public static String sMessageJedisAddress;
	public static String sMessageJedisAccount;
	
	public static String sStaticJedisAddress;
	public static String sStaticJedisAccount;
	
	public static String sCreditServiceHost;
    public static int iCreditServicePost;

	public static String sFlopLottoServiceHost;
	public static int iFlopLottoServicePost;

	public static String sVoiceServiceHost;
	public static int iVoiceServicePost;

	//抽水间隔,时间单位为小时
	public static int iDrawTime;

	//海外版底池抽水,玩家为2人时 抽水上限比例
	public static int iProportion1 = 50;

	//海外版底池抽水,玩家为3-4人时 抽水上限比例
	public static int iProportion2 = 75;

	//海外版底池抽水,玩家为>=5人时 抽水上限比例
	public static int iProportion3 = 100;

	//授信超时
	public static int iCreditAppyTimeOut;

	//随机开始值
	public static int randomStart = 20;
	//随机结束值
	public static int randomEnd = 50;

	//玩家本桌验证次数限制
	public static int verifyCount = 50;
	//玩家本天验证次数限制
	public static int verifyTotalCount = 100;

	//声纹验证弹框倒计时 单位:秒
	public static int verifyTime = 60;
    
	private static ConfigaturationManager instance;
	
	//私有的构造函数
	private ConfigaturationManager (){
		
		File directory = new File("");//设定为当前文件夹
	    String dirName = directory.getAbsolutePath();
	    
	    //获取配置文件
	    String fileName = dirName + "/../conf/global_conf.properties";
	    Configuration stConfigaturation =  new Configuration(fileName);
	    System.out.println("ConfigaturationManagerConfigaturationManager : " + fileName);
	    System.out.println("ConfigaturationManagerConfigaturationManager : " + stConfigaturation);
	    //获取配置相关的值
	    gHttpUrl = stConfigaturation.getValue("http_url");
	    gDataBaseConfigFile = stConfigaturation.getValue("data_base_config_file");
	    
	    sUserServiceHost = stConfigaturation.getValue("user_service_host");
		iUserServicePost = Integer.parseInt(stConfigaturation.getValue("user_service_post"));
		
		sClubServiceHost = stConfigaturation.getValue("club_service_host");
		iClubServicePost = Integer.parseInt(stConfigaturation.getValue("club_service_post"));
		
		sMsgServiceHost = stConfigaturation.getValue("msg_service_host");
		iMsgServicePost = Integer.parseInt(stConfigaturation.getValue("msg_service_post"));
		
		sApnServiceHost = stConfigaturation.getValue("apn_service_host");
		iApnServicePost = Integer.parseInt(stConfigaturation.getValue("apn_service_post"));
		
		sRoomHallServiceHost = stConfigaturation.getValue("roomhall_service_host");
		iRoomHallServicePost = Integer.parseInt(stConfigaturation.getValue("roomhall_service_post"));
		
		sNormalGameServiceHost = stConfigaturation.getValue("normalgame_service_host");
		iNormalGameServicePost = Integer.parseInt(stConfigaturation.getValue("normalgame_service_post"));
		
		sSNGGameServiceHost = stConfigaturation.getValue("snggame_service_host");
		iSNGGameServicePost = Integer.parseInt(stConfigaturation.getValue("snggame_service_post"));
		
		sLBSServiceHost = stConfigaturation.getValue("lbs_service_host");
		iLBSServicePost = Integer.parseInt(stConfigaturation.getValue("lbs_service_post"));
		
		sBillGameServiceHost = stConfigaturation.getValue("bill_service_host");
		iBillGameServicePost = Integer.parseInt(stConfigaturation.getValue("bill_service_post"));
		
		sMessageJedisAddress = stConfigaturation.getValue("message_redis_address");
		sMessageJedisAccount = stConfigaturation.getValue("message_redis_account");
		
		sStaticJedisAddress = stConfigaturation.getValue("static_redis_address");
		sStaticJedisAccount = stConfigaturation.getValue("static_redis_account");
		
	    sCreditServiceHost = stConfigaturation.getValue("credit_service_host");
	    iCreditServicePost = Integer.parseInt(stConfigaturation.getValue("credit_service_post"));

		sFlopLottoServiceHost = stConfigaturation.getValue("floplotto_service_host");
		iFlopLottoServicePost = Integer.parseInt(stConfigaturation.getValue("floplotto_service_post"));

		iCreditAppyTimeOut = Integer.parseInt(stConfigaturation.getValue("credit_apply_timeout"));

		iDrawTime = Integer.parseInt(stConfigaturation.getValue("draw_time"));

		sVoiceServiceHost = stConfigaturation.getValue("voice_service_host");
		iVoiceServicePost = Integer.parseInt(stConfigaturation.getValue("voice_service_post"));
	}
	
	//单例模式
	public static synchronized ConfigaturationManager getInstance() {  
		if (instance == null) {  
			instance = new ConfigaturationManager();  
		}  
		return instance;  
	} 

}
