/**
 * 包名:com.texaspoker.timer
 * 文件名:TimeoutPolicy.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月15日-下午3:23:26
 * 修改时间:2015年10月15日-下午3:23:26
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.utils.timer;

/**
 * 类名称:TimeoutPolicy 类描述: 超时触发机制
 */

public enum TimeoutPolicy {
  /** Expires entries based on when they were last accessed */
  ACCESSED,
  /** Expires entries based on when they were created */
  CREATED;
}
