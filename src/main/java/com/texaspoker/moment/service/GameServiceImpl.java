package com.texaspoker.moment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.texaspoker.club.ClubManager;
import com.texaspoker.comm.GameFeeUtil;
import com.texaspoker.league.LeagueManager;
import com.texaspoker.moment.TexasPoker.BUYIN_CONTROL_TYPE;
import com.texaspoker.moment.TexasPoker.BuyinControl;
import com.texaspoker.moment.TexasPoker.GAME_ACT_TYPE;
import com.texaspoker.moment.TexasPoker.GameRoomInfo;
import com.texaspoker.moment.TexasPoker.InsuranceUserInfo;
import com.texaspoker.moment.TexasPoker.QuickGameRoomInfo;
import com.texaspoker.moment.TexasPoker.UserGameInfoNet;
import com.texaspoker.moment.TexasPokerCommon.CREATE_ROOM_TYPE;
import com.texaspoker.moment.TexasPokerCommon.ClubBaseInfo;
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
import com.texaspoker.moment.TexasPokerGameService.CancelGameRequest;
import com.texaspoker.moment.TexasPokerGameService.CancelGameResponse;
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
import com.texaspoker.moment.TexasPokerGameService.ForceStandupRequest;
import com.texaspoker.moment.TexasPokerGameService.ForceStandupResponse;
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
import com.texaspoker.moment.TexasPokerGameService.KickUserGameRoomRequest;
import com.texaspoker.moment.TexasPokerGameService.KickUserGameRoomResponse;
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
import com.texaspoker.moment.TexasPokerGameService.SetGameThinkingIntervalRequest;
import com.texaspoker.moment.TexasPokerGameService.SetGameThinkingIntervalResponse;
import com.texaspoker.moment.TexasPokerGameService.SetPreActionRequest;
import com.texaspoker.moment.TexasPokerGameService.SetPreActionResponse;
import com.texaspoker.moment.TexasPokerGameService.SetQuickGameRoomInfoRequest;
import com.texaspoker.moment.TexasPokerGameService.SetQuickGameRoomInfoResponse;
import com.texaspoker.moment.TexasPokerGameService.SetStandByUserGameInfosRequest;
import com.texaspoker.moment.TexasPokerGameService.SetStandByUserGameInfosResponse;
import com.texaspoker.moment.TexasPokerGameService.SetStraddleRequest;
import com.texaspoker.moment.TexasPokerGameService.SetStraddleResponse;
import com.texaspoker.moment.TexasPokerGameService.ShowCardRequest;
import com.texaspoker.moment.TexasPokerGameService.ShowCardResponse;
import com.texaspoker.moment.TexasPokerGameService.ShowTableCardsRequest;
import com.texaspoker.moment.TexasPokerGameService.ShowTableCardsResponse;
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
import com.texaspoker.moment.TexasPokerGameService.UserDelegateRequest;
import com.texaspoker.moment.TexasPokerGameService.UserDelegateResponse;
import com.texaspoker.moment.TexasPokerGameService.UserInsuranceSettingRequest;
import com.texaspoker.moment.TexasPokerGameService.UserInsuranceSettingResponse;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueBaseInfo;
import com.texaspoker.moment.TexasPokerHttpLeague.LeagueInfo;
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
		stGameRoomBaseInfoBuilder.setBIPLimit(request.getStGameRoomBaseInfo().getBIPLimit());
		stGameRoomBaseInfoBuilder.setBGPSLimit(request.getStGameRoomBaseInfo().getBGPSLimit());
		stGameRoomBaseInfoBuilder.setLLeagueID(request.getStGameRoomBaseInfo().getLLeagueID());
		stGameRoomBaseInfoBuilder.setIThinkingInterval(15);
		stGameRoomBaseInfoBuilder.setBStraddle(request.getStGameRoomBaseInfo().getBStraddle());
		
		//添加联盟相关信息
		LeagueBaseInfo stLeagueBaseInfo = LeagueManager.getInstance().getLeagueBaseInfo(stGameRoomBaseInfoBuilder.getLLeagueID());
		if(stLeagueBaseInfo!=null){
			stGameRoomBaseInfoBuilder.setSLeagueName(stLeagueBaseInfo.getSLeagueName());
		}
		GameRoomIDResultInfo stGameRoomIDResultInfo = GameRoomManager.getInstance().createGameRoom(request.getUuid()
				,stGameRoomBaseInfoBuilder.build());
		if(stGameRoomIDResultInfo.geteErrorCodeType() == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
			if(stGameRoomIDResultInfo.getlGameRoomID() > 0){
				GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(stGameRoomIDResultInfo.getlGameRoomID());
				
				if(stGameRoomDetailInfo!=null){
					stCreateGameRoomResponseBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
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
//					System.out.println("enterGameRoom : " + stGameRoomDetailInfo.getStGameRoomInfo());
					stEnterGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stEnterGameRoomResponseBuilder.setStGameRoomInfo(stGameRoomDetailInfo.getStGameRoomInfo());
				}else{
					stEnterGameRoomResponseBuilder.setEErrCode(eErrorCodeType);
				}
			}else{
				stEnterGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
			
		}catch(Exception e){
//			System.out.println("out1gService throw unexpected exception, : " + request.getUuid());
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
				stStandUpResponseBuilder.setLGameRoomID(request.getLRoomID());
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
				rst = stGameRoomDetailInfo.doFlodAction(request.getUuid(),request.getLBettingID(),false);
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
				
				UserGameInfoNet sUserGameInfoNetAdd = null; 
				if(stGameRoomDetailInfo.getStGameRoomInfo()!=null){
					for(UserGameInfoNet sUserGameInfoNet : stGameRoomDetailInfo.getStGameRoomInfo().getVUserGameInfosList()){
						if(sUserGameInfoNet.getStUserInfo().getUuid() == request.getUuid()){
							sUserGameInfoNetAdd = sUserGameInfoNet;
						}
					}
				}
				
				if(sUserGameInfoNetAdd!=null){
					stGameActionResponseBuilder.setLRemainStacks(sUserGameInfoNetAdd.getLRemainStacks());
					stGameActionResponseBuilder.setLStacks(sUserGameInfoNetAdd.getLBetStacks());
				}
				
				stGameActionResponseBuilder.setEGameActType(request.getEGameActType());
				stGameActionResponseBuilder.setLBettingID(request.getLBettingID());
			
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
			long clubid = request.getLClubID();
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID() > 0 
					&& request.getLClubID() == 0){
				rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN;
			}else{
				if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID() == 0){
					//如果不是联盟局，并且是俱乐部局
					if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
						clubid = stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID();
					}
				}

				//不需要买入控制
				if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getBBuyinControl() == 0){
					rst = stGameRoomDetailInfo.buyBuyin(request.getUuid(),clubid, request.getLStacks());
				}else{
					
					if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
						//如果是俱乐部局，则首先检查创建牌局的是否为俱乐部管理员
						if(ClubManager.getInstance().checkClubManager(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLChatRoomID()
								,stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid())){
							//如果买入的人再买入的俱乐部里面是管理员的话，则直接买入成功
							if(ClubManager.getInstance().checkClubManager(clubid,request.getUuid())){
								rst = stGameRoomDetailInfo.buyBuyin(request.getUuid(),clubid, request.getLStacks());
							}else{
								//发送买入控制的push
								rst = stGameRoomDetailInfo.buyBuyinControl(request.getUuid(),clubid,request.getLStacks());
							}
						}else{
							//如果是俱乐部局，但是不是管理员创建的牌局，则只有房主可以不进行买入控制，直接买入
							if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
								rst = stGameRoomDetailInfo.buyBuyin(request.getUuid(),clubid, request.getLStacks());
							}else{
								//发送买入控制的push
								rst = stGameRoomDetailInfo.buyBuyinControl(request.getUuid(),clubid,request.getLStacks());
							}
						}
					}else{
						//如果不是俱乐部局，则检查买入的是否为牌局创建者，如果是牌局创建者，则直接买入成功
						if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
							rst = stGameRoomDetailInfo.buyBuyin(request.getUuid(),clubid, request.getLStacks());
						}else{
							//发送买入控制的push
							rst = stGameRoomDetailInfo.buyBuyinControl(request.getUuid(),clubid,request.getLStacks());
						}
					}				
				}
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
			stGameRoomDetailInfo.leaveGameRoom(request.getUuid());
			stLeaveGameRoomResponseBuilderBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
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
			stStartGameResponseBuilder.setLGameRoomID(request.getLRoomID());
			stStartGameResponseBuilder.setLStartTime(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLStartTime());
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
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID() > 0){
				//如果是俱乐部管理员，则开始
				LeagueInfo stLeagueInfo = stGameRoomDetailInfo.getStLeagueInfo();
		
				if(stLeagueInfo!=null){
					boolean bClubManager = false;
					for(ClubBaseInfo stClubBaseInfo:stLeagueInfo.getVClubBaseInfosList()){
						
						bClubManager = ClubManager.getInstance().checkClubManager(stClubBaseInfo.getLClubID(),request.getUuid());
					
						if(bClubManager){
							break;
						}
					}
					
					if(bClubManager){
						ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
						if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_ACCEPT){
							rst = stGameRoomDetailInfo.buyinControlActionAccept(request.getUuid(),request.getLActionUuid());
						}else if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_DENY){
							rst = stGameRoomDetailInfo.buyinControlActionDeny(request.getUuid(),request.getLActionUuid(),false);
						}
						
						if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
							stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
						}else{
							stBuyinActionResponseBuilder.setEErrCode(rst);
						}
					}
				}else{
					stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_LEAGUE);
				}
			}else{
				if(stGameRoomDetailInfo.checkClubManager(request.getUuid()) 
						|| (stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid())){
					ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
					if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_ACCEPT){
						rst = stGameRoomDetailInfo.buyinControlActionAccept(request.getUuid(),request.getLActionUuid());
					}else if(request.getEBuyinControlMessageType() == BUYIN_CONTROL_TYPE.BUYIN_CONTROL_TYPE_DENY){
						rst = stGameRoomDetailInfo.buyinControlActionDeny(request.getUuid(),request.getLActionUuid(),false);
					}
					
					if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
						stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					}else{
						stBuyinActionResponseBuilder.setEErrCode(rst);
					}
				}else{
					stBuyinActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
				}
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
				stPauseGameResponseBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
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
				
				if(stGameRoomDetailInfo.getStGameRoomInfo()!=null){
					for(UserGameInfoNet sUserGameInfoNet : stGameRoomDetailInfo.getStGameRoomInfo().getVUserGameInfosList()){
						if(sUserGameInfoNet.getStUserInfo().getUuid() == request.getUuid()){

							stUseDelayItemResponseBuilder.setStUserBaseInfoNet(sUserGameInfoNet.getStUserInfo());
							stUseDelayItemResponseBuilder.setLActTime(sUserGameInfoNet.getLActTime());
							stUseDelayItemResponseBuilder.setIDelayCost(sUserGameInfoNet.getIDelayCost());
							stUseDelayItemResponseBuilder.setIDelaySingleTime(sUserGameInfoNet.getIDelaySingleTime());
							stUseDelayItemResponseBuilder.setIDelayLong(sUserGameInfoNet.getIDelayLong());
							stUseDelayItemResponseBuilder.setIDelayTotalTimes(sUserGameInfoNet.getIDelayTotalTimes());
						}
					}
				}
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
			//检查用户设置权限
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
				ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				rst = stGameRoomDetailInfo.resetBuyinState(request.getBBuyinControl());
				
				if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					stSetBuyinControlStateResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stSetBuyinControlStateResponseBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
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
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
				ERROR_CODE_TYPE rst = ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS;
				rst = stGameRoomDetailInfo.resetBuyinRatio(request.getUuid(), request.getIMinBuyinRatio(), request.getIMaxBuyinRatio());
				
				if(rst == ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS){
					stResetBuyinRatioResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stResetBuyinRatioResponseBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
				}else{
					stResetBuyinRatioResponseBuilder.setEErrCode(rst);
				}
			}else{
				stResetBuyinRatioResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
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
			stStartQuickRoomGameResponseBuilder.setStGameRoomBaseInfo(stGameRoomResultInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
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
			
			if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLLeagueID() > 0){
				//如果是俱乐部管理员，则开始
				LeagueInfo stLeagueInfo = stGameRoomDetailInfo.getStLeagueInfo();
		
				if(stLeagueInfo!=null){
					for(ClubBaseInfo stClubBaseInfo:stLeagueInfo.getVClubBaseInfosList()){
						
						
						if(stClubBaseInfo.getStCreateUserBaseInfo().getUuid() == request.getUuid()){
							ArrayList<BuyinControl> vBuyinControls = stGameRoomDetailInfo.getAllBuyinControls(stClubBaseInfo.getLClubID());
							
							for(BuyinControl stBuyinControl:vBuyinControls){
								stGetAllBuyinControlsResponseBuilder.addVBuyinControls(stBuyinControl);
							}
						}
					}
					stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				}else{
					stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_LEAGUE);
				}
			}else{
				//如果创建牌局的人不是俱乐部的管理员
				if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getECreateRoomType() == CREATE_ROOM_TYPE.CLUB_ROOM){
					if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
						ArrayList<BuyinControl> vBuyinControls = stGameRoomDetailInfo.getAllBuyinControls(0);
						
						if(vBuyinControls!=null){
							stGetAllBuyinControlsResponseBuilder.addAllVBuyinControls(vBuyinControls);
						}
						stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
						
					}else{
						stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
					}
				}
				else{
					//如果不是管理员创建牌局，则只有房主可以获得买入申请信息
					if(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getStCreateUserBaseInfo().getUuid() == request.getUuid()){
						ArrayList<BuyinControl> vBuyinControls = stGameRoomDetailInfo.getAllBuyinControls(0);
						
						if(vBuyinControls!=null){
							stGetAllBuyinControlsResponseBuilder.addAllVBuyinControls(vBuyinControls);
						}
						stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
						
					}else{
						stGetAllBuyinControlsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_AUTH_BUYINACTION);
					}
				}
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
			ArrayList<Long> vStandingByUuids = new ArrayList<Long>(stGameRoomDetailInfo.getStandByUuids());
