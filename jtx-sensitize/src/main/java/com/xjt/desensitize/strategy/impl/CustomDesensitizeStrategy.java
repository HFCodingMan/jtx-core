package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义脱敏策略
 * 根据用户指定的参数进行脱敏
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class CustomDesensitizeStrategy extends AbstractDesensitizeStrategy {

    /**
     * 执行自定义脱敏
     *
     * @param origin     原始字符串
     * @param startKeep  开始保留字符数
     * @param endKeep    结尾保留字符数
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     */
    public String desensitize(String origin, int startKeep, int endKeep, char maskChar) {
        return mask(origin, startKeep, endKeep, maskChar);
    }

    @Override
    public String desensitize(String origin) {
        // 默认保留前后各2位字符
        return mask(origin, 2, 2, DEFAULT_MASK);
    }
}