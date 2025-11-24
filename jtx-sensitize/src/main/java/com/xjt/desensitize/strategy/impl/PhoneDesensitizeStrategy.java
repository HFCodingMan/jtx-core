package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 手机号脱敏策略
 * 保留前3后4，如：13812345678 -> 138****5678
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class PhoneDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 3, 4, DEFAULT_MASK);
    }
}