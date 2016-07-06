/**
 * 包名:com.texaspoker.timer
 * 文件名:NamedThreadFactory.java
 * 创建人: chenxi
 * 创建日期:2015年10月15日-下午3:05:31
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.utils.timer;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类名称:NamedThreadFactory 类描述: 生成带名字的线程，方便调试
 */
public class NamedThreadFactory implements ThreadFactory {
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String nameFormat;

  /**
   * Creates a thread factory that names threads according to the {@code nameFormat} by supplying a
   * single argument to the format representing the thread number.
   */
  public NamedThreadFactory(String nameFormat) {
    this.nameFormat = nameFormat;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r, String.format(nameFormat, threadNumber.getAndIncrement()));
    thread.setDaemon(true);
    return thread;
  }
}
