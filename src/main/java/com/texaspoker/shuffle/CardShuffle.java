/**
 * 包名:com.texaspoker.shuffle
 * 文件名:CardShuffle.java
 * 创建人:atum
 * 修改人:atum
 * 创建日期:2015年10月19日-上午11:38:48
 * 修改时间:2015年10月19日-上午11:38:48
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.shuffle;

import java.util.Random;

/**
 * 类名称:CardShuffle 类描述: 洗牌算法
 */
public class CardShuffle {
  // 使用Fisher_Yates随机算法
  static void shuffle(int[] m_cards) {
    // 测试用种子数 k 因子 ,
    long random_k = (long) (10010 * (Math.random() + 1));
    Random randomseed = new Random(System.currentTimeMillis() + random_k);
    // 第一遍洗牌
    int cardlength = 52;
    for (int count = 0; count < cardlength; count++) {
      // 从n张牌后面一个随机位置抽一张牌
      int random = count + randomseed.nextInt(cardlength - count);
      // 交换当前牌和随机位置的牌
      int randomCard = m_cards[random];
      m_cards[random] = m_cards[count];
      m_cards[count] = randomCard;
    }
  }

  static void safeshuffle(int[] orig_cards) {
    // 洗一遍
    shuffle(orig_cards);
    // 再随机洗2,3遍
    for (int safecount = 1; safecount < 3; safecount++) {
      if (Math.random() > 0.4) {
        shuffle(orig_cards);
      }

    }
  }

  public static void main(String[] args) {

    long start_time = 0;
    long used_time = 0;
    int mCards[] = new int[52];
    // 牌型分布表, 横竖为1~52号牌在52个位置的分布计数
    int[][] position_map = new int[52][52];

    // 随机洗牌
    start_time = System.currentTimeMillis();

    // 测试次数 100万次
    for (int test_hand = 1; test_hand <= 1000000; test_hand++) {
      // 初始化一手牌, 顺序排列
      for (int card = 0; card <= 51; card++) {
        mCards[card] = card;
      }
      // 安全洗牌一次
      safeshuffle(mCards);
      // 统计牌的分布
      for (int posi = 0; posi < 52; posi++) {
        position_map[posi][mCards[posi]]++;
      }
    }
    used_time = System.currentTimeMillis() - start_time;

    // 显示用掉的时间
//    System.out.println("Test card suffle by Pokermaster.com");
//    System.out.println("Used " + used_time + "ms ..");
//    System.out.println("====================================");
    // 显示洗过的牌
    // for (int mCard : mCards) {
    // System.out.println(mCard);
    // }

    for (int i = 0; i < 52; i++) {
      for (int j = 0; j < 52; j++) {
        System.out.print(position_map[i][j] + ",");
      }
//      System.out.println(" ");
    }

  }
}
