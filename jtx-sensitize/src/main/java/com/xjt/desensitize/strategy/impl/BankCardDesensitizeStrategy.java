package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 银行卡号脱敏策略
 * 保留后4，如：6222021234567890123 -> ************0123
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class BankCardDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 0, 4, DEFAULT_MASK);
    }
}