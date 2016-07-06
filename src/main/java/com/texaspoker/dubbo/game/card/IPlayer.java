/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:IPlayer.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-下午1:58:20
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;

import java.util.List;

/**
 * 类名称:IPlayer
 * 类描述:玩家接口
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-下午1:58:20
 * 修改备注:
 */
public interface IPlayer {

  // 底牌
  public Card[] getCards();

  // 用户名
  public void setId(String id);

  public String getId();

  // UUID
  public void setUuid(long uuid);

  public long getUuid();

  // 匹配牌堆
  public List<Card> getRankingList();

  public void setRankingList(List<Card> rankinglist);

  // 匹配牌型
  public RankingEnum getRankingEnum();

  public void setRankingEnum(RankingEnum rankingEnum);

}
