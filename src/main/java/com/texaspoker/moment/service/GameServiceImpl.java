package com.texaspoker.moment.service;

import java.util.ArrayList;
import java.util.Set;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.texaspoker.moment.TexasPoker.BUYIN_CONTROL_TYPE;
import com.texaspoker.moment.TexasPoker.BuyinControl;
import com.texaspoker.moment.TexasPoker.GAME_ACT_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.InsuranceUserInfo;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.ERROR_CODE_TYPE;
import com.texaspoker.moment.TexasPokerGameService;
import com.texaspoker.moment.TexasPoker.GameRoomBaseInfo;
import com.texaspoker.moment.TexasPokerGameService.BuyBuyInRequest;
import com.texaspoker.moment.TexasPokerGameService.BuyBuyInResponse;
import com.texaspoker.moment.TexasPokerGameService.BuyInsuranceDelayRequest;
import com.texaspoker.moment.TexasPokerGameService.BuyInsuranceDelayResponse;
import com.texaspoker.moment.TexasPokerGameService.BuyInsuranceRequest;
import com.texaspoker.moment.TexasPokerGameService.BuyInsuranceResponse;
import com.texaspoker.moment.TexasPokerGameService.BuyinActionRequest;
import com.texaspoker.moment.TexasPokerGameService.BuyinActionResponse;
import com.texaspoker.moment.TexasPokerGameService.CreateGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.CreateGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.CreateQuickGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.CreateQuickGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.CreatorInsuranceSettingRequest;
import com.texaspoker.moment.TexasPokerGameService.CreatorInsuranceSettingResponse;
import com.texaspoker.moment.TexasPokerGameService.EnterGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.EnterGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.EnterQuickGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.EnterQuickGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.GameActionRequest;
import com.texaspoker.moment.TexasPokerGameService.GameActionResponse;
import com.texaspoker.moment.TexasPokerGameService.GetAllBuyinControlsRequest;
import com.texaspoker.moment.TexasPokerGameService.GetAllBuyinControlsResponse;
import com.texaspoker.moment.TexasPokerGameService.GetGameRoomInfoRequest;
import com.texaspoker.moment.TexasPokerGameService.GetGameRoomInfoResponse;
import com.texaspoker.moment.TexasPokerGameService.GetGameSettingRequest;
import com.texaspoker.moment.TexasPokerGameService.GetGameSettingResponse;
import com.texaspoker.moment.TexasPokerGameService.GetLastHandIDRequest;
import com.texaspoker.moment.TexasPokerGameService.GetLastHandIDResponse;
import com.texaspoker.moment.TexasPokerGameService.GetQuickGameRoomInfoRequest;
import com.texaspoker.moment.TexasPokerGameService.GetQuickGameRoomInfoResponse;
import com.texaspoker.moment.TexasPokerGameService.GetStandByUserGameInfosRequest;
import com.texaspoker.moment.TexasPokerGameService.GetStandByUserGameInfosResponse;
import com.texaspoker.moment.TexasPokerGameService.GetStandByUuidsRequest;
import com.texaspoker.moment.TexasPokerGameService.GetStandByUuidsResponse;
import com.texaspoker.moment.TexasPokerGameService.GetUserQuickGameRoomInfosRequest;
import com.texaspoker.moment.TexasPokerGameService.GetUserQuickGameRoomInfosResponse;
import com.texaspoker.moment.TexasPokerGameService.GiveMoneyRequest;
import com.texaspoker.moment.TexasPokerGameService.GiveMoneyResponse;
import com.texaspoker.moment.TexasPokerGameService.HelloGameRequest;
import com.texaspoker.moment.TexasPokerGameService.HelloGameResponse;
import com.texaspoker.moment.TexasPokerGameService.LeaveGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.LeaveGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.PauseGameRequest;
import com.texaspoker.moment.TexasPokerGameService.PauseGameResponse;
import com.texaspoker.moment.TexasPokerGameService.QuitQuickGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.QuitQuickGameRoomResponse;
import com.texaspoker.moment.TexasPokerGameService.RemoveStandByUserGameInfosRequest;
import com.texaspoker.moment.TexasPokerGameService.RemoveStandByUserGameInfosResponse;
import com.texaspoker.moment.TexasPokerGameService.ResetBuyinRatioRequest;
import com.texaspoker.moment.TexasPokerGameService.ResetBuyinRatioResponse;
import com.texaspoker.moment.TexasPokerGameService.SetBuyinControlStateRequest;
import com.texaspoker.moment.TexasPokerGameService.SetBuyinControlStateResponse;
import com.texaspoker.moment.TexasPokerGameService.SetGameRoomInfoRequest;
import com.texaspoker.moment.TexasPokerGameService.SetGameRoomInfoResponse;
import com.texaspoker.moment.TexasPokerGameService.SetQuickGameRoomInfoRequest;
import com.texaspoker.moment.TexasPokerGameService.SetQuickGameRoomInfoResponse;
import com.texaspoker.moment.TexasPokerGameService.SetStandByUserGameInfosRequest;
import com.texaspoker.moment.TexasPokerGameService.SetStandByUserGameInfosResponse;
import com.texaspoker.moment.TexasPokerGameService.ShowCardRequest;
import com.texaspoker.moment.TexasPokerGameService.ShowCardResponse;
import com.texaspoker.moment.TexasPokerGameService.SitDownRequest;
import com.texaspoker.moment.TexasPokerGameService.SitDownResponse;
import com.texaspoker.moment.TexasPokerGameService.StandUpRequest;
import com.texaspoker.moment.TexasPokerGameService.StandUpResponse;
import com.texaspoker.moment.TexasPokerGameService.StartGameRequest;
import com.texaspoker.moment.TexasPokerGameService.StartGameResponse;
import com.texaspoker.moment.TexasPokerGameService.StartQuickRoomGameRequest;
import com.texaspoker.moment.TexasPokerGameService.StartQuickRoomGameResponse;
import com.texaspoker.moment.TexasPokerGameService.UseDelayItemRequest;
import com.texaspoker.moment.TexasPokerGameService.UseDelayItemResponse;
import com.texaspoker.moment.TexasPokerGameService.UseItemRequest;
import com.texaspoker.moment.TexasPokerGameService.UseItemResponse;
import com.texaspoker.moment.TexasPokerGameService.UserInsuranceSettingRequest;
import com.texaspoker.moment.TexasPokerGameService.UserInsuranceSettingResponse;
import com.texaspoker.moment.room.GameRoomDetailInfo;
import com.texaspoker.moment.room.GameRoomIDResultInfo;
import com.texaspoker.moment.room.GameRoomManager;
import com.texaspoker.moment.room.GameRoomResultInfo;
import com.texaspoker.moment.room.QuickGameRoomManager;
import com.texaspoker.moment.room.QuickGameRoomResultInfo;
import com.texaspoker.user.UserManager;


