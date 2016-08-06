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

import com.texaspoker.bill.BillManager;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.TexasPoker.BuyInsuranceInfo;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.INSURANCE_ACT;
import com.texaspoker.moment.TexasPoker.INSURANCE_BET_STATUS;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomManager;

//import com.texaspoker.bill.BillManager;
//import com.texaspoker.logger.TableLogService;
//import com.texaspoker.moment.room.GameRoomDetailInfo;
//import com.texaspoker.moment.room.GameRoomManager;
//import com.texaspoker.user.UserManager;

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
    // 检查保险
    int roundStatus = getRoundStatus(eRoundState);
    InsuranceInfo insuranceInfo = InsuranceManager.getInstance().checkInsurance(lGameRoomID, lHandID, roundStatus);
    // 检查是否需要自动购买逻辑
    doInsuranceAutoBuy(insuranceInfo);
    return getApiInfoFromInsuranceInfo(insuranceInfo);
  }

  // 发牌后获取保险接口
  public static TexasPoker.GameRoomInfo getInsuranceResult(GameRoomInfo stGameRoomInfo, long lHandID, int tableCard) {
    // 获取房间id
    long lGameRoomID = stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId();
    // 结算保险
    InsuranceInfo insuranceInfo = InsuranceManager.getInstance().settleInsurance(lGameRoomID, lHandID, tableCard);
    // 返回保险结构体
    TexasPoker.GameRoomInfo resultInfo = doInsuranceResult(insuranceInfo, stGameRoomInfo);
    // 检查是否需要微调账单
    doInsuranceResultAdjust(insuranceInfo);
    // 打印结算日志
    printInsuranceResultLog(insuranceInfo);
    return resultInfo;
  }

  // 每一手结账时调用保险结算接口
  public static TexasPoker.GameRoomInfo getInsuranceBill(GameRoomInfo stGameRoomInfo, long lHandID) {
    // 获取房间id
    long lGameRoomID = stGameRoomInfo.getStGameRoomBaseInfo().getLGameRoomId();
    // 结算保险
    InsuranceInfo insuranceInfo = InsuranceManager.getInstance().getInsurance(lGameRoomID, lHandID);
    if (insuranceInfo == null) {
      return stGameRoomInfo;
    } else {
      return doInsuranceBill(insuranceInfo, stGameRoomInfo);
    }
  }

  // 保险购买接口
  public static TexasPoker.InsuranceInfo doInsuranceAction(long lGameRoomID, long lHandID,
      TexasPoker.GAME_ROOM_GAME_STATE eRoundState, long uuid, List<BuyInsuranceInfo> stBuyInsuranceInfos) {
    int roundStatus = getRoundStatus(eRoundState);
    InsuranceInfo insuranceInfo = null;
    for (BuyInsuranceInfo buyInfo : stBuyInsuranceInfos) {
      if (buyInfo.getEInsuranceAct().equals(INSURANCE_ACT.INSURANCE_ACT_BET)) {
        insuranceInfo =
            InsuranceManager.getInstance().buyInsurance(lGameRoomID, lHandID, roundStatus, uuid, buyInfo.getLPotID(),
                buyInfo.getLBetStacks());
      } else if (buyInfo.getEInsuranceAct().equals(INSURANCE_ACT.INSURANCE_ACT_FOLD)) {
        insuranceInfo =
            InsuranceManager.getInstance().foldInsurance(lGameRoomID, lHandID, roundStatus, uuid, buyInfo.getLPotID());
      } else {
        logger.error("roomId:" + lGameRoomID + ",handId:" + lHandID + ",eRoundState" + eRoundState + ",buy info:"
            + buyInfo);
      }
    }
    // 先保存购买时的超时信息
    TexasPoker.InsuranceInfo resultInfo = getApiInfoFromInsuranceInfo(insuranceInfo);
    // 返回当时状态
    return resultInfo;
  }

  // 检查是否进行超时逻辑
  public static void doInsuranceTimeout(long lGameRoomID) {
    // 本次操作执行完毕以后，再来查看是否需要超时继续往下走
    InsuranceManager.getInstance().checkRoomTimeout(lGameRoomID);
  }

  // 延时购买接口
  public static TexasPoker.InsuranceInfo delayInsuranceAction(long lGameRoomID, long lHandID,
      TexasPoker.GAME_ROOM_GAME_STATE eRoundState, long uuid) {
    InsuranceManager.getInstance().delayInsurance(lGameRoomID, lHandID, getRoundStatus(eRoundState), uuid);
    return getApiInfoFromInsuranceInfo(InsuranceManager.getInstance().getInsurance(lGameRoomID));
  }

  // 房主控制开关接口
  public static boolean setCreaterInsuranceClose(long lGameRoomID, long uuid, boolean close) {
    InsuranceManager.getInstance().openInsuranceByCreater(lGameRoomID, uuid, close);
    return true;
  }

  // 用户控制开关接口
  public static boolean setUserInsuranceClose(long lGameRoomID, long uuid, boolean close) {
    return InsuranceManager.getInstance().openInsuranceByUser(lGameRoomID, uuid, close);
  }

  // 获取保险开关接口
  public static boolean getUserInsuranceClose(long lGameRoomID, long uuid) {
    return InsuranceManager.getInstance().getUserCloseStatus(lGameRoomID, uuid);
  }

  // 房主暂停牌局
  public static void setRoomPause(long lGameRoomID) {
    InsuranceManager.getInstance().setInsurancePause(lGameRoomID);
  }

  // 房主继续牌局
  public static void setRoomPlay(long lGameRoomID) {
    InsuranceManager.getInstance().setInsurancePlay(lGameRoomID);
  }

  /*
   * 游戏方API接口，由游戏方提供实现
   */

  // 保险购买行为超时接口
  public static void gotoNextStep(long lGameRoomID) {
     GameRoomDetailInfo stGameRoomDetail =
     GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
     if(stGameRoomDetail!=null){
     stGameRoomDetail.gotoNextStep();
     }
  }

  // billserver保险购买接口
  public static void setInsuranceBuyin(long lGameRoomID, long uuid, long buyin) {
     BillManager.getInstance().addInsuranceBuyin(uuid, lGameRoomID, buyin);
  }

  // billserver保险赔付接口
  public static void setInsuranceGetStack(long lGameRoomID, long uuid, long getstacks) {
     BillManager.getInstance().addInsuranceGetStacks(uuid, lGameRoomID, getstacks);
  }

  // 保险代理人账单信息接口
  public static boolean setInsuranceGame(long lGameRoomID, long nums) {
     BillManager.getInstance().setFinalInsuranceGetStacks(1000, lGameRoomID, nums);
    return true;
  }

  // 保险扣钻石接口
  public static boolean addUserCoin(long uuid, long nums) {
    // UserManager.getInstance().addUserCoin(uuid, nums);
    return true;
  }

  // 获取用户的延时操作次数
  public static long getUserDelayOperation(long lGameRoomID, long uuid) {
     GameRoomDetailInfo stGameRoomDetail =
     GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
     if(stGameRoomDetail!=null){
     return stGameRoomDetail.getDealyTimes(uuid);
     }else{
     return 0;
     }
  }

  // 设置用户的延时操作次数
  public static void setUserDelayOperation(long lGameRoomID, long uuid, int nums) {
     GameRoomDetailInfo stGameRoomDetail =
     GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
     if(stGameRoomDetail!=null){
     stGameRoomDetail.setDealyTimes(uuid, nums);
     }
  }

  // 用户购买超时，调用接口
  public static void notifyUserBuyTimeout(long lGameRoomID, long uuid, TexasPoker.InsuranceInfo info) {
	     GameRoomDetailInfo stGameRoomDetail =
	    	     GameRoomManager.getInstance().getGameRoomDetailInfo(lGameRoomID);
	     
	     if(stGameRoomDetail!=null){
	         stGameRoomDetail.pushInsuranceTimeout(uuid, info);
	     }
  }

  // 操作日志接口
  public static void insuranceDebug(String sLog) {
     TableLogService.getInstance().error(sLog);
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

  // 超时通知接口层
  public static void notifyUserBuyTimeout(long lGameRoomID, long uuid, InsuranceInfo info) {
    TexasPoker.InsuranceInfo insInfo = getApiInfoFromInsuranceInfo(info);
    notifyUserBuyTimeout(lGameRoomID, uuid, insInfo);
  }

  public static void doInsuranceAutoBuy(InsuranceInfo insuranceInfo) {
    if (insuranceInfo == null) {
      return;
    }
    // turn阶段超过14张out时，系统强制帮用户购买即可
    if (insuranceInfo.status == HandUtils.TURN) {
      for (Map.Entry<Long, List<OutsInfo>> userOuts : insuranceInfo.round_user_outs.entrySet()) {
        long uuid = userOuts.getKey();
        List<OutsInfo> outsInfos = userOuts.getValue();
        for (OutsInfo outsInfo : outsInfos) {
          if (outsInfo.outs.length > OutsInfo.OUTS_LIMIT && outsInfo.pay_min > 0) {
            insuranceInfo.setSysBuy(outsInfo.potId, uuid, outsInfo.pay_min);
          }
        }
      }
    }
  }

  public static TexasPoker.GameRoomInfo doInsuranceResult(InsuranceInfo insuranceInfo, TexasPoker.GameRoomInfo info) {
    TexasPoker.GameRoomInfo.Builder gameBuilder = info.toBuilder();
    // 每次都需要设置保险信息
    TexasPoker.InsuranceInfo insInfo = getApiInfoFromInsuranceInfo(insuranceInfo);
    if (insInfo != null) {
      gameBuilder.setVInsuranceInfos(insInfo);
    }
    // logger.debug("gameinfo:" + gameBuilder);
    return gameBuilder.build();
  }

  public static void doInsuranceResultAdjust(InsuranceInfo insuranceInfo) {
    // 在river阶段，调整可能产生-1,-2的保险购买问题
    if (insuranceInfo.status == HandUtils.TURN) {
      for (Map.Entry<Long, List<OutsInfo>> userOuts : insuranceInfo.round_user_outs.entrySet()) {
        for (OutsInfo outs : userOuts.getValue()) {
          // pot池小于4，并且这一轮保险没有中，并且上一轮购买过保险没中
          if (outs.pots < 4 && outs.result == 0 && outs.pay_min > 0 && outs.buy < 3) {
            // 用户买了几个，系统赔付几个
            outs.result = outs.buy;
            // 维持实时账单平衡
            insuranceInfo.setShopTotal(0, outs.result);
            insuranceInfo.addUserResultTotal(outs.winner, outs.result);
            logger.debug("roomId:" + insuranceInfo.roomId + ",handId:" + insuranceInfo.handId + ",result:"
                + outs.result);
          }
        }
      }
    }
  }

  public static TexasPoker.GameRoomInfo doInsuranceBill(InsuranceInfo insuranceInfo, TexasPoker.GameRoomInfo info) {
    TexasPoker.GameRoomInfo.Builder gameBuilder = info.toBuilder();
    // turn阶段不实时显示购买保险的当前筹码量信息，只有再river结算时，才显示
    for (int i = 0; i < info.getVUserGameInfosCount(); i++) {
      UserGameInfoNet userGameInfo = info.getVUserGameInfos(i);
      long uuid = userGameInfo.getStUserInfo().getUuid();
      if (uuid == 0) {
        continue;
      }
      long remains = userGameInfo.getLRemainStacks();
      // 计算这一手的保险情况
      logger.debug("uuid:" + uuid + ",remains before:" + remains);
      long buys = insuranceInfo.getUserAllBuyTotal(uuid);
      if (buys > 0) {
        remains -= buys;
        logger.debug("uuid:" + uuid + ",buytotal:" + buys);
      }
      long results = insuranceInfo.getUserAllResultTotal(uuid);
      if (results > 0) {
        remains += results;
        logger.debug("uuid:" + uuid + ",result total:" + results);
      }
      // 没有变化则不用重置
      if (remains == userGameInfo.getLRemainStacks()) {
        continue;
      }
      logger.debug("uuid:" + uuid + ",remains after:" + remains);
      TexasPoker.UserGameInfoNet.Builder userGameBuilder = userGameInfo.toBuilder();
      userGameBuilder.setLRemainStacks(remains);
      // logger.debug("userGameBuilder:" + userGameBuilder);
      gameBuilder.setVUserGameInfos(i, userGameBuilder);
    }
    // logger.debug("gameinfo:" + gameBuilder);
    return gameBuilder.build();
  }

  // 结构体转换函数
  public static TexasPoker.InsuranceInfo getApiInfoFromInsuranceInfo(InsuranceInfo insInfo) {
    if (insInfo == null) {
      return null;
    }
    if (insInfo.round_user_outs.isEmpty()) {
      logger.error("no user outs!!!");
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
        logger.debug("outs info:" + outs);
        TexasPoker.InsurancePotInfo.Builder potInfo = TexasPoker.InsurancePotInfo.newBuilder();
        potInfo.setLPotID(outs.potId);
        potInfo.setIPotIndex(outs.potIndex);
        potInfo.setLPotStacks(outs.pots);
        for (int i = 0; i < outs.outs.length; i++) {
          potInfo.addVOuts(outs.outs[i]);
        }
        potInfo.setIOdds(outs.odds_i);
        potInfo.setLTotalPotStacks(outs.pay_pots);
        potInfo.setLNoLosses(outs.pay_loss);
        potInfo.setLMinStacks(outs.pay_min);
        potInfo.setLMaxStacks(outs.pay_max);
        potInfo.setLBetStacks(outs.buy);
        potInfo.setLSysStacks(outs.sys_buy);
        // 保险的赔付额显示需要扣住自己的购买金额
        if (outs.result > 0) {
          potInfo.setLGetStacks(outs.result - outs.buy);
        } else {
          potInfo.setLGetStacks(outs.result);
        }
        userInfo.addVInsurancePotInfos(potInfo);
        if (outs.buy > 0)
          lTotalBuys++;
        if (outs.fold)
          lTotalFold++;
        if (outs.result > 0)
          lTotalGetStacks += (outs.result - outs.buy);
      }
      // 保险状态
      if (lTotalBuys > 0 && lTotalBuys < userOuts.getValue().size() && lTotalFold == 0) {
        // 自动购买逻辑
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETTING);
      } else if (lTotalBuys == 0 && lTotalFold > 0) {
        // 全部放弃逻辑
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_FOLD);
      } else if (lTotalBuys == 0 && lTotalFold == 0) {
        // 未购买逻辑
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETTING);
      } else {
        userInfo.setEInsuranceBetStatus(INSURANCE_BET_STATUS.INSURANCE_BET_BETED);
      }
      // 所以保险的赔付金额
      userInfo.setLTotalGetStacks(lTotalGetStacks);
      // 开关状态
      userInfo.setBDefaultInsurance(1);
      if (InsuranceManager.getInstance().getUserCloseStatus(insInfo.roomId, userId)) {
        userInfo.setBDefaultInsurance(0);
      } else {
        userInfo.setBCanInsurance(1);
      }
      // 超时
      InsuranceTimer.InsuranceTimeInfo timeInfo = InsuranceManager.getInstance().getUserTimeout(insInfo.roomId, userId);
      if (timeInfo != null) {
        userInfo.setLInsuranceStartTime(timeInfo.startTime);
        userInfo.setLInsuranceActDuration((timeInfo.endTime - timeInfo.startTime) / 1000);
      }
      // 延时
      int cost = InsuranceManager.getInstance().getUserDelayCost(insInfo.roomId, userId);
      userInfo.setIInsuranceDelayCost(cost);
      userInfo.setIInsuranceDelayLong(InsuranceTimer.INSURANCE_OPERATION_DELAY_SECONDS);
      // 查看这个用户是否第一次触发
      // if (insInfo.checkFirstOcur(userId)) {
      // userInfo.setBFirstInsurance(1);
      // } else {
      // userInfo.setBFirstInsurance(0);
      // }
      // 加入用户保险信息队列
      // logger.debug("userinfo:" + userInfo);
      apiInfo.addVInsuranceUserInfos(userInfo);
    }
    // 查看这一轮是否第一次触发
    if (insInfo.checkRoundFirstOcur()) {
      apiInfo.setIFirstInsurance(1);
    } else {
      apiInfo.setIFirstInsurance(0);
    }
    // 加入领先但是没有触发保险的用户
    if (insInfo.round_winner_no_outs != null) {
      for (Long winner : insInfo.round_winner_no_outs) {
        apiInfo.addVNoOutsUuids(winner);
      }
    }
    logger.debug("insuranceinfo:" + apiInfo);
    return apiInfo.build();
  }

  // 打印保险的赔付日志，用于统计
  public static void printInsuranceResultLog(InsuranceInfo info) {
    long allBuyTotal = 0;
    long allResultTotal = 0;
    for (Map.Entry<Long, List<OutsInfo>> entry : info.round_user_outs.entrySet()) {
      long uuid = entry.getKey();
      List<OutsInfo> userInfos = entry.getValue();
      // 计算这个用户这一次总共的够买量
      long buytotal = 0;
      long resulttotal = 0;
      for (OutsInfo userInfo : userInfos) {
        buytotal += userInfo.buy;
        resulttotal += userInfo.result;
      }
      // 统计汇总
      allBuyTotal += buytotal;
      allResultTotal += resulttotal;
      // 有购买行为
      if (buytotal > 0) {
        // 购买日志
        StringBuilder loginfo1 = new StringBuilder();
        loginfo1.append(info.groupId);
        loginfo1.append(",");
        loginfo1.append(info.roomId);
        loginfo1.append(",");
        loginfo1.append(info.handId);
        loginfo1.append(",");
        loginfo1.append(uuid);
        loginfo1.append(",insbuy,");
        loginfo1.append(buytotal);
        InsuranceApi.insuranceDebug(loginfo1.toString());
        // 赔付日志
        StringBuilder loginfo2 = new StringBuilder();
        loginfo2.append(info.groupId);
        loginfo2.append(",");
        loginfo2.append(info.roomId);
        loginfo2.append(",");
        loginfo2.append(info.handId);
        loginfo2.append(",");
        loginfo2.append(uuid);
        loginfo2.append(",insresult,");
        loginfo2.append(resulttotal);
        InsuranceApi.insuranceDebug(loginfo2.toString());
      } else {
        // 放弃日志
        StringBuilder loginfo = new StringBuilder();
        loginfo.append(info.groupId);
        loginfo.append(",");
        loginfo.append(info.roomId);
        loginfo.append(",");
        loginfo.append(info.handId);
        loginfo.append(",");
        loginfo.append(uuid);
        loginfo.append(",insfold");
        InsuranceApi.insuranceDebug(loginfo.toString());
      }
    }
    // 打印这一轮的代理人日志
    if (allBuyTotal > 0) {
      // 购买日志
      StringBuilder loginfo1 = new StringBuilder();
      loginfo1.append(info.groupId);
      loginfo1.append(",");
      loginfo1.append(info.roomId);
      loginfo1.append(",");
      loginfo1.append(info.handId);
      loginfo1.append(",");
      loginfo1.append(1000);
      loginfo1.append(",insbuy,");
      loginfo1.append(allBuyTotal);
      InsuranceApi.insuranceDebug(loginfo1.toString());
      // 赔付日志
      StringBuilder loginfo = new StringBuilder();
      loginfo.append(info.groupId);
      loginfo.append(",");
      loginfo.append(info.roomId);
      loginfo.append(",");
      loginfo.append(info.handId);
      loginfo.append(",");
      loginfo.append(1000);
      loginfo.append(",insresult,");
      loginfo.append(allResultTotal);
      InsuranceApi.insuranceDebug(loginfo.toString());
      logger.debug("roomId:" + info.roomId + ",handId:" + info.handId + ",1000,buytotal:" + allBuyTotal
          + ",resulttotal:" + allResultTotal);
    }
  }

}
