/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceInfo.java
 * 创建人:xichen
 * 创建日期:2016年6月30日-下午2:17:19
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * 类名称:InsuranceInfo
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月30日-下午2:17:19
 * 修改备注:
 */
public class InsuranceInfo {

  private static Logger logger = Logger.getLogger(InsuranceInfo.class);
  // 群组id
  public long groupId;
  // 房间id
  public long roomId;
  // 本手id
  public long handId;
  // 当前游戏状态
  public int status;

  // 代理人赔付金额
  public long proxy_shop;

  // 用户本手的保险总投入
  public ConcurrentMap<Long, ConcurrentMap<Long, Long>> hand_user_buy_totals =
      new ConcurrentHashMap<Long, ConcurrentMap<Long, Long>>();

  // 用户本手的保险总收益
  public ConcurrentMap<Long, Long> hand_user_result_totals = new ConcurrentHashMap<Long, Long>();

  // 用户本手的保险触发情况
  public ConcurrentMap<Long, Integer> hand_user_ocur_state = new ConcurrentHashMap<Long, Integer>();

  // 用户本手的每一轮购买记录
  // public ConcurrentMap<Integer, ConcurrentMap<Long, List<OutsInfo>>> hand_user_buys =
  // new ConcurrentHashMap<Integer, ConcurrentMap<Long, List<OutsInfo>>>();

  // 边池的用户outs信息
  public ConcurrentMap<Long, ConcurrentMap<Long, OutsInfo>> round_pot_user_outs =
      new ConcurrentHashMap<Long, ConcurrentMap<Long, OutsInfo>>();

  // 用户的所有outs
  public ConcurrentMap<Long, List<OutsInfo>> round_user_outs = new ConcurrentHashMap<Long, List<OutsInfo>>();

  // 清除上一轮的保险信息
  public void refreshRoundState(long handId, int status) {
    logger.debug("handId:" + handId + ",status:" + status);
    if (this.handId != handId) {
      // 手数更新
      this.handId = handId;
      hand_user_buy_totals.clear();
      hand_user_result_totals.clear();
      hand_user_ocur_state.clear();
      // 状态更新
      this.status = status;
      round_pot_user_outs.clear();
      round_user_outs.clear();
      logger.debug("roomId:" + roomId + ",handId:" + handId + ",refresh");
    } else if (this.status != status) {
      // 仅仅状态更新
      this.status = status;
      round_pot_user_outs.clear();
      round_user_outs.clear();
      logger.debug("roomId:" + roomId + ",status:" + status + ",refresh");
    } else {
      logger.debug("roomId:" + roomId + ",handId:" + this.handId + ",status:" + this.status + ",init");
    }
  }

  // 清除上一手的保险信息
  public void refreshHandState(long handId) {
    logger.debug("handId:" + handId);
    // 清空最后一轮数据
    round_pot_user_outs.clear();
    round_user_outs.clear();
    // 清空这一手数据
    hand_user_buy_totals.clear();
    hand_user_result_totals.clear();
    hand_user_ocur_state.clear();
  }

  // 设置边池信息
  public boolean isOutsEmpty() {
    return round_pot_user_outs.isEmpty();
  }