public class GameServiceImpl  implements TexasPokerGameService.GameService.Interface, TexasPokerGameService.GameService.BlockingInterface {

	@Override
	public CreateGameRoomResponse createGameRoom(RpcController controller,
			CreateGameRoomRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		CreateGameRoomResponse.Builder  stCreateGameRoomResponseBuilder = CreateGameRoomResponse.newBuilder();
		GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = GameRoomBaseInfo.newBuilder();
		stGameRoomBaseInfoBuilder.setStrRoomName(request.getStGameRoomBaseInfo().getStrRoomName());
		stGameRoomBaseInfoBuilder.setLDuration(request.getStGameRoomBaseInfo().getLDuration());
		stGameRoomBaseInfoBuilder.setBPrivateRoom(request.getStGameRoomBaseInfo().getBPrivateRoom());
		stGameRoomBaseInfoBuilder.setLChatRoomID(request.getStGameRoomBaseInfo().getLChatRoomID());
		stGameRoomBaseInfoBuilder.setBBuyIn(1);
		stGameRoomBaseInfoBuilder.setLSmallBlinds(request.getStGameRoomBaseInfo().getLSmallBlinds());
		stGameRoomBaseInfoBuilder.setLBigBlinds(request.getStGameRoomBaseInfo().getLBigBlinds());
		stGameRoomBaseInfoBuilder.setLBuyIn(request.getStGameRoomBaseInfo().getLBuyIn());
		stGameRoomBaseInfoBuilder.setLCreateTime(System.currentTimeMillis());
		stGameRoomBaseInfoBuilder.setLDuration(request.getStGameRoomBaseInfo().getLDuration());
		stGameRoomBaseInfoBuilder.setECreateRoomType(request.getStGameRoomBaseInfo().getECreateRoomType());
		stGameRoomBaseInfoBuilder.setIMaxBuyinRatio(request.getStGameRoomBaseInfo().getIMaxBuyinRatio());
		stGameRoomBaseInfoBuilder.setIMinBuyinRatio(request.getStGameRoomBaseInfo().getIMinBuyinRatio());
		stGameRoomBaseInfoBuilder.setEGameRoomSeniorType(request.getStGameRoomBaseInfo().getEGameRoomSeniorType());
		stGameRoomBaseInfoBuilder.setBBuyinControl(request.getStGameRoomBaseInfo().getBBuyinControl());
		stGameRoomBaseInfoBuilder.setLGameRoomId(0);
		stGameRoomBaseInfoBuilder.setIGameRoomUserMaxNums(request.getStGameRoomBaseInfo().getIGameRoomUserMaxNums());
		stGameRoomBaseInfoBuilder.setStCreateUserBaseInfo(UserManager.getInstance().getUserBaseInfo(request.getUuid()));
		stGameRoomBaseInfoBuilder.setIAnte(request.getStGameRoomBaseInfo().getIAnte());
		stGameRoomBaseInfoBuilder.setBInsurance(1);
		GameRoomIDResultInfo stGameRoomIDResultInfo = GameRoomManager.getInstance().createGameRoom(request.getUuid()
				,stGameRoomBaseInfoBuilder.build());
		if(stGameRoomIDResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			if(stGameRoomIDResultInfo.getlGameRoomID() > 0){
				GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomIDResultInfo.getlGameRoomID());
				if(stGameRoomDetailInfo!=null){
					stCreateGameRoomResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
					stCreateGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				}else{
					stCreateGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
				}
			}else{
				stCreateGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
		}else{
			stCreateGameRoomResponseBuilder.setEErrCode(stGameRoomIDResultInfo.geteErrorCodeType());
		}
		return stCreateGameRoomResponseBuilder.build();
	}

