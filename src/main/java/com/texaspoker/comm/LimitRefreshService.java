package com.texaspoker.comm;


public class LimitRefreshService  implements Runnable {

	@Override
	public void run() {
		System.out.println("refresh localemanager service !!!!!!!!!!");
		LocationManager.getInstance().reloadSetting();
	}

}
