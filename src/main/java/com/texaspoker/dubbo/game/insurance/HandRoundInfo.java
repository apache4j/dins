/**
 * 包名:com.texaspoker.dubbo.game.event
 * 文件名:RoundInfo.java
 * 创建人:xichen
 * 创建日期:2016年6月28日-下午3:27:24
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类名称:RoundInfo
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月28日-下午3:27:24
 * 修改备注:
 */
public class HandRoundInfo {
  /*
   * 构造数据
   */
  public int status;

  public HandRoundInfo(int status) {
    this.status = status;
  }

  // 是否已经计算本轮筹码信息
  public boolean checked = false;

  /*
   * 本手进行中的数据
   */
  // 本轮玩家的筹码量
  public Map<Long, Integer> user_bets = new HashMap<Long, Integer>();
  // 本轮玩家的flod
  public Map<Long, Integer> user_flods = new HashMap<Long, Integer>();

  /*
   * 本手结束后的数据
   */
  // 本轮总筹码量
  public int totalBets = 0;
  // 本轮分池筹码量
  public int poolBets = 0;
  // 本轮flod的筹码量
  public int flodsBets = 0;

  // 本轮allin玩家
  public Map<Long, Integer> user_allins = new HashMap<Long, Integer>();
  // 本轮allin筹码边池
  public LinkedHashMap<Integer, Integer> allin_bets = new LinkedHashMap<Integer, Integer>();
  // 本轮allin边池的用户
  public Map<Integer, Map<Long, Integer>> allin_user = new HashMap<Integer, Map<Long, Integer>>();

  /*
   * 状态数据，用于判断是否本轮allin
   */
  // 一直到本轮，玩家的总筹码量
  public Map<Long, Integer> user_totals = new HashMap<Long, Integer>();

}
