/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceManager.java
 * 创建人:xichen
 * 创建日期:2016年6月30日-下午2:44:22
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * 类名称:InsuranceManager
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月30日-下午2:44:22
 * 修改备注:
 */
public class InsuranceManager {

  private static Logger logger = Logger.getLogger(InsuranceManager.class);

  // 所有房间的保险信息
  private final ConcurrentMap<Long, InsuranceInfo> insurances_map = new ConcurrentHashMap<Long, InsuranceInfo>();

  // 购买保险的定时器
  private final InsuranceTimer insurance_timer = new InsuranceTimer();

  // 购买保险的开关
  private final InsuranceLimit insurance_open = new InsuranceLimit();

  // 购买的延时
  private final InsuranceDelay insurance_delay = new InsuranceDelay();

  private static InsuranceManager instance = new InsuranceManager();

  public static InsuranceManager getInstance() {
    return instance;
  }

  // 解析游戏数据
  public void onGameAction(String tableLog) {
    logger.debug(tableLog);
    InsuranceLog.onLogMessage(tableLog);
  }

  public void onTableOpenAction(long roomId) {
    insurance_timer.onRoomOpen(roomId);
    insurance_open.onRoomOpen(roomId);
    insurance_delay.onRoomOpen(roomId);
  }

  // 每一句结束时间调用
  public void onTableCloseAction(long roomId) {
    // 防止相互调用
    // HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    // if (handInfo != null) {
    // logger.error("roomId:" + roomId + ",handinfo not null");
    // HandsManager.getInstance().onTableClose(roomId);
    // }
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.debug("roomId:" + roomId + ",insuranceInfo null");
    } else {
      insurances_map.remove(roomId);
    }
    insurance_timer.onRoomClose(roomId);
    insurance_open.onRoomClose(roomId);
    insurance_delay.onRoomClose(roomId);
  }

  // 每一手结束时间调用，一次性写入账单
  public void onHandCloseAction(long roomId, long handId) {
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",handinfo null");
      return;
    }
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      // logger.error("roomId:" + roomId + ",insuranceInfo null");
      return;
    }
    // 写入用户买入账单
    for (Long uuid : insuranceInfo.hand_user_buy_totals.keySet()) {
      long userTotalBuys = 0;
      ConcurrentMap<Long, Long> potsBuys = insuranceInfo.hand_user_buy_totals.get(uuid);
      if (potsBuys != null) {
        for (Long potBuy : potsBuys.values()) {
          userTotalBuys += potBuy;
        }
      }
      // 这一手用户的买入总计
      if (userTotalBuys > 0) {
        InsuranceApi.setInsuranceBuyin(roomId, uuid, userTotalBuys);
        logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",totalbuys:" + userTotalBuys);
      }
    }
    // 写入用户赔付账单
    for (Map.Entry<Long, Long> resultEntry : insuranceInfo.hand_user_result_totals.entrySet()) {
      long uuid = resultEntry.getKey();
      long totals = resultEntry.getValue();
      if (totals > 0) {
        InsuranceApi.setInsuranceGetStack(roomId, uuid, totals);
        logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",totalresult:" + totals);
      }
    }
    // 写入代理人账单
    InsuranceApi.setInsuranceGame(roomId, insuranceInfo.getShopTotal());
    // 这一手结束后，清空保险的信息
    insuranceInfo.refreshHandState(handId);
  }

  // 内部使用，查询是否需要购买保险
  public InsuranceInfo checkInsurance(long roomId) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",handinfo null");
      return null;
    }
    return checkInsurance(handInfo);
  }

  // 内部使用
  private InsuranceInfo checkInsurance(HandInfo handInfo) {
    long roomId = handInfo.roomId;
    logger.debug("roomId:" + roomId + ",handinfo status :" + handInfo.gameStatus);
    // 检查牌局状态，现在只有turn，river能够买保险
    if (handInfo.gameStatus != HandUtils.FLOP && handInfo.gameStatus != HandUtils.TURN) {
      logger.error("roomId:" + roomId + ",handinfo status error:" + handInfo.gameStatus);
      return null;
    }
    // 刷新这一桌的保险超时
    insurance_timer.clearRoomTimeout(roomId);
    // 检查筹码信息
    handInfo.checkHandState();
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.debug("roomId:" + roomId + ",new insurance info");
      insuranceInfo = new InsuranceInfo();
      insuranceInfo.roomId = roomId;
      insuranceInfo.groupId = handInfo.groupId;
      insuranceInfo.handId = handInfo.handId;
      insuranceInfo.status = handInfo.insuranceStatus;
      insurances_map.put(roomId, insuranceInfo);
    }
    // 刷新这一桌的保险信息
    insuranceInfo.refreshRoundState(handInfo.handId, handInfo.insuranceStatus);
    // 是否计算分钱outs，river阶段才计算分钱outs
    boolean checkEvenOuts = insuranceInfo.status == HandUtils.TURN ? true : false;
    // 遍历每一个边池
    for (Map.Entry<Integer, Integer> allinPotEntry : handInfo.allin_pots.entrySet()) {
      // allin_bets定义为potId
      int allin_bets = allinPotEntry.getKey();
      int allin_pots = allinPotEntry.getValue();
      logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",potBets:" + allin_pots);
      // 遍历这个边池的玩家列表
      Map<Long, Integer> allinUsersMap = handInfo.allin_pots_user.get(allin_bets);
      // 这个边池至少2个玩家
      if (allinUsersMap.size() < 2) {
        logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",one user");
        continue;
      }
      // 这个边池多于3个玩家，不激活保险
      if (allinUsersMap.size() > 3) {
        logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",user size:" + allinUsersMap.size() + ",too more");
        continue;
      }
      List<HandUserInfo> allinUserInfos = new ArrayList<HandUserInfo>();
      for (Long uuid : allinUsersMap.keySet()) {
        allinUserInfos.add(handInfo.getUser(uuid));
        logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",user:" + uuid);
      }
      // 这个边池的领先却没有保险的玩家
      Set<Long> winnerList = OutsCheck.findWinnersInPlayer(allinUserInfos, handInfo);
      insuranceInfo.setWinnerNoOutsTip(winnerList);
      for (Long winner : winnerList) {
        if (insurance_open.checkUserCloseInsurance(roomId, winner) || insurance_open.checkUserDelegateState(roomId, winner)) {
          // 关闭了保险的人，不需要领先购买保险提示
          insuranceInfo.subWinnerNoOutsTips(winner);
        }
      }
      // 找出这个边池玩家的out保险
      List<OutsInfo> outsList = OutsCheck.findOutsInPlayers(allinUserInfos, handInfo, checkEvenOuts);
      if (outsList == null || outsList.size() == 0) {
        logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",outs size:" + outsList.size());
        continue;
      }
      logger.debug("roomId:" + roomId + ",potId:" + allin_bets + ",outs size:" + outsList.size());
      // 设置用户的保险购买信息
      for (OutsInfo outsInfo : outsList) {
        // 是否开启保险购买逻辑
        if (insurance_open.checkUserCloseInsurance(roomId, outsInfo.winner)) {
          logger.debug("roomId:" + roomId + ",uuid:" + outsInfo.winner + ",outs close");
          continue;
        }
        // 是否开启托管状态
        if (insurance_open.checkUserDelegateState(roomId, outsInfo.winner)) {
          logger.debug("roomId:" + roomId + ",uuid:" + outsInfo.winner + ",delegate close");
          continue;
        }
        logger.debug("roomId:" + roomId + ",outs info:" + outsInfo);
        long potId = allin_bets;
        long potBets = allinUsersMap.get(outsInfo.winner);
        // 检查这个边池的用户，之前是否购买过保险
        long potPreBuys = insuranceInfo.getUserBuyTotal(outsInfo.winner, potId);
        logger.debug("roomId" + roomId + ",potId" + potId + ",uuid:" + outsInfo.winner + ",prebuy:" + potPreBuys);
        if (potPreBuys == 0 && outsInfo.outs.length > OutsInfo.OUTS_LIMIT) {
          logger.debug("roomId:" + roomId + ",uuid:" + outsInfo.winner + ",outs too more");
          continue;
        }
        // 计算出这个玩家outs下的保险信息
        int potIndex = handInfo.getPotIdIndex(potId);
        outsInfo.setPayInfo(insuranceInfo.status, potId, potIndex, allin_pots, potPreBuys, potBets);
        // 保存这个玩家的保险信息
        insuranceInfo.setUserOuts(potId, outsInfo.winner, outsInfo);
        // 设置这个玩家的购买超时
        insurance_timer.setUserTimeout(roomId, outsInfo.winner, potId);
        // 这个领先玩家有保险
        insuranceInfo.subWinnerNoOutsTips(outsInfo.winner);
      }
    }
    // 返回提示保险信息
    // if (insuranceInfo.isOutsEmpty()) {
    // logger.debug("roomdId:" + roomId + ",outs null");
    // return null;
    // } else {
    logger.debug("roomdId:" + roomId + ",outs size:" + insuranceInfo.getAllOuts().size());
    return insuranceInfo;
    // }
  }

  // 内部使用
  public InsuranceInfo buyInsurance(long roomId, long uuid, long potId, long buys) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId:" + roomId + ",insurance info null");
      return null;
    }
    // 是否处于游戏暂停状态，否者购买行为无效
    if (insurance_timer.checkRoomPause(roomId)) {
      logger.debug("roomId:" + roomId + " pause,can not buy");
      return null;
    }
    // 能否扣币成功
    // if (!InsuranceApi.addUserPopularity(uuid, -buys)) {
    // logger.debug("roomId" + roomId + ",uuid:" + uuid + ",no enough popularitys");
    // return insuranceInfo;
    // }
    // 找到边池信息
    long finalBuys = insuranceInfo.setUserBuys(potId, uuid, buys);
    if (finalBuys > 0) {
      // 设置本手保险购买总额
      insuranceInfo.addUserBuyTotal(uuid, potId, finalBuys);
      // 去掉这个玩家的购买超时
      insurance_timer.removeUserTimeout(roomId, uuid, potId);
      // 写入账单
      // InsuranceApi.addInsuranceBuyin(roomId, uuid, finalBuys);
    } else {
      // 购买失败的情况下，退款
      // InsuranceApi.addUserPopularity(uuid, buys);
    }
    // 返回所有保险信息
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",potId:" + potId + ",buys:" + buys);
    return insuranceInfo;
  }

  // 内部使用，保险购买超时后，结算剩余玩家的强制
  public void foldInsurance(long roomId) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId" + roomId + ",insurance info null");
      return;
    }
    for (OutsInfo outsInfo : insuranceInfo.getAllOuts()) {
      if (outsInfo.fold || outsInfo.buy > 0) {
        continue;
      }
      // 进行弃牌操作
      foldInsurance(insuranceInfo, outsInfo.winner, outsInfo.potId);
    }
  }

  // 内部使用，这个用户购买超时后，开始结算
  public void flodInsurance(long roomId, long uuid) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId" + roomId + ",insurance info null");
      return;
    }
    for (OutsInfo outsInfo : insuranceInfo.getUserOuts(uuid)) {
      if (outsInfo.fold || outsInfo.buy > 0) {
        continue;
      }
      // 进行弃牌操作
      foldInsurance(insuranceInfo, outsInfo.winner, outsInfo.potId);
    }
  }

  // 内部使用，放弃购买保险
  public InsuranceInfo foldInsurance(long roomId, long uuid, long potId) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId" + roomId + ",insurance info null");
      return null;
    }
    // 去掉这个玩家的购买超时
    insurance_timer.removeUserTimeout(roomId, uuid, potId);
    // 保存放弃操作信息
    foldInsurance(insuranceInfo, uuid, potId);
    // 返回所有保险信息
    return insuranceInfo;
  }

  private void foldInsurance(InsuranceInfo insuranceInfo, long uuid, long potId) {
    // 保存放弃操作信息
    logger.debug("roomId:" + insuranceInfo.roomId + ",uuid:" + uuid + ",potId:" + potId + ",fold");
    long mustBuys = insuranceInfo.setUserFold(potId, uuid);
    if (mustBuys > 0) {
      logger.debug("roomId:" + insuranceInfo.roomId + ",uuid:" + uuid + ",potId:" + potId + ",can't fold,must buy min");
      // 强制购买
      insuranceInfo.setSysBuy(potId, uuid, mustBuys);
      // 设置本手保险购买总额
      insuranceInfo.addUserBuyTotal(uuid, potId, mustBuys);
      // 写入账单
      // InsuranceApi.addInsuranceBuyin(roomId, uuid, mustBuys);
    }
  }

  // 自动强制背保险
  public void forceInsurance(InsuranceInfo insuranceInfo, long uuid, long potId, long buys) {
    // 去掉这个玩家的购买超时
    insurance_timer.removeUserTimeout(insuranceInfo.roomId, uuid, potId);
    // 强制购买
    insuranceInfo.setSysBuy(potId, uuid, buys);
    // 设置本手保险购买总额
    insuranceInfo.addUserBuyTotal(uuid, potId, buys);
  }

  public void delayInsurance(long roomId, long uuid) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      return;
    }
    // 能否扣币成功
    long needBuy = insurance_delay.getUserDelayCost(roomId, uuid);
    // 扣币必须是负数！！！
    if (needBuy > 0 && !InsuranceApi.subUserCoin(uuid, needBuy)) {
      logger.debug("roomId" + roomId + ",uuid:" + uuid + ",no enough coins");
      return;
    }
    // 重新设置这个玩家的购买超时
    insurance_timer.addUserTimeout(roomId, uuid);
    // 设置玩家的购买次数
    insurance_delay.addUserDelayTimes(roomId, uuid);
  }

  // 房主控制保险
  public void openInsuranceByCreater(long roomId, long uuid, boolean close) {
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",close:" + close);
    insurance_open.setCreaterCloseStatus(roomId, uuid, close);
  }

  // 玩家控制保险
  public boolean openInsuranceByUser(long roomId, long uuid, boolean close) {
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",close:" + close);
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.debug("roomId:" + roomId + ",no insurance info");
      // 第一手信息初始化问题
      insurance_open.setUserOpenStatus(roomId, uuid, close);
      return true;
    }
    // 检查这一手是否有保险买入
    if (close) {
      // 买入则不让关闭保险
      // insuranceInfo.getUserAllBuyTotal(uuid) > 0
      // 只要本手触发保险就不准关闭了
      if (insuranceInfo.checkHandOcur()) {
        return false;
      } else {
        insurance_open.setUserOpenStatus(roomId, uuid, close);
        return true;
      }
    } else {
      // 恢复保险没有限制
      insurance_open.setUserOpenStatus(roomId, uuid, close);
      return true;
    }
  }

  // 获取玩家保险开关状态
  public boolean getUserCloseStatus(long roomId, long uuid) {
    return insurance_open.getUserCloseStatus(roomId, uuid);
  }

  // 控制玩家托管状态
  public void setUserDelegateStatus(long roomId, long uuid, boolean open) {
    if (open) {
      insurance_open.openUserDelegate(roomId, uuid);
    } else {
      insurance_open.closeUserDelegate(roomId, uuid);
    }
  }

  // 暂停保险超时逻辑
  public void setInsurancePause(long roomId) {
    insurance_timer.onRoomPause(roomId);
  }

  // 恢复保险超时逻辑
  public void setInsurancePlay(long roomId) {
    insurance_timer.onRoomPlay(roomId);
  }

  // 内部调用，结算购买的保险
  public InsuranceInfo settleInsurance(long roomId) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",nu hand info");
      return null;
    }
    return settleInsurance(handInfo);
  }

  private InsuranceInfo settleInsurance(HandInfo handInfo) {
    // 找出那一桌保险信息
    long roomId = handInfo.roomId;
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId:" + roomId + ",no insurance info");
      return null;
    }
    // 找到所有用户的购买信息
    int tableCard = handInfo.getTableCard();
    logger.debug("roomId:" + roomId + ",tablecard:" + tableCard);
    List<OutsInfo> userBuyInfos = insuranceInfo.getAllBuys();
    for (OutsInfo outsInfo : userBuyInfos) {
      logger.debug("roomId:" + roomId + "," + outsInfo);
      // 检查这个保险是否买中
      if (outsInfo.checkBuyWin(tableCard)) {
        logger.debug("roomId:" + roomId + ",result:" + outsInfo.result + ",outs hit!!!");
        insuranceInfo.setShopTotal(outsInfo.buy, outsInfo.result);
        insuranceInfo.addUserResultTotal(outsInfo.winner, outsInfo.result);
        // API接口，调用用户结算接口
        // InsuranceApi.addInsuranceGetStack(roomId, outsInfo.winner, outsInfo.result);
      } else {
        // 扣住买保险的钱
        logger.debug("roomId:" + roomId + ",outs miss...");
        insuranceInfo.setShopTotal(outsInfo.buy, 0);
      }
    }
    // API接口，保险实时赔付账单信息
    // InsuranceApi.setInsuranceGame(roomId, insuranceInfo.getShopTotal());
    // 清空用户的没谈保险提示
    insuranceInfo.clearWinnerNoOutsTip();
    // 返回所有保险结算结果
    return insuranceInfo;
  }

  // 主要用于最后一个人购买或者放弃时调用，用于解耦
  public void checkRoomTimeout(long roomId) {
    insurance_timer.checkRoomTimeout(roomId);
  }

  // 通知这个用户，购买超时了
  public void notifyUserTimeout(long roomId, long uuid) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      logger.error("roomId" + roomId + ",insurance info null");
      return;
    }
    // 调用游戏层
    InsuranceApi.notifyUserBuyTimeout(roomId, uuid, insuranceInfo);
  }

  // 获取用户超时时间
  public InsuranceTimer.InsuranceTimeInfo getUserTimeout(long roomId, long uuid) {
    return insurance_timer.getUserTimeout(roomId, uuid);
  }

  public int getUserDelayCost(long roomId, long uuid) {
    return insurance_delay.getUserDelayCost(roomId, uuid);
  }

  public InsuranceInfo getInsurance(long roomId) {
    return insurances_map.get(roomId);
  }

  // 外部使用，查询是否需要购买保险
  public InsuranceInfo checkInsurance(long roomId, long handId, int roundStatus) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",handinfo null");
      return null;
    }
    // 检查手数状态是否正确
    if (handInfo.handId != handId || handInfo.gameStatus != roundStatus) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",gamestatus:" + handInfo.gameStatus + ",state error");
      return null;
    }
    return checkInsurance(handInfo);
  }

  // 外部使用，购买pot保险
  public InsuranceInfo buyInsurance(long roomId, long handId, int roundStatus, long uuid, long potId, long buys) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",handinfo null");
      return null;
    }
    // 检查手数状态是否正确
    if (handInfo.handId != handId || handInfo.gameStatus != roundStatus) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",gamestatus:" + handInfo.gameStatus + ",state error");
      return null;
    }
    return buyInsurance(roomId, uuid, potId, buys);
  }

  // 外部使用，放弃购买保险
  public InsuranceInfo foldInsurance(long roomId, long handId, int roundStatus, long uuid, long potId) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",handinfo null");
      return null;
    }
    // 检查手数状态是否正确
    if (handInfo.handId != handId || handInfo.gameStatus != roundStatus) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",gamestatus:" + handInfo.gameStatus + ",state error");
      return null;
    }
    return foldInsurance(roomId, uuid, potId);
  }

  // 外部使用，延时购买保险
  public void delayInsurance(long roomId, long handId, int roundStatus, long uuid) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",handinfo null");
      return;
    }
    // 检查手数状态是否正确
    if (handInfo.handId != handId || handInfo.gameStatus != roundStatus) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",roundstatus:" + roundStatus + ",gamestatus:" + handInfo.gameStatus + ",state error");
      return;
    }
    delayInsurance(roomId, uuid);
  }

  // 外部使用，结算购买的保险
  public InsuranceInfo settleInsurance(long roomId, long handId, int tableCard) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",handinfo null");
      return null;
    }
    // 检查手数状态是否正确
    if (handInfo.handId != handId) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",handId errors");
      return null;
    }
    // 检查公共牌是否正确
    if (!handInfo.checkTableCard(tableCard)) {
      logger.error("roomId:" + roomId + ",tableCard error:" + tableCard);
      return null;
    }
    return settleInsurance(handInfo);
  }

  // 外部使用，获取当前手数的保险信息
  public InsuranceInfo getInsurance(long roomId, long handId) {
    InsuranceInfo info = insurances_map.get(roomId);
    if (info == null) {
      logger.error("roomId:" + roomId + ",handId:" + handId + ",info null");
      return null;
    }
    // 检查手数状态是否正确
    if (info.handId != handId) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",handId errors");
      return null;
    }
    return info;
  }
}
