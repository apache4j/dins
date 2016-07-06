/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceManager.java
 * 创建人:xichen
 * 创建日期:2016年6月30日-下午2:44:22
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名称:InsuranceManager
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月30日-下午2:44:22
 * 修改备注:
 */
public class InsuranceManager {

  // 所有房间的保险信息
  private final Map<Long, InsuranceInfo> insurances_map = new HashMap<Long, InsuranceInfo>();

  // 购买保险的定时器
  private final InsuranceTimer insurance_timer = new InsuranceTimer();

  // 购买保险的开关
  private final InsuranceLimit insurance_open = new InsuranceLimit();

  private static InsuranceManager instance = new InsuranceManager();

  public static InsuranceManager getInstance() {
    return instance;
  }

  // 解析游戏数据
  public void onGameAction(String tableLog) {
    InsuranceLog.onLogMessage(tableLog);
  }

  // 查询是否需要购买保险
  public InsuranceInfo checkInsurance(long roomId) {
    // 检查手数信息
    HandInfo handInfo = HandsManager.getInstance().getHandInfo(roomId);
    if (handInfo == null) {
      return null;
    }
    // 检查筹码信息
    handInfo.checkHandState();
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      insuranceInfo = new InsuranceInfo();
      insuranceInfo.roomId = roomId;
      insuranceInfo.handId = handInfo.handId;
      insuranceInfo.status = handInfo.status;
      insurances_map.put(roomId, insuranceInfo);
    }
    // 刷新这一桌的保险信息
    insuranceInfo.refreshRoundState(handInfo.status);
    // 遍历每一个边池
    for (Map.Entry<Integer, Integer> allinPotEntry : handInfo.allin_pots.entrySet()) {
      // allin_bets定义为potId
      int allin_bets = allinPotEntry.getKey();
      int allin_pots = allinPotEntry.getValue();
      // 遍历这个边池的玩家列表
      Map<Long, Integer> allinUsersMap = handInfo.allin_pots_user.get(allin_bets);
      List<HandUserInfo> allinUserInfos = new ArrayList<HandUserInfo>();
      for (Long uuid : allinUsersMap.keySet()) {
        allinUserInfos.add(handInfo.getUser(uuid));
      }
      // 找出这个边池玩家的out保险
      List<OutsInfo> outsList = OutsCheck.findOutsInPlayers(allinUserInfos, handInfo.tables);
      if (outsList == null || outsList.size() == 0) {
        continue;
      }
      // 设置这个边池信息
      insuranceInfo.setPotInfo(allin_bets, allin_pots, allinUsersMap.keySet());
      // 设置用户的保险购买信息
      for (OutsInfo outsInfo : outsList) {
        // 是否开启保险购买逻辑
        if (insurance_open.checkUserCloseInsurance(roomId, outsInfo.winner)) {
          continue;
        }
        long potId = allin_bets;
        long potBets = allinUsersMap.get(outsInfo.winner);
        long preBuys = insuranceInfo.getUserTotal(outsInfo.winner);
        // 计算出这个玩家outs下的保险信息
        outsInfo.setPayInfo(potId, allin_pots, preBuys, potBets);
        // 保存这个玩家的保险信息
        insuranceInfo.setUserOuts(allin_bets, outsInfo.winner, outsInfo);
        // 设置这个玩家的购买超时
        insurance_timer.setUserTimeout(roomId, outsInfo.winner, potId);
      }
    }
    // 返回提示保险信息
    return insuranceInfo;
  }

  // 购买pot保险
  public InsuranceInfo buyInsurance(long roomId, long uuid, long potId, long buys) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      return null;
    }
    // 找到边池信息
    insuranceInfo.setUserBuys(potId, uuid, buys);
    // 设置本手保险购买总额
    insuranceInfo.addUserTotal(uuid, buys);
    // 去掉这个玩家的购买超时
    insurance_timer.removeUserTimeout(roomId, uuid, potId);
    // 写入账单
    InsuranceApi.addInsuranceBuyin(roomId, uuid, buys);
    // 返回所有保险信息
    return insuranceInfo;
  }

  // 放弃购买保险
  public InsuranceInfo foldInsurance(long roomId, long uuid, long potId) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      return null;
    }
    // 去掉这个玩家的购买超时
    insurance_timer.removeUserTimeout(roomId, uuid, potId);
    // 保存放弃操作信息
    insuranceInfo.setUserFold(potId, uuid);
    // 返回所有保险信息
    return insuranceInfo;
  }

  // 延时购买保险
  public void delayInsurance(long roomId, long uuid) {
    // 检查保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      return;
    }
    // 重新设置这个玩家的购买超时
    insurance_timer.addUserTimeout(roomId, uuid);
  }

  // 房主控制保险
  public void openInsuranceByCreater(long roomId, long uuid, boolean close) {
    insurance_open.setCreaterOpenStatus(roomId, uuid, close);
  }

  // 玩家控制保险
  public void openInsuranceByUser(long roomId, long uuid, boolean close) {
    insurance_open.setUserOpenStatus(roomId, uuid, close);
  }

  // 结算购买的保险
  public InsuranceInfo settleInsurance(long roomId, int tableCard) {
    // 检查公共牌是否正确
    if (!HandsManager.getInstance().checkTableCard(roomId, tableCard)) {
      return null;
    }
    // 找出那一桌保险信息
    InsuranceInfo insuranceInfo = insurances_map.get(roomId);
    if (insuranceInfo == null) {
      return null;
    }
    // 找到所有用户的购买信息
    List<OutsInfo> userBuyInfos = insuranceInfo.getUserBuys();
    for (OutsInfo outsInfo : userBuyInfos) {
      // 检查这个保险是否买中
      if (outsInfo.checkBuyWin(tableCard)) {
        // API接口，调用用户结算接口
        InsuranceApi.addInsuranceGetStack(roomId, outsInfo.winner, outsInfo.result);
      } else {
        continue;
      }
    }
    // 返回所有保险结算结果
    return insuranceInfo;
  }

}
