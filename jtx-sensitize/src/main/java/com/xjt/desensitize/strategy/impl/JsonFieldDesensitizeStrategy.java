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
     * @param fieldConfigs 字段配置，多个配置用分号(;)分隔
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
        // 使用分号(;)分隔不同的字段配置，逗号(,)分隔参数
        if (StringUtils.hasText(fieldConfigs)) {
            List<String> configList = Arrays.asList(fieldConfigs.split(";"));
            desensitizer.addFieldConfigs(configList);
        }

        return desensitizer.desensitize(origin, maskChar);
    }


}