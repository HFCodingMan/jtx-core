package com.xjt.desensitize.enumservice;

import com.xjt.desensitize.enumtype.DesensitizeType;

/**
 * 脱敏策略服务接口
 *
 * @author JTX
 * @since 1.0.0
 */
public interface DesensitizeStrategyService {

    /**
     * 执行脱敏处理
     *
     * @param origin       原始字符串
     * @param type         脱敏类型
     * @param customFormat 自定义格式
     * @param startKeep    开始保留字符数
     * @param endKeep      结尾保留字符数
     * @param maskChar     脱敏字符
     * @return 脱敏后的字符串
     */
    String desensitize(String origin, DesensitizeType type, String customFormat,
                      int startKeep, int endKeep, char maskChar);
}