/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:Game.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-下午2:13:35
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 类名称:Game
 * 类描述:牌局信息
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-下午2:13:35
 * 修改备注:
 */
public class Game implements Serializable {

  private static final long serialVersionUID = -5528115023509898325L;

  private IDeck deck;

  private List<IPlayer> players;

  private List<Card> tableCards;

  // 创建新牌局
  public void newGame(IPlayer player1, IPlayer... _players) {
    // 洗牌
    deck = new Deck();
    deck.shuffle();
    // 准备发牌
    tableCards = new ArrayList<Card>();
    players = new ArrayList<IPlayer>();
    // 游戏至少需要一个玩家
    players.add(player1);
    players.addAll(Arrays.asList(_players));
  }

  // 创建已有牌局
  public void getGame(List<Card> _tables, List<IPlayer> _players) {
    // 准备发牌
    tableCards = new ArrayList<Card>();
    tableCards.addAll(_tables);
    // 游戏至少需要一个玩家
    players = new ArrayList<IPlayer>();
    players.addAll(_players);
  }

  // 发底牌
  public void deal() {
    for (IPlayer player : players) {
      player.getCards()[0] = deck.pop();
      player.getCards()[1] = deck.pop();
    }
    checkPlayerRanking();
  }

  public void callFlop() {
    deck.pop();
    tableCards.add(deck.pop());
    tableCards.add(deck.pop());
    tableCards.add(deck.pop());
    checkPlayerRanking();
  }

  public void betTurn() {
    deck.pop();
    tableCards.add(deck.pop());
    checkPlayerRanking();
  }

  public void betRiver() {
    deck.pop();
    tableCards.add(deck.pop());
    checkPlayerRanking();
  }

  // 找出每个人的牌型
  private void checkPlayerRanking() {
    for (IPlayer player : players) {
      CardRanking.checkRanking(player, tableCards);
      // System.out.println("players ranklist:" + player.getRankingList().toString());
    }
  }

  // 找出获胜者
  public List<IPlayer> getWinners() {
    checkPlayerRanking();
    // get default winner
    List<IPlayer> winnerlist = new ArrayList<IPlayer>();
    IPlayer winner = players.get(0);
    winnerlist.add(winner);
    // compare others
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
    return winnerlist;
  }


}
