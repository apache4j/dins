/**
 * 包名:com.texaspoker.shuffle
 * 文件名:ShuffleCard.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月19日-下午2:59:58
 * 修改时间:2015年10月19日-下午2:59:58
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.shuffle;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * 类名称:ShuffleCard 类描述:
 */
public class ShuffleAlgorithm {

  static int CARD_LENGTH = 52;

  /**
   * 弱伪随机洗牌算法 shuffle
   * 
   * @param cards void
   * @exception
   */
  public static void shuffle(int[] cards) {
    // 设置随机种子
    Random randomseed = new Random(ShuffleSeed.get());

    // 使用Fisher_Yates随机算法
    for (int count = 0; count < CARD_LENGTH; count++) {
      // 从n张牌后面一个随机位置抽一张牌
      int random = count + randomseed.nextInt(CARD_LENGTH - count);
      // 交换当前牌和随机位置的牌
      int randomCard = cards[random];
      cards[random] = cards[count];
      cards[count] = randomCard;
    }
  }

  /**
   * 强伪随机洗牌算法 secureShuffle
   * 
   * @param cards
   * @throws NoSuchAlgorithmException void
   * @exception 没有这个算法实现
   */
  public static void secureShuffle(int[] cards) {
    // 设置随机种子
    SecureRandom randomseed = null;
    try {
      randomseed = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }
    randomseed.setSeed(ShuffleSeed.get());

    // 使用Fisher_Yates随机算法
    for (int count = 0; count < CARD_LENGTH; count++) {
      // 从n张牌后面一个随机位置抽一张牌
      int random = count + randomseed.nextInt(CARD_LENGTH - count);
      // 交换当前牌和随机位置的牌
      int randomCard = cards[random];
      cards[random] = cards[count];
      cards[count] = randomCard;
    }
  }
}
