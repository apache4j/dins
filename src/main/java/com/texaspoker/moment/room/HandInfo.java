package com.texaspoker.moment.room;

import java.util.ArrayList;

public class HandInfo {
	private ArrayList<Integer> vCards=new ArrayList<Integer>();
	private long lRemainStacks;
	
	
	public ArrayList<Integer> getvCards() {
		return vCards;
	}
	public void setvCards(ArrayList<Integer> vCards) {
		this.vCards = vCards;
	}
	
	public void addCards(int card1 , int card2){
		vCards.clear();
		vCards.add(card1);
		vCards.add(card2);
	}
	
	public long getlRemainStacks() {
		return lRemainStacks;
	}
	public void setlRemainStacks(long lRemainStacks) {
		this.lRemainStacks = lRemainStacks;
	}

}
