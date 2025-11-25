package com.xjt.desensitize.strategy.impl;

import com.xjt.desensitize.strategy.AbstractDesensitizeStrategy;
import com.xjt.desensitize.util.JsonFieldDesensitizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * JSON字段脱敏策略
 * 支持对JSON字符串中的不同字段应用不同的脱敏类型
 *
 * 此策略使用独立的JsonFieldDesensitizer实现，不依赖外部策略映射
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class JsonFieldDesensitizeStrategy extends AbstractDesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        return desensitize(origin, "", DEFAULT_MASK);
    }

    /**
     * 对JSON字符串进行字段级脱敏处理
     *
     * @param origin     原始字符串
     * @param fieldConfigs 字段配置，多个配置用逗号分隔
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     */
    public String desensitize(String origin, String fieldConfigs, char maskChar) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }

        // 如果不是JSON格式，直接返回原字符串
        if (!JsonFieldDesensitizer.isValidJson(origin)) {
            return origin;
        }

        JsonFieldDesensitizer desensitizer = new JsonFieldDesensitizer();
        desensitizer.setDefaultMaskChar(maskChar);

        // 解析字段配置
        if (StringUtils.hasText(fieldConfigs)) {
            List<String> configList = Arrays.asList(fieldConfigs.split(","));
            desensitizer.addFieldConfigs(configList);
        }

        return desensitizer.desensitize(origin, maskChar);
    }

    /**
     * 使用配置好的脱敏器进行脱敏
     *
     * @param origin       原始字符串
     * @param desensitizer 配置好的脱敏器
     * @return 脱敏后的字符串
     */
    public String desensitize(String origin, JsonFieldDesensitizer desensitizer) {
        if (!StringUtils.hasText(origin) || desensitizer == null) {
            return origin;
        }

        if (!JsonFieldDesensitizer.isValidJson(origin)) {
            return origin;
        }

        return desensitizer.desensitize(origin, '*');
    }

    /**
     * 创建并配置脱敏器
     *
     * @param fieldConfigs 字段配置列表
     * @param maskChar     脱敏字符
     * @return 配置好的脱敏器
     */
    public JsonFieldDesensitizer createDesensitizer(List<String> fieldConfigs, char maskChar) {
        JsonFieldDesensitizer desensitizer = new JsonFieldDesensitizer();
        desensitizer.setDefaultMaskChar(maskChar);

        if (fieldConfigs != null && !fieldConfigs.isEmpty()) {
            desensitizer.addFieldConfigs(fieldConfigs);
        }

        return desensitizer;
    }

    /**
     * 创建简单的字段配置
     *
     * @param fieldPath 字段路径
     * @param type      脱敏类型
     * @return 配置字符串
     */
    public static String createFieldConfig(String fieldPath, String type) {
        return fieldPath + ":" + type;
    }

    /**
     * 创建带参数的字段配置
     *
     * @param fieldPath 字段路径
     * @param type      脱敏类型
     * @param params    参数字符串，如 "startKeep:2,endKeep:4"
     * @return 配置字符串
     */
    public static String createFieldConfig(String fieldPath, String type, String params) {
        StringBuilder config = new StringBuilder(fieldPath).append(":").append(type);
        if (StringUtils.hasText(params)) {
            config.append(":").append(params);
        }
        return config.toString();
    }

    /**
     * 验证JSON格式
     *
     * @param jsonString JSON字符串
     * @return 是否为有效JSON
     */
    public static boolean isValidJson(String jsonString) {
        return JsonFieldDesensitizer.isValidJson(jsonString);
    }
}