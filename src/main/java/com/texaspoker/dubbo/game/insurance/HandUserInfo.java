/**
 * 包名:com.texaspoker.dubbo.game.event
 * 文件名:UserInfo.java
 * 创建人:xichen
 * 创建日期:2016年6月28日-下午3:19:07
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

/**
 * 类名称:UserInfo
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月28日-下午3:19:07
 * 修改备注:
 */
public class HandUserInfo {
  // 玩家id
  long uuid;
  // 玩家底牌
  int[] hands = new int[2];
  // 玩家状态
  // int betStatus = HandUtils.PRE_FLOP;

  // 在桌状态
  boolean fold = false;
  boolean allin = false;

  // 筹码情况
  int handChip = 0;
  int betChip = 0;

  // int winChip = 0;

  public long getUuid() {
    return uuid;
  }

  public int[] getHands() {
    return hands;
  }
}
