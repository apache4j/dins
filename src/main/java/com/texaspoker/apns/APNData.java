package com.texaspoker.apns;

import com.texaspoker.bill.BillManager;
import com.texaspoker.dubbo.game.insurance.InsuranceManager;
import com.texaspoker.logger.TableLogService;
import com.texaspoker.moment.TexasPoker;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.user.UserManager;

public class APNData {
	public String strToken;
	public String msg;
	public int badge;
}

