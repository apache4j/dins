/**
 * 包名:com.texaspoker.timer
 * 文件名:Assert.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月15日-下午4:30:49
 * 修改时间:2015年10月15日-下午4:30:49
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.utils.timer;

/**
 * 类名称:Assert 类描述:
 */
public final class Assert {
  private Assert() {}

  public static <T> T notNull(T reference, String parameterName) {
    if (reference == null)
      throw new NullPointerException(parameterName + " cannot be null");
    return reference;
  }

  public static void operation(boolean condition, String message) {
    if (!condition)
      throw new UnsupportedOperationException(message);
  }

  public static void state(boolean expression, String errorMessageFormat, Object... args) {
    if (!expression)
      throw new IllegalStateException(String.format(errorMessageFormat, args));
  }
}
