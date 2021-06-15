package com.texaspoker.jedis;

import com.texaspoker.configuration.ConfigaturationManager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisManager {

    private static JedisPool staticsPool;
    
    private static JedisPool msgPool;

    /**
     * 建立连接池 真实环境，一般把配置参数缺抽取出来。
     * 
     */
    private static void createJedisPool() {

    	// 建立连接池配置参数
        JedisPoolConfig config = new JedisPoolConfig();

        // 设置空间连接
        config.setMaxIdle(50);
        config.setMinIdle(10);

        // 设置最大连接数
        config.setMaxTotal(500);

        // 设置最大阻塞时间，记住是毫秒数milliseconds
        config.setMaxWaitMillis(1000);

        // 设置空间连接
        config.setTestOnBorrow(true);
        config.setNumTestsPerEvictionRun(5);
        config.setMinEvictableIdleTimeMillis(300000);
        config.setTimeBetweenEvictionRunsMillis(300000);

        // 创建连接池
        // pool = new JedisPool(config, "10.66.88.141",
        // 6379,100000,"18eb7740-dd7e-4ca2-80fc-494849d714f4:yilumi31415");
//        staticsPool = new JedisPool(config, ConfigaturationManager.sStaticJedisAddress, 6379,2000,ConfigaturationManager.sStaticJedisAccount);
//        msgPool = new JedisPool(config, ConfigaturationManager.sMessageJedisAddress, 6379,2000,ConfigaturationManager.sMessageJedisAccount);
        if (ConfigaturationManager.sStaticJedisAccount == null || ConfigaturationManager.sStaticJedisAccount.length() == 0) {
            System.out.println("no pass word " + ConfigaturationManager.sStaticJedisAddress + " " + ConfigaturationManager.sStaticJedisAccount);
            staticsPool = new JedisPool(config, ConfigaturationManager.sStaticJedisAddress, 6379,2000);
        } else {
            System.out.println("have pass word " + ConfigaturationManager.sStaticJedisAddress + " " + ConfigaturationManager.sStaticJedisAccount);
            staticsPool = new JedisPool(config, ConfigaturationManager.sStaticJedisAddress, 6379,2000,ConfigaturationManager.sStaticJedisAccount);

        }
        if (ConfigaturationManager.sMessageJedisAccount == null || ConfigaturationManager.sMessageJedisAccount.length() == 0) {
            System.out.println("no pass word " + ConfigaturationManager.sMessageJedisAddress + " " + ConfigaturationManager.sMessageJedisAccount);
            msgPool = new JedisPool(config, ConfigaturationManager.sMessageJedisAddress, 6379,2000);
        } else {
            System.out.println("have pass word " + ConfigaturationManager.sMessageJedisAddress + " " + ConfigaturationManager.sMessageJedisAccount);
            msgPool = new JedisPool(config, ConfigaturationManager.sMessageJedisAddress, 6379,2000,ConfigaturationManager.sMessageJedisAccount);

        }

    }

    /**
     * 在多线程环境同步初始化
     */
    private static synchronized void staticsPoolInit() {
        if (staticsPool == null)
            createJedisPool();
    }

    /**
     * 获取一个jedis 对象
     * 
     * @return
     */
    public static Jedis getJedis() {

        if (staticsPool == null)
        	staticsPoolInit();
        return staticsPool.getResource();
    }

    /**
     * 归还一个连接
     * 
     * @param jedis
     */
    public static void returnRes(Jedis jedis) {
    	staticsPool.returnResourceObject(jedis);
    }
    
    /**
     * 在多线程环境同步初始化
     */
    private static synchronized void msgPoolInit() {
        if (msgPool == null)
            createJedisPool();
    }

    /**
     * 获取一个jedis 对象
     * 
     * @return
     */
    public static Jedis getMsgJedis() {

        if (msgPool == null)
        	msgPoolInit();
        return msgPool.getResource();
    }

    /**
     * 归还一个连接
     * 
     * @param jedis
     */
    public static void returnMsgRes(Jedis jedis) {
    	msgPool.returnResourceObject(jedis);
    }
}
