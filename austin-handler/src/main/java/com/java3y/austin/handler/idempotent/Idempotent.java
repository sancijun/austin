package com.java3y.austin.handler.idempotent;

import java.lang.annotation.*;

/**
 * 幂等自定义注解
 * @author sancijun
 * @date 2022/04/18
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /** 幂等 Redis  Key 前缀，用于区分业务*/
    String prefix() default "";

    /** 组成幂等 Key 的 subkey，通过拼接前缀及参数生成幂等业务唯一key */
    String[] subkeys() default {};

    /** 如果接口有多个参数，需要指定subKeys包含在哪个目标参数中 */
    String target() default "";
}
