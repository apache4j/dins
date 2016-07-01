/**
 * 包名:com.texaspoker.shuffle
 * 文件名:ShuffleSeed.java
 * 创建人:chenxi
 * 修改人:chenxi
 * 创建日期:2015年10月19日-下午2:59:47
 * 修改时间:2015年10月19日-下午2:59:47
 * Copyright (c) 2015 illuminate 公司-版权所有
 */
package com.texaspoker.shuffle;

/**
 * 类名称:ShuffleSeed 类描述:
 */
public class ShuffleSeed {

  public static long get() {
    // 测试用种子数 k 因子 ,
    long random_k = (long) (10010 * (Math.random() + 1));
    long random_seed = System.currentTimeMillis() + random_k;
    return random_seed;
  }

}
