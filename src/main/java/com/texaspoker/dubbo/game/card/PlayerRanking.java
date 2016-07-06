/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:PlayerRanking.java
 * 创建人:xichen
 * 创建日期:2016年4月7日-下午4:54:10
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

/**
 * 类名称:PlayerRanking
 * 类描述:玩家之间的比牌算法
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月7日-下午4:54:10
 * 修改备注:
 */
public class PlayerRanking {

  // 比较2个人大小
  public static Integer compareRanking(IPlayer player1, IPlayer player2) {
    int cardRank1 = player1.getRankingEnum().ordinal();
    int cardRank2 = player2.getRankingEnum().ordinal();
    if (cardRank1 > cardRank2) {
      return 1;
    } else if (cardRank1 < cardRank2) {
      return -1;
    }

    RankingEnum rankingEnum = player1.getRankingEnum();
    switch (rankingEnum) {
      case ROYAL_FLUSH:
        return 0;
      case STRAIGHT_FLUSH:
        return checkStraightFlush(player1, player2);
      case FOUR_OF_A_KIND:
        return checkFourOfAKind(player1, player2);
      case FULL_HOUSE:
        return checkFullHouse(player1, player2);
      case FLUSH:
        return checkFlush(player1, player2);
      case STRAIGHT:
        return checkStraight(player1, player2);
      case THREE_OF_A_KIND:
        return checkThreeOfAKind(player1, player2);
      case TWO_PAIR:
        return checkTwoPair(player1, player2);
      case ONE_PAIR:
        return checkOnePair(player1, player2);
      case HIGH_CARD:
        return checkHighCard(player1, player2);
      default:
        break;
    }
    return 0;
  }

  // 比较同花顺
  private static Integer checkStraightFlush(IPlayer player1, IPlayer player2) {
    // 这种情况，2人一定是同花，只需要比较顺子高牌就行
    int highSequence1 = player1.getRankingList().get(0).getRankToInt();
    int highSequence2 = player2.getRankingList().get(0).getRankToInt();
    if (highSequence1 > highSequence2) {
      return 1;
    } else if (highSequence1 < highSequence2) {
      return -1;
    } else {
      return 0;
    }
  }

  // 比较四条
  private static Integer checkFourOfAKind(IPlayer player1, IPlayer player2) {
    // 先比四条
    int fourKind1 = player1.getRankingList().get(0).getRankToInt();
    int fourKind2 = player2.getRankingList().get(0).getRankToInt();
    if (fourKind1 > fourKind2) {
      return 1;
    } else if (fourKind1 < fourKind2) {
      return -1;
    }
    // 比较第5张牌
    return checkRemainCards(player1, player2, 5);
  }

  // 比较葫芦
  private static Integer checkFullHouse(IPlayer player1, IPlayer player2) {
    // 先比较三条
    int threeKind1 = player1.getRankingList().get(0).getRankToInt();
    int threeKind2 = player2.getRankingList().get(0).getRankToInt();
    if (threeKind1 > threeKind2) {
      return 1;
    } else if (threeKind1 < threeKind2) {
      return -1;
    }
    // 比较对子
    int pair1 = player1.getRankingList().get(3).getRankToInt();
    int pair2 = player2.getRankingList().get(3).getRankToInt();
    if (pair1 > pair2) {
      return 1;
    } else if (pair1 < pair2) {
      return -1;
    } else {
      return 0;
    }
  }

  // 比较同花
  private static Integer checkFlush(IPlayer player1, IPlayer player2) {
    // 从高到低依次比较
    return checkRemainCards(player1, player2, 1);
  }

  // 比较顺子
  private static Integer checkStraight(IPlayer player1, IPlayer player2) {
    // 从高到低依次比较
    return checkRemainCards(player1, player2, 1);
  }

  // 比较三条
  private static Integer checkThreeOfAKind(IPlayer player1, IPlayer player2) {
    // 先比较三条
    int threeKind1 = player1.getRankingList().get(0).getRankToInt();
    int threeKind2 = player2.getRankingList().get(0).getRankToInt();
    if (threeKind1 > threeKind2) {
      return 1;
    } else if (threeKind1 < threeKind2) {
      return -1;
    }
    // 比较剩余的2张牌
    return checkRemainCards(player1, player2, 4);
  }

  // 比较二对
  private static Integer checkTwoPair(IPlayer player1, IPlayer player2) {
    // 比较第一对
    int pair1 = player1.getRankingList().get(0).getRankToInt();
    int pair2 = player2.getRankingList().get(0).getRankToInt();
    if (pair1 > pair2) {
      return 1;
    } else if (pair1 < pair2) {
      return -1;
    }
    // 比较第二对
    pair1 = player1.getRankingList().get(2).getRankToInt();
    pair2 = player2.getRankingList().get(2).getRankToInt();
    if (pair1 > pair2) {
      return 1;
    } else if (pair1 < pair2) {
      return -1;
    }
    // 比较第五张
    return checkRemainCards(player1, player2, 5);
  }

  // 比较一对
  private static Integer checkOnePair(IPlayer player1, IPlayer player2) {
    // 比较第一对
    int pair1 = player1.getRankingList().get(0).getRankToInt();
    int pair2 = player2.getRankingList().get(0).getRankToInt();
    if (pair1 > pair2) {
      return 1;
    } else if (pair1 < pair2) {
      return -1;
    }
    // 比较剩余的3张牌
    return checkRemainCards(player1, player2, 3);
  }

  // 比较高牌
  private static Integer checkHighCard(IPlayer player1, IPlayer player2) {
    return checkRemainCards(player1, player2, 1);
  }

  // 从第i张牌开始，降序比较大小
  private static Integer checkRemainCards(IPlayer player1, IPlayer player2, int indexCard) {
    // 从高到低依次比较
    for (int i = indexCard - 1; i < 5; i++) {
      int high1 = player1.getRankingList().get(i).getRankToInt();
      int high2 = player2.getRankingList().get(i).getRankToInt();
      if (high1 > high2) {
        return 1;
      } else if (high1 < high2) {
        return -1;
      } else {
        continue;
      }
    }
    return 0;
  }
}
