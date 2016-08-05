/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceLimit.java
 * 创建人:xichen
 * 创建日期:2016年7月2日-下午12:16:21
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * 类名称:InsuranceLimit
 * 类描述:保险开关
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月2日-下午12:16:21
 * 修改备注:
 */
public class InsuranceLimit {

  private static Logger logger = Logger.getLogger(InsuranceLimit.class);

  // 房主的操作状态
  private ConcurrentMap<Long, Set<Long>> room_creater_limits = new ConcurrentHashMap<Long, Set<Long>>();
  // 玩家的操作状态
  private ConcurrentMap<Long, Set<Long>> room_user_limits = new ConcurrentHashMap<Long, Set<Long>>();

  // 房间的屏蔽列表
  private ConcurrentMap<Long, Set<Long>> room_limits = new ConcurrentHashMap<Long, Set<Long>>();

  public void onRoomOpen(long roomId) {
    logger.debug("roomId:" + roomId + ",open table");
  }

  public void onRoomClose(long roomId) {
    logger.debug("roomId:" + roomId + ",close table");
    room_creater_limits.remove(roomId);
    room_user_limits.remove(roomId);
    room_limits.remove(roomId);
  }

  // 检查是否关闭这个用户的保险购买信息
  public boolean checkUserCloseInsurance(long roomId, long uuid) {
    Set<Long> roomStatus = room_limits.get(roomId);
    if (roomStatus == null) {
      return false;
    } else {
      return roomStatus.contains(uuid);
    }
  }

  // 获取房主开关状态
  public boolean getCreaterCloseStatus(long roomId, long uuid) {
    Set<Long> limitSet = room_creater_limits.get(roomId);
    if (limitSet == null) {
      return false;
    }
    if (limitSet.contains(uuid)) {
      return true;
    } else {
      return false;
    }
  }

  // 设置房主的开关列表
  public void setCreaterCloseStatus(long roomId, long uuid, boolean close) {
    setOpenStatus(room_creater_limits, roomId, uuid, close);
    refreshRoomOpenState(roomId);
  }

  // 获取用户开关状态
  public boolean getUserCloseStatus(long roomId, long uuid) {
    Set<Long> limitSet = room_user_limits.get(roomId);
    if (limitSet == null) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",close:false");
      return false;
    }
    if (limitSet.contains(uuid)) {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",close:ture");
      return true;
    } else {
      logger.debug("roomId:" + roomId + ",uuid:" + uuid + ",close:false");
      return false;
    }
  }

  // 设置玩家的开关列表
  public void setUserOpenStatus(long roomId, long uuid, boolean close) {
    setOpenStatus(room_user_limits, roomId, uuid, close);
    refreshRoomOpenState(roomId);
  }

  // 更新这个房间的保险关闭信息
  private void refreshRoomOpenState(long roomId) {
    // 刷新前，清空房间的开关信息
    Set<Long> roomStatus = room_limits.get(roomId);
    if (roomStatus == null) {
      roomStatus = Collections.synchronizedSet(new HashSet<Long>());
      room_limits.put(roomId, roomStatus);
    } else {
      roomStatus.clear();
    }
    // 更新房主关闭信息
    Set<Long> createrStatus = room_creater_limits.get(roomId);
    if (createrStatus != null) {
      roomStatus.addAll(createrStatus);
    }
    // 更新玩家关闭信息
    Set<Long> userStatus = room_user_limits.get(roomId);
    if (userStatus != null) {
      roomStatus.addAll(userStatus);
    }
  }

  // 设置关闭信息
  private static void setOpenStatus(Map<Long, Set<Long>> limits, long roomid, long uuid, boolean close) {
    if (close) {
      Set<Long> roomStatus = limits.get(roomid);
      // 是否这个房间没有关闭过保险
      if (roomStatus == null) {
        roomStatus = Collections.synchronizedSet(new HashSet<Long>());
        limits.put(roomid, roomStatus);
      }
      // 这个用户是否已经关闭过保险
      if (roomStatus.contains(uuid)) {
        return;
      }
      // 加入屏蔽集合
      roomStatus.add(uuid);
    } else {
      Set<Long> roomStatus = limits.get(roomid);
      // 没有这个保险屏蔽信息，直接返回成功
      if (roomStatus == null) {
        return;
      }
      // 去掉这个屏蔽
      roomStatus.remove(uuid);
    }
  }
}
