package com.texaspoker.moment.room;

import com.google.protobuf.ByteString;

import java.util.ArrayList;

public class HandInfo {
	private ArrayList<Integer> vCards=new ArrayList<Integer>();
	private long lRemainStacks;
	private ArrayList<ByteString> vSecureCards=new ArrayList<ByteString>();//加密手牌
	
	public ArrayList<Integer> getvCards() {
		return vCards;
	}
	public void setvCards(ArrayList<Integer> vCards) {
		this.vCards = vCards;
	}

	public ArrayList<ByteString> getvSecureCards() {
		return vSecureCards;
	}

	public void setvSecureCards(ArrayList<ByteString> vSecureCards) {
		this.vSecureCards = vSecureCards;
	}
	
	public void addCards(int card1 , int card2){
		vCards.clear();
		vCards.add(card1);
		vCards.add(card2);
	}

	public void addSecureCards(ByteString card){
		vSecureCards.clear();
		vSecureCards.add(card);
	}
	public long getlRemainStacks() {
		return lRemainStacks;
	}
	public void setlRemainStacks(long lRemainStacks) {
		this.lRemainStacks = lRemainStacks;
	}

}
