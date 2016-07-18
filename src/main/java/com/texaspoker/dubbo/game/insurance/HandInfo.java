/**
 * 包名:com.texaspoker.dubbo.game.event
 * 文件名:HandInfo.java
 * 创建人:xichen
 * 创建日期:2016年6月28日-下午3:10:17
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;


/**
 * 类名称:HandInfo
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月28日-下午3:10:17
 * 修改备注:
 */
public class HandInfo {

  private static Logger logger = Logger.getLogger(HandInfo.class);

  // 房间id
  public long roomId;
  // 本手id
  public long handId;
  // 当前游戏状态
  public int gameStatus;
  // 是否已经在Allin保险状态
  public int insuranceStatus;
  public boolean insuranceIsOn = false;
  // 桌上公共牌
  public int tables[] = new int[5];
  // 本手玩家信息
  public ConcurrentMap<Long, HandUserInfo> userInfoMap = new ConcurrentHashMap<Long, HandUserInfo>();
  // 每一轮下注情况
  public ConcurrentMap<Integer, HandRoundInfo> roundInfos = new ConcurrentHashMap<Integer, HandRoundInfo>();

  /*
   * 检查状态的临时数据，无需线程安全
   */
  // 这一手的所有边池信息
  public Map<Integer, Integer> allin_pots = new HashMap<Integer, Integer>();
  // 这一手的边池用户筹码情况
  public Map<Integer, Map<Long, Integer>> allin_pots_user = new HashMap<Integer, Map<Long, Integer>>();

  // 清理
  public void clear() {
    gameStatus = HandUtils.PRE_FLOP;

    insuranceStatus = HandUtils.PRE_FLOP;
    insuranceIsOn = false;

    userInfoMap.clear();
    roundInfos.clear();

    allin_pots.clear();
    allin_pots_user.clear();
  }

  public void setGameStatus(int status) {
    logger.debug("roomId:" + roomId + ",gameStatus:" + status);
    this.gameStatus = status;
    // 设置这一轮状态信息
    HandRoundInfo roundInfo = roundInfos.get(status);
    if (roundInfo == null) {
      roundInfo = new HandRoundInfo(status);
      roundInfo.status = status;
      roundInfos.put(status, roundInfo);
    }
  }

  // 检查当前状态的公共牌
  public boolean checkTableCard(int tableCard) {
    // 保险状态下，先比较前三张
    if (insuranceIsOn) {
      if (insuranceStatus == HandUtils.FLOP1) {
        return tableCard == tables[0];
      } else if (insuranceStatus == HandUtils.FLOP2) {
        return tableCard == tables[1];
      } else if (insuranceStatus == HandUtils.FLOP3) {
        return tableCard == tables[2];
      }
    }
    // 比较剩下的2张
    if (gameStatus == HandUtils.TURN) {
      return tableCard == tables[3];
    } else if (gameStatus == HandUtils.RIVER) {
      return tableCard == tables[4];
    }
    return false;
  }

  // 获取当前状态的公共牌
  public int[] getTableCards() {
    if (insuranceIsOn) {
      if (insuranceStatus == HandUtils.FLOP1) {
        int[] cards = new int[1];
        cards[0] = tables[0];
        return cards;
      } else if (insuranceStatus == HandUtils.FLOP2) {
        int[] cards = new int[2];
        cards[0] = tables[0];
        cards[1] = tables[1];
        return cards;
      } else if (insuranceStatus == HandUtils.FLOP3) {
        int[] cards = new int[3];
        cards[0] = tables[0];
        cards[1] = tables[1];
        cards[2] = tables[2];
        return cards;
      }
    }
    // 比较剩下的2张
    if (gameStatus == HandUtils.FLOP) {
      int[] cards = new int[3];
      cards[0] = tables[0];
      cards[1] = tables[1];
      cards[2] = tables[2];
      return cards;
    }
    if (gameStatus == HandUtils.TURN) {
      int[] cards = new int[4];
      cards[0] = tables[0];
      cards[1] = tables[1];
      cards[2] = tables[2];
      cards[3] = tables[3];
      return cards;
    } else if (gameStatus == HandUtils.RIVER) {
      int[] cards = new int[5];
      cards[0] = tables[0];
      cards[1] = tables[1];
      cards[2] = tables[2];
      cards[3] = tables[3];
      cards[4] = tables[4];
      return cards;
    }
    return null;
  }