  // 用户保险记录操作
  public void setUserOuts(long potId, long uuid, OutsInfo info) {
    // 边池的outs信息
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      userOuts = new ConcurrentHashMap<Long, OutsInfo>();
      round_pot_user_outs.put(potId, userOuts);
    }
    userOuts.put(uuid, info);
    // 用户的outs信息
    List<OutsInfo> outsInfos = round_user_outs.get(uuid);
    if (outsInfos == null) {
      outsInfos = Collections.synchronizedList(new ArrayList<OutsInfo>());
      round_user_outs.put(uuid, outsInfos);
    }
    outsInfos.add(info);
    // 记录用户触发信息
    setUserOcur(uuid);
  }

  public OutsInfo getUserOuts(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      return null;
    }
    return userOuts.get(uuid);
  }

  // 设置用户购买信息
  public long setUserBuys(long potId, long uuid, long buys) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      logger.error("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",no potId outs info");
      return 0;
    }
    if (!userOuts.containsKey(uuid)) {
      logger.error("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",no uuid outs info");
      return 0;
    }
    // 这个边池已经购买过了！！！
    OutsInfo info = userOuts.get(uuid);
    if (info.getBuy() > 0) {
      logger.error("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",has already buy " + buys);
      return 0;
    }
    long finalBuys = buys;
    if (buys < info.pay_min) {
      logger.error("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",must>min_pay:" + info.pay_min);
      finalBuys = info.pay_min;
    }
    if (buys > info.pay_max) {
      logger.error("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",must<max_pay:" + info.pay_max);
      finalBuys = info.pay_max;
    }
    // 这是购买金额
    info.setBuy(finalBuys);
    logger.debug("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",buys:" + finalBuys);
    // 设置用户购买记录
    // ConcurrentMap<Long, List<OutsInfo>> roundUserBuys = hand_user_buys.get(status);
    // if (roundUserBuys == null) {
    // roundUserBuys = new ConcurrentHashMap<Long, List<OutsInfo>>();
    // hand_user_buys.put(status, roundUserBuys);
    // }
    // List<OutsInfo> buyInfos = roundUserBuys.get(uuid);
    // if (buyInfos == null) {
    // buyInfos = Collections.synchronizedList(new ArrayList<OutsInfo>());
    // roundUserBuys.put(uuid, buyInfos);
    // }
    // buyInfos.add(info);
    return finalBuys;
  }

  // 检查用户之前是否购买过
  // public long checkUserPreviousBuy(long potId, long uuid) {
  // ConcurrentMap<Long, List<OutsInfo>> roundUserBuys = hand_user_buys.get(status - 1);
  // if (roundUserBuys == null) {
  // return 0;
  // }
  // List<OutsInfo> buyInfos = roundUserBuys.get(uuid);
  // if (buyInfos == null) {
  // return 0;
  // }
  // for (OutsInfo info : buyInfos) {
  // if (info.potId == potId && info.winner == uuid) {
  // return info.buy;
  // }
  // }
  // return 0;
  // }

  // 设置用户放弃
  public long setUserFold(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      logger.error("roomId" + roomId + ",potId" + potId + ",uuid:" + uuid + ",no potId outs info");
      return 0;
    }
    if (!userOuts.containsKey(uuid)) {
      logger.error("roomId" + roomId + ",potId" + potId + ",uuid:" + uuid + ",no user outs info");
      return 0;
    }
    // 是否需要强制购买保险
    OutsInfo info = userOuts.get(uuid);
    if (info.pay_min > 0) {
      return info.pay_min;
    } else {
      info.setFold();
    }
    return 0;
  }

  // 用户购买操作
  public void addUserBuyTotal(long uuid, long potId, long buy) {
    // 保险总投入
    ConcurrentMap<Long, Long> potTotals = hand_user_buy_totals.get(uuid);
    if (potTotals == null) {
      potTotals = new ConcurrentHashMap<Long, Long>();
      hand_user_buy_totals.put(uuid, potTotals);
    }
    Long totals = potTotals.get(potId);
    if (totals == null) {
      totals = buy;
    } else {
      totals += buy;
    }
    potTotals.put(potId, totals);
    logger.debug("roomId:" + roomId + ",potId:" + potId + ",uuid:" + uuid + ",total:" + totals);
  }

  // 获取用户保险投入
  public long getUserBuyTotal(long uuid, long potId) {
    ConcurrentMap<Long, Long> potTotals = hand_user_buy_totals.get(uuid);
    if (potTotals == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",potId:" + potId + ",no user info");
      return 0;
    }
    Long totals = potTotals.get(potId);
    if (totals == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",potId:" + potId + ",no pot info");
      return 0;
    }
    return totals;
  }

  // 获取这个用户的保险总投入
  public long getUserAllBuyTotal(long uuid) {
    ConcurrentMap<Long, Long> potTotals = hand_user_buy_totals.get(uuid);
    if (potTotals == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",no user info");
      return 0;
    }
    long totals = 0;
    for (Long bets : potTotals.values()) {
      totals += bets;
    }
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",total:" + totals);
    return totals;
  }

  // 用户保险的赔付信息
  public void addUserResultTotal(long uuid, long result) {
    // 保险总投入
    Long totals = hand_user_result_totals.get(uuid);
    if (totals == null) {
      totals = result;
    } else {
      totals += result;
    }
    hand_user_result_totals.put(uuid, totals);
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",total:" + totals);
  }

  // 获取用户保险的所有赔付总额
  public long getUserAllResultTotal(long uuid) {
    Long totals = hand_user_result_totals.get(uuid);
    if (totals == null) {
      return 0;
    }
    return totals;
  }

  // 设置牌局的保险实时赔付情况
  public void setShopTotal(long buy, long result) {
    logger.debug("roomId:" + roomId + ",handId:" + handId + ",buy:" + buy + ",result:" + result);
    proxy_shop += buy;
    proxy_shop -= result;
  }

  public long getShopTotal() {
    logger.debug("roomId:" + roomId + ",handId:" + handId + ",proxy:" + proxy_shop);
    return this.proxy_shop;
  }

  public void setUserOcur(long uuid) {
    if (hand_user_ocur_state.containsKey(uuid)) {
      return;
    }
    hand_user_ocur_state.put(uuid, status);
  }

  public boolean checkFirstOcur(long uuid) {
    Integer status = hand_user_ocur_state.get(uuid);
    if (status == null) {
      return true;
    } else if (status == this.status) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * 工具函数，与业务逻辑无关
   */

  // 获取用户购买的保险信息
  public List<OutsInfo> getAllBuys() {
    List<OutsInfo> buyInfos = new ArrayList<OutsInfo>();
    for (Long potId : round_pot_user_outs.keySet()) {
      ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
      for (OutsInfo info : userOuts.values()) {
        if (info.buy > 0) {
          buyInfos.add(info);
        }
      }
    }
    return buyInfos;
  }

  // 获取所有保险信息
  public List<OutsInfo> getAllOuts() {
    List<OutsInfo> allInfos = new ArrayList<OutsInfo>();
    for (Long potId : round_pot_user_outs.keySet()) {
      allInfos.addAll(round_pot_user_outs.get(potId).values());
    }
    return allInfos;
  }

  // 打印所有outs信息
  public void printOutsInfo() {
    List<OutsInfo> infos = getAllOuts();
    for (OutsInfo info : infos) {
      logger.debug("status:" + status + "," + info);
    }
  }

  // 打印所有outs信息
  public void printBuysInfo() {
    List<OutsInfo> infos = getAllBuys();
    for (OutsInfo info : infos) {
      logger.debug("status:" + status + "," + info);
    }
  }

}
