package com.texaspoker.comm;

public class SSOProxy {
	//检查包头是否正确
	public static int GetPackageCmd(byte[] bData)  {
		// TODO Auto-generated method stub
		//判断起始符
		int cmd = (bData[6] | bData[5] << 8);
		return cmd;
	};
}
