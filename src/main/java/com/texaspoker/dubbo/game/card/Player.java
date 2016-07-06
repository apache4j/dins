/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:Player.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-下午2:10:22
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import java.io.Serializable;
import java.util.List;

/**
 * 类名称:Player
 * 类描述:玩家信息
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-下午2:10:22
 * 修改备注:
 */
public class Player implements IPlayer, Serializable {

  private static final long serialVersionUID = 2257244926124592784L;

  private String id;

  private long uuid;

  private Card[] cards = new Card[2];

  private RankingEnum rankingEnum = null;

  private List<Card> rankingList = null;

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public void setUuid(long uuid) {
    this.uuid = uuid;
  }

  public long getUuid() {
    return this.uuid;
  }

  public RankingEnum getRankingEnum() {
    return rankingEnum;
  }

  public void setRankingEnum(RankingEnum rankingEnum) {
    this.rankingEnum = rankingEnum;
  }

  public List<Card> getRankingList() {
    return rankingList;
  }

  public void setRankingList(List<Card> rankingList) {
    this.rankingList = rankingList;
  }

  public Card[] getCards() {
    return cards;
  }

  public void setCards(Card[] cards) {
    this.cards = cards;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (!(obj instanceof Player)) {
      return false;
    } else {
      Player player2 = (Player) obj;
      if (cards[0].equals(player2.getCards()[0]) && cards[1].equals(player2.getCards()[1])) {
        return true;
      }
      if (cards[1].equals(player2.getCards()[0]) && cards[2].equals(player2.getCards()[1])) {
        return true;
      }
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Integer.valueOf(cards.toString());
  }

  @Override
  public String toString() {
    return cards.toString();
  }
}
