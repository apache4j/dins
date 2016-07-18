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

  // 用户保险总投入
  public ConcurrentMap<Long, Long> user_totals = new ConcurrentHashMap<Long, Long>();

  // 边池的用户outs信息
  public ConcurrentMap<Long, ConcurrentMap<Long, OutsInfo>> pot_user_outs =
      new ConcurrentHashMap<Long, ConcurrentMap<Long, OutsInfo>>();

  // 用户的所有outs
  public ConcurrentMap<Long, List<OutsInfo>> user_outs = new ConcurrentHashMap<Long, List<OutsInfo>>();

  // 清除上一轮的保险信息
  public void refreshRoundState(int status) {
    logger.debug("status:" + status);
    if (this.status != status) {
      // 状态更新了
      this.status = status;
      pot_user_outs.clear();
      user_outs.clear();
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
    ConcurrentMap<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      userOuts = new ConcurrentHashMap<Long, OutsInfo>();
      pot_user_outs.put(potId, userOuts);
    }
    userOuts.put(uuid, info);
    // 用户的outs信息
    List<OutsInfo> outsInfos = user_outs.get(uuid);
    if (outsInfos == null) {
      outsInfos = Collections.synchronizedList(new ArrayList<OutsInfo>());
      user_outs.put(uuid, outsInfos);
    }
    outsInfos.add(info);
  }

  public OutsInfo getUserOuts(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return null;
    }
    return userOuts.get(uuid);
  }

  // 设置用户购买信息
  public boolean setUserBuys(long potId, long uuid, long buys) {
    ConcurrentMap<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return false;
    }
    if (!userOuts.containsKey(uuid)) {
      return false;
    }
    // 这个边池已经购买过了！！！
    if (userOuts.get(uuid).getBuy() > 0) {
      logger.error("roomId" + roomId + ",handId:" + handId + ",potId" + potId + ",uuid:" + uuid + ",has already buy "
          + buys);
      return false;
    }
    userOuts.get(uuid).setBuy(buys);
    return true;
  }

  // 设置用户放弃
  public void setUserFold(long potId, long uuid) {
    ConcurrentMap<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return;
    }
    if (!userOuts.containsKey(uuid)) {
      return;
    }
    userOuts.get(uuid).setFold();
  }

  // 获取用户购买的保险信息
  public List<OutsInfo> getUserBuys() {
    List<OutsInfo> buyInfos = new ArrayList<OutsInfo>();
    for (Long potId : pot_user_outs.keySet()) {
      ConcurrentMap<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
      for (OutsInfo info : userOuts.values()) {
        if (info.buy > 0) {
          buyInfos.add(info);
        }
      }
    }
    return buyInfos;
  }

  // 获取所有保险信息
  public List<OutsInfo> getUserOuts() {
    List<OutsInfo> allInfos = new ArrayList<OutsInfo>();
    for (Long potId : pot_user_outs.keySet()) {
      allInfos.addAll(pot_user_outs.get(potId).values());
    }
    return allInfos;
  }

  // 用户购买操作
  public void addUserTotal(long uuid, long buy) {
    // 保险总投入
    Long totals = user_totals.get(uuid);
    if (totals == null) {
      totals = Long.valueOf(buy);
    } else {
      totals += buy;
    }
    user_totals.put(uuid, totals);
  }

  // 获取用户保险投入
  public long getUserTotal(long uuid) {
    Long totals = user_totals.get(uuid);
    if (totals == null) {
      return 0;
    }
    return user_totals.get(uuid);
  }

  // 打印所有outs信息
  public void printOutsInfo() {
    List<OutsInfo> infos = getUserOuts();
    for (OutsInfo info : infos) {
      logger.debug("status:" + status + "," + info);
    }
  }

  // 打印所有outs信息
  public void printBuysInfo() {
    List<OutsInfo> infos = getUserBuys();
    for (OutsInfo info : infos) {
      logger.debug("status:" + status + "," + info);
    }
  }

}
