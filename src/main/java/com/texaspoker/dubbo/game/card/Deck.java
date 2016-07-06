/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:Deck.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-上午11:22:19
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * 类名称:Deck
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-上午11:22:19
 * 修改备注:
 */
public class Deck implements IDeck, Serializable {

  private static final long serialVersionUID = -6583582066767488999L;

  private List<Card> cards;
  private Random random = new Random();

  public Deck() {
    createDeck();
  }

  private void createDeck() {
    cards = new ArrayList<Card>();
    for (CardSuitEnum suit : CardSuitEnum.values()) {
      for (CardRankEnum rank : CardRankEnum.values()) {
        cards.add(new Card(suit, rank));
      }
    }
  }

  private void shuffleDeck() {
    for (int i = 0; i < cards.size(); i++) {
      int swap = random.nextInt(cards.size() - i);
      Card temp = cards.get(swap);
      cards.set(swap, cards.get(i));
      cards.set(i, temp);
    }
  }

  /*
   * 发牌
   */
  public Card pop() {
    return cards.remove(random.nextInt(cards.size()));
  }

  /*
   * 洗牌,洗3次牌保证概率
   */
  public void shuffle() {
    for (int i = 0; i < 3; i++) {
      shuffleDeck();
    }
  }
}
