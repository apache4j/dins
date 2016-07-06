/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceTimer.java
 * 创建人:xichen
 * 创建日期:2016年7月1日-下午4:31:05
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.texaspoker.dubbo.utils.timer.TimeoutListener;
import com.texaspoker.dubbo.utils.timer.TimeoutMap;

/**
 * 类名称:InsuranceTimer
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月1日-下午4:31:05
 * 修改备注:
 */
public class InsuranceTimer {

  public static final int INSURANCE_OPERATION_TIMEOUT_SECONDS = 30;
  public static final int INSURANCE_OPERATION_DELAY_SECONDS = 10;

  // 用户保险超时信息
  private class InsuranceTimeInfo {
    long startTime;
    long endTime;
    Set<Long> potIds;
  }

  // 所有房间的购买超时
  public TimeoutMap<Long, Map<Long, InsuranceTimeInfo>> room_timer;

  // 定时器初始化
  public InsuranceTimer() {
    room_timer =
        TimeoutMap.builder().variableTimeout().timeout(30, TimeUnit.SECONDS)
            .timeoutListener(new TimeoutListener<Long, Map<Long, InsuranceTimeInfo>>() {
              public void expired(Long thekey, Map<Long, InsuranceTimeInfo> thevalue) {
                InsuranceTimeoutHandler(thekey);
              }
            }).build();
  }

  // 超时处理响应，这个牌桌保险时间超时，需要进行后续操作
  public void InsuranceTimeoutHandler(Long roomId) {
    // API接口，通知这个房间保险超时
    InsuranceApi.gotoNextStep(roomId);
  }

  // 设置玩家超时
  public void setUserTimeout(long roomId, long uuid, long potId) {
    Map<Long, InsuranceTimeInfo> userTimes = room_timer.get(roomId);
    if (userTimes == null) {
      userTimes = new HashMap<Long, InsuranceTimeInfo>();
      room_timer.put(roomId, userTimes);
    }
    // 是否设置过这个玩家的超时
    InsuranceTimeInfo timeInfo = userTimes.get(uuid);
    if (timeInfo == null) {
      timeInfo = new InsuranceTimeInfo();
      timeInfo.startTime = System.currentTimeMillis();
      timeInfo.endTime = timeInfo.startTime + INSURANCE_OPERATION_TIMEOUT_SECONDS * 1000;
      timeInfo.potIds = new HashSet<>();
      userTimes.put(uuid, timeInfo);
    }
    // 增加这个用户的边池超时时间
    timeInfo.potIds.add(potId);
  }

  // 玩家购买延时
  public void addUserTimeout(long roomId, long uuid) {
    Map<Long, InsuranceTimeInfo> userTimes = room_timer.get(roomId);
    if (userTimes == null) {
      return;
    }
    InsuranceTimeInfo timeInfo = userTimes.get(uuid);
    if (timeInfo == null) {
      return;
    }
    // 增加这一个用户的超时时间
    timeInfo.endTime += INSURANCE_OPERATION_DELAY_SECONDS * 1000;
    // 遍历所有玩家的超时时间，找出最大的那一个
    long maxStartTime = 0;
    long maxEndTime = 0;
    for (Map.Entry<Long, InsuranceTimeInfo> usertime : userTimes.entrySet()) {
      if (usertime.getValue().endTime > maxEndTime) {
        maxStartTime = usertime.getValue().startTime;
        maxEndTime = usertime.getValue().endTime;
      }
    }
    // 找出当前这个超时的剩余时间
    long newTimeSpan = maxEndTime - maxStartTime;
    long oldTimeSpan = room_timer.getTimeout(roomId);
    // 是否超时时间延长了
    if (newTimeSpan > oldTimeSpan) {
      room_timer.setTimeout(roomId, newTimeSpan, TimeUnit.MILLISECONDS);
    }
  }

  // 玩家购买保险
  public void removeUserTimeout(long roomId, long uuid, long potId) {
    Map<Long, InsuranceTimeInfo> userTimes = room_timer.get(roomId);
    if (userTimes == null) {
      return;
    }
    InsuranceTimeInfo timeInfo = userTimes.get(uuid);
    if (timeInfo == null) {
      return;
    }
    // 先去除这个玩家的边池超时
    timeInfo.potIds.remove(potId);
    // 检查是否这个玩家的所有边池超时都清空了
    if (!timeInfo.potIds.isEmpty()) {
      // 边池超时并没有完全情况，所有用户的超时不变，牌局超时不变
      return;
    }
    // 所以边池已经清空，这次去除这个玩家时间信息
    userTimes.remove(uuid);
    // 是否所有玩家都已经购买了保险
    if (userTimes.isEmpty()) {
      // 通知游戏超时结束
      InsuranceTimeoutHandler(roomId);
      return;
    }
    // 重新计算超时时间，遍历所有玩家的超时时间，找出剩下最大的那一个
    long maxStartTime = 0;
    long maxEndTime = 0;
    for (Map.Entry<Long, InsuranceTimeInfo> usertime : userTimes.entrySet()) {
      if (usertime.getValue().endTime > maxEndTime) {
        maxStartTime = usertime.getValue().startTime;
        maxEndTime = usertime.getValue().endTime;
      }
    }
    // 找出当前这个超时的剩余时间
    long newTimeSpan = maxEndTime - maxStartTime;
    long oldTimeSpan = room_timer.getTimeout(roomId);
    // 是否超时时间缩短了
    if (newTimeSpan < oldTimeSpan) {
      room_timer.setTimeout(roomId, newTimeSpan, TimeUnit.MILLISECONDS);
    }
  }
}
