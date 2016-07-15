/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:RankingUtil.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-下午2:25:27
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import static com.texaspoker.dubbo.game.card.RankingEnum.FLUSH;
import static com.texaspoker.dubbo.game.card.RankingEnum.FOUR_OF_A_KIND;
import static com.texaspoker.dubbo.game.card.RankingEnum.FULL_HOUSE;
import static com.texaspoker.dubbo.game.card.RankingEnum.HIGH_CARD;
import static com.texaspoker.dubbo.game.card.RankingEnum.ONE_PAIR;
import static com.texaspoker.dubbo.game.card.RankingEnum.ROYAL_FLUSH;
import static com.texaspoker.dubbo.game.card.RankingEnum.STRAIGHT;
import static com.texaspoker.dubbo.game.card.RankingEnum.STRAIGHT_FLUSH;
import static com.texaspoker.dubbo.game.card.RankingEnum.THREE_OF_A_KIND;
import static com.texaspoker.dubbo.game.card.RankingEnum.TWO_PAIR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *  01) ROYAL_FLUSH,
 *  02) STRAIGHT_FLUSH,
 *  03) FOUR_OF_A_KIND,
 *  04) FULL_HOUSE,
 *  05) FLUSH,
 *  06) STRAIGHT,
 *  07) THREE_OF_A_KIND,
 *  08) TWO_PAIR,
 *  09) ONE_PAIR,
 *  10) HIGH_CARD
 */

/**
 * 类名称:RankingUtil
 * 类描述:牌型比对逻辑
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-下午2:25:27
 * 修改备注:
 */
public class CardRanking {

  // 用户匹配的牌型
  public static Integer getRankingToInt(IPlayer player) {
    return player.getRankingEnum().ordinal();
  }

  // 设置用户牌型
  public static void setRankingEnum(IPlayer player, RankingEnum rankingEnum, List<Card> rankingList) {
    player.setRankingEnum(rankingEnum);
    player.setRankingList(rankingList);
  }

