package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 身份证号脱敏策略
 * 保留前6后4，如：11010119900307899X -> 110101********99X
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class IdCardDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 6, 4, DEFAULT_MASK);
    }
}