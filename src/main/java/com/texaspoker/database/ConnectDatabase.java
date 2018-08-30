package com.texaspoker.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import com.texaspoker.apns.APNManager;
import com.texaspoker.bill.BillManager;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.TimeoutHashMap;
import com.texaspoker.configuration.ConfigaturationManager;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.moment.TexasPoker.BUYIN_CONTROL_STATUS;
import com.texaspoker.moment.TexasPoker.ChatRoomInfo;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_SENIOR_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.QUICK_GAME_ROOM_STATUS;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPoker.USER_GAME_ROLE;
import com.texaspoker.moment.TexasPoker.USER_GAME_STATE;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.ID_TYPE;
import com.texaspoker.moment.TexasPokerCommon.SNG_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.UserBaseInfoNet;
import com.texaspoker.moment.TexasPokerCommon.GAME_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.VIP_TYPE;
import com.texaspoker.moment.room.ChatRoomManager;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.moment.room.QuickGameRoomManager;
import com.texaspoker.user.UserManager;

public class ConnectDatabase {
	
    ResultSet rs=null;
    
	private static ConnectDatabase instance; 
	public DataSource mDatabasePool;
	
	//私有的构造函数
	private ConnectDatabase (){
	}
	
	//单例模式
	public static synchronized ConnectDatabase getInstance() {  
		if (instance == null) {  
			instance = new ConnectDatabase();  
			Properties p = new Properties();
			try {
				File directory = new File("");//设定为当前文件夹
			    String dirName = directory.getAbsolutePath();
			    String fileName = dirName + ConfigaturationManager.gDataBaseConfigFile;
				FileInputStream stFileInputStream = new FileInputStream(fileName);
				
				p.load(stFileInputStream);
				try {
					instance.mDatabasePool = BasicDataSourceFactory.createDataSource(p);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e){
				
			}
		}  
		return instance;  
	} 

    //读取房间信息
    public void loadAllGameRooms(TimeoutHashMap<Long,GameRoomDetailInfo> mapGameRoomInfoMap){
   	 Connection stConnection = null;
			Statement statement;
			try {
				stConnection = mDatabasePool.getConnection();
				statement = stConnection.createStatement();
				
				String loadGameRoomSql="select * from t_game_room where gameroomtype='GAME_ROOM_NORMAL_INSURANCE'";
				ResultSet gameRoomRS=null;
				
				gameRoomRS=statement.executeQuery(loadGameRoomSql);
//				System.out.println("database : " + loadGameRoomSql);
	             while(gameRoomRS.next()){
	     			 //将新的牌局放到GameRoomManager中
	     			 GameRoomDetailInfo stGameRoomDetailInfo = new GameRoomDetailInfo();
	     			
	            	 GameRoomInfo.Builder stGameRoomInfoBuild =  GameRoomInfo.newBuilder();
	            	 
	            	 //首先加载chatroombaseinfo的基本信息
	            	 GameRoomBaseInfo.Builder stGameRoomBaseInfo = GameRoomBaseInfo.newBuilder();
	            	 stGameRoomBaseInfo.setLGameRoomId(gameRoomRS.getLong("roomid"));
	            	 stGameRoomBaseInfo.setStrRoomName(gameRoomRS.getString("roomname"));
	            	 stGameRoomBaseInfo.setLSmallBlinds(gameRoomRS.getLong("smallblinds"));
	            	 stGameRoomBaseInfo.setLBigBlinds(gameRoomRS.getLong("bigblinds"));
	            	 stGameRoomBaseInfo.setLBuyIn(gameRoomRS.getLong("buyin"));
	            	 stGameRoomBaseInfo.setLCreateTime(gameRoomRS.getLong("starttime"));
	            	 stGameRoomBaseInfo.setLDuration(gameRoomRS.getLong("duration"));
	            	 stGameRoomBaseInfo.setECreateRoomType(CREATE_ROOM_TYPE.valueOf(gameRoomRS.getString("createroomtype")));
	            	 stGameRoomBaseInfo.setBBuyinControl(gameRoomRS.getInt("buyincontrol"));
	            	 stGameRoomBaseInfo.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
	
	            	 UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(gameRoomRS.getLong("createuser"));
	            	 if(stUserBaseInfoNetBuilder==null){
	            		 continue;
	            	 }
	            	 
	            	 stGameRoomBaseInfo.setStCreateUserBaseInfo(stUserBaseInfoNetBuilder);
	            	 stGameRoomBaseInfo.setBPrivateRoom(gameRoomRS.getInt("privateroom"));
	            	 stGameRoomBaseInfo.setLChatRoomID(gameRoomRS.getInt("chatroomid"));
	            	 stGameRoomBaseInfo.setIAnte(gameRoomRS.getInt("ante"));
	            	 stGameRoomBaseInfo.setBInsurance(gameRoomRS.getInt("insurance"));
	            	 stGameRoomBaseInfo.setBIPLimit(gameRoomRS.getInt("iplimit"));
	            	 stGameRoomBaseInfo.setBGPSLimit(gameRoomRS.getInt("gpslimit"));
	            	 stGameRoomBaseInfo.setIMaxBuyinRatio(gameRoomRS.getInt("maxbuyinratio"));
	            	 stGameRoomBaseInfo.setIMinBuyinRatio(gameRoomRS.getInt("minbuyinratio"));
	            	 stGameRoomBaseInfo.setEGameRoomSeniorType(GAME_ROOM_SENIOR_TYPE.valueOf(gameRoomRS.getInt("gameroomseniortype")));
	            	 stGameRoomBaseInfo.setBBuyIn(1);
	            	 stGameRoomBaseInfo.setLLeagueID(gameRoomRS.getLong("leagueid"));
	            	 stGameRoomBaseInfo.setSLeagueName(gameRoomRS.getString("leaguebname"));
	            	 
	            	 stGameRoomInfoBuild.setStGameRoomBaseInfo(stGameRoomBaseInfo.build());
	            	 stGameRoomInfoBuild.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
	            	 stGameRoomInfoBuild.setEGameState(GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Over);	
	            	 
	            	 //设置聊天室和牌局之间的关系
	            	 ChatRoomInfo stChatRoomInfo  = ChatRoomManager.getInstance().getChatRoomInfo(stGameRoomBaseInfo.getLChatRoomID());
	            	 if(stChatRoomInfo!=null){
	            		 ChatRoomManager.getInstance().addGameRooms(stGameRoomBaseInfo.getLChatRoomID(), stGameRoomBaseInfo.getLGameRoomId());
	            	 }else{
	            		 ClubManager.getInstance().addGameRooms(stGameRoomBaseInfo.getLChatRoomID(), stGameRoomBaseInfo.getLGameRoomId());
	            	 }
	            	
	     			//设置房间的其他玩家，因为现在还没有玩家同意，所以设置9个空用户
	     			for(int i = 0;i< 9;i++){
	     				UserGameInfoNet.Builder otherUserGameInfoBuilder = UserGameInfoNet.newBuilder();
	     				
	     				//设置userbaseinfo
	     				UserBaseInfoNet.Builder userBaseInfoBuilder = UserBaseInfoNet.newBuilder();
	     				userBaseInfoBuilder.setUuid(0);
	     				userBaseInfoBuilder.setEType(ID_TYPE.ID_TYPE_NA);
	     				userBaseInfoBuilder.setStrID("");
	     				userBaseInfoBuilder.setStrCover("");
	     				userBaseInfoBuilder.setStrNick("");
	     				userBaseInfoBuilder.setStrPhoneNumber("");
	     				userBaseInfoBuilder.setStrPhoneNumber("");
	     				userBaseInfoBuilder.setIGender(0);
	     				userBaseInfoBuilder.setEVipType(VIP_TYPE.BLUE_CARD);
	     				userBaseInfoBuilder.setLLoginTime(0);
	     				otherUserGameInfoBuilder.setStUserInfo(userBaseInfoBuilder);
	     				
	     				//设置其他属性
	     				otherUserGameInfoBuilder.setLRoomId(0);
	     				otherUserGameInfoBuilder.setLRemainStacks(0);
	     				otherUserGameInfoBuilder.setLBetStacks(0);
	     				otherUserGameInfoBuilder.setBBuyin(1);
	     				otherUserGameInfoBuilder.setBGameDealer(0);
	     				otherUserGameInfoBuilder.setEGameRole(USER_GAME_ROLE.USER_GAME_ROLE_NORMAL);
	     				otherUserGameInfoBuilder.setBGameDealer(0);
	     				otherUserGameInfoBuilder.setEGameState(USER_GAME_STATE.USER_GAME_STATE_STANDBY);
	     				
	     				stGameRoomInfoBuild.addVUserGameInfos(otherUserGameInfoBuilder);
	     			}
	     			    			
	     			stGameRoomDetailInfo.setStGameRoomInfo(stGameRoomInfoBuild.build());
	            	//加chatroomid 加载到内存中
	            	mapGameRoomInfoMap.setCurrent(stGameRoomInfoBuild.getStGameRoomBaseInfo().getLGameRoomId(),stGameRoomDetailInfo);
	            	 
	            	//设置用户和其所创建的牌局的关系
	            	UserManager.getInstance().addUserCreateGameRooms(stGameRoomInfoBuild.getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid(), stGameRoomInfoBuild.getStGameRoomBaseInfo().getLGameRoomId());
	            	//加载gameroom邀请人员信息
	            	 
//	            	BillManager.getInstance().finishGameRoomAndReturn(stGameRoomBaseInfo.getLGameRoomId());
	            		            	
					TableLogService.getInstance().error(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID()
								+"," +stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId()
								+"," + stGameRoomDetailInfo.getStGameRoomInfo().getLGameHandID()
								+"," + 0
								+"," + "closetable" );
						
					//房间到时，删除房间
					ConnectDatabase.getInstance().removeGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					ConnectDatabase.getInstance().removeGameRoomMember(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					UserManager.getInstance().removeUserCreateGameRooms(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()
									,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
						

					ChatRoomManager.getInstance().removeChatRoomGameRoom(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID(),stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					ClubManager.getInstance().removeChatRoomGameRoom(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID(),stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
						
					BillManager.getInstance().setGameOverTag(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId(),stGameRoomDetailInfo.getMaxPot(),stGameRoomDetailInfo.getHandID());	
					mapGameRoomInfoMap.removeKey(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					
					List<Long> vPushUsers = BillManager.getInstance().getBillInfoUsers(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					for(long uuid:vPushUsers){
						pushAppPush(uuid,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStrRoomName());
					}
					
					//重新设置买入控制相关
					//把所有没有审批的买入请求，全部设置为拒绝
					ConnectDatabase.getInstance().gameoverBuyInControls(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					
					//把实时买入记录拷贝到历史买入记录数据库中
					ConnectDatabase.getInstance().copyToHistoryBuyInControls(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
					
					//把买入记录从实时买入记录中删除
					ConnectDatabase.getInstance().removeBuyInControls(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
	             }
	             
	             stConnection.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
             	try {
                	 stConnection.close();
           			} catch (SQLException e1) {
           				// TODO Auto-generated catch block
           				e1.printStackTrace();
           			}
			}
    }
    
    
    //游戏房间信息落地
    public boolean removeGameRoomMember(long roomid){
   	 Connection stConnection = null;
     	try{
       	stConnection = mDatabasePool.getConnection();
          	String insertChatRoomMemberSl="delete from t_game_room_members where roomid=?";

//          	System.out.println("database : " + insertChatRoomMemberSl);
                  //保存终端设备信息
         	PreparedStatement ps=stConnection.prepareStatement(insertChatRoomMemberSl);
           ps.setLong(1, roomid);

            int result=ps.executeUpdate();
            if(result<=0){
            	stConnection.close();
            	return false;
            }
            	
            stConnection.close();
            return true;

        }catch(Exception e){
            e.printStackTrace();
        	try {
          	 stConnection.close();
     			} catch (SQLException e1) {
     				// TODO Auto-generated catch block
     				e1.printStackTrace();
     			}
        }
     	return false;
     }
    
    public boolean removeGameRoomInfo(long roomid){
   	 Connection stConnection = null;
      	try{
       	stConnection = mDatabasePool.getConnection();
          	String insertChatRoomMemberSl="delete from t_game_room where roomid=?";

//          	System.out.println("database : " + insertChatRoomMemberSl);
                  //保存终端设备信息
         	PreparedStatement ps=stConnection.prepareStatement(insertChatRoomMemberSl);
           ps.setLong(1, roomid);
           
             int result=ps.executeUpdate();
             if(result<=0){
             	stConnection.close();
             	return false;
             }
             	
             stConnection.close();
             return true;

         }catch(Exception e){
             e.printStackTrace();
           	try {
               	 stConnection.close();
          			} catch (SQLException e1) {
          				// TODO Auto-generated catch block
          				e1.printStackTrace();
          			}
         }
      	return false;
      }
    
 	void pushAppPush(long touuid,String sRoomName){
		UserBaseInfoNet.Builder stFromUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(touuid);		
		//获取push相关信息
        if(stFromUserBaseInfoNetBuilder!=null){
        	APNManager.getInstance().gameOverPush(touuid, sRoomName, 1);
        }
	}
    
    public void loadAllQuickGameRooms(ConcurrentHashMap<Long,QuickGameRoomInfo.Builder> vQuickGameRooms,Map<String,Long> vCodeQuickGameRooms,ConcurrentHashMap<Long,HashSet<Long>> vUserQuickGameRooms){
   	 Connection stConnection = null;
   	 Statement statement;
   	 try {
   		 stConnection = mDatabasePool.getConnection();
   		 statement = stConnection.createStatement();
   		 String loadQuickGameRoomSql="select * from t_quick_game_info where roomstate!=3 and gameroomtype="+GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE_VALUE;
   		 ResultSet gameRoomRS=null;
   		 gameRoomRS=statement.executeQuery(loadQuickGameRoomSql);
   		 
   		 while(gameRoomRS.next()){
   			 //将新的牌局放到GameRoomManager中
   			 QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomInfo.newBuilder();
	     			
   			 stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.valueOf(gameRoomRS.getInt("roomstate")));
   			 stQuickGameRoomInfoBuilder.setSCryptCode(gameRoomRS.getString("code"));
   			 long lChatRoomID = gameRoomRS.getLong("roomid");

   			 if(ChatRoomManager.getInstance().getChatRoomInfo(lChatRoomID)!=null){
       			 stQuickGameRoomInfoBuilder.setStChatRoomInfo(ChatRoomManager.getInstance().getChatRoomInfo(lChatRoomID));
       			 
       			 if(stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_START){
       				 //开始但是没有结束的
       				 long lGameRoomID = gameRoomRS.getLong("gameroomid");
       				 GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
       				 if(stGameRoomDetailInfo != null){
       					 stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
       				 }
                        	 for(int i = 0 ; i < stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfosCount() ; i++){
                                	 UserBaseInfoNet stUserBaseInfoNet = stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfos(i);
                              		 HashSet<Long> vQuickGameRoomIDs = vUserQuickGameRooms.get(stUserBaseInfoNet.getUuid());
                                        if(vQuickGameRoomIDs == null){
                                                vQuickGameRoomIDs = new HashSet<Long>();
                                        }
                                        vQuickGameRoomIDs.add(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
                                        vUserQuickGameRooms.put(stUserBaseInfoNet.getUuid(),vQuickGameRoomIDs);
                         	}
						
				stQuickGameRoomInfoBuilder.setEQuickGameRoomStatus(QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH);
				ConnectDatabase.getInstance().insertQuickRoomInfo(stQuickGameRoomInfoBuilder.build());

				QuickGameRoomManager.getInstance().removeUserCreateQuickGameRoom(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLCreateUser(),lChatRoomID);

			}else if(stQuickGameRoomInfoBuilder.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_NOTSTART){
       				 GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = GameRoomBaseInfo.newBuilder();
       				 stGameRoomBaseInfoBuilder.setStrRoomName(gameRoomRS.getString("roomname"));
       				 stGameRoomBaseInfoBuilder.setLSmallBlinds(gameRoomRS.getLong("smallbliand"));
       				 stGameRoomBaseInfoBuilder.setLBigBlinds(gameRoomRS.getLong("bigbliand"));
       				 stGameRoomBaseInfoBuilder.setLBuyIn(gameRoomRS.getLong("lbuyin"));
       				 stGameRoomBaseInfoBuilder.setBBuyIn(gameRoomRS.getInt("bbuyin"));
       				 stGameRoomBaseInfoBuilder.setLDuration(gameRoomRS.getLong("duration"));
       				 stGameRoomBaseInfoBuilder.setLGameRoomId(0);
       				 stGameRoomBaseInfoBuilder.setBPrivateRoom(0);
       				 stGameRoomBaseInfoBuilder.setEGameRoomType(GAME_ROOM_TYPE.GAME_ROOM_NORMAL_INSURANCE);
       				 
       				 stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(gameRoomRS.getInt("maxbuyinratio"));
       				 stGameRoomBaseInfoBuilder.setIMinBuyinRatio(gameRoomRS.getInt("minbuyinratio"));
       				 stGameRoomBaseInfoBuilder.setBInsurance(gameRoomRS.getInt("insurance"));
       				 stGameRoomBaseInfoBuilder.setEGameRoomSeniorType(GAME_ROOM_SENIOR_TYPE.valueOf(gameRoomRS.getInt("gameroomseniortype")));
		            	 
       				 stGameRoomBaseInfoBuilder.setLCreateTime(gameRoomRS.getLong("createtime"));
       				 UserBaseInfoNet.Builder stUserBaseInfoNetBuilder = UserManager.getInstance().getUserBaseInfo(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLCreateUser());
       				 stGameRoomBaseInfoBuilder.setStCreateUserBaseInfo(stUserBaseInfoNetBuilder);
       				 
       				 stQuickGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);

                       		  for(int i = 0 ; i < stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfosCount() ; i++){
                                	 UserBaseInfoNet stUserBaseInfoNet = stQuickGameRoomInfoBuilder.getStChatRoomInfo().getVUserBaseInfos(i);
                              		   HashSet<Long> vQuickGameRoomIDs = vUserQuickGameRooms.get(stUserBaseInfoNet.getUuid());
                                        if(vQuickGameRoomIDs == null){
                                                vQuickGameRoomIDs = new HashSet<Long>();
                                        }
                                        vQuickGameRoomIDs.add(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
                                        vUserQuickGameRooms.put(stUserBaseInfoNet.getUuid(),vQuickGameRoomIDs);
                        	 }
                         	vQuickGameRooms.put(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId(), stQuickGameRoomInfoBuilder);
                        	 vCodeQuickGameRooms.put(stQuickGameRoomInfoBuilder.getSCryptCode(), stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
       			 }
       			 
   			 }
   		 }
   		 
   		 stConnection.close();
   	 } catch (Exception e) {
   		 // TODO Auto-generated catch block
   		 e.printStackTrace();
				
   		 try {
   			 stConnection.close();
   		 } catch (SQLException e1) {
   			 // TODO Auto-generated catch block
   			 e1.printStackTrace();
           }
		}
    }     

    //游戏房间邀请信息落地
    public boolean deleteInGameUser(long lRoomid,long uuid){
   	 Connection stConnection = null;
     	try{            		
       	stConnection = mDatabasePool.getConnection();
          	String insertChatRoomMemberSl="delete from t_game_room_members where roomid=? and uuid=?";

//          	System.out.println("database : " + insertChatRoomMemberSl);
                  //保存终端设备信息
         	PreparedStatement ps=stConnection.prepareStatement(insertChatRoomMemberSl);
           ps.setLong(1, lRoomid);
           ps.setLong(2, uuid);

           ps.executeUpdate();
          
           stConnection.close();
           return true;
     	}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
         	try {
            	 stConnection.close();
       			} catch (SQLException e1) {
       				// TODO Auto-generated catch block
       				e1.printStackTrace();
       			}
			return false;
		}
     }
    
    //添加聊天室用户
    public boolean insertChatRoomMembers(long roomid , long uuid){
   	 Connection stConnection = null;
     	try{
    		stConnection = mDatabasePool.getConnection();
    		String insertChatRoomMemberSl="insert into t_chat_room_members set roomid=?"
    		+",uuid=?"
    		+ " on duplicate key update roomid=?"
    		+",uuid=?";
    		
//    		System.out.println("database : " + insertChatRoomMemberSl);
            //保存终端设备信息
    		PreparedStatement ps=stConnection.prepareStatement(insertChatRoomMemberSl);
           ps.setLong(1, roomid);
           ps.setLong(2, uuid);
           ps.setLong(3, roomid);
           ps.setLong(4, uuid);
           int result=ps.executeUpdate();
           
           stConnection.close();
           if(result > 0){
           	return true;
           }else{
           	return false;
           }
        }catch(Exception e){
            e.printStackTrace();
             	try {
                 	 stConnection.close();
            			} catch (SQLException e1) {
            				// TODO Auto-generated catch block
            				e1.printStackTrace();
            			}
        }
        return false;
    }
    
    public boolean insertQuickRoomInfo(QuickGameRoomInfo stQuickGameRoomInfo){
      	 Connection stConnection = null;
        	try{                 		
        		long lGameRoomID = 0;
        		if(stQuickGameRoomInfo.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_START
        				||stQuickGameRoomInfo.getEQuickGameRoomStatus() == QUICK_GAME_ROOM_STATUS.QUICK_GAME_ROOM_FINISH){
        			lGameRoomID = stQuickGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId();
        		}
        		
   	        String insertTransactionInfo="insert into t_quick_game_info set roomid=?"
   	        		+",gameroomid=?"
   	        		+",roomstate=?"
   	        		+",code=?"
   	        		+",roomname=?"
   	        		+",smallbliand=?"
   	        		+",bigbliand=?"
   	        		+",lbuyin=?"
   	        		+",bbuyin=?"
   	        		+",duration=?"
   	        		+",createtime=?"
   	        		+",gameroomseniortype=?"
   	        		+",maxbuyinratio=?"
   	        		+",minbuyinratio=?"
   	        		+",ante=?"
   	        		+",insurance=?"
   	        		+",gameroomtype=?"
   	        		+ " on duplicate key update gameroomid=?"
   	        		+",roomstate=?"
   	        		+",code=?"
   	        		+",roomname=?"
   	        		+",smallbliand=?"
   	        		+",bigbliand=?"
   	        		+",lbuyin=?"
   	        		+",bbuyin=?"
   	        		+",duration=?"
   	        		+",createtime=?"
   	        		+",gameroomseniortype=?"
   	        		+",maxbuyinratio=?"
   	        		+",minbuyinratio=?"
   	        		+",ante=?"
   	        		+",insurance=?"
   	        		+",gameroomtype=?";
   	        
   	        stConnection = mDatabasePool.getConnection();
   	        PreparedStatement ps=stConnection.prepareStatement(insertTransactionInfo);
   	        
   	        ps.setLong(1, stQuickGameRoomInfo.getStChatRoomInfo().getStChatRoomBaseInfo().getLChatRoomId());
   	        ps.setLong(2, lGameRoomID);
   	        ps.setInt(3, stQuickGameRoomInfo.getEQuickGameRoomStatus().getNumber());
   	        ps.setString(4, stQuickGameRoomInfo.getSCryptCode());
   	        ps.setString(5, stQuickGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
   	        ps.setLong(6, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
   	        ps.setLong(7, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
   	        ps.setLong(8, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLBuyIn());  
   	        ps.setInt(9, stQuickGameRoomInfo.getStGameRoomBaseInfo().getBBuyIn());
   	        ps.setLong(10, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLDuration());
   	        ps.setLong(11, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime());
   	        ps.setInt(12, stQuickGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomSeniorType().getNumber());
   	        ps.setInt(13, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio());
   	        ps.setInt(14, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIMinBuyinRatio());
   	        ps.setInt(15, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
   	        ps.setInt(16, stQuickGameRoomInfo.getStGameRoomBaseInfo().getBInsurance());
   	        ps.setInt(17, stQuickGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomType().getNumber());
   	        ps.setLong(18, lGameRoomID);
   	        ps.setInt(19, stQuickGameRoomInfo.getEQuickGameRoomStatus().getNumber());
   	        ps.setString(20, stQuickGameRoomInfo.getSCryptCode());
   	        ps.setString(21, stQuickGameRoomInfo.getStGameRoomBaseInfo().getStrRoomName());
   	        ps.setLong(22, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLSmallBlinds());
   	        ps.setLong(23, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLBigBlinds());
   	        ps.setLong(24, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLBuyIn());  
   	        ps.setInt(25, stQuickGameRoomInfo.getStGameRoomBaseInfo().getBBuyIn());
   	        ps.setLong(26, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLDuration());
   	        ps.setLong(27, stQuickGameRoomInfo.getStGameRoomBaseInfo().getLCreateTime());
   	        ps.setInt(28, stQuickGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomSeniorType().getNumber());
   	        ps.setInt(29, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIMaxBuyinRatio());
   	        ps.setInt(30, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIMinBuyinRatio());
   	        ps.setInt(31, stQuickGameRoomInfo.getStGameRoomBaseInfo().getIAnte());
   	        ps.setInt(32, stQuickGameRoomInfo.getStGameRoomBaseInfo().getBInsurance());
   	        ps.setInt(33, stQuickGameRoomInfo.getStGameRoomBaseInfo().getEGameRoomType().getNumber());
   	        
              ps.executeUpdate();
             
              stConnection.close();
              return true;
        	}catch (Exception e) {
   			// TODO Auto-generated catch block
   			e.printStackTrace();
            	try {
               	 stConnection.close();
          			} catch (SQLException e1) {
          				// TODO Auto-generated catch block
          				e1.printStackTrace();
          			}
   			return false;
   		}
       }
    
    //游戏房间信息落地
    public boolean insertGameRoomInfo(GameRoomDetailInfo stGameRoomDetailInfo){
   	 Connection stConnection = null;
     	try{       		
     		stConnection = mDatabasePool.getConnection();
     		
   		String insql="insert into t_game_room set roomid=?" 
    				+",roomname=?"
    				+",smallblinds=?"
    				+",bigblinds=?"
    				+",buyin=?"
    				+",starttime=?"
    				+",duration=?"
    				+",createuser=?"
    				+",privateroom=?"
    				+",chatroomid=?"
     				+",gameroomtype=?"
     				+",createroomtype=?"
     				+",gameroomseniortype=?"
     				+",maxbuyinratio=?"
     				+",minbuyinratio=?"
     				+",buyincontrol=?"
     				+",ante=?"
     				+",insurance=?"
     				+",gpslimit=?"
     				+",iplimit=?"
     				+",leagueid=?"
     				+",leaguebname=?";
     				
           //上面的方法比下面的方法有优势，一方面是安全性，另一方面我忘记了……
           PreparedStatement ps=stConnection.prepareStatement(insql);
           ps.setLong(1, stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId());
           ps.setString(2, stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStrRoomName());
           ps.setLong(3,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLSmallBlinds());
           ps.setLong(4,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLBigBlinds());
           ps.setLong(5,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLBuyIn());
           ps.setLong(6,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLCreateTime());
           ps.setLong(7,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLDuration());
           ps.setLong(8,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid());
           ps.setInt(9,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBPrivateRoom());
           ps.setLong(10,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID());
           ps.setString(11,stGameRoomDetailInfo.getStGameRoomInfo().getEGameRoomType().toString());
           ps.setString(12,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getECreateRoomType().toString());
           ps.setInt(13,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getEGameRoomSeniorType().getNumber());
           ps.setInt(14,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getIMaxBuyinRatio());
           ps.setInt(15,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getIMinBuyinRatio());
           ps.setInt(16,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBBuyinControl());
           ps.setInt(17,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getIAnte());
           ps.setInt(18,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBInsurance());
           ps.setInt(19,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBGPSLimit());
           ps.setInt(20,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBIPLimit());
           ps.setLong(21,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID());
           ps.setString(22,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getSLeagueName());
           
           //保存链接 
           int result=ps.executeUpdate();
           if(result<=0){
            	stConnection.close();
            	return false;
            }else{
                stConnection.close();
                return true;
            }
        }catch(Exception e){
            e.printStackTrace();
         	try {
             	 stConnection.close();
        			} catch (SQLException e1) {
        				// TODO Auto-generated catch block
        				e1.printStackTrace();
        			}
        }
     	return false;
     }
    
    //游戏房间邀请信息落地
    public boolean insertInGameUser(long lRoomid,long uuid){
   	 Connection stConnection = null;
     	try{       
   		String insql="insert into t_game_room_members set roomid=?,uuid=? on duplicate key update uuid=?";
           //上面的方法比下面的方法有优势，一方面是安全性，另一方面我忘记了……
   		stConnection = mDatabasePool.getConnection();
           PreparedStatement ps=stConnection.prepareStatement(insql);
           ps.setLong(1, lRoomid);
           ps.setLong(2, uuid);
           ps.setLong(3,uuid);
       
           //保存链接 
           int result=ps.executeUpdate();
           stConnection.close();
           if(result>0)
           	return true;
           else
           	return false;
     	}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
         	try {
            	 stConnection.close();
       			} catch (SQLException e1) {
       				// TODO Auto-generated catch block
       				e1.printStackTrace();
       			}
			return false;
		}
     }
    
    //聊天室相关信息
    public boolean insertRoomInfo(ChatRoomInfo stChatRoomInfo){
   	 Connection stConnection = null;
    	try{
    		      		
   		String insql="insert into t_room_info set roomid=?"
   				+",roomname=?"
   				+",roomtempname=?"
   				+",roomicon=?"
   				+",maxmember=?"
   				+",curMember=?"
   				+",createUser=?"
   				+",roomtype=?"
   				+",ownercreate=?"
   				+",ownerinvite=?";
   		
           //上面的方法比下面的方法有优势，一方面是安全性，另一方面我忘记了……
   		stConnection = mDatabasePool.getConnection();
           PreparedStatement ps=stConnection.prepareStatement(insql);
           ps.setLong(1, stChatRoomInfo.getStChatRoomBaseInfo().getLChatRoomId());
           ps.setString(2, stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomName());
           ps.setString(3, stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomTempName());
           ps.setString(4, stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomIcon());
           ps.setInt(5,stChatRoomInfo.getStChatRoomBaseInfo().getNMaxMembers());
           ps.setInt(6,stChatRoomInfo.getStChatRoomBaseInfo().getNCurMembers());
           ps.setLong(7,stChatRoomInfo.getStChatRoomBaseInfo().getLCreateUser());
           ps.setInt(8,stChatRoomInfo.getStChatRoomBaseInfo().getIRoomType());
           ps.setInt(9,+stChatRoomInfo.getStChatRoomBaseInfo().getNOwnerCreate());
           ps.setInt(10,stChatRoomInfo.getStChatRoomBaseInfo().getNOwnerInvite());
           
           //保存链接 
           int result=ps.executeUpdate();
           if(result<=0){
            	stConnection.close();
            	return false;
            }
           
           //保存每个屋子的玩家
           for(int i = 0; i < stChatRoomInfo.getVUserBaseInfosCount() ; i ++){
           	UserBaseInfoNet stUserBaseInfoNet = stChatRoomInfo.getVUserBaseInfos(i);
               insql="insert into t_chat_room_members set roomid=?,uuid=?";
//               System.out.println("database : " + insql);
               ps=stConnection.prepareStatement(insql);
               ps.setLong(1, stChatRoomInfo.getStChatRoomBaseInfo().getLChatRoomId());
               ps.setLong(2, stUserBaseInfoNet.getUuid());
               ps.executeUpdate();
           }
           
           stConnection.close();
           return true;

       }catch(Exception e){
           e.printStackTrace();
         	try {
            	 stConnection.close();
       			} catch (SQLException e1) {
       				// TODO Auto-generated catch block
       				e1.printStackTrace();
       			}
       }
       return false;
    }
    
    //删除聊天室某个用户
    public boolean removeFromChatRoomUser(long roomid , long removeUuid){
   	 Connection stConnection = null;
        try{
       	stConnection = mDatabasePool.getConnection();
          	String insertChatRoomMemberSl="delete from t_chat_room_members where roomid=? and uuid=?";

//          	System.out.println("database : " + insertChatRoomMemberSl);
                  //保存终端设备信息
         	PreparedStatement ps=stConnection.prepareStatement(insertChatRoomMemberSl);
           ps.setLong(1, roomid);
           ps.setLong(2, removeUuid);   
      
            int result=ps.executeUpdate();
            stConnection.close();
            if(result > 0){
           	 return true;
             }
            return false;
        }catch(Exception e){
            e.printStackTrace();
         	try {
             	 stConnection.close();
        			} catch (SQLException e1) {
        				// TODO Auto-generated catch block
        				e1.printStackTrace();
        			}
            return false;
        }
    }
    
    //获取大盲数据结构
    public Map<Integer,Long>  getBlindsByType(SNG_ROOM_TYPE eSNGRoomType,int iBlindType){
    	Connection stConnection = null;
   		Statement statement;
   		Map<Integer,Long> mBlindsLevel = new HashMap<Integer,Long>();
   			try {
   				stConnection = mDatabasePool.getConnection();
   				statement = stConnection.createStatement();
   		   		
   				String loadGameRoomSql="select level,value from t_sng_blindinfo where blindtype="+iBlindType;
   				ResultSet gameRoomRS=null;
   				
   				gameRoomRS=statement.executeQuery(loadGameRoomSql);

   	             while(gameRoomRS.next()){
   	     			 //将新的牌局放到GameRoomManager中
   	     			
   	            	mBlindsLevel.put(gameRoomRS.getInt("level"), gameRoomRS.getLong("value"));
   	             }
   	             
   	             stConnection.close();
   	             return mBlindsLevel;
   			} catch (Exception e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   				
   				try {
   					stConnection.close();
              	} catch (SQLException e1) {
              		// TODO Auto-generated catch block
              		e1.printStackTrace();
              	}
   			}
   			return null;
       }
    
    //管理员处理 买入申请  更新入库(管理员处理)
    public void dealBuyInInfoV2(long gameroomid,long applicantuuid,long manageruuid,String managerNickname,BUYIN_CONTROL_STATUS eBuyinControlStatus,long lActionTime){
        Connection stConnection = null;
        try {
            stConnection = mDatabasePool.getConnection();
            String insql = "update t_realtime_buyin_info  set buyincontrolstatus = ?,actiontime = ?,manageruuid = ?,managernickname = ? where gameroomid = ? and applicantuuid=? and buyincontrolstatus=1";
            PreparedStatement ps = stConnection.prepareStatement(insql);
            
            ps.setInt(1, eBuyinControlStatus.getNumber());
            ps.setLong(2, lActionTime);
            ps.setLong(3, manageruuid);
            ps.setString(4, managerNickname);
            ps.setLong(5, gameroomid);
            ps.setLong(6, applicantuuid);
            
            ps.executeUpdate();
            stConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (stConnection != null)
                    stConnection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    public void gameoverBuyInControls(long gameroomid){
        Connection stConnection = null;
        try {
            stConnection = mDatabasePool.getConnection();
            String insql = "update t_realtime_buyin_info  set buyincontrolstatus = ?,actiontime = ?,manageruuid = ? where gameroomid = ? and buyincontrolstatus=1";
            PreparedStatement ps = stConnection.prepareStatement(insql);
            
            ps.setInt(1, BUYIN_CONTROL_STATUS.BUYIN_CONTROL_STATUS_BUYIN_DENY_VALUE);
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, 1000);
            ps.setLong(4, gameroomid);
            
            ps.executeUpdate();
            stConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (stConnection != null)
                    stConnection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    //把实时买入记录复制到历史买入记录中
    public void copyToHistoryBuyInControls(long gameroomid){
        Connection stConnection = null;
        try {
            stConnection = mDatabasePool.getConnection();
            String insql = "insert into t_history_buyin_info (gameroomid,gameroomname,gameroomtype,applicantuuid,applicantnickname,applicanticon,applicantshowid,applicantgender"
            		+ ",manageruuid,managernickname,buyincontrolstatus,buyinstacks,createtime,actiontime,remark,applicantclubid,applicantclubname,dealtype,dealremark) "
            		+ "select  gameroomid,gameroomname,gameroomtype,applicantuuid,applicantnickname,applicanticon,applicantshowid,applicantgender"
            		+ ",manageruuid,managernickname,buyincontrolstatus,buyinstacks,createtime,actiontime,remark,applicantclubid,applicantclubname,dealtype,dealremark "
            		+ "from t_realtime_buyin_info where gameroomid=? "
            		+ "group by applicantuuid,createtime";
            
            PreparedStatement ps = stConnection.prepareStatement(insql);
            
            ps.setLong(1, gameroomid);
            
            ps.executeUpdate();
            stConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (stConnection != null)
                    stConnection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    //删除牌局的实时买入记录
    public void removeBuyInControls(long gameroomid){
        Connection stConnection = null;
        try {
            stConnection = mDatabasePool.getConnection();
            String insql = "delete from t_realtime_buyin_info where gameroomid = ?";
            PreparedStatement ps = stConnection.prepareStatement(insql);
            
            ps.setLong(1, gameroomid);
            
            ps.executeUpdate();
            stConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (stConnection != null)
                    stConnection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    
}
 
