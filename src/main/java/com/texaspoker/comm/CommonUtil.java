package com.texaspoker.comm;

import com.texaspoker.club.ClubManager;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPoker.ChatRoomInfo;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPokerHttpClub.ClubInfo;
import com.texaspoker.moment.room.ChatRoomManager;
import com.texaspoker.moment.room.QuickGameRoomManager;

public class CommonUtil {
	
	public static String getRoomFromText(CREATE_ROOM_TYPE eCreateRoomType,long lFromRoomID){
		if(eCreateRoomType == CREATE_ROOM_TYPE.CAHT_ROOM){
			ChatRoomInfo stChatRoomInfo = ChatRoomManager.getInstance().getChatRoomInfo(lFromRoomID);
			if(stChatRoomInfo!=null){
				// 将牌局ID存到关联的聊天室里面
				String sFromText="";
				if(stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomName()!=null 
						&& stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomName().length()!=0 ){
					sFromText = stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomName()/*+"圈子局"*/;
				}else{
					sFromText = stChatRoomInfo.getStChatRoomBaseInfo().getStrRoomTempName()/*+"圈子局"*/;
				}
				return sFromText;
			}
		}else if(eCreateRoomType == CREATE_ROOM_TYPE.CLUB_ROOM){
			String sFromText=""/*"来自"*/;
			ClubInfo.Builder stClubInfoBuilder = ClubManager.getInstance().getClubInfo(lFromRoomID);
			
			if(stClubInfoBuilder.getStClubBaseInfo().getSClubName()!=null){	
				sFromText += stClubInfoBuilder.getStClubBaseInfo().getSClubName();
			}
			return sFromText;
		}else if(eCreateRoomType == CREATE_ROOM_TYPE.QUICK_GAME_ROOM){
			QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(lFromRoomID);
			if(stQuickGameRoomInfoBuilder!=null){
				String sFromText="";
				if(stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getStrRoomName()!=null 
						&& stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getStrRoomName().length()!=0 ){
					sFromText = stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getStrRoomName()/*+"快速局"*/;
				}else{
					sFromText = stQuickGameRoomInfoBuilder.getStChatRoomInfo().getStChatRoomBaseInfo().getStrRoomTempName()/*+"快速局"*/;
				}
				return sFromText;
			}
		}
		
		return "";
	}

}
