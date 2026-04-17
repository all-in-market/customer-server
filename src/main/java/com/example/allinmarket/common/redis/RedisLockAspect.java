package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RedisLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(redisLock)")
    public Object run(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {

        // SpEL 표현식을 실제 락 키로 변환
        String lockKey = resolveKey(joinPoint, redisLock.key());

        // RLock은 락 키 단위로 관리되는 분산락 객체
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;

        // 락 획득 대기 중 interrupt 상황 발생 체크 필요 (ex. 락 대기 중 서버 창을 닫을 때 InterruptedException 발생)
        try {
            acquired = lock.tryLock(
                    redisLock.lockWaitTimeSeconds(),
                    redisLock.lockTimeoutSeconds(),
                    redisLock.timeUnit()
            );

            if (!acquired) {
                log.warn("[Redis] Redis락 획득에 실패. key={}", lockKey);
                throw new BaseException(ErrorEnum.REDIS_LOCK_CONFLICT);
            }

            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Redis] Redis 락 처리 중 인터럽트 발생. key={}", lockKey, e);
            throw new BaseException(ErrorEnum.REDIS_LOCK_INTERRUPTED);

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * key = "'lock:payment:' + #paymentId" 이와 같이 전달된 key 값에서
     * #paymentId 값을 동적으로 추출하여 완성된 전체 key 문자열을 반환하는 메서드
     * keyExpression에 "'lock:payment:' + #paymentId" 이 값이 들어옴
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 해당 메서드의 파라미터 이름 리스트
        String[] paramNames = signature.getParameterNames();

        // 해당 메서드의 파라미터의 실제 값 리스트
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // SpEL 식을 해석해서 실제 사용할 키 문자열을 반환
        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }

}