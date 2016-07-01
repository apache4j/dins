package com.texaspoker.comm;

import java.security.SecureRandom;

public class SecureSeedGenerator {
	public static long getSeed() {
		SecureRandom rand = new SecureRandom();
		// /dev/urandom是伪随机数（非阻塞），/dev/random是真随机数（阻塞）
		//现在SecureRandom为了防止阻塞，只有取种时才从/dev/random取随机数
		//其余情况都从/dev/urandom取随机数，与我们的期望不同
		//所以此处用取种的方式生成随机数
		//测试服Anubis的Haveged已经配置好了
		byte[] bytes = rand.generateSeed(8);

		long seed = (((long) bytes[0] & 0xff) << 56) | (((long) bytes[1] & 0xff) << 48)
				| (((long) bytes[2] & 0xff) << 40) | (((long) bytes[3] & 0xff) << 32) | (((long) bytes[4] & 0xff) << 24)
				| (((long) bytes[5] & 0xff) << 16) | (((long) bytes[6] & 0xff) << 8) | (((long) bytes[7] & 0xff) << 0);
		return seed;
	}
}
