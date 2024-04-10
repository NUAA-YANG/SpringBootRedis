package com.yzx;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzx.pojo.User;
import com.yzx.service.UserService;
import com.yzx.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;


@SpringBootTest
class SpringBootRedisApplicationTests {


    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserService userService;


    @Test
    void contextLoads() {
        // ops表示操作
        redisTemplate.opsForValue();//用于操作String
        redisTemplate.opsForList();//用于操作list集合
        redisTemplate.opsForHash();//用于操作hash集合
        redisTemplate.opsForSet();//用于操作set集合
        redisTemplate.opsForZSet();//用于操作ZSet集合
        // 获取redis数据库的连接(可清空数据库等)
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        // 举例：存储字符串
        redisTemplate.opsForValue().set("myKey","SpringBootRedis");
        System.out.println(redisTemplate.opsForValue().get("myKey"));
    }

    // 直接将对象转化为json后存储
    @Test
    void testJsonUser() throws JsonProcessingException {
        //开发中一般使用json对象测试
        User user = new User("王美丽", 22);
        //json对象
        String jsonUser = new ObjectMapper().writeValueAsString(user);
        redisTemplate.opsForValue().set("user",jsonUser);
        System.out.println(redisTemplate.opsForValue().get("user"));
    }


    //将对象序列化后存储
    @Test
    public void testSerializableUser(){
        User user = new User("李大胆", 35);
        redisTemplate.opsForValue().set("user",user);
        System.out.println(redisTemplate.opsForValue().get("user"));
    }

    //测试利用自定义的工具存储键值对
    @Test
    void saveKey(){
        User user = new User("yzx2",63);
        redisUtil.set("cache:user:"+user.getName(),user);
    }

    //设置缓存
    @Test
    public User queryByName(String name){
        String key = "cache:user:" + name;
        //1. 从redis中直接查询缓存
        User reidsUser = (User)redisUtil.get(key);
        //2. 若redis查询对象存在，则直接返回
        if (reidsUser != null){
            System.out.println("redis直接查询成功：" + reidsUser);
            return reidsUser;
        }else {
        //3. 若redis查询对象不存在，则前往数据库查询
            User sqlUser = userService.getUserByName(name);
            //3.1 若数据库查询为空，则返回前台错误，同时将这个空对象存入redis并设置失效时长，防止缓存穿透
            if (sqlUser == null){
                System.out.println("mysql查询为空，已将空对象存入redis：" + sqlUser);
                redisUtil.set(key,new User(),300);
                return null;
            }else {
                //3.2 若数据库查询不为空，则直接返回前台，同时将非空对象存入redis
                System.out.println("mysql查询成功，已将对象存入redis：" + sqlUser);
                redisUtil.set(key,sqlUser);
                return sqlUser;
            }
        }
    }

    @Test
    public void printUser(){
        queryByName("yzx");
    }

}
