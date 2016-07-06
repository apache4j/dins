/**
 * 包名:com.texaspoker.dubbo.game.outs
 * 文件名:CheckUtils.java
 * 创建人:xichen
 * 创建日期:2016年6月16日-下午7:51:33
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import com.texaspoker.dubbo.game.card.Card;
import com.texaspoker.dubbo.game.card.CardRankEnum;
import com.texaspoker.dubbo.game.card.CardSuitEnum;
import com.texaspoker.dubbo.game.card.Player;

/**
 * 类名称:CheckUtils
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月16日-下午7:51:33
 * 修改备注:
 */
public class OutsUtils {

  // 转换玩家信息
  public static Player getPlayer(HandUserInfo info) {
    // 设置用户id
    Player player = new Player();
    player.setUuid(info.getUuid());
    // 设置用户手牌
    Card[] cards = new Card[2];
    cards[0] = getCardInfo(info.getHands()[0]);
    cards[1] = getCardInfo(info.getHands()[1]);
    player.setCards(cards);

    return player;
  }

  // 转换卡牌索引
  public static int getCardIndex(Card card) {
    int order = 0;
    switch (card.getSuit()) {
      case SPADES:
        order = 0;
        break;
      case HEARTS:
        order = 13;
        break;
      case CLUBS:
        order = 26;
        break;
      case DIAMONDS:
        order = 39;
        break;
      default:
        break;
    }
    switch (card.getRank()) {
      case ACE:
        order += 0;
        break;
      case CARD_2:
        order += 1;
        break;
      case CARD_3:
        order += 2;
        break;
      case CARD_4:
        order += 3;
        break;
      case CARD_5:
        order += 4;
        break;
      case CARD_6:
        order += 5;
        break;
      case CARD_7:
        order += 6;
        break;
      case CARD_8:
        order += 7;
        break;
      case CARD_9:
        order += 8;
        break;
      case CARD_10:
        order += 9;
        break;
      case JACK:
        order += 10;
        break;
      case QUEEN:
        order += 11;
        break;
      case KING:
        order += 12;
        break;
      default:
        break;
    }
    return order;
  }

  // 转换卡牌信息
  public static Card getCardInfo(int index) {
    // 牌花色
    CardSuitEnum suit = null;
    int order = 0;
    if (index < 13) {
      suit = CardSuitEnum.SPADES;
      order = index;
    } else if (index < 26) {
      suit = CardSuitEnum.HEARTS;
      order = index - 13;
    } else if (index < 39) {
      suit = CardSuitEnum.CLUBS;
      order = index - 26;
    } else {
      suit = CardSuitEnum.DIAMONDS;
      order = index - 39;
    }
    // 牌大小
    CardRankEnum rank = null;
    switch (order) {
      case 0:
        rank = CardRankEnum.ACE;
        break;
      case 1:
        rank = CardRankEnum.CARD_2;
        break;
      case 2:
        rank = CardRankEnum.CARD_3;
        break;
      case 3:
        rank = CardRankEnum.CARD_4;
        break;
      case 4:
        rank = CardRankEnum.CARD_5;
        break;
      case 5:
        rank = CardRankEnum.CARD_6;
        break;
      case 6:
        rank = CardRankEnum.CARD_7;
        break;
      case 7:
        rank = CardRankEnum.CARD_8;
        break;
      case 8:
        rank = CardRankEnum.CARD_9;
        break;
      case 9:
        rank = CardRankEnum.CARD_10;
        break;
      case 10:
        rank = CardRankEnum.JACK;
        break;
      case 11:
        rank = CardRankEnum.QUEEN;
        break;
      case 12:
        rank = CardRankEnum.KING;
        break;
      default:
        break;
    }
    return new Card(suit, rank);
  }

}
