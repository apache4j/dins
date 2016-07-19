/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceInfo.java
 * 创建人:xichen
 * 创建日期:2016年6月30日-下午2:17:19
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.Collection;
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

  // 房间id
  public long roomId;
  // 本手id
  public long handId;
  // 当前游戏状态
  public int status;

  // 代理人赔付金额
  public long proxy_shop;

  // 用户本手的保险总投入
  public ConcurrentMap<Long, ConcurrentMap<Long, Long>> hand_user_totals =
      new ConcurrentHashMap<Long, ConcurrentMap<Long, Long>>();

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
      hand_user_totals.clear();
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
    }
  }

  // 设置边池信息
  public void setPotInfo(long potId, long potStacks, Collection<Long> users) {
    // pot_stacks.put(potId, potStacks);
    // pot_users.put(potId, new HashSet<>(users));
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
  }

  public OutsInfo getUserOuts(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      return null;
    }
    return userOuts.get(uuid);
  }

  // 设置用户购买信息
  public boolean setUserBuys(long potId, long uuid, long buys) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      return false;
    }
    if (!userOuts.containsKey(uuid)) {
      return false;
    }
    // 这个边池已经购买过了！！！
    OutsInfo info = userOuts.get(uuid);
    if (info.getBuy() > 0) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",potId" + potId + ",uuid:" + uuid + ",has already buy "
          + buys);
      return false;
    }
    // 这是购买金额
    info.setBuy(buys);
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
    return true;
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
  public void setUserFold(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = round_pot_user_outs.get(potId);
    if (userOuts == null) {
      return;
    }
    if (!userOuts.containsKey(uuid)) {
      return;
    }
    userOuts.get(uuid).setFold();
  }

  // 用户购买操作
  public void addUserTotal(long uuid, long potId, long buy) {
    // 保险总投入
    ConcurrentMap<Long, Long> potTotals = hand_user_totals.get(uuid);
    if (potTotals == null) {
      potTotals = new ConcurrentHashMap<Long, Long>();
      hand_user_totals.put(uuid, potTotals);
    }
    Long totals = potTotals.get(potTotals);
    if (totals == null) {
      totals = Long.valueOf(buy);
    } else {
      totals += buy;
    }
    potTotals.put(uuid, totals);
  }

  // 获取用户保险投入
  public long getUserTotal(long uuid, long potId) {
    ConcurrentMap<Long, Long> potTotals = hand_user_totals.get(uuid);
    if (potTotals == null) {
      return 0;
    }
    Long totals = potTotals.get(potId);
    if (totals == null) {
      return 0;
    }
    return potTotals.get(uuid);
  }

  // 设置牌局的保险实时赔付情况
  public void setShopTotal(long buy, long result) {
    proxy_shop += buy;
    proxy_shop -= result;
  }

  public long getShopTotal() {
    return this.proxy_shop;
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
