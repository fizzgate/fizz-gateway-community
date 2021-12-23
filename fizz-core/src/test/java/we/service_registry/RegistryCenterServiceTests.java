package we.service_registry;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.Fizz;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.service_registry.eureka.FizzEurekaServiceRegistration;
import we.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class RegistryCenterServiceTests {

    @Resource
    StringRedisTemplate         stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    RegistryCenterService       registryCenterService;

    // @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        registryCenterService = new RegistryCenterService();
        ReflectionUtils.set(registryCenterService, "rt", reactiveStringRedisTemplate);
    }

    // @Test
    void initTest() throws Throwable {

        System.setProperty("server.port", "8866");
        Fizz.context = new GenericApplicationContext();
        Fizz.context.refresh();

        Map<String, String> registryCenterServiceMap = new HashMap<>();
        String yml = FileUtil.readString("eureka.yml", CharsetUtil.CHARSET_UTF_8);
        registryCenterServiceMap.put("1", "{\"id\":1,\"name\":\"default\",\"type\":1,\"format\":1,\"content\":\"" + yml + "\",\"isDeleted\":0}");
        stringRedisTemplate.opsForHash().putAll("fizz_registry", registryCenterServiceMap);

        registryCenterService.init();
        RegistryCenter def = registryCenterService.getRegistryCenter("default");
        FizzServiceRegistration fizzServiceRegistration = def.getFizzServiceRegistration();
        fizzServiceRegistration.register();
        // Thread.currentThread().join();
    }
}
