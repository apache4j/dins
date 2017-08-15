package com.texaspoker.comm;

public class GameFeeUtil {
	
	public static int getServiceFeeRat(int lSmallBlinds){
		switch(lSmallBlinds){
		case 1:
		case 2:
		case 5:
		case 10:
		case 20:
		case 25:
		case 50:
		case 100:
		case 200:
		case 300:
		case 500:
		case 1000:
			return 10;
		}
		return 10;
	}
	
	public static long getLeagueGameFee(int lSmallBlinds,int iMaxPlayers){
		int rate = 1;
		if(iMaxPlayers == 2){
			rate = 2;
		}
		
		long originalStacks = 10;
		
		switch(lSmallBlinds){
		case 1:
			originalStacks = 30;
			break;
		case 2:
			originalStacks = 60;
			break;
		case 5:
			originalStacks = 120;
			break;
		case 10:
			originalStacks = 200;
			break;
		case 20:
			originalStacks = 200;
			break;
		case 25:
			originalStacks = 200;
			break;
		case 50:
			originalStacks = 200;
			break;
		case 100:
			originalStacks = 300;
			break;
		case 200:
			originalStacks = 400;
			break;
		case 300:
			originalStacks = 900;
			break;
		case 500:
			originalStacks = 1500;
			break;
		case 1000:
			originalStacks = 4000;
			break;
		}
		return originalStacks/rate;
	}
	
	public static int getShowCardItemFee(int lSmallBlinds){
		switch(lSmallBlinds){
		case 1:
			return 10;
		case 2:
			return 20;
		case 5:
			return 30;
		case 10:
			return 40;
		case 20:
			return 50;
		case 25:
			return 60;
		case 50:
			return 70;
		case 100:
			return 80;
		case 200:
			return 90;
		case 300:
			return 100;
		case 500:
			return 120;
		case 1000:
			return 200;
		}
		return 10;
	}
}