	@Override
	public EnterGameRoomResponse enterGameRoom(RpcController controller,
			EnterGameRoomRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		EnterGameRoomResponse.Builder  stEnterGameRoomResponseBuilder = EnterGameRoomResponse.newBuilder();

		try{
			GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
			
			if(stGameRoomDetailInfo!=null){
				ERROR_CODE_TYPE eErrorCodeType =  stGameRoomDetailInfo.enterGameRoom(request.getUuid());
				if(eErrorCodeType == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					stEnterGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stEnterGameRoomResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
				}else{
					stEnterGameRoomResponseBuilder.setEErrCode(eErrorCodeType);
				}
			}else{
				System.out.println("1111111111222222222222222 : " + request.getLGameRoomID());
				stEnterGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
			
		}catch(Exception e){
			System.out.println("out1gService throw unexpected exception, : " + request.getUuid());
			e.printStackTrace();
		}
		
		return stEnterGameRoomResponseBuilder.build();
	}

	@Override
	public SitDownResponse sitDown(RpcController controller,
			SitDownRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		SitDownResponse.Builder  stSitDownResponseBuilder = SitDownResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE eErrorCodeType = stGameRoomDetailInfo.sitdown(request.getUuid(),request.getIPos());
			
			if(eErrorCodeType == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stSitDownResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stSitDownResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stSitDownResponseBuilder.setEErrCode(eErrorCodeType);
			}
		}else{
			stSitDownResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stSitDownResponseBuilder.build();
	}

	@Override
	public StandUpResponse standup(RpcController controller,
			StandUpRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		StandUpResponse.Builder  stStandUpResponseBuilder = StandUpResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE eErrorCodeType = stGameRoomDetailInfo.doStandUpAction(request.getUuid(), false);
			
			if(eErrorCodeType == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stStandUpResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stStandUpResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stStandUpResponseBuilder.setEErrCode(eErrorCodeType);
			}
		}else{
			stStandUpResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stStandUpResponseBuilder.build();
	}

	@Override
	public GameActionResponse gameAction(RpcController controller,
			GameActionRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		GameActionResponse.Builder  stGameActionResponseBuilder = GameActionResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			
			//获取游戏房间信息
			GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomDetailInfo.getStGameRoomInfo().toBuilder();
			
			stGameRoomInfoBuilder.setBStateChange(0);
			stGameRoomDetailInfo.setStGameRoomInfo(stGameRoomInfoBuilder.build()) ;
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			
			switch(request.getEGameActType().getNumber()){
			case GAME_ACT_TYPE.GAME_ACT_TYPE_FOLD_VALUE:{
				rst = stGameRoomDetailInfo.doFlodAction(request.getUuid(),request.getLBettingID());
			}
			break;
			case GAME_ACT_TYPE.GAME_ACT_TYPE_CHECK_VALUE:{	
				rst = stGameRoomDetailInfo.doCheckAction(request.getUuid(),request.getLBettingID());
			}
			break;
			case GAME_ACT_TYPE.GAME_ACT_TYPE_CALL_VALUE:{
				rst = stGameRoomDetailInfo.doCallAction(request.getUuid(),request.getLStacks(),request.getLBettingID());
			}
			break;
			case GAME_ACT_TYPE.GAME_ACT_TYPE_BET_VALUE:
			case GAME_ACT_TYPE.GAME_ACT_TYPE_RAISE_VALUE:
			case GAME_ACT_TYPE.GAME_ACT_TYPE_ALLIN_VALUE:{
				rst = stGameRoomDetailInfo.doRaiseAction(request.getUuid(),request.getEGameActType(), request.getLStacks(),request.getLBettingID());
			}
			break;
			}
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stGameActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stGameActionResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
				
				//把目前牌桌的状态push给用户
				GameRoomManager.getInstance().setGameRoomDetailInfo(stGameRoomDetailInfo);
				stGameRoomDetailInfo.removeTimeoutTimesUser(request.getUuid());
			}else{
				stGameActionResponseBuilder.setEErrCode(rst);
			}
		}else{
			stGameActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGameActionResponseBuilder.build();
	}

