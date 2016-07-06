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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 类名称:InsuranceInfo
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月30日-下午2:17:19
 * 修改备注:
 */
public class InsuranceInfo {

  // 房间id
  public long roomId;
  // 本手id
  public long handId;
  // 当前游戏状态
  public int status;

  // 用户保险总投入
  public Map<Long, Long> user_totals = new HashMap<Long, Long>();

  // 用户当前这一轮所有边池的保险信息
  // public Map<String, OutsInfo> user_outs = new HashMap<String, OutsInfo>();

  // 用户当前这一轮的保险购买信息
  // public Map<String, OutsInfo> user_buys = new HashMap<String, OutsInfo>();

  public Map<Long, Map<Long, OutsInfo>> pot_user_outs = new HashMap<Long, Map<Long, OutsInfo>>();

  public Map<Long, Long> pot_stacks = new HashMap<Long, Long>();
  public Map<Long, Set<Long>> pot_users = new HashMap<Long, Set<Long>>();

  // 清除上一轮的保险信息
  public void refreshRoundState(int status) {
    if (this.status != status) {
      // 状态更新了
      this.status = status;
      // user_outs.clear();
      // user_buys.clear();
      pot_user_outs.clear();
      pot_stacks.clear();
      pot_users.clear();
    }
  }

  // 设置边池信息
  public void setPotInfo(long potId, long potStacks, Collection<Long> users) {
    pot_stacks.put(potId, potStacks);
    pot_users.put(potId, new HashSet<>(users));
  }

  // 用户保险记录操作
  public void setUserOuts(long potId, long uuid, OutsInfo info) {
    // user_outs.put(getPotUserKey(potId, uuid), info);
    Map<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      userOuts = new HashMap<Long, OutsInfo>();
      pot_user_outs.put(potId, userOuts);
    }
    userOuts.put(uuid, info);
  }

  public OutsInfo getUserOuts(long potId, long uuid) {
    // return user_outs.get(getPotUserKey(potId, uuid));
    Map<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return null;
    }
    return userOuts.get(uuid);
  }

  // 设置用户购买信息
  public void setUserBuys(long potId, long uuid, long buys) {
    Map<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return;
    }
    userOuts.get(uuid).setBuyInfo(buys);
  }

  // 设置用户放弃
  public void setUserFold(long potId, long uuid) {
    Map<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
    if (userOuts == null) {
      return;
    }
    userOuts.get(uuid).setFold();
  }

  // 获取用户购买的保险信息
  public List<OutsInfo> getUserBuys() {
    List<OutsInfo> buyInfos = new ArrayList<OutsInfo>();
    for (Long potId : pot_user_outs.keySet()) {
      Map<Long, OutsInfo> userOuts = pot_user_outs.get(potId);
      for (OutsInfo info : userOuts.values()) {
        if (info.buy > 0) {
          buyInfos.add(info);
        }
      }
    }
    // return user_buys.values();
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
    // 设置这个边池的保险购买
    // user_buys.put(getPotUserKey(potId, uuid), info);
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
    return user_totals.get(uuid);
  }

  // 保险表里面的key值信息连接副
  // private static final String KEY_SPLIT = ":";

  // private String getPotUserKey(long potId, long uuid) {
  // return String.valueOf(potId) + KEY_SPLIT + String.valueOf(uuid);
  // }

  // private int getPotIdFromKey(String key) {
  // return Integer.valueOf(key.split(KEY_SPLIT)[0]);
  // }
  //
  // private long getUuidFromKey(String key) {
  // return Long.valueOf(key.split(KEY_SPLIT)[1]);
  // }

}
