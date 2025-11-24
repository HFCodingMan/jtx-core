package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 地址脱敏策略
 * 保留前6后4，如：北京市朝阳区建国门外大街1号 -> 北京市朝阳区建国门******1号
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class AddressDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 6, 4, DEFAULT_MASK);
    }
}