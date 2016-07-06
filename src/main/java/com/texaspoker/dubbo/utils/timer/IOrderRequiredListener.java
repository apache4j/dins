package com.texaspoker.dubbo.utils.timer;

/**
 * 类名称:IOrderRequiredListener
 * 类描述: 将具有相同特征值任务按顺序执行,不同特征值可以多线程执行的listener的接口
 * 创建人:lordscott
 * 修改人:lordscott
 * 修改时间:Mar 25, 2016-2:57:37 PM
 * 修改备注:
 */
public interface IOrderRequiredListener {
  /**
   * 获取任务的特征值(可以简单理解为房间号)
   * (这里描述这个方法适用条件 – 可选)
   * @return
   * long
   * @exception
  */
  public long getOrderIndex();
}
