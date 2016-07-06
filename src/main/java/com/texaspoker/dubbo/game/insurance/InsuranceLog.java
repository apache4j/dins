/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceLog.java
 * 创建人:xichen
 * 创建日期:2016年7月2日-下午10:46:23
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

/**
 * 类名称:InsuranceLog
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月2日-下午10:46:23
 * 修改备注:
 */
public class InsuranceLog {

  private final static String ACTION_OPEN = "open";
  private final static String ACTION_CLEAR_TABLE = "cleartable";
  private final static String ACTION_CLOSE_TABLE = "closetable";

  private final static String ACTION_SB = "sb";
  private final static String ACTION_BB = "bb";
  private final static String ACTION_ANTI = "anti";
  private final static String ACTION_HAND = "hand";
  private final static String ACTION_CALL = "call";
  private final static String ACTION_RAISE = "raise";
  private final static String ACTION_CHECK = "check";
  private final static String ACTION_SHOWDOWN = "showdown";
  private final static String ACTION_FOLD = "fold";

  private final static String ACTION_FLOP1 = "flop1";
  private final static String ACTION_FLOP2 = "flop2";
  private final static String ACTION_FLOP3 = "flop3";
  private final static String ACTION_TURN = "turn";
  private final static String ACTION_RIVER = "river";

  public static void onLogMessage(String message) {

    // 区分边界，获取关键信息
    String[] tableLogContentArray = message.split(",");
    // long groupId = 0;
    long roomId = 0;
    long turnId = 0;
    long uuid = 0;
    String actionStr = null;

    try {
      // groupId = Long.parseLong(tableLogContentArray[0]);
      roomId = Long.parseLong(tableLogContentArray[1]);
      turnId = Long.parseLong(tableLogContentArray[2]);
      uuid = Long.parseLong(tableLogContentArray[3]);
      actionStr = tableLogContentArray[4];
    } catch (java.lang.ArrayIndexOutOfBoundsException ee) {
      ee.printStackTrace();
    }

    // 牌局开局，这时还没有turnid，前移一位
    if (tableLogContentArray[3].equalsIgnoreCase(ACTION_OPEN)) {
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_CLOSE_TABLE)) {
      // 清理本次牌局
      HandsManager.getInstance().delHandInfo(roomId);
      return;
    }

    if (actionStr.equalsIgnoreCase(ACTION_SB)) {
      // 本手开始
      HandsManager.getInstance().setHandOpen(roomId, turnId);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_CLEAR_TABLE)) {
      // 本手结束
      HandsManager.getInstance().setHandClose(roomId, turnId);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_SHOWDOWN)) {
      return;
    }

    // 强制下注
    if (actionStr.equalsIgnoreCase(ACTION_SB) || actionStr.equalsIgnoreCase(ACTION_BB)
        || actionStr.equalsIgnoreCase(ACTION_ANTI)) {
      int betChips = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setUserBets(roomId, uuid, betChips);
      return;
    }

    // 下注信息
    if (actionStr.equalsIgnoreCase(ACTION_CALL) || actionStr.equalsIgnoreCase(ACTION_RAISE)) {
      int betChips = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setUserBets(roomId, uuid, betChips);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_FOLD)) {
      HandsManager.getInstance().setUserFold(roomId, uuid);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_CHECK)) {
      return;
    }

    // 手牌信息
    if (actionStr.equalsIgnoreCase(ACTION_HAND)) {
      int[] hands = new int[2];
      hands[0] = Integer.parseInt(tableLogContentArray[5]);
      hands[1] = Integer.parseInt(tableLogContentArray[6]);
      int handChip = Integer.parseInt(tableLogContentArray[7]);
      HandsManager.getInstance().setUserCards(roomId, uuid, hands, handChip);
      return;
    }

    // 公共牌信息
    if (actionStr.equalsIgnoreCase(ACTION_FLOP1)) {
      int tableCard = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP1, tableCard);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_FLOP2)) {
      int tableCard = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP2, tableCard);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_FLOP3)) {
      int tableCard = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP3, tableCard);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_TURN)) {
      int tableCard = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.TURN, tableCard);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_RIVER)) {
      int tableCard = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.RIVER, tableCard);
      return;
    }

  }
}
