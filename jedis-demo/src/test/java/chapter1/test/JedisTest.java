package chapter1.test;

import chapter1.test.jedis.util.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 建立连接
//        jedis = new Jedis("127.0.0.1", 6379);
        // 改成从连接池获取连接
        jedis = JedisConnectionFactory.getJedis();
        // 设置密码
        jedis.auth("123456");
        // 选择数据库
        jedis.select(0);
    }

    @Test
    void testString() {
        // 添加数据
        String result = jedis.set("name", "zhangsan");
        System.out.println("result = " + result);
        // 获取数据
        String name = jedis.get("name");
        System.out.println("name = " + name);
    }

    @Test
    void testHash() {
        // 添加数据
        jedis.hset("user:1", "name", "zhangsan");
        jedis.hset("user:1", "age", "24");

        // 获取数据
        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println("map = " + map);
    }
    @AfterEach
    void tearDown() {
        // 释放连接（所有任务执行完后如果jedis存在则关闭jedis）
        if (jedis != null) {
            jedis.close();
        }
    }
}
