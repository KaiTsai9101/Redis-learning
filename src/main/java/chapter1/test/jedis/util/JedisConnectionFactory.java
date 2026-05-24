package chapter1.test.jedis.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisConnectionFactory {
    private static final JedisPool jedisPool;

    static {
        // 配置连接池
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大连接数
        jedisPoolConfig.setMaxTotal(8);
        // 最大空闲连接（预备，有线程就不用临时创建，一般与最大连接数相同）
        jedisPoolConfig.setMaxIdle(8);
        // 最小空闲连接（超过一段时间没线程访问则关闭到最小连接数）
        jedisPoolConfig.setMinIdle(0);
        // 获取连接时的最大等待时间（默认-1，表示无限等待）
        jedisPoolConfig.setMaxWaitMillis(1000);

        // 创建连接池对象
        /*
        参数1：连接池配置对象
        参数2：Redis服务器地址
        参数3：Redis服务器端口号
        参数4：连接超时时间
        参数5：Redis密码
         */
        jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379,
                1000, "123456");
    }

    public static Jedis getJedis(){
        return jedisPool.getResource();
    }
}