  // 设置这一手用户信息
  public HandUserInfo getUser(long uuid) {
    return userInfoMap.get(uuid);
  }

  public void setUser(HandUserInfo user) {
    userInfoMap.put(user.uuid, user);
  }

  // 记录玩家的下注信息
  public void setRoundUserBets(long uuid, int bets) {
    HandRoundInfo roundInfo = roundInfos.get(gameStatus);
    if (roundInfo == null) {
      logger.error("roomId:" + roomId + ",gameStatus:" + gameStatus + ",no roundinfo");
      return;
    }
    Integer userBets = roundInfo.user_bets.get(uuid);
    if (userBets == null) {
      roundInfo.user_bets.put(uuid, bets);
    } else {
      Integer allBets = userBets + bets;
      roundInfo.user_bets.put(uuid, allBets);
    }
  }

  // 记录flod的玩家信息
  public void setRoundUserFlod(long uuid) {
    HandRoundInfo roundInfo = roundInfos.get(gameStatus);
    if (roundInfo == null) {
      logger.error("roomId:" + roomId + ",gameStatus:" + gameStatus + ",no roundinfo");
      return;
    }
    Integer userBets = roundInfo.user_bets.get(uuid);
    if (userBets == null) {
      roundInfo.user_flods.put(uuid, 0);
    } else {
      roundInfo.user_flods.put(uuid, userBets);
    }
  }

  // 获取这一手所有的allin边池信息
  public void checkHandState() {
    logger.debug("roomId:" + roomId + ",insuranceIsOn:" + insuranceIsOn + ",insuranceStatus:" + insuranceStatus);
    // 已经在全部allin，购买的保险的状态中，前面已经结算过筹码
    if (this.insuranceIsOn) {
      return;
    }
    int handState = this.gameStatus;
    for (int roundState = HandUtils.PRE_FLOP; roundState <= handState; roundState++) {
      HandRoundInfo roundInfo = roundInfos.get(gameStatus);
      if (roundInfo == null) {
        return;
      }
      // 是否已经检查过
      if (roundInfo.checked) {
        logger.debug("roomId:" + roomId + ",roundState:" + roundState + ",checked");
        continue;
      }
      // 检查新一轮的筹码情况
      logger.debug("roomId:" + roomId + ",roundState:" + roundState + ",checking");
      checkRoundState(roundState);
      // 将这一轮的allin边池信息导入本手汇总
      allin_pots.putAll(roundInfo.allin_bets);
      allin_pots_user.putAll(roundInfo.allin_user);
    }
    // 设置这一手开启保险状态
    this.insuranceIsOn = true;
  }

