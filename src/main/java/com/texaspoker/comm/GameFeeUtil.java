package com.texaspoker.comm;

import java.math.BigDecimal;

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

	/**
	 * 钻石收费
	 * @param lSmallBlinds
	 * @param iMaxPlayers
	 * @param type 0 普通牌局  1 授信牌局
     * @return
     */
	public static long getLeagueGameFee(int lSmallBlinds,int iMaxPlayers,int type){
		int rate = 1;
		if(iMaxPlayers == 2){
			rate = 2;
		}
		
		long originalStacks = 10;

		if (type == 1){
			return 0;
		}
		
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

	/**
	 * 打赏看牌扣币
	 * @param lSmallBlinds
	 * @param type 0 普通牌局  1 授信牌局
     * @return
     */
	public static int getShowCardItemFee(int lSmallBlinds, int type){

		if (type == 1){
			lSmallBlinds = lSmallBlinds/100;
		}

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

	/**
	 * 授信局入座收费 收取德扑币
	 * @param iSeatFeeRate 费率
	 * @param lStacks 买入
     * @return 四舍五入取整  (费率*买入)/1000000
     */
	public static int getSeatFee(int iSeatFeeRate, long lStacks){
		return new BigDecimal((double) (iSeatFeeRate * lStacks)/1000000 + "").setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
	}
}
