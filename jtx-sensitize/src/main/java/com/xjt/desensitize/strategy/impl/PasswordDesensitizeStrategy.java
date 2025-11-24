package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.DesensitizeStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 密码脱敏策略
 * 全部隐藏，如：123456 -> ******
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class PasswordDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }
        return "******";
    }
}