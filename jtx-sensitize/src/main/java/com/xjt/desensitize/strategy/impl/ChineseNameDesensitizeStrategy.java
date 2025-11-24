package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;

/**
 * 中文姓名脱敏策略
 * 隐藏首字符，如：张三 -> *三，欧阳修 -> **修
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class ChineseNameDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return mask(origin, 0, 1, DEFAULT_MASK);
    }
}