/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.beans.factory.config;

import com.fizzgate.config.FizzConfigConfiguration;
import com.fizzgate.context.config.annotation.FizzRefreshScope;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class FizzBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationContextAware, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzBeanFactoryPostProcessor.class);

    private static ApplicationContext applicationContext;


    private       ConfigurableEnvironment     environment;

    private final Map<String, String>         property2beanMap             = new HashMap<>();

    private       ReactiveStringRedisTemplate reactiveStringRedisTemplate;


    protected static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String fizzConfigEnable = environment.getProperty("fizz.config.enable", Consts.S.TRUE);
        if (fizzConfigEnable.equals(Consts.S.TRUE)) {
            // initReactiveStringRedisTemplate();
            // initFizzPropertySource();

            Map<String, Object> sources = FizzEnvironmentPostProcessor.getSources();
            MapPropertySource fizzPropertySource = new MapPropertySource(FizzConfigConfiguration.PROPERTY_SOURCE + "AfterBeanFactory", sources);
            environment.getPropertySources().addFirst(fizzPropertySource);

            initBeanProperty2beanMap(beanFactory);
        }
    }

    /* private void initReactiveStringRedisTemplate() {
        RedisReactiveProperties redisReactiveProperties = new RedisReactiveProperties() {
        };

        String host = environment.getProperty("aggregate.redis.host");
        if (StringUtils.isBlank(host)) {
            redisReactiveProperties.setType(RedisReactiveProperties.CLUSTER);
            redisReactiveProperties.setClusterNodes(environment.getProperty("aggregate.redis.clusterNodes"));
        } else {
            redisReactiveProperties.setHost(host);
            redisReactiveProperties.setPort(Integer.parseInt(environment.getProperty("aggregate.redis.port")));
            redisReactiveProperties.setDatabase(Integer.parseInt(environment.getProperty("aggregate.redis.database")));
        }

        String password = environment.getProperty("aggregate.redis.password");
        if (StringUtils.isNotBlank(password)) {
            redisReactiveProperties.setPassword(password);
        }

        reactiveStringRedisTemplate = ReactiveRedisHelper.getStringRedisTemplate(redisReactiveProperties);
    } */

    /* private void initFizzPropertySource() {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> sources = new HashMap<>();
        MapPropertySource fizzPropertySource = new MapPropertySource(FizzConfigConfiguration.PROPERTY_SOURCE, sources);
        propertySources.addFirst(fizzPropertySource);

        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> fizzConfigs = reactiveStringRedisTemplate.opsForHash().entries("fizz_config");
        fizzConfigs.collectList()
                   .defaultIfEmpty(Collections.emptyList())
                   .flatMap(
                           es -> {
                               if (es.isEmpty()) {
                                   LOGGER.info("no fizz configs");
                               } else {
                                   String value = null;
                                   try {
                                       for (Map.Entry<Object, Object> e : es) {
                                           String key = (String) e.getKey();
                                           value = (String) e.getValue();
                                           Map<String, Object> config = JacksonUtils.readValue(value, new TypeReference<Map<String, Object>>(){});
                                           sources.put(key, config.get(key));
                                       }
                                   } catch (Throwable t) {
                                       result.code = Result.FAIL;
                                       result.msg  = "init fizz configs error, json: " + value;
                                       result.t    = t;
                                   }
                               }
                               return Mono.empty();
                           }
                   )
                   .onErrorReturn(
                           throwable -> {
                               result.code = Result.FAIL;
                               result.msg  = "init fizz configs error";
                               result.t    = throwable;
                               return true;
                           },
                           result
                   )
                   .block();

        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        if (!sources.isEmpty()) {
            LOGGER.info("fizz configs: {}", JacksonUtils.writeValueAsString(sources));
        }

        String channel = "fizz_config_channel";
        reactiveStringRedisTemplate.listenToChannel(channel)
                                   .doOnError(
                                           t -> {
                                               result.code = Result.FAIL;
                                               result.msg  = "lsn " + channel + " channel error";
                                               result.t    = t;
                                               LOGGER.error("lsn channel {} error", channel, t);
                                           }
                                   )
                                   .doOnSubscribe(
                                           s -> {
                                               LOGGER.info("success to lsn on {}", channel);
                                           }
                                   )
                                   .doOnNext(
                                           msg -> {
                                               String message = msg.getMessage();
                                               try {
                                                   Map<String, Object> changedPropertyValueMap = new HashMap<>();
                                                   List<Map<String, Object>> changes = JacksonUtils.readValue(message, new TypeReference<List<Map<String, Object>>>(){});
                                                   for (Map<String, Object> change : changes) {
                                                       int isDeleted = (int) change.remove("isDeleted");
                                                       Map.Entry<String, Object> propertyValue = change.entrySet().iterator().next();
                                                       String property = propertyValue.getKey();
                                                       Object v = null;
                                                       if (isDeleted == 1) {
                                                           sources.remove(property);
                                                       } else {
                                                           v = propertyValue.getValue();
                                                           sources.put(property, v);
                                                       }
                                                       changedPropertyValueMap.put(property, v);
                                                   }
                                                   LOGGER.info("new fizz configs: {}", JacksonUtils.writeValueAsString(sources));
                                                   FizzRefreshEvent refreshEvent = new FizzRefreshEvent(applicationContext, FizzRefreshEvent.ENV_CHANGE, changedPropertyValueMap);
                                                   applicationContext.publishEvent(refreshEvent);
                                               } catch (Throwable t) {
                                                   LOGGER.error("update fizz config {} error", message, t);
                                               }
                                           }
                                   )
                                   .subscribe();

        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    } */

    private void initBeanProperty2beanMap(ConfigurableListableBeanFactory beanFactory) {
        ClassLoader beanClassLoader = beanFactory.getBeanClassLoader();
        Iterator<String> beanNamesIterator = beanFactory.getBeanNamesIterator();
        while (beanNamesIterator.hasNext()) {
            String beanName = beanNamesIterator.next();
            if (beanName.startsWith(GenericScope.SCOPED_TARGET_PREFIX)) {
                AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(beanName);
                try {
                    beanDefinition.resolveBeanClass(beanClassLoader);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                Class<?> beanClass = null;
                try {
                    beanClass = beanDefinition.getBeanClass();
                } catch (IllegalStateException e) {
                    LOGGER.warn("get {} bean class exception: {}", beanName, e.getMessage());
                    continue;
                }

                FizzRefreshScope an = beanClass.getAnnotation(FizzRefreshScope.class);
                if (an != null) {
                    ReflectionUtils.doWithFields(
                            beanClass,
                            field -> {
                                Value annotation = field.getAnnotation(Value.class);
                                if (annotation != null) {
                                    property2beanMap.put(extractPlaceholderKey(annotation.value()), beanName);
                                }
                            }
                    );
                    ReflectionUtils.doWithMethods(
                            beanClass,
                            method -> {
                                Value annotation = method.getAnnotation(Value.class);
                                if (annotation != null) {
                                    property2beanMap.put(extractPlaceholderKey(annotation.value()), beanName);
                                }
                            }
                    );
                }
            }
        }

        LOGGER.info("fizz refresh scope property to bean map: {}", JacksonUtils.writeValueAsString(property2beanMap));
    }

    private String extractPlaceholderKey(String propertyPlaceholder) {
        int begin = 2;
        int end = propertyPlaceholder.indexOf(':');
        if (end < 0) {
            end = propertyPlaceholder.indexOf('}');
        }
        return propertyPlaceholder.substring(begin, end);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public String getBean(String property) {
        return property2beanMap.get(property);
    }
}
