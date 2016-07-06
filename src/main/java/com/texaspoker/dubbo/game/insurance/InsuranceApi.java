/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceApiProxy.java
 * 创建人:xichen
 * 创建日期:2016年7月1日-下午6:51:42
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.BuyInsuranceInfo;
import com.texaspoker.moment.TexasPoker.GAME_ROOM_GAME_STATE;
import com.texaspoker.moment.TexasPoker.INSURANCE_BET_STATUS;
import com.texaspoker.moment.TexasPoker.InsurancePotInfo;
import com.texaspoker.moment.TexasPoker.InsuranceUserInfo;
import com.texaspoker.moment.room.GameRoomManager;

/**
 * 类名称:InsuranceApiProxy
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月1日-下午6:51:42
 * 修改备注:
 */
public class InsuranceApi {

  /*
   * 保险方API接口，由保险提供实现
   */

  // 牌局数据同步接口
  public static void parseGameAction(String sTableLog) {
    InsuranceManager.getInstance().onGameAction(sTableLog);
  }

  // allin状态下开始计算保险接口
  public static TexasPoker.InsuranceInfo checkInsurance(long lGameRoomID) {
	  TexasPoker.InsuranceInfo.Builder insuranceInfoBuilder = TexasPoker.InsuranceInfo.newBuilder();
    
    return insuranceInfoBuilder.build();
  }

  // 保险购买接口
  public static TexasPoker.InsuranceInfo doInsuranceAction(long lGameRoomID,long lGameHandID, long uuid,GAME_ROOM_GAME_STATE eGameRoomState,
      List<BuyInsuranceInfo> stBuyInsuranceInfo) {
	  TexasPoker.InsuranceInfo.Builder insuranceInfoBuilder = TexasPoker.InsuranceInfo.newBuilder();
	    
	    return insuranceInfoBuilder.build();
  }

  // 发牌后获取保险接口
  public static TexasPoker.InsuranceInfo getInsuranceResult(long lGameRoomID, int tableCard) {
	  TexasPoker.InsuranceInfo.Builder insuranceInfoBuilder = TexasPoker.InsuranceInfo.newBuilder();
	  return insuranceInfoBuilder.build();
  }

  // 房主控制开关接口
  public static TexasPoker.InsuranceInfo setCreaterInsuranceClose(long lGameRoomID, long uuid, boolean close) {
	  TexasPoker.InsuranceInfo.Builder insuranceInfoBuilder = TexasPoker.InsuranceInfo.newBuilder();
	  return insuranceInfoBuilder.build();
  }

  // 用户控制开关接口
  public static TexasPoker.InsuranceInfo setUserInsuranceClose(long lGameRoomID, long uuid, boolean close) {
	  TexasPoker.InsuranceInfo.Builder insuranceInfoBuilder = TexasPoker.InsuranceInfo.newBuilder();
	  return insuranceInfoBuilder.build();
  }

  /*
   * 游戏方API接口，由游戏方提供实现
   */

  // 保险购买行为超时接口
  public static void gotoNextStep(long lGameRoomID) {
	  GameRoomManager.getInstance().gotoNextStep(lGameRoomID);
  }

  // billserver保险结账接口
  public static void addInsuranceBuyin(long lGameRoomID, long uuid, long buyin) {

  }

  public static void addInsuranceGetStack(long lGameRoomID, long uuid, long getstacks) {

  }

  // 结构体转换函数
  private static List<InsurancePotInfo> getPotInfoListFromInsuranceInfo(InsuranceInfo insInfo) {
    List<InsurancePotInfo> resultList = new ArrayList<InsurancePotInfo>();

//    for (Long potId : insInfo.pot_user_outs.keySet()) {
//      InsurancePotInfo.Builder potInfoBuilder = InsurancePotInfo.newBuilder();
//      potInfoBuilder.setLPotID(potId);
//      potInfoBuilder.setLPotStacks(insInfo.pot_stacks.get(potId));
//      // 设置这个边池参与用户
//      for (Long potUuid : insInfo.pot_users.get(potId)) {
//        potInfoBuilder.addVUuids(potUuid);
//      }
//      // 设置这个边池的保险用户
//      Map<Long, OutsInfo> outsInfos = insInfo.pot_user_outs.get(potId);
//      for (OutsInfo outsInfo : outsInfos.values()) {
//        InsuranceUserInfo.Builder userInfoBuilder = InsuranceUserInfo.newBuilder();
//        userInfoBuilder.setUuid(outsInfo.winner);
//        // userInfoBuilder.setVOdds(index, value);
//        userInfoBuilder.setLEqualStacks(outsInfo.pay_earn);
//        userInfoBuilder.setLNoLosses(outsInfo.pay_loss);
//        if (outsInfo.fold) {
//          userInfoBuilder.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_FOLD);
//        } else if (outsInfo.buy > 0) {
//          userInfoBuilder.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETED);
//        } else {
//          userInfoBuilder.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETTING);
//        }
//        potInfoBuilder.addVInsuranceUserInfos(userInfoBuilder.build());
//      }
//      resultList.add(potInfoBuilder.build());
//    }
    return resultList;
  }
}
