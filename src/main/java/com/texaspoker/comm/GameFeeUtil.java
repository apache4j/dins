package com.texaspoker.comm;

import com.texaspoker.configuration.ConfigaturationManager;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

	/**
	 * 底池抽水
	 * @param map 需要抽水的池子 池子编号-池子对应筹码量 筹码量 *100
	 * @param lBigBlinds 大盲(短牌前注) *100
	 * @param pumpProportion 抽水比例 *10000
	 * @param pumpLimit 抽水上限 *100
	 * @param userNum 玩家数
	 * @param isChange 是否开启根据人数变化 1开启 0未开启
     * @return 每个池子实际抽水值
     */
	public static Map<Integer, Long> getBottomPump(Map<Integer, Long> map, long lBigBlinds, int pumpProportion, int pumpLimit, int userNum, int isChange){

		long totalPump = 0l; //总抽水值
		long drawLimit = 0; //抽水上限 向上取整 人数变换开关开启(抽水上限*大盲*比例值)/10000  开关未开启(抽水上限*大盲)/100

		//计算抽水上限
		if (isChange == 1){
			if (userNum == 2){
				drawLimit = new BigDecimal((double) (pumpLimit * lBigBlinds * ConfigaturationManager.getInstance().iProportion1)/10000 + "").setScale(0, BigDecimal.ROUND_UP).longValue();
			} else if (userNum == 3 || userNum == 4){
				drawLimit = new BigDecimal((double) (pumpLimit * lBigBlinds * ConfigaturationManager.getInstance().iProportion2)/10000 + "").setScale(0, BigDecimal.ROUND_UP).longValue();
			} else {
				drawLimit = new BigDecimal((double) (pumpLimit * lBigBlinds * ConfigaturationManager.getInstance().iProportion3)/10000 + "").setScale(0, BigDecimal.ROUND_UP).longValue();
			}
		} else {
			drawLimit = new BigDecimal((double) (pumpLimit * lBigBlinds)/100 + "").setScale(0, BigDecimal.ROUND_UP).longValue();
		}

		//计算每个池子抽水值
		Map<Integer, Long> pumpMap = new HashMap<Integer, Long>();
		for (Map.Entry<Integer, Long> potMap : map.entrySet()){
			if (potMap.getKey() != null && potMap.getValue() != null){
				//池子抽水 向上取整 (池子筹码量*抽水比例)/10000
				long draw = new BigDecimal((double) (potMap.getValue() * pumpProportion)/10000 + "").setScale(0, BigDecimal.ROUND_UP).longValue();
				totalPump = totalPump + draw;
				pumpMap.put(potMap.getKey(),draw);
			}
		}

		//上封顶为无限
		/*if (drawLimit == 0){
			return pumpMap;
		}*/

		//判断总抽水值是否超过抽水上限,如果超过抽水上限则按照比例承担抽水上限
		if (totalPump > drawLimit && drawLimit != 0){
			if (pumpMap != null){
				int pumpMapCount = pumpMap.size();
				for (Map.Entry<Integer, Long> drawMap : pumpMap.entrySet()){
					pumpMapCount--;
					if (drawMap.getKey() != null && drawMap.getValue() != null){

						//池子实际抽水值 四舍五入
						long draw = new BigDecimal((double) (drawLimit * drawMap.getValue())/totalPump + "").setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
						if (pumpMapCount == 0) {//最后一个池子直接分完全部 保证总和等于drawLimit
							pumpMap.put(drawMap.getKey(), drawLimit);
						} else {
							pumpMap.put(drawMap.getKey(), draw);
						}

						drawLimit = drawLimit - draw;
					}
				}
			}
		}

		return pumpMap;
	}
}
