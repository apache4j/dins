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
    // 用户的超时数据
    ConcurrentMap<Long, InsuranceTimeInfo> userTimeInfos = new ConcurrentHashMap<Long, InsuranceTimeInfo>();;
  }

  // 所有房间的购买超时
  public TimeoutMap<Long, RoomTimeInfo> room_timer;

  // 所有暂停的房间信息
  public ConcurrentMap<Long, RoomTimeInfo> room_pause = new ConcurrentHashMap<Long, RoomTimeInfo>();

  // 所有用户的购买超时
  public TimeoutMap<String, InsuranceTimeInfo> user_timer;

  // 所有暂停的用户信息
  public ConcurrentMap<String, InsuranceTimeInfo> user_pause = new ConcurrentHashMap<String, InsuranceTimeInfo>();

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
                InsuranceRoomTimeoutHandler(thekey, thevalue);
              }
            }).build();
    user_timer =
        TimeoutMap.builder().variableTimeout().timeout(insurance_timeout_seconds, TimeUnit.SECONDS)
            .timeoutListener(new TimeoutListener<String, InsuranceTimeInfo>() {
              public void expired(String thekey, InsuranceTimeInfo thevalue) {
                InsuranceUserTimeoutHandler(thekey, thevalue);
              }
            }).build();
  }

  // 超时处理响应，这个牌桌保险时间超时，需要进行后续操作
  public void InsuranceRoomTimeoutHandler(Long roomId, RoomTimeInfo info) {
    logger.debug("roomId:" + roomId + ",has been timeout");
    if (info != null) {
      // 房间正常超时，每个用户都以此弃牌
      for (Map.Entry<Long, InsuranceTimeInfo> entry : info.userTimeInfos.entrySet()) {
        // 先删除用户的超时，避免重复
        long uuid = entry.getKey();
        info.userTimeInfos.remove(uuid);
        String key = getUserTimeoutKey(roomId, entry.getKey());
        user_timer.remove(key);
        // 这个用户自动弃牌
        InsuranceManager.getInstance().flodInsurance(roomId, uuid);
        // 通知超时
        InsuranceManager.getInstance().notifyUserTimeout(roomId, uuid);
      }
    } else {
      // 房间因为购买，放弃，已经不需要超时，则直接清理房间超时的购买逻辑
      InsuranceManager.getInstance().foldInsurance(roomId);
    }
    // API接口，通知这个房间保险超时
    InsuranceApi.gotoNextStep(roomId);
  }

  // 超时处理响应，这个牌桌保险时间超时，需要进行后续操作
  public void InsuranceUserTimeoutHandler(String key, InsuranceTimeInfo info) {
    logger.debug("key:" + key + ",has been timeout");
    // 清理用户超时的购买逻辑
    long roomId = getUserTimeoutRoomID(key);
    long uuid = getUserTimeoutUUID(key);
    // 检查是否房间的超时逻辑还在，不然不需要超时了
    RoomTimeInfo roomTimeInfo = room_timer.get(roomId);
    if (roomTimeInfo == null) {
      return;
    }
    // 清理房间超时的map信息
    InsuranceTimeInfo timeInfo = roomTimeInfo.userTimeInfos.get(uuid);
    if (timeInfo == null) {
      return;
    }
    roomTimeInfo.userTimeInfos.remove(uuid);
    // 这个用户自动弃牌
    InsuranceManager.getInstance().flodInsurance(roomId, uuid);
    // 通知超时
    InsuranceManager.getInstance().notifyUserTimeout(roomId, uuid);
  }

  public void onRoomOpen(long roomId) {
    logger.debug("roomId:" + roomId + ",open table");
  }

  public void onRoomClose(long roomId) {
    logger.debug("roomId:" + roomId + ",close table");
    room_timer.remove(roomId);
  }

  public void onRoomPause(long roomId) {
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
    logger.debug("roomId:" + roomId + ",pause table");
    // 迁移相关用户
    for (Map.Entry<Long, InsuranceTimeInfo> entry : roomTimeInfo.userTimeInfos.entrySet()) {
      long uuid = entry.getKey();
      user_pause.put(getUserTimeoutKey(roomId, uuid), entry.getValue());
      user_timer.remove(getUserTimeoutKey(roomId, uuid));
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",pause");
    }
  }

  public void onRoomPlay(long roomId) {
    RoomTimeInfo roomTimeInfo = room_pause.get(roomId);
    if (roomTimeInfo == null) {
      logger.error("roomId:" + roomId + ",play room info null");
      return;
    }
    // 重新计算开始时刻
    long currentTime = System.currentTimeMillis();
    long pausePastedSpan = currentTime - roomTimeInfo.pauseTime;
    roomTimeInfo.startTime += pausePastedSpan;
    roomTimeInfo.endTime += pausePastedSpan;
    roomTimeInfo.pauseTime = 0;
    // 重新加时列表,计算剩余超时时间
    long leftTime = roomTimeInfo.endTime - currentTime;
    room_timer.put(roomId, roomTimeInfo, leftTime, TimeUnit.MILLISECONDS);
    room_pause.remove(roomId);
    logger.debug("roomId:" + roomId + ",play table");
    // 迁移相关用户,所有玩家的超时都需要刷新时刻
    for (Map.Entry<Long, InsuranceTimeInfo> entry : roomTimeInfo.userTimeInfos.entrySet()) {
      long uuid = entry.getKey();
      InsuranceTimeInfo timeInfo = entry.getValue();
      // 重置用户时刻
      timeInfo.startTime += pausePastedSpan;
      timeInfo.endTime += pausePastedSpan;
      // 再设置用户超时
      long userLeftTime = timeInfo.endTime - currentTime;
      user_timer.put(getUserTimeoutKey(roomId, uuid), timeInfo, userLeftTime, TimeUnit.MILLISECONDS);
      user_pause.remove(getUserTimeoutKey(roomId, uuid));
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",play,leftTime:" + userLeftTime);
    }
  }

  // 检查是否处于游戏暂停模式
  public boolean checkRoomPause(long roomId) {
    if (room_timer.containsKey(roomId)) {
      logger.debug("roomId:" + roomId + ",is not in pause!");
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
    RoomTimeInfo roomInfo = room_timer.get(roomId);
    if (roomInfo == null) {
      // 房间超时信息突然没有了，这哥们的操作应该是最后一个人的，所以可以调用超时操作
      InsuranceRoomTimeoutHandler(roomId, roomInfo);
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
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",set room timeout");
    }
    // 是否设置过这个玩家的超时
    InsuranceTimeInfo timeInfo = roomTimeInfo.userTimeInfos.get(uuid);
    if (timeInfo == null) {
      timeInfo = new InsuranceTimeInfo();
      timeInfo.startTime = roomTimeInfo.startTime;
      timeInfo.endTime = timeInfo.startTime + insurance_timeout_seconds * 1000;
      roomTimeInfo.userTimeInfos.put(uuid, timeInfo);
      user_timer.put(getUserTimeoutKey(roomId, uuid), timeInfo);
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",set user timeout");
    }
    // 增加这个用户的边池超时时间
    timeInfo.potIds.add(potId);
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
      logger.error("roomId:" + roomId + ",uuid:" + uuid + ",no user info");
      return;
    }
    // 增加这一个用户的超时时间
    timeInfo.endTime += insurance_delay_seconds * 1000;
    // 重新设置这个房间的超时时间
    long userTimeLeft = timeInfo.endTime - System.currentTimeMillis();
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",reset timeout:" + userTimeLeft);
    user_timer.remove(getUserTimeoutKey(roomId, uuid));
    user_timer.put(getUserTimeoutKey(roomId, uuid), timeInfo, userTimeLeft, TimeUnit.MILLISECONDS);
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
      // long newTimeSpan = roomTimeInfo.endTime - roomTimeInfo.startTime;
      long newTimeLeft = roomTimeInfo.endTime - System.currentTimeMillis();
      logger.debug("roomId:" + roomId + ",reset room timeout:" + newTimeLeft);
      room_timer.remove(roomId);
      room_timer.put(roomId, roomTimeInfo, newTimeLeft, TimeUnit.MILLISECONDS);
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
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",remain user timeout");
      return;
    }
    // 清除用户的超时逻辑
    user_timer.remove(getUserTimeoutKey(roomId, uuid));
    // 所以边池已经清空，这次去除这个玩家时间信息
    roomTimeInfo.userTimeInfos.remove(uuid);
    logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",remove user timeout");
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
      roomTimeInfo.endTime = maxEndTime;
      long newTimeLeft = roomTimeInfo.endTime - System.currentTimeMillis();
      logger.debug("roomId:" + roomId + ",reset room timeout:" + newTimeLeft);
      room_timer.remove(roomId);
      room_timer.put(roomId, roomTimeInfo, newTimeLeft, TimeUnit.MILLISECONDS);
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

  // 用户的超时map的key映射
  public static String getUserTimeoutKey(long roomId, long uuid) {
    StringBuilder key = new StringBuilder();
    key.append(roomId);
    key.append(":");
    key.append(uuid);
    return key.toString();
  }

  // 跟进用户的超时key，找出房间信息
  public static long getUserTimeoutRoomID(String key) {
    return Long.parseLong(key.split(":")[0]);
  }

  public static long getUserTimeoutUUID(String key) {
    return Long.parseLong(key.split(":")[1]);
  }
}
