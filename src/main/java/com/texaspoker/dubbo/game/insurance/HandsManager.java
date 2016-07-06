/**
 * 包名:com.texaspoker.dubbo.game.event
 * 文件名:handsManager.java
 * 创建人:xichen
 * 创建日期:2016年6月27日-下午5:37:57
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.HashMap;
import java.util.Map;

/**
 * 类名称:handsManager
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月27日-下午5:37:57
 * 修改备注:
 */
public class HandsManager {

  private final static Map<Long, HandInfo> hands_map = new HashMap<Long, HandInfo>();

  private static HandsManager instance = new HandsManager();

  public static HandsManager getInstance() {
    return instance;
  }

  // 新一手牌局
  public void setHandOpen(long roomId, long handId) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      handInfo = new HandInfo();
      handInfo.roomId = roomId;
      hands_map.put(roomId, handInfo);
    }
    handInfo.handId = handId;
    handInfo.setStatus(HandUtils.PRE_FLOP);
  }

  public void setHandClose(long roomId, long handId) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return;
    }
    handInfo.clear();
  }

  // 获取这一个手数信息
  public HandInfo getHandInfo(long roomId) {
    return hands_map.get(roomId);
  }

  public void delHandInfo(long roomId) {
    hands_map.remove(roomId);
  }

  public void setUserBets(long roomId, long uuid, int bets) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return;
    }
    HandUserInfo userInfo = handInfo.getUser(uuid);
    if (userInfo == null) {
      return;
    }
    // 纪录总筹码量
    userInfo.betChip += bets;
    // 是否allin
    if (userInfo.betChip == userInfo.handChip) {
      userInfo.allin = true;
    }
    // 记录每一轮筹码量
    handInfo.setRoundUserBets(uuid, bets);
  }

  public void setUserFold(long roomId, long uuid) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return;
    }
    HandUserInfo userInfo = handInfo.getUser(uuid);
    if (userInfo == null) {
      return;
    }
    userInfo.fold = false;
    // 记录玩家弃牌操作
    handInfo.setRoundUserFlod(uuid);
  }

  public void setUserCards(long roomId, long uuid, int[] hands, int handChip) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return;
    }
    if (hands.length != 2) {
      return;
    }
    HandUserInfo userInfo = new HandUserInfo();
    userInfo.uuid = uuid;
    userInfo.hands[0] = hands[0];
    userInfo.hands[1] = hands[1];
    userInfo.handChip = handChip;
    handInfo.setUser(userInfo);
  }

  public void setTableCards(long roomId, int status, int tables) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return;
    }
    // 设置状态
    handInfo.setStatus(status);
    // 设置公共牌
    if (status == HandUtils.FLOP1) {
      handInfo.tables[0] = tables;
    } else if (status == HandUtils.FLOP2) {
      handInfo.tables[1] = tables;
    } else if (status == HandUtils.FLOP3) {
      handInfo.tables[2] = tables;
    } else if (status == HandUtils.TURN) {
      handInfo.tables[3] = tables;
    } else if (status == HandUtils.RIVER) {
      handInfo.tables[4] = tables;
    }
  }

  // 检查当前状态的公共牌
  public boolean checkTableCard(long roomId, int tableCard) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      return false;
    }
    return handInfo.checkTableCard(tableCard);
  }

}
