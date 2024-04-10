# SpringBoot整合

## 1. 环境搭建

编辑配置文件`redis.conf`，注释`bind 127.0.0.1 -::1`行，表示允许远程连接

![image-20240328163404222](https://cdn.jsdelivr.net/gh/NUAA-YANG/TyporaPicture@main//img/202403281634182.png)

编辑配置文件`redis.conf`，关闭保护模式，设置`protected-mode no`

![image-20240328163509927](https://cdn.jsdelivr.net/gh/NUAA-YANG/TyporaPicture@main//img/202403281635573.png)

新建`SpringBoot`项目，选择如下依赖

![image-20240328162157353](https://cdn.jsdelivr.net/gh/NUAA-YANG/TyporaPicture@main//img/202403281622932.png)

设置配置连接文件`application.properties`

```yaml
server.port=8080
# 配置redis
spring.redis.host=192.168.31.104
spring.redis.port=6379
```



## 2. 测试连接

打开`SpringBoot`的测试类

```java
@SpringBootTest
class SpringBootRedisApplicationTests {
    @Autowired
    private RedisTemplate redisTemplate;
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
}
```



## 3. 存储对象

创建一个`User`对象用于测试

```java
public class User{
    private String name;
    private int age;
}
```



### 3.1 Json存储

---

第一种方式为将对象直接转化为`Json`存储

```java
//传入系统自带的模板
@Autowired
private RedisTemplate redisTemplate;
//测试json对象存入
@Test
public void testUserJson() throws JsonProcessingException {
    //开发中一般使用json对象测试
    User user = new User("王美丽", 22);
    //json对象
    String jsonUser = new ObjectMapper().writeValueAsString(user);
    redisTemplate.opsForValue().set("user",jsonUser);
    Object user1 = redisTemplate.opsForValue().get("user");
    System.out.println(user1);
}
```



### 3.2 JDK序列化

---

第二种方式为直接将对象序列化

```java
//对象序列化才可以存入到Redis中
public class User implements Serializable {
    private String name;
    private int age;
}
```

存储到`Redis`中

```java
//传入系统自带的模板
@Autowired
private RedisTemplate redisTemplate;
//序列化对象存入
@Test
public void testSerializableUser(){
    User user = new User("李大胆", 35);
    redisTemplate.opsForValue().set("user",user);
    System.out.println(redisTemplate.opsForValue().get("user"));
}
```



### 3.3 Redis序列化

---

以上两种方式均会造成`redis`中的乱码

我们新建配置类`RedisConfig.java`，保证可以序列化，固定模板

```java
@Configuration
public class RedisConfig {
    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        //  RedisTemplate对象，使用<String, Object>更方便
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);

        //Json的序列化配置
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // String的序列化配置
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }
}
```

在调用时，传入我们自己创建的对象`redisTemplate`，此时`User`不需要序列化便可存储，并且`redis`中不会造成乱码

```java
//传入我们自定义的模板
@Autowired
@Qualifier("redisTemplate")
private RedisTemplate redisTemplate;
//测试对象存入
@Test
public void testUserSerializable() throws JsonProcessingException {
    User user2 = new User("李大胆", 35);
    redisTemplate.opsForValue().set("user2",user2);
    System.out.println(redisTemplate.opsForValue().get("user2"));
}
```





## 4. 简便工具类

因为我们使用`RedisTemplate`很麻烦，我们就采用自定义工具的来封装`RedisTemplate`中的方法

新建`RedisUtil.java`文件

```java
//在真实的开发中，一般都会使用redis的工具
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // =============================common============================
    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }



    /**
     * @author yzx
     * @param pattern 传入的匹配字符
     * @return 返回所有的键
     * @since 2022/9/18 11:21
     */
    public Set keys(String pattern){
        if (pattern.equals(null)){
            return null;
        }else {
            return redisTemplate.keys(pattern);
        }
    }


    // ============================String=============================
    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     * @param key 键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    // ================================Map=================================
    /**
     * HashGet
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     * @param key 键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     * @param key 键
     * @param item 项
     * @param by 要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     * @param key 键
     * @param item 项
     * @param by 要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================set=============================
    /**
     * 根据key获取Set中的所有值
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     * @param key 键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     * @param key 键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0)
                expire(key, time);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    // ===============================list=================================

    /**
     * 获取list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束 0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     * @param key 键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0)
                expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0)
                expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
```

在其他类中引入上述工具来调用相关的方法

```java
//此时是我们自定义的对象工具
//可以直接调用来使用相关的方法
@Autowired
private RedisUtil redisUtil;

@Test
void saveKey(){
    User user = new User("xxxx",63);
    //存储对象
    redisUtil.set(user.getName(),user);
    //获取对象
    User reidsUser = (User)redisUtil.get("xxxx");
}
```



## 5. 读写缓存

缓存流程如下：

1. 客户端发起请求，若`redis`中存在数据，则直接返回给客户端
2. 若`redis`中不存在数据，则直接查询数据库，将数据返回客户端的同时写入`redis`

![image-20240329171407764](https://cdn.jsdelivr.net/gh/NUAA-YANG/TyporaPicture@main//img/202403291714827.png)

举例说明

```java
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
        //3.1 若数据库查询为空，则返回前台错误
        // 同时将这个空对象存入redis并设置失效时长，防止缓存穿透
        if (sqlUser == null){
            System.out.println("mysql查询为空，已将空对象存入redis：" + sqlUser);
            redisUtil.set(key,new User(),300);
            return null;
        }else {
            //3.2 若数据库查询不为空，则直接返回前台，同时将非空对象存入redis
            System.out.println("mysql查询成功，已将该对象存入redis：" + sqlUser);
            redisUtil.set(key,sqlUser);
            return sqlUser;
        }
    }
}
```





