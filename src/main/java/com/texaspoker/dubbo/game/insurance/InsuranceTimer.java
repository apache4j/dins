/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceTimer.java
 * 创建人:xichen
 * 创建日期:2016年7月1日-下午4:31:05
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

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

  private static Logger logger = Logger.getLogger(InsuranceTimer.class);

  public static final int INSURANCE_OPERATION_TIMEOUT_SECONDS = 30;
  public static final int INSURANCE_OPERATION_DELAY_SECONDS = 20;

  private int insurance_timeout_seconds = 0;
  private int insurance_delay_seconds = 0;

  // 用户保险超时信息
  public class InsuranceTimeInfo {
    long startTime;
    long endTime;
    Set<Long> potIds = new HashSet<>();
  }

  // 房间超时信息
  public class RoomTimeInfo {
    long roomId;
    long startTime;
    long pauseTime;
    long endTime;
    ConcurrentMap<Long, InsuranceTimeInfo> userTimeInfos =
        new ConcurrentHashMap<Long, InsuranceTimer.InsuranceTimeInfo>();;
  }

  // 所有房间的购买超时
  public TimeoutMap<Long, RoomTimeInfo> room_timer;

  // 所有暂停的房间信息
  public ConcurrentMap<Long, RoomTimeInfo> room_pause = new ConcurrentHashMap<Long, RoomTimeInfo>();

  // 定时器初始化
  public InsuranceTimer() {
    InitTimer(INSURANCE_OPERATION_TIMEOUT_SECONDS, INSURANCE_OPERATION_DELAY_SECONDS);
  }

  // 自定义初始化
  public InsuranceTimer(int insuranceTimeout, int insuranceDelayTime) {
    InitTimer(insuranceTimeout, insuranceDelayTime);
  }

  // 支持自定义房间超时逻辑
  private void InitTimer(int insuranceTimeout, int insuranceDelayTime) {
    insurance_timeout_seconds = insuranceTimeout;
    insurance_delay_seconds = insuranceDelayTime;
    room_timer =
        TimeoutMap.builder().variableTimeout().timeout(insurance_timeout_seconds, TimeUnit.SECONDS)
            .timeoutListener(new TimeoutListener<Long, RoomTimeInfo>() {
              public void expired(Long thekey, RoomTimeInfo thevalue) {
                InsuranceTimeoutHandler(thekey);
              }
            }).build();
  }

  // 超时处理响应，这个牌桌保险时间超时，需要进行后续操作
  public void InsuranceTimeoutHandler(Long roomId) {
    logger.debug("roomId:" + roomId + ",has been timeout");
    // 清理房间超时的购买逻辑
    InsuranceManager.getInstance().foldInsurance(roomId);
    // API接口，通知这个房间保险超时
    InsuranceApi.gotoNextStep(roomId);
  }

  public void onRoomOpen(long roomId) {
    logger.debug("roomId:" + roomId + ",open table");
  }

  public void onRoomClose(long roomId) {
    logger.debug("roomId:" + roomId + ",close table");
    room_timer.remove(roomId);
  }

  public void onRoomPause(long roomId) {
    logger.debug("roomId:" + roomId + ",pause table");
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      logger.error("roomId:" + roomId + ",pause room info null");
      return;
    }
    // 设置暂停时刻
    roomTimeInfo.pauseTime = System.currentTimeMillis();
    // 迁移到暂停列表
    room_pause.put(roomId, roomTimeInfo);
    room_timer.remove(roomId);
  }

  public void onRoomPlay(long roomId) {
    logger.debug("roomId:" + roomId + ",play table");
    RoomTimeInfo roomTimeInfo = room_pause.get(roomId);
    if (roomTimeInfo == null) {
      logger.error("roomId:" + roomId + ",play room info null");
      return;
    }
    // 计算剩余超时时间
    long leftTime = roomTimeInfo.endTime - roomTimeInfo.pauseTime;
    // 重新计算开始时刻
    long pausePastedSpan = System.currentTimeMillis() - roomTimeInfo.pauseTime;
    roomTimeInfo.startTime += pausePastedSpan;
    roomTimeInfo.endTime += pausePastedSpan;
    roomTimeInfo.pauseTime = 0;
    // 所有玩家的超时都需要刷新时刻
    for (InsuranceTimeInfo timeInfo : roomTimeInfo.userTimeInfos.values()) {
      timeInfo.startTime += pausePastedSpan;
      timeInfo.endTime += pausePastedSpan;
    }
    // 重新加时列表
    room_timer.put(roomId, roomTimeInfo, leftTime, TimeUnit.MILLISECONDS);
    room_pause.remove(roomId);
  }

  // 检查是否处于游戏暂停模式
  public boolean checkRoomPause(long roomId) {
    if (room_timer.containsKey(roomId)) {
      logger.error("roomId:" + roomId + ",is not in pause!");
      return false;
    }
    if (room_pause.containsKey(roomId)) {
      return true;
    }
    return false;
  }

  // 清空房间的超时记录
  public void clearRoomTimeout(long roomId) {
    logger.debug("roomId:" + roomId + ",clean timeout");
    room_timer.remove(roomId);
  }

  // 查询房间是否需要超时操作,只限于主动购买或者放弃时需要调用，破解嵌套调用
  public void checkRoomTimeout(long roomId) {
    logger.debug("roomId:" + roomId + ",check timeout");
    if (!room_timer.containsKey(roomId)) {
      // 房间超时信息突然没有了，这哥们的操作应该是最后一个人的，所以可以调用超时操作
      InsuranceTimeoutHandler(roomId);
    }
  }

  // 设置玩家超时
  public void setUserTimeout(long roomId, long uuid, long potId) {
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      roomTimeInfo = new RoomTimeInfo();
      roomTimeInfo.roomId = roomId;
      roomTimeInfo.startTime = System.currentTimeMillis();
      roomTimeInfo.endTime = roomTimeInfo.startTime + insurance_timeout_seconds * 1000;
      room_timer.put(roomId, roomTimeInfo);
    }
    // 是否设置过这个玩家的超时
    InsuranceTimeInfo timeInfo = roomTimeInfo.userTimeInfos.get(uuid);
    if (timeInfo == null) {
      timeInfo = new InsuranceTimeInfo();
      timeInfo.startTime = roomTimeInfo.startTime;
      timeInfo.endTime = timeInfo.startTime + insurance_timeout_seconds * 1000;
      roomTimeInfo.userTimeInfos.put(uuid, timeInfo);
    }
    // 增加这个用户的边池超时时间
    timeInfo.potIds.add(potId);
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",set timeout");
  }

  // 玩家购买延时
  public void addUserTimeout(long roomId, long uuid) {
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      logger.error("roomId:" + roomId + ",no room info");
      return;
    }
    InsuranceTimeInfo timeInfo = roomTimeInfo.userTimeInfos.get(uuid);
    if (timeInfo == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",no user info");
      return;
    }
    // 增加这一个用户的超时时间
    timeInfo.endTime += insurance_delay_seconds * 1000;
    // 遍历所有玩家的超时时间，找出最大的那一个
    long maxEndTime = 0;
    for (Map.Entry<Long, InsuranceTimeInfo> usertime : roomTimeInfo.userTimeInfos.entrySet()) {
      if (usertime.getValue().endTime > maxEndTime) {
        maxEndTime = usertime.getValue().endTime;
      }
    }
    // 是否超时时间延长了
    if (roomTimeInfo.endTime < maxEndTime) {
      // 重新设定超时时刻
      roomTimeInfo.endTime = maxEndTime;
      // 重新设置超时时间
      long newTimeSpan = roomTimeInfo.endTime - roomTimeInfo.startTime;
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",add timeout");
      room_timer.setTimeout(roomId, newTimeSpan, TimeUnit.MILLISECONDS);
    }

    // 返回这个用户的新延时总时长，单位s
    // return (timeInfo.endTime - timeInfo.startTime) / 1000;
  }

  // 玩家购买保险
  public void removeUserTimeout(long roomId, long uuid, long potId) {
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      return;
    }
    InsuranceTimeInfo timeInfo = roomTimeInfo.userTimeInfos.get(uuid);
    if (timeInfo == null) {
      return;
    }
    // 先去除这个玩家的边池超时
    timeInfo.potIds.remove(potId);
    // 检查是否这个玩家的所有边池超时都清空了
    if (!timeInfo.potIds.isEmpty()) {
      // 边池超时并没有完全结束，所有用户的超时不变，牌局超时不变
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",remove no timeout");
      return;
    }
    // 所以边池已经清空，这次去除这个玩家时间信息
    roomTimeInfo.userTimeInfos.remove(uuid);
    // 是否所有玩家都已经购买了保险
    if (roomTimeInfo.userTimeInfos.isEmpty()) {
      // 删除超时逻辑
      clearRoomTimeout(roomId);
      // （日，小心会产生嵌套调用！！！）通知游戏超时结束，开始一下轮发牌
      // InsuranceTimeoutHandler(roomId);
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",clear room timeout");
      return;
    }
    // 重新计算超时时间，遍历所有玩家的超时时间，找出剩下最大的那一个
    long maxStartTime = 0;
    long maxEndTime = 0;
    for (Map.Entry<Long, InsuranceTimeInfo> usertime : roomTimeInfo.userTimeInfos.entrySet()) {
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
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",sub timeout");
    }
  }

  public InsuranceTimeInfo getUserTimeout(long roomId, long uuid) {
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",no timeout info!");
      return null;
    }
    return roomTimeInfo.userTimeInfos.get(uuid);
  }
}
