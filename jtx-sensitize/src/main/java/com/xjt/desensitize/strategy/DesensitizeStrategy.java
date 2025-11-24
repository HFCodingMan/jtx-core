package com.xjt.desensitize.strategy;

/**
 * 脱敏策略接口
 *
 * @author JTX
 * @since 1.0.0
 */
public interface DesensitizeStrategy {

    /**
     * 执行脱敏处理
     *
     * @param origin 原始字符串
     * @return 脱敏后的字符串
     */
    String desensitize(String origin);
}