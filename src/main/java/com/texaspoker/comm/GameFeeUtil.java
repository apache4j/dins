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
	
	public static long getLeagueGameFee(int lSmallBlinds){
		switch(lSmallBlinds){
		case 1:
			return 30;
		case 2:
			return 60;
		case 5:
			return 120;
		case 10:
			return 200;
		case 20:
			return 200;
		case 25:
			return 200;
		case 50:
			return 200;
		case 100:
			return 300;
		case 200:
			return 400;
		case 300:
			return 900;
		case 500:
			return 1500;
		case 1000:
			return 400;
		}
		return 10;
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
