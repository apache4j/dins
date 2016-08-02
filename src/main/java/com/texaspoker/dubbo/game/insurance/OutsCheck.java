/**
 * 包名:com.texaspoker.dubbo.game.outs
 * 文件名:PlayerCheck.java
 * 创建人:xichen
 * 创建日期:2016年6月16日-下午7:36:48
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.texaspoker.dubbo.game.card.Card;
import com.texaspoker.dubbo.game.card.CardRankEnum;
import com.texaspoker.dubbo.game.card.CardRanking;
import com.texaspoker.dubbo.game.card.CardSuitEnum;
import com.texaspoker.dubbo.game.card.IPlayer;
import com.texaspoker.dubbo.game.card.PlayerRanking;

/**
 * 类名称:PlayerCheck
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年6月16日-下午7:36:48
 * 修改备注:
 */
public class OutsCheck {

  private static Logger logger = Logger.getLogger(OutsCheck.class);

  // 接口层
  public static List<OutsInfo> findOutsInPlayers(List<HandUserInfo> playerCards, HandInfo handInfo, boolean checkEven) {
    // 获取这一手当前的公共牌信息
    int[] publicCards = handInfo.getTableCards();
    logger.debug("tables:" + Arrays.toString(publicCards));
    // 转换玩家信息
    List<IPlayer> players = new ArrayList<IPlayer>();
    for (HandUserInfo playerInfo : playerCards) {
      players.add(OutsUtils.getPlayer(playerInfo));
    }
    // 转换公共牌信息
    List<Card> tableCards = new ArrayList<Card>();
    if (publicCards != null) {
      for (int i = 0; i < publicCards.length; i++) {
        tableCards.add(OutsUtils.getCardInfo(publicCards[i]));
      }
    }
    // 找出胜者
    List<IPlayer> winners = findWinnerInPlayers(players, tableCards);
    if (winners == null) {
      // 没有胜者，没有保险
      return null;
    }
    // 找出剩余的牌
    List<IPlayer> allPlayers = new ArrayList<IPlayer>();
    for (HandUserInfo playerInfo : handInfo.userInfoMap.values()) {
      allPlayers.add(OutsUtils.getPlayer(playerInfo));
    }
    List<Card> remainCards = findRemainCards(allPlayers, tableCards);
    if (remainCards == null) {
      // 没有剩余牌？扯淡，肯定有问题了
      return null;
    }
    // logger.debug("remainCards:" + remainCards);
    // 检查一个胜者的out数量
    List<OutsInfo> results = new ArrayList<OutsInfo>();
    for (IPlayer winner : winners) {
      // 先找outs
      List<Card> outsCards = findOutsInPlayer(winner, players, tableCards, remainCards);
      logger.debug("winner:" + winner.getUuid() + ",outs.size:" + outsCards.size());
      if (outsCards.size() > 0) {
        OutsInfo outsInfo = new OutsInfo();
        outsInfo.winner = winner.getUuid();
        outsInfo.outs = new int[outsCards.size()];
        for (int k = 0; k < outsCards.size(); k++) {
          outsInfo.outs[k] = OutsUtils.getCardIndex(outsCards.get(k));
        }
        results.add(outsInfo);
        // 在有outs情况下，再看是否检查分钱outs
        if (checkEven) {
          List<Card> evenCards = findEvensInPlayer(winner, players, tableCards, remainCards);
          logger.debug("winner:" + winner.getUuid() + ",outs.size:" + evenCards.size());
          if (evenCards.size() > 0) {
            outsInfo.evens = new int[evenCards.size()];
            for (int i = 0; i < evenCards.size(); i++) {
              outsInfo.evens[i] = OutsUtils.getCardIndex(evenCards.get(i));
            }
          }
        }
      }
    }
    return results;
  }

  // 只找分钱outs
  public static List<Card> findEvensInPlayer(IPlayer winner, List<IPlayer> players, List<Card> tableCards,
      List<Card> remainCards) {
    // 依次查找out
    List<Card> outsCards = new ArrayList<Card>();
    for (Card remainCard : remainCards) {
      List<Card> checkCards = new ArrayList<Card>(tableCards);
      checkCards.add(remainCard);
      // logger.debug("checkCards:" + checkCards);
      // 检查这一张是否是outs
      if (checkEvensForWinner(winner, players, checkCards)) {
        outsCards.add(remainCard);
      }
    }
    // logger.debug("outsCards:" + outsCards);
    return outsCards;
  }

  // 找出单个赢家的outs,默认不查找平分outs
  public static List<Card> findOutsInPlayer(IPlayer winner, List<IPlayer> players, List<Card> tableCards,
      List<Card> remainCards) {
    // 依次查找out
    List<Card> outsCards = new ArrayList<Card>();
    for (Card remainCard : remainCards) {
      List<Card> checkCards = new ArrayList<Card>(tableCards);
      checkCards.add(remainCard);
      // logger.debug("checkCards:" + checkCards);
      // 检查这一张是否是outs
      if (checkOutsForWinner(winner, players, checkCards)) {
        outsCards.add(remainCard);
      }
    }
    // logger.debug("outsCards:" + outsCards);
    return outsCards;
  }

