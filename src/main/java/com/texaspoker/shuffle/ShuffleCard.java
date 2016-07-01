/**
 * 包名:com.texaspoker.shuffle
 * 文件名:ShuffleCard.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月19日-下午3:45:44
 * 修改时间:2015年10月19日-下午3:45:44
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.shuffle;

/**
 * 类名称:ShuffleCard 类描述:
 */
public class ShuffleCard {

  static int CARD_LENGTH = 52;

  /**
   * 初始化,顺序排列
   */
  public static void initShuffle(int[] cards) {
    for (int card = 0; card < CARD_LENGTH; card++) {
      cards[card] = card;
    }
  }

  /**
   * 按照概率洗1-3次 safeShuffle
   * 
   * @param cards void
   * @exception
   */
  public static void safeShuffle(int[] cards) {
    // 先洗一遍
    ShuffleAlgorithm.shuffle(cards);

    // 再随机洗2,3遍
    for (int safecount = 1; safecount < 3; safecount++) {
      if (Math.random() > 0.4) {
        ShuffleAlgorithm.shuffle(cards);
      }
    }
  }

  /**
   * 直接洗3遍 strongShuffle
   * 
   * @param cards void
   * @exception
   */
  public static void strongShuffle(int[] cards) {
    // 洗3遍
    for (int safecount = 0; safecount < 3; safecount++) {
      ShuffleAlgorithm.shuffle(cards);
    }
  }

  /**
   * 强随机洗3遍 secureShuffle
   * 
   * @param cards void
   * @exception
   */
  public static void secureShuffle(int[] cards) {
    // 洗3遍
    for (int safecount = 0; safecount < 3; safecount++) {
      ShuffleAlgorithm.secureShuffle(cards);
    }
  }
}
