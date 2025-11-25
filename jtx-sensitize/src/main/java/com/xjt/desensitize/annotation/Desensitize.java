package com.xjt.desensitize.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.xjt.desensitize.enumtype.DesensitizeType;
import com.xjt.desensitize.serializer.DesensitizeSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解
 * 使用在需要进行脱敏处理的字段上
 *
 * @author JTX
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    /**
     * 脱敏类型
     *
     * @return 脱敏类型
     */
    DesensitizeType type();

    /**
     * 自定义脱敏格式
     * 当type为CUSTOM时使用，支持占位符：
     * ${start} - 开始保留的字符数
     * ${end} - 结尾保留的字符数
     * ${mask} - 脱敏字符（默认为*）
     * 示例：${start:3}${mask:****}${end:4} -> 138****5678
     *
     * @return 自定义脱敏格式
     */
    String customFormat() default "";

    /**
     * 开始保留的字符数
     * 仅在type为CUSTOM或需要自定义保留字符数时有效
     *
     * @return 开始保留字符数
     */
    int startKeep() default 0;

    /**
     * 结尾保留的字符数
     * 仅在type为CUSTOM或需要自定义保留字符数时有效
     *
     * @return 结尾保留字符数
     */
    int endKeep() default 0;

    /**
     * 脱敏字符
     * 默认使用*
     *
     * @return 脱敏字符
     */
    char maskChar() default '*';

    /**
     * 字段级脱敏配置
     * 当需要对JSON字符串中的不同字段应用不同的脱敏类型时使用
     * 配置格式：字段路径:脱敏类型[:参数]
     * 多个配置用逗号分隔
     *
     * 示例：
     * "user.phone:PHONE,user.idCard:ID_CARD,user.email:EMAIL"
     * "items[*].price:PHONE,items[*].secret:PASSWORD:startKeep:0,endKeep:0,maskChar:#"
     * "contactInfo.phone:PHONE:maskChar:#,contactInfo.email:EMAIL"
     *
     * 支持的脱敏类型：USERNAME, ID_CARD, PHONE, EMAIL, BANK_CARD,
     *                   CHINESE_NAME, PASSWORD, ADDRESS, CUSTOM
     *
     * @return 字段级脱敏配置
     */
    String fieldConfigs() default "";

    /**
     * 是否启用脱敏
     * 可用于动态控制脱敏开关
     *
     * @return 是否启用脱敏
     */
    boolean enabled() default true;
}