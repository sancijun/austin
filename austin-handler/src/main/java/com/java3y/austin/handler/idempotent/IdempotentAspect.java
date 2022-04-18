package com.java3y.austin.handler.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 幂等切面
 * @author sancijun
 * @date 2022/04/18
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    @Value("${expire:1800}")
    private Integer expire;

    @Autowired
    private RedisTemplate redisTemplate;

    @Pointcut("@annotation(com.java3y.austin.handler.deduplication.Idempotent)")
    public void pointcut() {}


    @Around("pointcut() && @annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        try {
            Integer status = 0;
            // 1.获取参数生成业务唯一标识 key，从 Redis 获取消息记录状态
            String key = uniqueKey(joinPoint, idempotent);
            // 2.插入消费状态记录
            boolean first = redisTemplate.opsForValue().setIfAbsent(key,status, expire, TimeUnit.SECONDS);
            // 3.插入不成功：消费成功则直接返回，消费不成功则抛出异常；
            if (!first){
                status = (Integer) redisTemplate.opsForValue().get(key);
                log.warn("idempotent intercept, key={}, status={}", key, status);
                return status.equals(0) ? ConsumeConcurrentlyStatus.RECONSUME_LATER
                        : ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            // 4.执行业务代码
            Object result = joinPoint.proceed();
            // 5.设置为消费成功
            redisTemplate.opsForValue().set(key, 1, expire, TimeUnit.SECONDS);
            log.info("idempotent success, key={}", key);
            return result;
        } catch (Throwable throwable) {
            // 6.消费失败删除消息状态记录，等待重试
            String key = uniqueKey(joinPoint, idempotent);
            redisTemplate.delete(key);
            log.warn("idempotent error, key={}", key, throwable);
            throw throwable;
        }
    }

    /**
     * 根据注解指定的参数生成业务唯一Key
     * @param joinPoint
     * @param idempotent
     * @return
     */
    private String uniqueKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        StringBuilder key = new StringBuilder();
        // 1.获取注解中需要组成业务唯一 Key 的参数
        String[] subkeys = idempotent.subkeys();
        // 2.这里获取到所有的参数值的数组
        Object[] args = joinPoint.getArgs();
        // 3.获取方法的所有参数名称的字符串数组
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        String[] targetMethodParams = methodSignature.getParameterNames();

        if(StringUtils.isNotBlank(idempotent.target())){
            // 4.1 从指定对象中获取subkeys
            int idx = ArrayUtils.indexOf(targetMethodParams, idempotent.target());
            Map map = new BeanMap(args[idx]);
            for (String param : subkeys) {
                Optional.ofNullable(map.get(param)).ifPresent(sk->key.append(sk.toString()));
            }
        }else {
            // 4.2 从方法入参列表中获取subkeys
            for (String param : subkeys) {
                int idx = ArrayUtils.indexOf(args, param);
                key.append(idx == -1 ? "" : args[idx]);
            }
        }
        // 5.业务标识前缀+MD5(subkeys)组成幂等key
        return idempotent.prefix()+ Md5Crypt.md5Crypt(key.toString().getBytes());
    }
}
