/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceDelay.java
 * 创建人:xichen
 * 创建日期:2016年7月8日-下午7:04:30
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 类名称:InsuranceDelay
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月8日-下午7:04:30
 * 修改备注:
 */
public class InsuranceDelay {

  public final ConcurrentMap<Long, ConcurrentMap<Long, Integer>> room_user_delays =
      new ConcurrentHashMap<Long, ConcurrentMap<Long, Integer>>();

  public void onRoomOpen(long roomId) {
    // ConcurrentMap<Long, Integer> user_delays = new ConcurrentHashMap<Long, Integer>();
    // room_user_delays.put(roomId, user_delays);
  }

  public void onRoomClose(long roomId) {
    // room_user_delays.remove(roomId);
  }

  public int getUserDelayCost(long roomId, long uuid) {
    // ConcurrentMap<Long, Integer> user_delays = room_user_delays.get(roomId);
    // if (user_delays == null) {
    // user_delays = new ConcurrentHashMap<Long, Integer>();
    // room_user_delays.put(roomId, user_delays);
    // }
    // int times = 0;
    // if (!user_delays.containsKey(uuid)) {
    // user_delays.put(uuid, 0);
    // return getDelayCost(0);
    // } else {
    // times = user_delays.get(uuid);
    // return getDelayCost(times);
    // }
    int times = (int) InsuranceApi.getUserDelayOperation(roomId, uuid);
    return getDelayCost(times);
  }

  public void addUserDelayTimes(long roomId, long uuid) {
    // ConcurrentMap<Long, Integer> user_delays = room_user_delays.get(roomId);
    // if (user_delays == null) {
    // user_delays = new ConcurrentHashMap<Long, Integer>();
    // room_user_delays.put(roomId, user_delays);
    // }
    // int times = 0;
    // if (!user_delays.containsKey(uuid)) {
    // user_delays.put(uuid, 1);
    // } else {
    // times = user_delays.get(uuid);
    // user_delays.put(uuid, times + 1);
    // }
    int times = (int) InsuranceApi.getUserDelayOperation(roomId, uuid);
    InsuranceApi.setUserDelayOperation(roomId, uuid, times + 1);
  }

  private static int getDelayCost(int times) {
    switch (times) {
      case 0:
        return 0;
      case 1:
        return 20;
    }
    return 50 * (times - 1);
  }
}