	@Override
	public BuyBuyInResponse buyBuyIn(RpcController controller,
			BuyBuyInRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		BuyBuyInResponse.Builder  stBuyBuyInResponseBuilder = BuyBuyInResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			
			//不需要买入控制
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBBuyinControl() == 0 || 
					(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBBuyinControl() == 1
					&&stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid())){
				rst = stGameRoomDetailInfo.buyBuyin(request.getUuid(), request.getLStacks());
			}
			//需要买入控制
			else{
				//发送买入控制的push
				rst = stGameRoomDetailInfo.buyBuyinControl(request.getUuid(), request.getLStacks());
			}
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stBuyBuyInResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stBuyBuyInResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stBuyBuyInResponseBuilder.setEErrCode(rst);
			}
		}else{
			stBuyBuyInResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stBuyBuyInResponseBuilder.build();
	}

	@Override
	public LeaveGameRoomResponse leaveGameRoom(RpcController controller,
			LeaveGameRoomRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		LeaveGameRoomResponse.Builder  stLeaveGameRoomResponseBuilderBuilder = LeaveGameRoomResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			UserGameInfoNet stUserGameInfoNet = stGameRoomDetailInfo.leaveGameRoom(request.getUuid());
			
			if(stUserGameInfoNet != null){
				stLeaveGameRoomResponseBuilderBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stLeaveGameRoomResponseBuilderBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stLeaveGameRoomResponseBuilderBuilder.setEErrCode(rst);
			}
		}else{
			stLeaveGameRoomResponseBuilderBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stLeaveGameRoomResponseBuilderBuilder.build();
	}

	@Override
	public ShowCardResponse showCard(RpcController controller,
			ShowCardRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		ShowCardResponse.Builder  stShowCardResponseBuilder = ShowCardResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			
			ERROR_CODE_TYPE rst = stGameRoomDetailInfo.showcard(request.getLHandID(), request.getUuid(), request.getBShowCardOne(), request.getBShowCardTwo());
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stShowCardResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stShowCardResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stShowCardResponseBuilder.setEErrCode(rst);
			}
		}else{
			stShowCardResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stShowCardResponseBuilder.build();
	}

	@Override
	public StartGameResponse startGame(RpcController controller,
			StartGameRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		StartGameResponse.Builder  stStartGameResponseBuilder = StartGameResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.startGame(request.getUuid());
			
			stStartGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			stStartGameResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
		}else{
			stStartGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stStartGameResponseBuilder.build();
	}

	@Override
	public BuyinActionResponse buyinAction(RpcController controller,
			BuyinActionRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		BuyinActionResponse.Builder  stBuyinActionResponseBuilder = BuyinActionResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());

		if(stGameRoomDetailInfo!=null){
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
				ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_ACCEPT){
					rst = stGameRoomDetailInfo.buyinControlActionAccept(request.getLActionUuid());
				}else if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_DENY){
					rst = stGameRoomDetailInfo.buyinControlActionDeny(request.getLActionUuid(),false);
				}
				
				if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stBuyinActionResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
				}else{
					stBuyinActionResponseBuilder.setEErrCode(rst);
				}
			}else{
				stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
			}
			
		}else{
			stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stBuyinActionResponseBuilder.build();
	}

	@Override
	public PauseGameResponse pauseGame(RpcController controller,
			PauseGameRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		PauseGameResponse.Builder  stPauseGameResponseBuilder = PauseGameResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBGamePause() == 0
					&&request.getBPauseGame() == 1){
				rst = stGameRoomDetailInfo.pauseGame(request.getUuid());
			}else if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBGamePause() == 1
					&&request.getBPauseGame() == 0){
				rst = stGameRoomDetailInfo.resumeGame(request.getUuid());
			}else{
				rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_ERROR_PAUSE_COMMAND; 
			}
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stPauseGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stPauseGameResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stPauseGameResponseBuilder.setEErrCode(rst);
			}
		}else{
			stPauseGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stPauseGameResponseBuilder.build();
	}

	@Override
	public UseItemResponse useItem(RpcController controller,
			UseItemRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		UseItemResponse.Builder  stUseItemResponseBuilder = UseItemResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			rst = stGameRoomDetailInfo.useItem(request.getUuid(), request.getTouuid(), request.getLMsgID() , request.getIItemID(), request.getSExtra());
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stUseItemResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			}else{
				stUseItemResponseBuilder.setEErrCode(rst);
			}
			
		}else{
			stUseItemResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stUseItemResponseBuilder.build();
	}

	@Override
	public UseDelayItemResponse useDelayItem(RpcController controller,
			UseDelayItemRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		UseDelayItemResponse.Builder  stUseDelayItemResponseBuilder = UseDelayItemResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			rst = stGameRoomDetailInfo.useDelayItem(request.getUuid(), request.getLBettingID());
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stUseDelayItemResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stUseDelayItemResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stUseDelayItemResponseBuilder.setEErrCode(rst);
			}
			
		}else{
			stUseDelayItemResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stUseDelayItemResponseBuilder.build();
	}
	
	@Override
	public SetBuyinControlStateResponse setBuyinControlState(
			RpcController controller, SetBuyinControlStateRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		SetBuyinControlStateResponse.Builder  stSetBuyinControlStateResponseBuilder = SetBuyinControlStateResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
				ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				rst = stGameRoomDetailInfo.resetBuyinState(request.getBBuyinControl());
				
				if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					stSetBuyinControlStateResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stSetBuyinControlStateResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
				}else{
					stSetBuyinControlStateResponseBuilder.setEErrCode(rst);
				}
			}else{
				stSetBuyinControlStateResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
			}
		}else{
			stSetBuyinControlStateResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stSetBuyinControlStateResponseBuilder.build();
	}
	
	@Override
	public ResetBuyinRatioResponse resetBuyinRatio(RpcController controller,
			ResetBuyinRatioRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		ResetBuyinRatioResponse.Builder  stResetBuyinRatioResponseBuilder = ResetBuyinRatioResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
			rst = stGameRoomDetailInfo.resetBuyinRatio(request.getUuid(), request.getIMinBuyinRatio(), request.getIMaxBuyinRatio());
			
			if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
				stResetBuyinRatioResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stResetBuyinRatioResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
			}else{
				stResetBuyinRatioResponseBuilder.setEErrCode(rst);
			}
			
		}else{
			stResetBuyinRatioResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stResetBuyinRatioResponseBuilder.build();
	}

	@Override
	public CreateQuickGameRoomResponse createQuickGameRoom(
			RpcController controller, CreateQuickGameRoomRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		CreateQuickGameRoomResponse.Builder  stCreateQuickGameRoomResponseBuilder = CreateQuickGameRoomResponse.newBuilder();

		QuickGameRoomResultInfo stQuickGameRoomResultInfo = QuickGameRoomManager.getInstance().createQuickGameRoom(request.getUuid(), request.getStGameRoomBaseInfo());
		
		if(stQuickGameRoomResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			stCreateQuickGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			stCreateQuickGameRoomResponseBuilder.setStQuickGameRoomInfo(stQuickGameRoomResultInfo.getStQuickGameRoomInfo());
		}else{
			stCreateQuickGameRoomResponseBuilder.setEErrCode(stQuickGameRoomResultInfo.geteErrorCodeType());
		}	
		
		return stCreateQuickGameRoomResponseBuilder.build();
	}

	@Override
	public EnterQuickGameRoomResponse enterQuickGameRoom(
			RpcController controller, EnterQuickGameRoomRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		EnterQuickGameRoomResponse.Builder  stEnterQuickGameRoomResponseBuilder = EnterQuickGameRoomResponse.newBuilder();
		QuickGameRoomResultInfo stQuickGameRoomResultInfo = QuickGameRoomManager.getInstance().enterQuickGameRoom(request.getUuid(), request.getSCryptCode());
		if(stQuickGameRoomResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			stEnterQuickGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			stEnterQuickGameRoomResponseBuilder.setStQuickGameRoomInfo(stQuickGameRoomResultInfo.getStQuickGameRoomInfo());
			if(stQuickGameRoomResultInfo.getStGameRoomInfo()!=null){
				stEnterQuickGameRoomResponseBuilder.setStGameRoomInfo(stQuickGameRoomResultInfo.getStGameRoomInfo());
			}
		}else{
			stEnterQuickGameRoomResponseBuilder.setEErrCode(stQuickGameRoomResultInfo.geteErrorCodeType());
		}	
		
		return stEnterQuickGameRoomResponseBuilder.build();
	}

	@Override
	public StartQuickRoomGameResponse startQuickRoomGame(
			RpcController controller, StartQuickRoomGameRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		StartQuickRoomGameResponse.Builder  stStartQuickRoomGameResponseBuilder = StartQuickRoomGameResponse.newBuilder();
		
		GameRoomResultInfo stGameRoomResultInfo = QuickGameRoomManager.getInstance().startQuickGameRoom(request.getUuid(), request.getLQuickGameRoomID());
		
		if(stGameRoomResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			stStartQuickRoomGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			stStartQuickRoomGameResponseBuilder.setStGameRoomInfo(stGameRoomResultInfo.getStGameRoomInfo());
		}else{
			stStartQuickRoomGameResponseBuilder.setEErrCode(stGameRoomResultInfo.geteErrorCodeType());
		}	
		
		return stStartQuickRoomGameResponseBuilder.build();
	}
	
	@Override
	public QuitQuickGameRoomResponse quitQuickGameRoom(
			RpcController controller, QuitQuickGameRoomRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		QuitQuickGameRoomResponse.Builder  stQuitQuickGameRoomResponseBuilder = QuitQuickGameRoomResponse.newBuilder();
			
		QuickGameRoomResultInfo stQuickGameRoomResultInfo = QuickGameRoomManager.getInstance().quitQuickGameRoom(request.getUuid(), request.getLQuickGameRoomID());
			
		if(stQuickGameRoomResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			stQuitQuickGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
		}else{
			stQuitQuickGameRoomResponseBuilder.setEErrCode(stQuickGameRoomResultInfo.geteErrorCodeType());
		}	
		
		return stQuitQuickGameRoomResponseBuilder.build();
	}

	@Override
	public SetStandByUserGameInfosResponse setStandByUserGameInfos(
			RpcController controller, SetStandByUserGameInfosRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		SetStandByUserGameInfosResponse.Builder  stSetStandByUserGameInfosResponseBuilder = SetStandByUserGameInfosResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.setStandByUserGameInfos(request.getStUserGameInfoNet().getStUserInfo().getUuid(), request.getStUserGameInfoNet().toBuilder());
			stSetStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
		}else{
			stSetStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stSetStandByUserGameInfosResponseBuilder.build();
	}

	@Override
	public RemoveStandByUserGameInfosResponse removeStandByUserGameInfos(
			RpcController controller, RemoveStandByUserGameInfosRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		RemoveStandByUserGameInfosResponse.Builder  stRemoveStandByUserGameInfosResponseBuilder = RemoveStandByUserGameInfosResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.removeStandByUserGameInfos(request.getUuid());
			stRemoveStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
		}else{
			stRemoveStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stRemoveStandByUserGameInfosResponseBuilder.build();
	}

	@Override
	public GetStandByUserGameInfosResponse getStandByUserGameInfos(
			RpcController controller, GetStandByUserGameInfosRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		GetStandByUserGameInfosResponse.Builder  stGetStandByUserGameInfosResponseBuilder = GetStandByUserGameInfosResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			UserGameInfoNet.Builder stUserGameInfoNetBuilder = stGameRoomDetailInfo.getStandByUserGameInfos(request.getUuid());

			if(stUserGameInfoNetBuilder!=null){
				stGetStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stGetStandByUserGameInfosResponseBuilder.setStUserGameInfoNet(stUserGameInfoNetBuilder.build());
			}else{
				stGetStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
		}else{
			stGetStandByUserGameInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGetStandByUserGameInfosResponseBuilder.build();
	}


	@Override
	public GetGameRoomInfoResponse getGameRoomInfo(RpcController controller,
			GetGameRoomInfoRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		GetGameRoomInfoResponse.Builder  stGetGameRoomInfoResponseBuilder = GetGameRoomInfoResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			
			GameRoomInfo stGameRoomInfo = stGameRoomDetailInfo.getStGameRoomInfo();
			if(stGameRoomInfo!=null){
				
				GameRoomInfo.Builder stGameRoomInfoBuilder = stGameRoomInfo.toBuilder();
			
				if(request.getUuid() > 0){
					
					GameRoomBaseInfo.Builder stGameRoomBaseInfoBuilder = stGameRoomInfoBuilder.getStGameRoomBaseInfoBuilder();
					boolean bUsedInGame = stGameRoomDetailInfo.usedInGame(request.getUuid());
					if(bUsedInGame){
						stGameRoomBaseInfoBuilder.setBInGame(1);
					}else{
						stGameRoomBaseInfoBuilder.setBInGame(0);
					}
					stGameRoomInfoBuilder.setStGameRoomBaseInfo(stGameRoomBaseInfoBuilder);
				}

				stGetGameRoomInfoResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stGetGameRoomInfoResponseBuilder.setStGameRoomInfo(stGameRoomInfoBuilder);
			}else{
				stGetGameRoomInfoResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
		}else{
			stGetGameRoomInfoResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGetGameRoomInfoResponseBuilder.build();
	}

	@Override
	public SetGameRoomInfoResponse setGameRoomInfo(RpcController controller,
			SetGameRoomInfoRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		SetGameRoomInfoResponse.Builder  stSetGameRoomInfoResponseBuilder = SetGameRoomInfoResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null 
				&& request.getStGameRoomInfo()!=null 
				&& request.getStGameRoomInfo().getStGameRoomBaseInfo().getLGameRoomId() == request.getLGameRoomID()){
			
			stGameRoomDetailInfo.setStGameRoomInfo(request.getStGameRoomInfo());

			stSetGameRoomInfoResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
		}else{
			stSetGameRoomInfoResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stSetGameRoomInfoResponseBuilder.build();
	}
	
	@Override
	public GiveMoneyResponse giveMoney(RpcController controller,
			GiveMoneyRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetAllBuyinControlsResponse getAllBuyinControls(
			RpcController controller, GetAllBuyinControlsRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		GetAllBuyinControlsResponse.Builder  stGetAllBuyinControlsResponseBuilder = GetAllBuyinControlsResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null&&stGameRoomDetailInfo.getStGameRoomInfo()!=null){
			if(request.getUuid() == stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid()){
				ArrayList<BuyinControl> vBuyinControls = stGameRoomDetailInfo.getAllBuyinControls();
				if(vBuyinControls!=null){
					stGetAllBuyinControlsResponseBuilder.addAllVBuyinControls(vBuyinControls);
				}
				stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				
			}else{
				stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
			}
		}else{
			stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGetAllBuyinControlsResponseBuilder.build();
	}

	@Override
	public GetStandByUuidsResponse getStandByUuids(RpcController controller,
			GetStandByUuidsRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		GetStandByUuidsResponse.Builder  stGetStandByUuidsResponseBuilder = GetStandByUuidsResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null&&stGameRoomDetailInfo.getStGameRoomInfo()!=null){
			ArrayList<Long> vUuids = new ArrayList<Long>(stGameRoomDetailInfo.getStandByUuids());
			if(vUuids!=null){
				stGetStandByUuidsResponseBuilder.addAllVStandByUuids(vUuids);
			}
			stGetStandByUuidsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				
		}else{
			stGetStandByUuidsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGetStandByUuidsResponseBuilder.build();
	}

	@Override
	public GetLastHandIDResponse getLastHandID(RpcController controller,
			GetLastHandIDRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		GetLastHandIDResponse.Builder  stGetLastHandIDResponse = GetLastHandIDResponse.newBuilder();
		
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null&&stGameRoomDetailInfo.getStGameRoomInfo()!=null){
			long lHandID = stGameRoomDetailInfo.getLastHandID();
			stGetLastHandIDResponse.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
			stGetLastHandIDResponse.setLHandID(lHandID);
		}else{
			stGetLastHandIDResponse.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
		return stGetLastHandIDResponse.build();
	}
	
	@Override
	public GetQuickGameRoomInfoResponse getQuickGameRoomInfo(
			RpcController controller, GetQuickGameRoomInfoRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		GetQuickGameRoomInfoResponse.Builder  stGetQuickGameRoomInfoRequestBuilder = GetQuickGameRoomInfoResponse.newBuilder();
		
		QuickGameRoomInfo.Builder stQuickGameRoomInfoBuilder = QuickGameRoomManager.getInstance().getQuickGameRoomInfoByRoomID(request.getLQuickGameRoomID());
		if(stQuickGameRoomInfoBuilder!=null){
			stGetQuickGameRoomInfoRequestBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
			stGetQuickGameRoomInfoRequestBuilder.setStQuickGameRoomInfo(stQuickGameRoomInfoBuilder);
		}else{
			stGetQuickGameRoomInfoRequestBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
		}
		
		return stGetQuickGameRoomInfoRequestBuilder.build();
	}

	@Override
	public SetQuickGameRoomInfoResponse setQuickGameRoomInfo(
			RpcController controller, SetQuickGameRoomInfoRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		SetQuickGameRoomInfoResponse.Builder  stSetQuickGameRoomInfoRequestBuilder = SetQuickGameRoomInfoResponse.newBuilder();
		
		
		if(request.getStQuickGameRoomInfo()!=null){
			QuickGameRoomManager.getInstance().setQuickGameRoomInfos(request.getStQuickGameRoomInfo().toBuilder());
			stSetQuickGameRoomInfoRequestBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		}else{
			stSetQuickGameRoomInfoRequestBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
		}
		
		return stSetQuickGameRoomInfoRequestBuilder.build();
	}


	@Override
	public GetUserQuickGameRoomInfosResponse getUserQuickGameRoomInfos(
			RpcController controller, GetUserQuickGameRoomInfosRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		GetUserQuickGameRoomInfosResponse.Builder  stGetUserQuickGameRoomInfosResponseBuilder = GetUserQuickGameRoomInfosResponse.newBuilder();
		Set<Long>  vQuickRooms = QuickGameRoomManager.getInstance().getUserQuickGameRoomInfos(request.getUuid());
		if(vQuickRooms!=null){
			stGetUserQuickGameRoomInfosResponseBuilder.addAllVQuickRooms(vQuickRooms);
			stGetUserQuickGameRoomInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		}else{
			stGetUserQuickGameRoomInfosResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_QUICK_GAMEROOM);
		}
		
		return stGetUserQuickGameRoomInfosResponseBuilder.build();	
	}
	
	@Override
	public void createGameRoom(RpcController controller,
			CreateGameRoomRequest request,
			RpcCallback<CreateGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enterGameRoom(RpcController controller,
			EnterGameRoomRequest request,
			RpcCallback<EnterGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sitDown(RpcController controller, SitDownRequest request,
			RpcCallback<SitDownResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void standup(RpcController controller, StandUpRequest request,
			RpcCallback<StandUpResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameAction(RpcController controller, GameActionRequest request,
			RpcCallback<GameActionResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void buyBuyIn(RpcController controller, BuyBuyInRequest request,
			RpcCallback<BuyBuyInResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leaveGameRoom(RpcController controller,
			LeaveGameRoomRequest request,
			RpcCallback<LeaveGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showCard(RpcController controller, ShowCardRequest request,
			RpcCallback<ShowCardResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startGame(RpcController controller, StartGameRequest request,
			RpcCallback<StartGameResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void buyinAction(RpcController controller,
			BuyinActionRequest request, RpcCallback<BuyinActionResponse> done) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void pauseGame(RpcController controller, PauseGameRequest request,
			RpcCallback<PauseGameResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void useItem(RpcController controller, UseItemRequest request,
			RpcCallback<UseItemResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void useDelayItem(RpcController controller,
			UseDelayItemRequest request, RpcCallback<UseDelayItemResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBuyinControlState(RpcController controller,
			SetBuyinControlStateRequest request,
			RpcCallback<SetBuyinControlStateResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetBuyinRatio(RpcController controller,
			ResetBuyinRatioRequest request,
			RpcCallback<ResetBuyinRatioResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createQuickGameRoom(RpcController controller,
			CreateQuickGameRoomRequest request,
			RpcCallback<CreateQuickGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enterQuickGameRoom(RpcController controller,
			EnterQuickGameRoomRequest request,
			RpcCallback<EnterQuickGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startQuickRoomGame(RpcController controller,
			StartQuickRoomGameRequest request,
			RpcCallback<StartQuickRoomGameResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStandByUserGameInfos(RpcController controller,
			SetStandByUserGameInfosRequest request,
			RpcCallback<SetStandByUserGameInfosResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeStandByUserGameInfos(RpcController controller,
			RemoveStandByUserGameInfosRequest request,
			RpcCallback<RemoveStandByUserGameInfosResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getStandByUserGameInfos(RpcController controller,
			GetStandByUserGameInfosRequest request,
			RpcCallback<GetStandByUserGameInfosResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void quitQuickGameRoom(RpcController controller,
			QuitQuickGameRoomRequest request,
			RpcCallback<QuitQuickGameRoomResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getGameRoomInfo(RpcController controller,
			GetGameRoomInfoRequest request,
			RpcCallback<GetGameRoomInfoResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGameRoomInfo(RpcController controller,
			SetGameRoomInfoRequest request,
			RpcCallback<SetGameRoomInfoResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void giveMoney(RpcController controller, GiveMoneyRequest request,
			RpcCallback<GiveMoneyResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getAllBuyinControls(RpcController controller,
			GetAllBuyinControlsRequest request,
			RpcCallback<GetAllBuyinControlsResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getStandByUuids(RpcController controller,
			GetStandByUuidsRequest request,
			RpcCallback<GetStandByUuidsResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getLastHandID(RpcController controller,
			GetLastHandIDRequest request,
			RpcCallback<GetLastHandIDResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getQuickGameRoomInfo(RpcController controller,
			GetQuickGameRoomInfoRequest request,
			RpcCallback<GetQuickGameRoomInfoResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuickGameRoomInfo(RpcController controller,
			SetQuickGameRoomInfoRequest request,
			RpcCallback<SetQuickGameRoomInfoResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getUserQuickGameRoomInfos(RpcController controller,
			GetUserQuickGameRoomInfosRequest request,
			RpcCallback<GetUserQuickGameRoomInfosResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HelloGameResponse helloGame(RpcController controller,
			HelloGameRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		HelloGameResponse.Builder  stHelloGameResponseBuilder = HelloGameResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.helloGame(request.getUuid());
		}
		stHelloGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stHelloGameResponseBuilder.build();
	}

	@Override
	public void helloGame(RpcController controller, HelloGameRequest request,
			RpcCallback<HelloGameResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BuyInsuranceResponse buyInsurance(RpcController controller,
			BuyInsuranceRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		BuyInsuranceResponse.Builder  stBuyInsuranceResponseBuilder = BuyInsuranceResponse.newBuilder();
		stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		
		GameRoomInfo stGameRoomInfo = stGameRoomDetailInfo.buyInsurance(request.getUuid(), request.getLGameHandID(), request.getEGameRoomStatus(), request.getStBuyInsuranceInfoList());
		
		if(stGameRoomInfo!=null){
			stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			stBuyInsuranceResponseBuilder.setStGameRoomInfo(stGameRoomInfo);
		}else{
			stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);	
		}
		stGameRoomDetailInfo.doInsuranceTimeout();
		return stBuyInsuranceResponseBuilder.build();
	}

	@Override
	public BuyInsuranceDelayResponse buyInsuranceDelay(
			RpcController controller, BuyInsuranceDelayRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		
		BuyInsuranceDelayResponse.Builder stBuyInsuranceDelayResponseBuilder = BuyInsuranceDelayResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());

		if(stGameRoomDetailInfo!=null){
			InsuranceUserInfo stInsuranceUserInfo= stGameRoomDetailInfo.buyInsuranceDealy(request.getUuid(), request.getLGameHandID(), request.getEGameRoomStatus());
			
			if(stInsuranceUserInfo!=null){
				stBuyInsuranceDelayResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stBuyInsuranceDelayResponseBuilder.setStInsuranceUserInfo(stInsuranceUserInfo);
				
				return stBuyInsuranceDelayResponseBuilder.build();
			}else{
				stBuyInsuranceDelayResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
		}else{
			stBuyInsuranceDelayResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);	
		}
		
		return stBuyInsuranceDelayResponseBuilder.build();
	}

	@Override
	public CreatorInsuranceSettingResponse creatorInsuranceSetting(
			RpcController controller, CreatorInsuranceSettingRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		CreatorInsuranceSettingResponse.Builder  stCreatorInsuranceSettingResponseBuilder = CreatorInsuranceSettingResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		ERROR_CODE_TYPE eErrorCodeType = stGameRoomDetailInfo.creatorInsuranceSetting(request.getUuid(), request.getLSettedUuid(), request.getBCanInsurance());
		stCreatorInsuranceSettingResponseBuilder.setEErrCode(eErrorCodeType);
		return stCreatorInsuranceSettingResponseBuilder.build();
	}

	@Override
	public UserInsuranceSettingResponse userInsuranceSetting(
			RpcController controller, UserInsuranceSettingRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		UserInsuranceSettingResponse.Builder  stUserInsuranceSettingResponseBuilder= UserInsuranceSettingResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		ERROR_CODE_TYPE eErrorCodeType = stGameRoomDetailInfo.userInsuranceSetting(request.getUuid(), request.getBDefaultInsurance());
		stUserInsuranceSettingResponseBuilder.setEErrCode(eErrorCodeType);
		return stUserInsuranceSettingResponseBuilder.build();
	}

	@Override
	public void buyInsurance(RpcController controller,
			BuyInsuranceRequest request, RpcCallback<BuyInsuranceResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void buyInsuranceDelay(RpcController controller,
			BuyInsuranceDelayRequest request,
			RpcCallback<BuyInsuranceDelayResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void creatorInsuranceSetting(RpcController controller,
			CreatorInsuranceSettingRequest request,
			RpcCallback<CreatorInsuranceSettingResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void userInsuranceSetting(RpcController controller,
			UserInsuranceSettingRequest request,
			RpcCallback<UserInsuranceSettingResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GetGameSettingResponse getGameSetting(RpcController controller,
			GetGameSettingRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		GetGameSettingResponse.Builder  stGetGameSettingResponseBuilder = GetGameSettingResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		System.out.println("getGameSetting : " + request.getLGameRoomID() + "  " + stGameRoomDetailInfo);
		int bNotBuyInsurance = stGameRoomDetailInfo.getGameSetting(request.getUuid());

		stGetGameSettingResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
		stGetGameSettingResponseBuilder.setBNotBuyInsurance(bNotBuyInsurance);
		return stGetGameSettingResponseBuilder.build();
	}

	@Override
	public void getGameSetting(RpcController controller,
			GetGameSettingRequest request,
			RpcCallback<GetGameSettingResponse> done) {
		// TODO Auto-generated method stub
		
	}
}
