package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.DesensitizeStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮箱脱敏策略
 * 隐藏@前部分，如：zhangsan@example.com -> ***@example.com
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class EmailDesensitizeStrategy implements DesensitizeStrategy {

    /**
     * 重复脱敏字符（JDK 1.8兼容）
     */
    private String repeatMask(char maskChar, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(maskChar);
        }
        return sb.toString();
    }

    @Override
    public String desensitize(String origin) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }

        int atIndex = origin.indexOf('@');
        if (atIndex <= 0) {
            return origin;
        }

        String localPart = origin.substring(0, atIndex);
        String domainPart = origin.substring(atIndex);

        // 如果@前面只有一个字符，直接脱敏
        if (localPart.length() <= 1) {
            return repeatMask('*', Math.max(localPart.length(), 3)) + domainPart;
        }

        // 保留第一个字符，其余用*代替
        return localPart.charAt(0) + repeatMask('*', Math.max(localPart.length() - 1, 2)) + domainPart;
    }
}