  // 计算这一轮的allin边池信息
  private void checkRoundState(int status) {
    HandRoundInfo roundInfo = roundInfos.get(status);
    if (roundInfo == null) {
      logger.error("roomId:" + roomId + ",roundState:" + status + ",no roundinfo");
      return;
    }
    // 计算筹码量
    calcRoundState(status);
    // 设置检查状态
    roundInfo.checked = true;
    // 本轮是否有allin玩家
    if (roundInfo.user_allins.size() == 0) {
      logger.debug("roomId:" + roomId + ",roundState:" + status + ",no allin");
      return;
    }
    // 反向建立有序的玩家allin筹码量
    Map<Integer, Long> allinBets = new TreeMap<Integer, Long>();
    for (Map.Entry<Long, Integer> allinUser : roundInfo.user_allins.entrySet()) {
      if (!allinBets.containsKey(allinUser.getValue())) {
        allinBets.put(allinUser.getValue(), allinUser.getKey());
      }
    }
    logger.debug("roomId:" + roomId + ",roundState:" + status + ",potIds:" + allinBets.keySet());
    // 为allin玩家建立边池,默认按照筹码升序排列(allin总量排序)
    Integer preAllinBet = 0;
    for (Map.Entry<Integer, Long> allinBet : allinBets.entrySet()) {
      // 当前allin的筹码量
      int roundAllinBet = 0;
      if (roundInfo.allin_bets.isEmpty()) {
        // 本轮筹码量最小的allin，找到这一轮最小allin的那个筹码量
        roundAllinBet = roundInfo.user_bets.get(allinBet.getValue());
      } else {
        // 先找出本轮的这一个allin玩家，在这一轮的筹码量
        for (Map.Entry<Long, Integer> allinUser : roundInfo.user_allins.entrySet()) {
          if (allinUser.getValue().equals(allinBet.getKey())) {
            roundAllinBet = roundInfo.user_bets.get(allinUser.getKey());
            break;
          }
        }
      }
      // 这一个allin边池能吃到的flod筹码量
      int roundflodBets = 0;
      for (Map.Entry<Long, Integer> userFlodBetEntry : roundInfo.user_flods.entrySet()) {
        int userFlodBet = userFlodBetEntry.getValue();
        if (userFlodBet <= roundAllinBet) {
          roundflodBets += userFlodBet;
          userFlodBetEntry.setValue(0);
        } else {
          roundflodBets += roundAllinBet;
          userFlodBetEntry.setValue(userFlodBet - roundAllinBet);
        }
      }
      // 这一个allin边池包含的玩家
      Map<Long, Integer> allinUsers = new HashMap<Long, Integer>();
      for (Map.Entry<Long, Integer> allinUser : roundInfo.user_bets.entrySet()) {
        // 有多少玩家的筹码量比这个allin大
        if (allinUser.getValue() >= roundAllinBet) {
          allinUsers.put(allinUser.getKey(), roundAllinBet - preAllinBet);
        }
      }
      // 这个allin边池的玩家
      roundInfo.allin_user.put(allinBet.getKey(), allinUsers);
      logger.debug("roomId:" + roomId + ",roundState:" + status + ",potId:" + allinBet.getKey() + ",alluser:"
          + allinUsers.keySet());
      // 这个allin的边池总量
      int allinPool = (roundAllinBet - preAllinBet) * allinUsers.size() + roundflodBets;
      roundInfo.allin_bets.put(allinBet.getKey(), allinPool);
      logger.debug("roomId:" + roomId + ",roundState:" + status + ",potId:" + allinBet.getKey() + ",potBets:"
          + allinPool);
      // 定义第一个边池
      preAllinBet = roundAllinBet;
    }
  }

  // 计算这一轮的筹码信息
  private void calcRoundState(int status) {
    HandRoundInfo roundInfo = roundInfos.get(status);
    if (roundInfo == null) {
      return;
    }
    // 处理上一轮筹码数据
    int preStatus = status - 1;
    if (roundInfos.containsKey(preStatus)) {
      // 后面几轮，用上一轮初始化
      roundInfo.user_totals.putAll(roundInfos.get(preStatus).user_totals);
    } else {
      // preflop 第一轮，初始化
      for (Long userId : roundInfo.user_bets.keySet()) {
        roundInfo.user_totals.put(userId, 0);
      }
    }
    // 处理本轮筹码
    for (Map.Entry<Long, Integer> betUser : roundInfo.user_bets.entrySet()) {
      long uuid = betUser.getKey();
      int betChip = betUser.getValue();
      // 这一轮是否下过筹码
      if (betChip > 0) {
        // 计算单个用户累加筹码
        int preTotalChip = roundInfo.user_totals.get(uuid);
        int curTotalChip = preTotalChip + betChip;
        roundInfo.user_totals.put(uuid, curTotalChip);
        // 计算本轮累加筹码
        roundInfo.totalBets += betChip;
        // 检查是否本轮allin
        if (userInfoMap.get(uuid).handChip == curTotalChip) {
          roundInfo.user_allins.put(uuid, curTotalChip);
        }
        if (roundInfo.user_flods.containsKey(uuid)) {
          roundInfo.flodsBets += betChip;
        }
      } else if (betChip == 0 && status == HandUtils.PRE_FLOP) {
        // 没有下注，也没有flod，很可能是开局站起了，直接当作flod
        roundInfo.user_flods.put(uuid, betChip);
      }
      roundInfo.poolBets = roundInfo.totalBets;
    }
    logger.debug("roomId:" + roomId + ",handId:" + handId + ",user_bets:" + roundInfo.user_bets);
    logger.debug("roomId:" + roomId + ",handId:" + handId + ",user_totals:" + roundInfo.user_totals);
  }
}
