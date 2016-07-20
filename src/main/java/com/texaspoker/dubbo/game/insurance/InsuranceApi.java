/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceApiProxy.java
 * 创建人:xichen
 * 创建日期:2016年7月1日-下午6:51:42
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.BuyInsuranceInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.INSURANCE_ACT;
import com.texaspoker.moment.TexasPoker.INSURANCE_BET_STATUS;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.room.GameRoomDetailInfo;
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

  private static Logger logger = Logger.getLogger(InsuranceApi.class);

  /*
   * 保险方API接口，由保险提供实现
   */

  // 牌局数据同步接口
  public static void parseGameAction(String sTableLog) {
    InsuranceManager.getInstance().onGameAction(sTableLog);
  }

  // allin状态下开始计算保险接口
  public static TexasPoker.InsuranceInfo checkInsurance(long lGameRoomID, long lHandID,
      TexasPoker.GAME_ROOM_GAME_STATE eRoundState) {
    InsuranceInfo insuranceInfo =
        InsuranceManager.getInstance().checkInsurance(lGameRoomID, lHandID, getRoundStatus(eRoundState));
    return getApiInfoFromInsuranceInfo(insuranceInfo);
  }

  // 发牌后获取保险接口
  public static TexasPoker.GameRoomInfo getInsuranceResult(GameRoomInfo stGameRoomInfo, long lHandID, int tableCard) {
    InsuranceInfo insuranceInfo =
        InsuranceManager.getInstance().settleInsurance(stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId(),
            lHandID, tableCard);
    if (insuranceInfo.status != HandUtils.RIVER) {
      return stGameRoomInfo;
    } else {
      return doInsuranceResult(insuranceInfo, stGameRoomInfo);
    }
  }

  // 保险购买接口
  public static TexasPoker.InsuranceInfo doInsuranceAction(long lGameRoomID, long lHandID,
      TexasPoker.GAME_ROOM_GAME_STATE eRoundState, long uuid, List<BuyInsuranceInfo> stBuyInsuranceInfos) {
    InsuranceInfo insuranceInfo = null;
    for (BuyInsuranceInfo buyInfo : stBuyInsuranceInfos) {
      if (buyInfo.getEInsuranceAct().equals(INSURANCE_ACT.INSURANCE_ACT_BET)) {
        insuranceInfo =
            InsuranceManager.getInstance().buyInsurance(lGameRoomID, lHandID, getRoundStatus(eRoundState), uuid,
                buyInfo.getLPotID(), buyInfo.getLBetStacks());
      } else if (buyInfo.getEInsuranceAct().equals(INSURANCE_ACT.INSURANCE_ACT_FOLD)) {
        insuranceInfo =
            InsuranceManager.getInstance().foldInsurance(lGameRoomID, lHandID, getRoundStatus(eRoundState), uuid,
                buyInfo.getLPotID());
      }
    }
    return getApiInfoFromInsuranceInfo(insuranceInfo);
  }

  // 延时购买接口
  public static TexasPoker.InsuranceInfo delayInsuranceAction(long lGameRoomID, long lHandID,
      TexasPoker.GAME_ROOM_GAME_STATE eRoundState, long uuid) {
    InsuranceManager.getInstance().delayInsurance(lGameRoomID, lHandID, getRoundStatus(eRoundState), uuid);
    return getApiInfoFromInsuranceInfo(InsuranceManager.getInstance().getInsurance(lGameRoomID));
  }

  // 房主控制开关接口
  public static TexasPoker.InsuranceInfo setCreaterInsuranceClose(long lGameRoomID, long uuid, boolean close) {
    InsuranceManager.getInstance().openInsuranceByCreater(lGameRoomID, uuid, close);
    return getApiInfoFromInsuranceInfo(InsuranceManager.getInstance().getInsurance(lGameRoomID));
  }

  // 用户控制开关接口
  public static TexasPoker.InsuranceInfo setUserInsuranceClose(long lGameRoomID, long uuid, boolean close) {
    InsuranceManager.getInstance().openInsuranceByUser(lGameRoomID, uuid, close);
    return getApiInfoFromInsuranceInfo(InsuranceManager.getInstance().getInsurance(lGameRoomID));
  }

  /*
   * 游戏方API接口，由游戏方提供实现
   */

  // 保险购买行为超时接口
  public static void gotoNextStep(long lGameRoomID) {
	  GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
	  if(stGameRoomDetailInfo!=null){
		  stGameRoomDetailInfo.gotoNextStep();
	  }
  }

  // billserver保险购买接口
  public static void addInsuranceBuyin(long lGameRoomID, long uuid, long buyin) {

  }

  // billserver保险赔付接口
  public static void addInsuranceGetStack(long lGameRoomID, long uuid, long getstacks) {

  }

  // 保险代理人账单信息接口
  public static boolean setInsuranceGame(long lGameRoomID, long nums) {
    return true;
  }

  // 保险扣钻石接口
  public static boolean addUserCoin(Long uuid, long nums) {
    return true;
  }

  /*
   * 接口转换工具类
   */

  private static int getRoundStatus(TexasPoker.GAME_ROOM_GAME_STATE eRoomState) {
    if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_PreFlop)) {
      return HandUtils.PRE_FLOP;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop)) {
      return HandUtils.FLOP;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_One)) {
      return HandUtils.FLOP1;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Two)) {
      return HandUtils.FLOP2;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Flop_Three)) {
      return HandUtils.FLOP3;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_Turn)) {
      return HandUtils.TURN;
    } else if (eRoomState.equals(TexasPoker.GAME_ROOM_GAME_STATE.ROOM_GAME_STATE_River)) {
      return HandUtils.RIVER;
    }
    return HandUtils.PRE_FLOP;
  }

  private static TexasPoker.GameRoomInfo doInsuranceResult(InsuranceInfo insuranceInfo, TexasPoker.GameRoomInfo info) {
    TexasPoker.GameRoomInfo.Builder gameBuilder = info.toBuilder();
    for (int i = 0; i < info.getVUserGameInfosCount(); i++) {
      UserGameInfoNet userGameInfo = info.getVUserGameInfos(i);
      long uuid = userGameInfo.getStUserInfo().getUuid();
      if (!insuranceInfo.round_user_outs.containsKey(uuid)) {
        continue;
      }
      long remains = userGameInfo.getLRemainStacks();
      for (OutsInfo outsInfo : insuranceInfo.round_user_outs.get(uuid)) {
        if (outsInfo.result > 0) {
          remains += outsInfo.result;
        }
        if (outsInfo.buy > 0) {
          remains -= outsInfo.buy;
        }
        if (remains >= 0) {
          userGameInfo.toBuilder().setLRemainStacks(remains);
          gameBuilder.setVUserGameInfos(i, userGameInfo);
        } else {
          // 不应该来这里
          // error log
        }
      }
    }
    return gameBuilder.build();
  }

  // 结构体转换函数
  private static TexasPoker.InsuranceInfo getApiInfoFromInsuranceInfo(InsuranceInfo insInfo) {
    if (insInfo == null) {
      return null;
    }
    TexasPoker.InsuranceInfo.Builder apiInfo = TexasPoker.InsuranceInfo.newBuilder();
    for (Map.Entry<Long, List<OutsInfo>> userOuts : insInfo.round_user_outs.entrySet()) {
      Long userId = userOuts.getKey();
      TexasPoker.InsuranceUserInfo.Builder userInfo = TexasPoker.InsuranceUserInfo.newBuilder();
      userInfo.setUuid(userId);
      // 边池信息
      long lTotalGetStacks = 0;
      long lTotalBuys = 0;
      long lTotalFold = 0;
      for (OutsInfo outs : userOuts.getValue()) {
        if (outs.winner != userId) {
          logger.error("outs info errors,outs winner:" + outs.winner + ",userId:" + userId);
          continue;
        }
        TexasPoker.InsurancePotInfo.Builder potInfo = TexasPoker.InsurancePotInfo.newBuilder();
        potInfo.setLPotID(outs.potId);
        potInfo.setLPotStacks(outs.pots);
        for (int i = 0; i < outs.outs.length; i++) {
          potInfo.addVOuts(outs.outs[i]);
        }
        potInfo.setIOdds((int) outs.odds * 100);
        potInfo.setLTotalPotStacks(outs.pay_pots);
        potInfo.setLNoLosses(outs.pay_loss);
        potInfo.setLMinStacks(outs.pay_min);
        potInfo.setLMaxStacks(outs.pay_max);
        potInfo.setLBetStacks(outs.buy);
        potInfo.setLGetStacks(outs.result);
        userInfo.addVInsurancePotInfos(potInfo);
        if (outs.buy > 0)
          lTotalBuys++;
        if (outs.fold)
          lTotalFold++;
        if (outs.result > 0)
          lTotalGetStacks += outs.result;
      }
      // 保险状态
      if (lTotalBuys > 0) {
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETED);
      } else if (lTotalBuys == 0 && lTotalFold > 0) {
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_FOLD);
      } else {
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETTING);
      }
      // 所以保险的赔付金额
      userInfo.setLTotalGetStacks(lTotalGetStacks);
      // 开关状态
      if (InsuranceManager.getInstance().getCreaterCloseStatus(insInfo.roomId, userId)) {
        userInfo.setBCanInsurance(0);
      } else {
        userInfo.setBCanInsurance(1);
      }
      if (InsuranceManager.getInstance().getUserCloseStatus(insInfo.roomId, userId)) {
        userInfo.setBDefaultInsurance(0);
      } else {
        userInfo.setBCanInsurance(1);
      }
      // 超时
      InsuranceTimer.InsuranceTimeInfo timeInfo = InsuranceManager.getInstance().getUserTimeout(insInfo.roomId, userId);
      userInfo.setLInsuranceStartTime(timeInfo.startTime);
      userInfo.setLInsuranceActDuration(timeInfo.endTime - timeInfo.startTime);
      // 延时
      int cost = InsuranceManager.getInstance().getUserDelayCost(insInfo.roomId, userId);
      userInfo.setIInsuranceDelayCost(cost);
      // 加入用户保险信息队列
      apiInfo.addVInsuranceUserInfos(userInfo);
    }
    return apiInfo.build();
  }
}
