package com.texaspoker.comm;

import com.google.protobuf.ByteString;
import com.texaspoker.moment.TexasPokerCmd.TEXAS_CMD;
import com.texaspoker.moment.TexasPokerCommon.Package;

public class TCPUtil  {
	
	//将对象转成二进制字节流
	 public static Package genPackage(long uuid , ByteString head,ByteString body,TEXAS_CMD eCmd,long lSeq) 
     {   	
		//生成package builder元素
		Package.Builder stPackageBuilder =  Package.newBuilder();
	    stPackageBuilder.setISeqNo(0);
	    stPackageBuilder.setCEncodeType(0);
	    stPackageBuilder.setUuid(uuid);
	    stPackageBuilder.setECmd(eCmd);
	    stPackageBuilder.setBody(body);
	    stPackageBuilder.setHead(head);
	    stPackageBuilder.setISeqNo(lSeq);
	    stPackageBuilder.setLCurrentSystemTime(System.currentTimeMillis());
    	//生成package包
	    Package stPackage = stPackageBuilder.build();
        return stPackage;   
     }   
	
		//将对象转成二进制字节流
	 public static Package genPackage(long uuid , ByteString head,ByteString body,TEXAS_CMD eCmd) 
     {   	
		//生成package builder元素
		Package.Builder stPackageBuilder =  Package.newBuilder();
	    stPackageBuilder.setISeqNo(0);
	    stPackageBuilder.setCEncodeType(0);
	    stPackageBuilder.setUuid(uuid);
	    stPackageBuilder.setECmd(eCmd);
	    stPackageBuilder.setBody(body);
	    stPackageBuilder.setHead(head);
	    stPackageBuilder.setISeqNo(0);
	    stPackageBuilder.setLCurrentSystemTime(System.currentTimeMillis());
    	//生成package包
	    Package stPackage = stPackageBuilder.build();
        return stPackage;   
     }      
}
