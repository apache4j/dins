/**
 * 包名:com.texaspoker.dubbo.game.outs
 * 文件名:OutsResult.java
 * 创建人:xichen
 * 创建日期:2016年6月16日-下午7:25:13
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

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
  public static final int OUTS_LIMIT = 15;
  // outs赔率表
  public static final double[] OUTS_ODDS = new double[] {30, 16, 10, 8, 6, 5, 4, 3.5, 3, 2.5, 2.2, 2, 1.8, 1.6, 1.4};

  // 边池pot
  public long potId = 0;
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

  // 保险金额-等利
  public long pay_earn = 0;

  // 保险金额-全部
  public long pay_pots = 0;

  // 购买金额
  public long buy = 0;

  // 是否放弃
  public boolean fold = false;

  // 保险结果
  public long result = 0;

  // 设置保险购买信息
  public void setPayInfo(long potId, long pots, long prebuys, long bets) {
    if (winner == 0 || outs == null) {
      throw new RuntimeException("outs info winner or outs error");
    }
    int outsNum = outs.length;
    if (outsNum > OUTS_LIMIT) {
      throw new RuntimeException("outs size too large");
    }
    this.odds = OUTS_ODDS[outsNum - 1];
    this.potId = potId;
    this.pots = pots;
    this.bets = bets;
    pay_loss = getBuyPrice((prebuys + bets) / odds);
    pay_earn = getBuyPrice((prebuys + pots) / (odds + 1));
    pay_pots = getBuyPrice(pots / odds);
  }

  // 设置购买信息
  public void setBuyInfo(long buys) {
    if (pay_earn == buys || pay_loss == buys || pay_pots == buys) {
      this.buy = buys;
    } else {
      return;
    }
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
        result = getPayCoin();
        return true;
      }
    }
    result = 0;
    return false;
  }

  // 获取保险赔付金额，向下取整，不能多赔
  private long getPayCoin() {
    double payCoin = 0;
    if (buy == pay_earn) {
      payCoin = buy * (odds + 1);
    } else if (buy == pay_loss) {
      payCoin = buy * odds;
    } else if (buy == pay_pots) {
      payCoin = buy * odds;
    }
    return (long) payCoin;
  }

  // 保险金额采用向下取整策略，不保证完全赔付
  private long getBuyPrice(double price) {
    return (long) price;
  }
}
