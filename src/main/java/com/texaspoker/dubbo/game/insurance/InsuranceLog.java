/**
 * 包名:com.texaspoker.dubbo.game.insurance
 * 文件名:InsuranceLog.java
 * 创建人:xichen
 * 创建日期:2016年7月2日-下午10:46:23
 * Copyright (c) 2016 Illuminate 公司-版权所有
 */
package com.texaspoker.dubbo.game.insurance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * 类名称:InsuranceLog
 * 类描述:
 * 创建人:xichen
 * 修改人:xichen
 * 修改时间:2016年7月2日-下午10:46:23
 * 修改备注:
 */
public class InsuranceLog {

  // private static Logger logger = Logger.getLogger(InsuranceLog.class);

  private final static String ACTION_OPEN = "open";
  private final static String ACTION_CLEAR_TABLE = "cleartable";
  private final static String ACTION_CLOSE_TABLE = "closetable";

  private final static String ACTION_STARTROUND = "startround";
  private final static String ACTION_SB = "sb";
  private final static String ACTION_BB = "bb";
  private final static String ACTION_ANTE = "ante";
  private final static String ACTION_HAND = "hand";
  private final static String ACTION_CALL = "call";
  private final static String ACTION_RAISE = "raise";
  private final static String ACTION_CHECK = "check";
  private final static String ACTION_SHOWDOWN = "showdown";
  private final static String ACTION_FOLD = "fold";

  private final static String ACTION_FLOP1 = "flop1";
  private final static String ACTION_FLOP2 = "flop2";
  private final static String ACTION_FLOP3 = "flop3";

  private final static String ACTION_FLOP = "flop";
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
    } catch (java.lang.ArrayIndexOutOfBoundsException ee) {
      ee.printStackTrace();
    }

    // 牌局开局，这时还没有turnid，前移一位
    if (tableLogContentArray[3].equalsIgnoreCase(ACTION_OPEN)) {
      return;
    } else if (tableLogContentArray[3].equalsIgnoreCase(ACTION_STARTROUND)) {
      // 本手开始
      HandsManager.getInstance().setHandOpen(roomId, turnId);
      return;
    }

    try {
      uuid = Long.parseLong(tableLogContentArray[3]);
      actionStr = tableLogContentArray[4];
    } catch (java.lang.ArrayIndexOutOfBoundsException eee) {
      eee.printStackTrace();
    }

    if (actionStr.equalsIgnoreCase(ACTION_CLEAR_TABLE)) {
      // 本手结束
      HandsManager.getInstance().setHandClose(roomId, turnId);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_CLOSE_TABLE)) {
      // 清理本次牌局
      HandsManager.getInstance().delHandInfo(roomId);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_SHOWDOWN)) {
      return;
    }

    // 强制下注
    if (actionStr.equalsIgnoreCase(ACTION_SB) || actionStr.equalsIgnoreCase(ACTION_BB)
        || actionStr.equalsIgnoreCase(ACTION_ANTE)) {
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

    // 前三张单独发牌信息
    if (actionStr.equalsIgnoreCase(ACTION_FLOP1)) {
      int[] tableCards = new int[1];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP1, tableCards);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_FLOP2)) {
      int[] tableCards = new int[1];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP2, tableCards);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_FLOP3)) {
      int[] tableCards = new int[1];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP3, tableCards);
      return;
    }

    // 正常公共牌信息
    if (actionStr.equalsIgnoreCase(ACTION_FLOP)) {
      int[] tableCards = new int[3];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      tableCards[1] = Integer.parseInt(tableLogContentArray[6]);
      tableCards[2] = Integer.parseInt(tableLogContentArray[7]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.FLOP, tableCards);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_TURN)) {
      int[] tableCards = new int[1];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.TURN, tableCards);
      return;
    } else if (actionStr.equalsIgnoreCase(ACTION_RIVER)) {
      int[] tableCards = new int[1];
      tableCards[0] = Integer.parseInt(tableLogContentArray[5]);
      HandsManager.getInstance().setTableCards(roomId, HandUtils.RIVER, tableCards);
      return;
    }
  }

  private static void readFile(String filepath) {

    File fileToRead = new File(filepath);
    BufferedReader bf = null;
    try {
      bf = new BufferedReader(new FileReader(fileToRead));
      String line;
      while ((line = bf.readLine()) != null) {
        onLogMessage(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (bf != null)
        try {
          bf.close();
          bf = null;
        } catch (IOException ee) {
          ee.printStackTrace();
        }
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("TableLogReader args size error");
      return;
    }
    String logfilepath = args[0];

    // System.out.println("=== start === @: " + time());
    try {
      readFile(logfilepath);
    } catch (NullPointerException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // System.out.println("=== ended === @: " + time());
  }

}