//			System.out.println("getStandByUuids vStandingByUuids : " + vStandingByUuids);
			if(vStandingByUuids!=null){
				stGetStandByUuidsResponseBuilder.addAllVStandingByUuids(vStandingByUuids);
			}
			
			ArrayList<Long> vAllUuids = new ArrayList<Long>(stGameRoomDetailInfo.getAllStandByUuids());
//			System.out.println("getStandByUuids vAllUuids : " + vAllUuids);
			for(long lUuid:vAllUuids){
				if(!vStandingByUuids.contains(lUuid)){
					stGetStandByUuidsResponseBuilder.addVStandByUuids(lUuid);
				}
			}			
			stGetStandByUuidsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				
		}else{
			stGetStandByUuidsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);
		}
		
//		System.out.println("getStandByUuids : " + stGetStandByUuidsResponseBuilder);
		
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
//		System.out.println("getUserQuickGameRoomInfos uuid : " + request.getUuid());
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
		if(!stGameRoomDetailInfo.checkUserRebuy(request.getUuid(), request.getStBuyInsuranceInfoList())){
			GameRoomInfo stGameRoomInfo = stGameRoomDetailInfo.buyInsurance(request.getUuid(), request.getLGameHandID(), request.getEGameRoomStatus(), request.getStBuyInsuranceInfoList());
			if(stGameRoomInfo!=null){
				stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
				stBuyInsuranceResponseBuilder.setLGameRoomID(request.getLGameRoomID());
			}else{
				stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
			stGameRoomDetailInfo.doInsuranceTimeout();
		}else{
			stBuyInsuranceResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
		}
		return stBuyInsuranceResponseBuilder.build();
	}

	@Override
	public BuyInsuranceDelayResponse buyInsuranceDelay(
			RpcController controller, BuyInsuranceDelayRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		
		BuyInsuranceDelayResponse.Builder stBuyInsuranceDelayResponseBuilder = BuyInsuranceDelayResponse.newBuilder();

		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());

