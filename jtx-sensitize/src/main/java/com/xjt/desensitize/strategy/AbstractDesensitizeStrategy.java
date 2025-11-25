package com.xjt.desensitize.strategy;

import org.springframework.util.StringUtils;

/**
 * 抽象脱敏策略基类
 *
 * @author JTX
 * @since 1.0.0
 */
public abstract class AbstractDesensitizeStrategy implements DesensitizeStrategy {

    /**
     * 默认脱敏字符
     */
    protected static final char DEFAULT_MASK = '*';

    /**
     * 执行通用脱敏逻辑
     *
     * @param origin     原始字符串
     * @param startKeep  开始保留字符数
     * @param endKeep    结尾保留字符数
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     */
    protected String mask(String origin, int startKeep, int endKeep, char maskChar) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }

        int length = origin.length();

        // 如果字符串长度小于等于要保留的字符总数，直接返回原字符串或全部脱敏
        if (length <= startKeep + endKeep) {
            if (length <= 2) {
                return !StringUtils.hasText(origin) ? origin : repeatMask(maskChar, length);
            }
            // 保留首尾，中间脱敏
            return origin.charAt(0) + repeatMask(maskChar, length - 2) + origin.charAt(length - 1);
        }

        // 计算需要脱敏的长度
        int maskLength = length - startKeep - endKeep;

        // 构建脱敏字符串
        StringBuilder result = new StringBuilder();

        // 添加开始保留部分
        if (startKeep > 0) {
            result.append(origin.substring(0, startKeep));
        }

        // 添加脱敏部分
        result.append(repeatMask(maskChar, maskLength));

        // 添加结尾保留部分
        if (endKeep > 0) {
            result.append(origin.substring(length - endKeep));
        }

        return result.toString();
    }

    /**
     * 重复脱敏字符
     * JDK 1.8兼容方法，替代String.repeat
     *
     * @param maskChar 脱敏字符
     * @param count    重复次数
     * @return 重复的字符串
     */
    protected String repeatMask(char maskChar, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(maskChar);
        }
        return sb.toString();
    }
}