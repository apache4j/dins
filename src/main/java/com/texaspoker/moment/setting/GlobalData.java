package com.texaspoker.moment.setting;

import com.texaspoker.moment.TexasPokerCommon.VIP_TYPE;

public class GlobalData {
	public static int VIP_BLUE_MaxInGameUserInfo = 12;
	public static int VIP_GOLD_MaxInGameUserInfo = 14;
	public static int VIP_PLATINUM_MaxInGameUserInfo = 20;
	public static int VIP_BLACK_MaxInGameUserInfo = 20;
	
	public static int VIP_BLUE_SimultaneousPlays = 1;
	public static int VIP_GOLD_SimultaneousPlays = 3;
	public static int VIP_PLATINUM_SimultaneousPlays = 5;
	public static int VIP_BLACK_SimultaneousPlays = 9;
	
	public static int getSimultaneousPlays(VIP_TYPE eVipType){
		switch(eVipType.getNumber()){
		case VIP_TYPE.BLUE_CARD_VALUE:{
			return VIP_BLUE_SimultaneousPlays;
		}
		case  VIP_TYPE.GOLD_CARD_VALUE:{
			return VIP_GOLD_SimultaneousPlays;
		}
		case  VIP_TYPE.PLATINUM_CARD_VALUE:{
			return VIP_PLATINUM_SimultaneousPlays;
		}
		case  VIP_TYPE.BLACK_CARD_VALUE:{
			return VIP_BLACK_SimultaneousPlays;
		}
		default:
			return VIP_BLUE_SimultaneousPlays;
		}
	}

}
