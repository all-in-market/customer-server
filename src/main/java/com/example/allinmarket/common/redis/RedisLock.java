package com.example.allinmarket.common.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    String key();

    // 락 획득 시도 최대 대기 시간
    long lockWaitTimeSeconds() default 10;

    // 락 TTL
    long lockTimeoutSeconds() default 5;

    // 시간 단위 기본: 초
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
