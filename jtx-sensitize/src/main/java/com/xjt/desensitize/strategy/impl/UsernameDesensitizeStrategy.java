package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 用户名脱敏策略
 * 隐藏首字符，如：张三 -> *三，admin -> *dmin
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class UsernameDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 0, 1, DEFAULT_MASK);
    }
}