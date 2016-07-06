/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:Card.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-上午11:00:44
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import java.io.Serializable;

/**
 * 类名称:Card
 * 类描述: 单张扑克牌信息
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-上午11:00:44
 * 修改备注:
 */
public class Card implements Serializable {

  private static final long serialVersionUID = 3800506076049061923L;

  private CardSuitEnum suit;
  private CardRankEnum rank;

  public Card(CardSuitEnum suit, CardRankEnum rank) {
    this.suit = suit;
    this.rank = rank;
  }

  public CardSuitEnum getSuit() {
    return suit;
  }

  public CardRankEnum getRank() {
    return rank;
  }

  public Integer getRankToInt() {
    return rank.ordinal();
  }

  public Integer getSuitToInt() {
    return suit.ordinal();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (!(obj instanceof Card)) {
      return false;
    } else {
      Card card2 = (Card) obj;
      return rank.equals(card2.getRank()) && suit.equals(card2.getSuit());
    }
  }

  @Override
  public int hashCode() {
    return Integer.valueOf(String.valueOf(rank.ordinal()) + String.valueOf(suit.ordinal()));
  }

  @Override
  public String toString() {
    return "Suit: " + suit.toString() + ", Rank :" + rank.toString();
  }

}
