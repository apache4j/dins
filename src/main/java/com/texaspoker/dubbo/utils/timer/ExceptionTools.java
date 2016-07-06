/**
 * 包名:com.texaspoker.utils
 * 文件名:ExceptionTools.java
 * 创建人: chenxi
 * 创建日期:2015年10月10日-下午3:37:17
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.utils.timer;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 类名称:ExceptionTools 类描述: 创建人:chenxi 修改人:chenxi 修改时间:2015年10月10日-下午3:37:17 修改备注:
 */
public class ExceptionTools {

  /*
   * 转换异常的堆栈信息，为字符串信息
   */
  public static String printExceptionStack(Exception e) {
    try {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      return "\r\n" + sw.toString() + "\r\n";
    } catch (Exception e2) {
      return "bad printExceptionStack";
    }
  }
}