  // 找出单个赢家的平分outs,用于测试用例
  public static List<Card> findEvensInPlayer(IPlayer winner, List<IPlayer> players, List<Card> tableCards) {
    // 依次查找out
    List<Card> outsCards = new ArrayList<Card>();
    // 直接在比牌人群里找到比牌玩家
    List<Card> remainCards = findRemainCards(players, tableCards);
    if (remainCards == null) {
      // 没有剩余牌？扯淡，肯定有问题了
      return null;
    }
    for (Card remainCard : remainCards) {
      List<Card> checkCards = new ArrayList<Card>(tableCards);
      checkCards.add(remainCard);
      // logger.debug("checkCards:" + checkCards);
      // 检查这一张是否是outs
      if (checkEvensForWinner(winner, players, checkCards)) {
        outsCards.add(remainCard);
      }
    }
    // logger.debug("outsCards:" + outsCards);
    return outsCards;
  }

  // 找出单个赢家的outs,用于测试用例
  public static List<Card> findOutsInPlayer(IPlayer winner, List<IPlayer> players, List<Card> tableCards) {
    // 依次查找out
    List<Card> outsCards = new ArrayList<Card>();
    // 直接在比牌人群里找到比牌玩家
    List<Card> remainCards = findRemainCards(players, tableCards);
    if (remainCards == null) {
      // 没有剩余牌？扯淡，肯定有问题了
      return null;
    }
    for (Card remainCard : remainCards) {
      List<Card> checkCards = new ArrayList<Card>(tableCards);
      checkCards.add(remainCard);
      // logger.debug("checkCards:" + checkCards);
      // 检查这一张是否是outs
      if (checkOutsForWinner(winner, players, checkCards)) {
        outsCards.add(remainCard);
      }
    }
    // logger.debug("outsCards:" + outsCards);
    return outsCards;
  }

  public static boolean checkOutsForWinner(IPlayer winner, List<IPlayer> players, List<Card> tableCards) {
    for (IPlayer player : players) {
      CardRanking.checkRanking(player, tableCards);
    }
    for (IPlayer player : players) {
      // 自己不用比较
      if (winner.getUuid() == player.getUuid()) {
        continue;
      }
      int compareResult = PlayerRanking.compareRanking(winner, player);
      if (compareResult == 0) {
        // 赢家和玩家牌型相等，不算outs
        continue;
      } else if (compareResult == 1) {
        // 赢家继续胜出
        continue;
      } else {
        // 赢家被bb
        return true;
      }
    }
    return false;
  }

  public static boolean checkEvensForWinner(IPlayer winner, List<IPlayer> players, List<Card> tableCards) {
    for (IPlayer player : players) {
      CardRanking.checkRanking(player, tableCards);
    }
    for (IPlayer player : players) {
      // 自己不用比较
      if (winner.getUuid() == player.getUuid()) {
        continue;
      }
      int compareResult = PlayerRanking.compareRanking(winner, player);
      if (compareResult == 0) {
        // 赢家和玩家牌型相等，不算outs
        return true;
      } else if (compareResult == 1) {
        // 赢家继续胜出
        continue;
      } else {
        // 赢家被bb
        continue;
      }
    }
    return false;
  }

  public static List<Card> findRemainCards(List<IPlayer> players, List<Card> tableCards) {
    // 先洗出一整副牌
    List<Card> remainCards = new ArrayList<Card>();
    for (CardSuitEnum suit : CardSuitEnum.values()) {
      for (CardRankEnum rank : CardRankEnum.values()) {
        remainCards.add(new Card(suit, rank));
      }
    }
    // 除去发过的牌
    for (IPlayer player : players) {
      Card[] handsCards = player.getCards();
      for (int i = 0; i < handsCards.length; i++) {
        remainCards.remove(player.getCards()[i]);
      }
    }
    for (Card card : tableCards) {
      remainCards.remove(card);
    }
    return remainCards;
  }

  // 找出当前的赢家
  public static List<IPlayer> findWinnerInPlayers(List<IPlayer> players, List<Card> tableCards) {
    // 检查玩家牌型
    for (IPlayer player : players) {
      CardRanking.checkRanking(player, tableCards);
      // logger.debug("players rankenum:" + player.getRankingEnum());
      // logger.debug("players ranklist:" + player.getRankingList());
    }
    // 获取牌型最大的玩家们
    List<IPlayer> winnerlist = new ArrayList<IPlayer>();
    IPlayer winner = players.get(0);
    winnerlist.add(winner);
    // 开始比较每个玩家
    for (int i = 1; i < players.size(); i++) {
      IPlayer player = players.get(i);
      int compareResult = PlayerRanking.compareRanking(winner, player);
      if (compareResult == 0) {
        winnerlist.add(player);
      } else if (compareResult == 1) {
        continue;
      } else if (compareResult == -1) {
        winner = player;
        winnerlist.clear();
        winnerlist.add(winner);
      }
    }
    // 同时有多个赢家
    if (winnerlist.size() > 0) {
      return winnerlist;
    }
    return null;
  }
}
