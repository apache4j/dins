/**
 * 包名:com.texaspoker.timer
 * 文件名:TimeoutListener.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月15日-下午3:20:52
 * 修改时间:2015年10月15日-下午3:20:52
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.utils.timer;

/**
 * 类名称:TimeoutListener 类描述: 超时事件监听器
 */
public interface TimeoutListener<K, V> {
  /**
   * Called when a map entry timeouts
   */
  void expired(K key, V value);
}
