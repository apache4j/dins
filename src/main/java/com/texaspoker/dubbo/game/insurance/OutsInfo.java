/**
åå * 包名:com.texaspoker.dubbo.game.outs
 * 文件名:OutsResult.java
 * 创建人:xichen
 * 创建日期:2016年6月16日-下午7:25:13
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.Arrays;

/**
 * 类名称:OutsResult
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月16日-下午7:25:13
 * 修改备注:
 */
public class OutsInfo {

  // outs数量在多少个内，才能买保险
  public static final int OUTS_LIMIT = 14;
  // outs赔率表，20个以上是0.8
  public static final double[] OUTS_ODDS = new double[] {31, 16, 10, 8, 6, 5, 4, 3.5, 3, 2.5, 2.3, 2, 1.8, 1.6, 1.4,
      1.3, 1.2, 1.1, 1.0, 0.8};
  public static final int OUTS_NUMS = 20;

  // 边池pot
  public long potId = 0;
  public int potIndex = 0;
  public long pots = 0;
  public long bets = 0;

  // 胜者
  public long winner = 0;

  // 保险的out信息
  public int[] outs;

  // 保险的赔率
  public double odds = 0;

  // 保险金额-保本
  public long pay_loss = 0;

  // 保险金额-全部
  public long pay_pots = 0;

  // 保险金额-最小
  public long pay_min = 0;

  // 保险金额-最大
  public long pay_max = 0;

  // 购买金额
  public long buy = 0;

  // 是否放弃
  public boolean fold = false;

  // 保险结果，包含购买金额
  public long result = 0;

  // 设置保险购买信息
  public void setPayInfo(int status, long potId, int potIndex, long pots, long prebuys, long bets) {
    if (winner == 0 || outs == null) {
      throw new RuntimeException("outs info winner or outs error");
    }
    int outsNum = outs.length > OUTS_NUMS ? OUTS_NUMS : outs.length;
    this.odds = OUTS_ODDS[outsNum - 1];
    this.potId = potId;
    this.potIndex = potIndex;
    this.pots = pots;
    this.bets = bets;
    // 根据状态判断最大值的比例
    double potMaxRaito = 0;
    if (status == HandUtils.FLOP) {
      potMaxRaito = 0.25;
    } else if (status == HandUtils.TURN) {
      potMaxRaito = 0.5;
    }
    // 找出最大购买值
    long pot_max = getBuyPrice(potMaxRaito * pots);
    long pot_odds = getBuyPrice(pots / odds);
    pay_max = pot_max > pot_odds ? pot_odds : pot_max;
    // 找出最小购买值，可为0，如果以前购买过，则至少为1
    pay_min = prebuys > 0 ? getBuyPrice(prebuys / odds) : 0;
    // 处理保本额度，不能比最大额度还大
    long temp_noloss = getBuyPrice((prebuys + bets) / odds);
    pay_loss = temp_noloss > pay_max ? pay_max : temp_noloss;
    // 全底池额度
    pay_pots = getBuyPrice((prebuys + pots) / odds);
  }

  // 设置购买信息
  public void setBuy(long buys) {
    this.buy = buys;
  }

  public long getBuy() {
    return this.buy;
  }

  public void setFold() {
    this.fold = true;
  }

  public boolean getFold() {
    return this.fold;
  }

  // 检查保险是否买中
  public boolean checkBuyWin(int tableCard) {
    for (int i = 0; i < outs.length; i++) {
      if (tableCard == outs[i]) {
        this.result = getPayCoin();
        return true;
      }
    }
    this.result = 0;
    return false;
  }

  // 获取保险赔付金额，向下取整，不能多赔
  private long getPayCoin() {
    double payCoin = 0;
    payCoin = buy * odds;
    // 赔付金额中还要退还之前的购买jine
    payCoin += buy;
    return (long) payCoin;
  }

  // 保险金额采用向下取整策略，不保证完全赔付
  private long getBuyPrice(double price) {
    long expectedPrice = (long) price;
    // 保证一定保险可买，最少买1
    if (expectedPrice == 0) {
      return 1;
    }
    return (long) price;
  }

  @Override
  public String toString() {
    return "outsinfo,uuid:" + winner + ",potId," + potId + ",odds:" + odds + ",pots:" + pots + ",pay_loss:" + pay_loss
        + ",pay_pots:" + pay_pots + ",pay_min:" + pay_min + ",pay_max:" + pay_max + ",buy:" + buy + ",fold:" + fold
        + ",result:" + result + ",outs:" + Arrays.toString(outs);
  }
}
