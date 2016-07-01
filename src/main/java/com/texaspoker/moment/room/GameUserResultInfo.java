package com.texaspoker.moment.room;

import java.util.ArrayList;
import java.util.List;

public class GameUserResultInfo {
	int mCardStrength;
	
	//首先比较牌型
	List<Integer> pairList = new ArrayList<Integer>();
	List<Integer> triList = new ArrayList<Integer>();
	List<Integer> singleList = new ArrayList<Integer>();
	int four = 0 ;//4张的手牌
	
	//同花的时候最大的那张手牌
	int flushMaxHandCard;
	
	//如果是顺子的时候，最大的一张牌
	int iStraightCard = 0;
	
	ArrayList<Integer> finalCards = new ArrayList<Integer>();
}
