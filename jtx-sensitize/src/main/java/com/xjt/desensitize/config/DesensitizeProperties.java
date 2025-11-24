package com.xjt.desensitize.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 脱敏配置属性
 *
 * @author JTX
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "jtx.desensitize")
public class DesensitizeProperties {

    /**
     * 是否启用脱敏功能
     */
    private boolean enabled = true;

    /**
     * 默认脱敏字符
     */
    private char defaultMask = '*';

    /**
     * 全局脱敏开关
     */
    private boolean globalEnabled = true;
}