  // 检查用户匹配的牌型
  public static void checkRanking(IPlayer player, List<Card> tableCards) {

    List<Card> rankingList = null;

    // STRAIGHT_FLUSH
    rankingList = getStraightFlush(player, tableCards);
    if (rankingList != null) {
      if (checkRoyalFlush(rankingList)) {
        setRankingEnum(player, ROYAL_FLUSH, rankingList);
      } else {
        setRankingEnum(player, STRAIGHT_FLUSH, rankingList);
      }
      return;
    }
    // FOUR_OF_A_KIND
    rankingList = getFourOfAKind(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, FOUR_OF_A_KIND, rankingList);
      return;
    }
    // FULL_HOUSE
    rankingList = getFullHouse(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, FULL_HOUSE, rankingList);
      return;
    }
    // FLUSH
    rankingList = getFlush(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, FLUSH, rankingList);
      return;
    }
    // STRAIGHT
    rankingList = getStraight(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, STRAIGHT, rankingList);
      return;
    }
    // THREE_OF_A_KIND
    rankingList = getThreeOfAKind(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, THREE_OF_A_KIND, rankingList);
      return;
    }
    // TWO_PAIR
    rankingList = getTwoPair(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, TWO_PAIR, rankingList);
      return;
    }
    // ONE_PAIR
    rankingList = getOnePair(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, ONE_PAIR, rankingList);
      return;
    }
    // HIGH_CARD
    rankingList = getHighCard(player, tableCards);
    if (rankingList != null) {
      setRankingEnum(player, HIGH_CARD, rankingList);
      return;
    }
  }

  // 找出皇家同花顺
  public static List<Card> getRoyalFlush(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getStraightFlush(player, tableCards);
    if (orderedList != null) {
      List<CardRankEnum> seqCards = getRankEnumList(orderedList);
      if (seqCards.contains(CardRankEnum.ACE) && seqCards.contains(CardRankEnum.CARD_10)
          && seqCards.contains(CardRankEnum.JACK) && seqCards.contains(CardRankEnum.QUEEN)
          && seqCards.contains(CardRankEnum.KING)) {
        return orderedList;
      }
    }
    return null;
  }

  // 检查是否皇家同花顺
  public static boolean checkRoyalFlush(List<Card> orderedList) {
    if (orderedList != null) {
      List<CardRankEnum> seqCards = getRankEnumList(orderedList);
      if (seqCards.contains(CardRankEnum.ACE) && seqCards.contains(CardRankEnum.CARD_10)
          && seqCards.contains(CardRankEnum.JACK) && seqCards.contains(CardRankEnum.QUEEN)
          && seqCards.contains(CardRankEnum.KING)) {
        return true;
      }
    }
    return false;
  }

  // 找出同花顺
  public static List<Card> getStraightFlush(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> suitList = checkFlush(orderedList);
    if (suitList != null && suitList.size() >= 5) {
      return checkSequence(suitList);
    }
    return null;
  }

  // 找出四条
  public static List<Card> getFourOfAKind(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> resultList = checkPair(orderedList, 4);
    if (resultList == null) {
      return null;
    }
    orderedList.removeAll(resultList);
    // 是否还有剩余牌
    if (orderedList.isEmpty()) {
      return resultList;
    }
    int remainSize = orderedList.size() < 1 ? orderedList.size() : 1;
    List<Card> remainCards = checkHigh(orderedList, remainSize);
    if (remainCards == null) {
      return null;
    }
    resultList.addAll(remainCards);
    return resultList;
  }

  // 找出葫芦
  public static List<Card> getFullHouse(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> threeList = checkPair(orderedList, 3);
    if (threeList == null) {
      return null;
    }
    orderedList.removeAll(threeList);
    List<Card> twoList = checkPair(orderedList, 2);
    if (twoList == null) {
      return null;
    }
    threeList.addAll(twoList);
    return threeList;
  }

  // 找出同花
  public static List<Card> getFlush(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> flushList = new ArrayList<Card>();

    List<Card> suitList = checkFlush(orderedList);
    if (suitList != null && suitList.size() >= 5) {
      flushList.addAll(suitList.subList(0, 5));
      return flushList;
    }
    return null;
  }

  // 找出顺子
  public static List<Card> getStraight(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    return checkSequence(orderedList);
  }

  // 找出3条
  public static List<Card> getThreeOfAKind(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> resultList = checkPair(orderedList, 3);
    if (resultList == null) {
      return null;
    }
    orderedList.removeAll(resultList);
    // 是否还有剩余牌
    if (orderedList.isEmpty()) {
      return resultList;
    }
    int remainSize = orderedList.size() < 2 ? orderedList.size() : 2;
    List<Card> remainCards = checkHigh(orderedList, remainSize);
    if (remainCards == null) {
      return null;
    }
    resultList.addAll(remainCards);
    return resultList;
  }

  // 找出二对
  public static List<Card> getTwoPair(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> twoPair1 = checkPair(orderedList, 2);
    if (twoPair1 == null) {
      return null;
    }
    orderedList.removeAll(twoPair1);
    List<Card> twoPair2 = checkPair(orderedList, 2);
    if (twoPair2 == null) {
      return null;
    }
    twoPair1.addAll(twoPair2);
    orderedList.removeAll(twoPair2);
    // 是否还有剩余牌
    if (orderedList.isEmpty()) {
      return twoPair1;
    }
    int remainSize = orderedList.size() < 1 ? orderedList.size() : 1;
    List<Card> remainCards = checkHigh(orderedList, remainSize);
    if (remainCards == null) {
      return null;
    }
    twoPair1.addAll(remainCards);
    return twoPair1;
  }

  // 找出一对
  public static List<Card> getOnePair(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    List<Card> resultList = checkPair(orderedList, 2);
    if (resultList == null) {
      return null;
    }
    orderedList.removeAll(resultList);
    // 是否还有剩余牌
    if (orderedList.isEmpty()) {
      return resultList;
    }
    int remainSize = orderedList.size() < 3 ? orderedList.size() : 3;
    List<Card> remainCards = checkHigh(orderedList, remainSize);
    if (remainCards == null) {
      return null;
    }
    resultList.addAll(remainCards);
    return resultList;
  }

  // 找出最大高牌
  public static List<Card> getHighCard(IPlayer player, List<Card> tableCards) {
    List<Card> orderedList = getOrderedCardList(player, tableCards);
    int remainSize = orderedList.size() < 5 ? orderedList.size() : 5;
    List<Card> resultList = checkHigh(orderedList, remainSize);
    if (resultList == null) {
      return null;
    }
    return resultList;
  }

  // 合并-用户底牌和牌桌公共牌
  private static List<Card> getMergedCardList(IPlayer player, List<Card> tableCards) {
    List<Card> merged = new ArrayList<Card>();
    merged.addAll(tableCards);
    merged.add(player.getCards()[0]);
    merged.add(player.getCards()[1]);
    return merged;
  }

  // 降序排序-用户底牌和牌桌公共牌
  private static List<Card> getOrderedCardList(IPlayer player, List<Card> tableCards) {
    List<Card> ordered = getMergedCardList(player, tableCards);
    Collections.sort(ordered, new Comparator<Card>() {
      public int compare(Card c1, Card c2) {
        if (c1.getRankToInt() > c2.getRankToInt()) {
          return -1;
        } else if (c1.getRankToInt() < c2.getRankToInt()) {
          return 1;
        } else {
          return c1.getSuitToInt() > c2.getSuitToInt() ? -1 : 1;
        }
      }
    });
    return ordered;
  }

  // 转换为近数字比对类型列表
  private static List<CardRankEnum> getRankEnumList(List<Card> cardList) {
    List<CardRankEnum> rankEnumList = new ArrayList<CardRankEnum>();
    for (Card card : cardList) {
      rankEnumList.add(card.getRank());
    }
    return rankEnumList;
  }

  // 找出最大的几张牌
  private static List<Card> checkHigh(List<Card> orderedList, int maxSize) {
    List<Card> highCards = new ArrayList<Card>();
    for (Card card : orderedList) {
      highCards.add(card);
      if (highCards.size() >= maxSize) {
        return highCards;
      }
    }
    return null;
  }

  // 找出相同的牌
  private static List<Card> checkPair(List<Card> orderedList, Integer pairSize) {
    List<Card> checkedPair = new ArrayList<Card>();
    Card preCard = null;
    for (int i = 0; i < orderedList.size(); i++) {
      Card curCard = orderedList.get(i);
      if (preCard == null) {
        preCard = curCard;
        continue;
      }
      if (curCard.getRank().equals(preCard.getRank())) {
        if (checkedPair.size() == 0) {
          checkedPair.add(preCard);
        }
        checkedPair.add(curCard);
        if (checkedPair.size() == pairSize) {
          return checkedPair;
        }
      } else {
        preCard = curCard;
        checkedPair.clear();
      }
    }
    if (checkedPair.size() == pairSize) {
      return checkedPair;
    }
    return null;
  }

  // 找出同花
  private static List<Card> checkFlush(List<Card> orderedList) {
    List<Card> suitList = new ArrayList<Card>();
    // sum card suit
    Map<CardSuitEnum, List<Card>> flushSuits = new HashMap<CardSuitEnum, List<Card>>();
    for (Card card : orderedList) {
      if (!flushSuits.containsKey(card.getSuit())) {
        flushSuits.put(card.getSuit(), new ArrayList<Card>(7));
      }
      flushSuits.get(card.getSuit()).add(card);
    }
    // find max cardsuit
    for (CardSuitEnum suit : flushSuits.keySet()) {
      if (flushSuits.get(suit).size() >= 5) {
        suitList.addAll(flushSuits.get(suit));
        return suitList;
      }
    }
    return null;
  }

  // 找出顺子,注意12345
  private static List<Card> checkSequence(List<Card> orderedList) {
    List<Card> sequenceList = new ArrayList<Card>();

    // normal sequence
    Card cardPrevious = null;
    for (Card card : orderedList) {
      if (cardPrevious == null) {
        cardPrevious = card;
        continue;
      }
      if (cardPrevious.getRankToInt() == card.getRankToInt()) {
        continue;
      }
      if ((cardPrevious.getRankToInt() - card.getRankToInt()) == 1) {
        if (sequenceList.size() == 0) {
          sequenceList.add(cardPrevious);
        }
        sequenceList.add(card);
        cardPrevious = card;
        if (sequenceList.size() == 5) {
          return sequenceList;
        }
      } else {
        sequenceList.clear();
        cardPrevious = card;
      }
    }

    // special sequence 54321
    if (sequenceList.size() == 4 && !orderedList.isEmpty()) {
      List<CardRankEnum> seqCards = getRankEnumList(sequenceList);
      if (seqCards.contains(CardRankEnum.CARD_2) && seqCards.contains(CardRankEnum.CARD_5)) {
        // check ace
        Card aceCard = orderedList.get(0);
        if (aceCard.getRank().equals(CardRankEnum.ACE)) {
          sequenceList.add(aceCard);
        }
      }

    }
    return (sequenceList.size() == 5) ? sequenceList : null;
  }
}
