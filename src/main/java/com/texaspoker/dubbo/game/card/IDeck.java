/**
 * 包名:com.texaspoker.dubbo.game.check
 * 文件名:IDeck.java
 * 创建人:xichen
 * 创建日期:2016年4月6日-上午11:10:33
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.card;


/**
 * 类名称:IDeck
 * 类描述:牌堆的通用接口
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年4月6日-上午11:10:33
 * 修改备注:
 */
public interface IDeck {

  /*
   * 发牌
   */
  public Card pop();

  /*
   * 洗牌
   */
  public void shuffle();
}
