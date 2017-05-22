/**
 * 包名:com.texaspoker.dubbo.game.event
 * 文件名:handsManager.java
 * 创建人:xichen
 * 创建日期:2016年6月27日-下午5:37:57
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * 类名称:handsManager
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月27日-下午5:37:57
 * 修改备注:
 */
public class HandsManager {

  private static Logger logger = Logger.getLogger(HandsManager.class);

  private final ConcurrentMap<Long, HandInfo> hands_map = new ConcurrentHashMap<Long, HandInfo>();

  private static HandsManager instance = new HandsManager();

  public static HandsManager getInstance() {
    return instance;
  }

  // 新一手牌局
  public void setHandOpen(long groupId, long roomId, long handId) {
//    logger.debug("roomId:" + roomId + ",handId:" + handId);
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      handInfo = new HandInfo();
      handInfo.groupId = groupId;
      handInfo.roomId = roomId;
      hands_map.put(roomId, handInfo);
    }
    handInfo.handId = handId;
    handInfo.setGameStatus(HandUtils.PRE_FLOP);
  }

  public void setHandClose(long roomId, long handId) {
//    logger.debug("roomId:" + roomId + ",handId:" + handId);
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",handId:" + handId + ",info null");
      return;
    }
    // 调用保险结算逻辑
    InsuranceManager.getInstance().onHandCloseAction(roomId, handId);
    // 清空这一手数据
    handInfo.clear();
  }

  // 获取这一个手数信息
  public HandInfo getHandInfo(long roomId) {
    return hands_map.get(roomId);
  }

  public void onTableOpen(long roomId) {
//    logger.debug("open roomId:" + roomId);
    // 调用本局开始逻辑
    InsuranceManager.getInstance().onTableOpenAction(roomId);
  }

  public void onTableClose(long roomId) {
//    logger.debug("remove roomId:" + roomId);
    hands_map.remove(roomId);
    // 调用本局结束逻辑
    InsuranceManager.getInstance().onTableCloseAction(roomId);
  }

  public void setUserBets(long roomId, long uuid, int bets) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",not exits");
      return;
    }
    // 是否需要新建用户
    HandUserInfo userInfo = handInfo.getUser(uuid);
    if (userInfo == null) {
      userInfo = new HandUserInfo();
      userInfo.uuid = uuid;
      handInfo.setUser(userInfo);
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
      logger.error("roomId:" + roomId + ",info null");
      return;
    }
    HandUserInfo userInfo = handInfo.getUser(uuid);
    if (userInfo == null) {
      // 可能是日志错误
      logger.debug("roomId:" + roomId + ",handId:" + handInfo.handId + ",uuid:" + uuid + ",info null");
      return;
    }
//    logger.debug("roomId:" + roomId + ",handId:" + handInfo.handId + ",uuid:" + uuid + ",fold");
    userInfo.fold = true;
    // 记录玩家弃牌操作
    handInfo.setRoundUserFlod(uuid);
  }

  public void setUserCards(long roomId, long uuid, int[] hands, int handChip) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",info null");
      return;
    }
    if (hands.length != 2) {
      logger.error("roomId:" + roomId + ",uuid:" + uuid + ",hands error");
      return;
    }
    // 是否需要新建用户
    HandUserInfo userInfo = handInfo.getUser(uuid);
    if (userInfo == null) {
      userInfo = new HandUserInfo();
      userInfo.uuid = uuid;
      handInfo.setUser(userInfo);
    }
    userInfo.hands[0] = hands[0];
    userInfo.hands[1] = hands[1];
    userInfo.handChip = handChip;
  }

  public void setTableCards(long roomId, int status, int[] tables) {
//    logger.debug("roomId:" + roomId + ",stats:" + status + ",tables:" + Arrays.toString(tables));
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null || tables == null) {
      logger.error("roomId:" + roomId + ",handinfo:" + handInfo + ",tables:" + tables);
      return;
    }
    // 是否已经处于保险状态
    handInfo.insuranceStatus = status;
    if (handInfo.insuranceIsOn) {
      // 设置公共牌状态后直接返回
      if (status == HandUtils.FLOP1) {
        handInfo.tables[0] = tables[0];
      } else if (status == HandUtils.FLOP2) {
        handInfo.tables[1] = tables[0];
      } else if (status == HandUtils.FLOP3) {
        handInfo.tables[2] = tables[0];
      }
    }
    // 设置游戏状态
    handInfo.setGameStatus(status);
    // 设置公共牌
    if (status == HandUtils.FLOP) {
      handInfo.tables[0] = tables[0];
      handInfo.tables[1] = tables[1];
      handInfo.tables[2] = tables[2];
    } else if (status == HandUtils.TURN) {
      handInfo.tables[3] = tables[0];
    } else if (status == HandUtils.RIVER) {
      handInfo.tables[4] = tables[0];
    }

  }

  // 检查当前状态的公共牌
  public boolean checkTableCard(long roomId, int tableCard) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",info null");
      return false;
    }
    return handInfo.checkTableCard(tableCard);
  }

  // 检查当前的allin状态
  public boolean checkAllinState(long roomId) {
    HandInfo handInfo = hands_map.get(roomId);
    if (handInfo == null) {
      logger.error("roomId:" + roomId + ",info null");
      return false;
    }
    return handInfo.checkAllinState();
  }
}