//		System.out.println("buyInsuranceDelay request.getUuid() : " + request.getUuid() + " gameroomid : " + request.getLGameHandID());
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
//		System.out.println("getGameSetting : " + request.getLGameRoomID() + "  " + stGameRoomDetailInfo);
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

	@Override
	public KickUserGameRoomResponse kickUserGameRoom(RpcController arg0,
			KickUserGameRoomRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		KickUserGameRoomResponse.Builder  stKickUserGameRoomResponseBuilder = KickUserGameRoomResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.kickUserInGame(request.getLKickUuid());
		}
		stKickUserGameRoomResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stKickUserGameRoomResponseBuilder.build();
	}

	@Override
	public UserDelegateResponse userDelegate(RpcController arg0,
			UserDelegateRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		UserDelegateResponse.Builder  stUserDelegateResponseBuilder = UserDelegateResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			
			if(request.getBDelegate() == 1){
				stGameRoomDetailInfo.userDelegate(request.getUuid());
			}else{
				stGameRoomDetailInfo.userReturnGame(request.getUuid());
			}
		}
		stUserDelegateResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stUserDelegateResponseBuilder.build();
	}

	@Override
	public void kickUserGameRoom(RpcController arg0,
			KickUserGameRoomRequest arg1,
			RpcCallback<KickUserGameRoomResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void userDelegate(RpcController arg0, UserDelegateRequest arg1,
			RpcCallback<UserDelegateResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CancelGameResponse cancelGame(RpcController arg0,
			CancelGameRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		CancelGameResponse.Builder  stCancelGameResponseBuilder = CancelGameResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.cancelGame();
		}
		stCancelGameResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stCancelGameResponseBuilder.build();
	}

	@Override
	public void cancelGame(RpcController arg0, CancelGameRequest arg1,
			RpcCallback<CancelGameResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ForceStandupResponse forceStandup(RpcController arg0, ForceStandupRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ForceStandupResponse.Builder  stForceStandupResponseBuilder = ForceStandupResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.forceStandup(request.getLForceStandUpUuid());
		}
		stForceStandupResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stForceStandupResponseBuilder.build();
	}

	@Override
	public SetGameThinkingIntervalResponse setGameThinkingInterval(RpcController arg0,
			SetGameThinkingIntervalRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		SetGameThinkingIntervalResponse.Builder  stSetGameThinkingIntervalResponseBuilder = SetGameThinkingIntervalResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.resetPlayThinkingInterval(request.getUuid(),request.getIThinkingInterval());
		}
		stSetGameThinkingIntervalResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		stSetGameThinkingIntervalResponseBuilder.setStGameRoomBaseInfo(stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo());
		
		return stSetGameThinkingIntervalResponseBuilder.build();
	}

	@Override
	public SetStraddleResponse setStraddle(RpcController arg0, SetStraddleRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		SetStraddleResponse.Builder  stSetStraddleResponseBuilder = SetStraddleResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			stGameRoomDetailInfo.setStraddle(request.getUuid(),request.getBStraddle());
		}
		stSetStraddleResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);	
		
		return stSetStraddleResponseBuilder.build();
	}

	@Override
	public void forceStandup(RpcController arg0, ForceStandupRequest arg1, RpcCallback<ForceStandupResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGameThinkingInterval(RpcController arg0, SetGameThinkingIntervalRequest arg1,
			RpcCallback<SetGameThinkingIntervalResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStraddle(RpcController arg0, SetStraddleRequest arg1, RpcCallback<SetStraddleResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SetPreActionResponse setPreAction(RpcController arg0, SetPreActionRequest request) throws ServiceException {
		// TODO Auto-generated method stub
		SetPreActionResponse.Builder  stSetPreActionResponseBuilder = SetPreActionResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			boolean bAction = stGameRoomDetailInfo.setPreAction(request.getUuid(),request.getEPreUserGameState());
			if(bAction){
				stSetPreActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
			}else{
				stSetPreActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
			}
		}else{
			stSetPreActionResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);	
		}
		
//		System.out.println("setPreAction 1: " + request.getEPreUserGameState());
		return stSetPreActionResponseBuilder.build();
	}

	@Override
	public ShowTableCardsResponse showTableCards(RpcController arg0, ShowTableCardsRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		ShowTableCardsResponse.Builder  stShowTableCardsResponseBuilder = ShowTableCardsResponse.newBuilder();
		GameRoomDetailInfo stGameRoomDetailInfo = GameRoomManager.getInstance().getGameRoomDetailInfo(request.getLGameRoomID());
		if(stGameRoomDetailInfo!=null){
			long lFee = GameFeeUtil.getShowCardItemFee((int)stGameRoomDetailInfo.getStGameRoomInfo().getStGameRoomBaseInfo().getLSmallBlinds());
			if(UserManager.getInstance().checkUserPopularity(request.getUuid(), lFee)){
				List<Integer> vTableCards = stGameRoomDetailInfo.showTableCards(request.getUuid(), request.getLGameHandID(),lFee);
				
				if(vTableCards!=null){
					stShowTableCardsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_SUCCESS);
					stShowTableCardsResponseBuilder.addAllVTableCards(vTableCards);
				}else{
					stShowTableCardsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_UNKNOWN);
				}
			}else{
				stShowTableCardsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NOT_ENOUGH_POPULARITY);
			}
		}else{
			stShowTableCardsResponseBuilder.setEErrCode(ERROR_CODE_TYPE.ERROR_CODE_TYPE_NO_GAME_ROOM);	
		}
		
		return stShowTableCardsResponseBuilder.build();
	}

	@Override
	public void setPreAction(RpcController arg0, SetPreActionRequest arg1, RpcCallback<SetPreActionResponse> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showTableCards(RpcController arg0, ShowTableCardsRequest arg1,
			RpcCallback<ShowTableCardsResponse> arg2) {
		// TODO Auto-generated method stub
		
	}
}
