package com.xjt.desensitize.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.xjt.desensitize.annotation.Desensitize;
import com.xjt.desensitize.enumservice.DesensitizeStrategyService;
import com.xjt.desensitize.enumtype.DesensitizeType;
import com.xjt.desensitize.strategy.impl.JsonFieldDesensitizeStrategy;
import com.xjt.desensitize.util.SpringContextHolder;

import java.io.IOException;
import java.util.Objects;

/**
 * 数据脱敏序列化器
 *
 * @author JTX
 * @since 1.0.0
 */
public class DesensitizeSerializer extends StdSerializer<Object> implements ContextualSerializer {

    /**
     * 脱敏策略服务
     */
    private transient DesensitizeStrategyService strategyService;

    /**
     * 脱敏类型
     */
    private DesensitizeType type;

    /**
     * 自定义脱敏格式
     */
    private String customFormat;

    /**
     * 开始保留字符数
     */
    private int startKeep;

    /**
     * 结尾保留字符数
     */
    private int endKeep;

    /**
     * 脱敏字符
     */
    private char maskChar;

    /**
     * 是否启用脱敏
     */
    private boolean enabled;

    /**
     * 字段级脱敏配置（用于JSON_FIELD类型）
     */
    private String fieldConfigs;

    public DesensitizeSerializer() {
        super(Object.class);
    }

    public DesensitizeSerializer(DesensitizeStrategyService strategyService) {
        super(Object.class);
        this.strategyService = strategyService;
    }

    public DesensitizeSerializer(DesensitizeStrategyService strategyService, DesensitizeType type,
                                 String customFormat, int startKeep, int endKeep, char maskChar, boolean enabled) {
        super(Object.class);
        this.strategyService = strategyService;
        this.type = type;
        this.customFormat = customFormat;
        this.startKeep = startKeep;
        this.endKeep = endKeep;
        this.maskChar = maskChar;
        this.enabled = enabled;
        this.fieldConfigs = "";
    }

    public DesensitizeSerializer(DesensitizeStrategyService strategyService, DesensitizeType type,
                                 String customFormat, int startKeep, int endKeep, char maskChar, boolean enabled, String fieldConfigs) {
        super(Object.class);
        this.strategyService = strategyService;
        this.type = type;
        this.customFormat = customFormat;
        this.startKeep = startKeep;
        this.endKeep = endKeep;
        this.maskChar = maskChar;
        this.enabled = enabled;
        this.fieldConfigs = fieldConfigs != null ? fieldConfigs : "";
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        try {
            // 获取字段上的Desensitize注解
            Desensitize annotation = property.getAnnotation(Desensitize.class);
            if (annotation == null) {
                return prov.findValueSerializer(property.getType().getRawClass(), property);
            }

            // 验证注解参数
            if (annotation.type() == null) {
                System.err.println("@Desensitize注解的type参数不能为空，跳过脱敏处理");
                return prov.findValueSerializer(property.getType().getRawClass(), property);
            }

            // 尝试从Spring容器中获取脱敏策略服务
            DesensitizeStrategyService strategyService = null;
            try {
                strategyService = SpringContextHolder.getBean(DesensitizeStrategyService.class);

                if (strategyService == null) {
                    System.err.println("从Spring容器获取到的DesensitizeStrategyService为null，跳过脱敏处理");
                    return prov.findValueSerializer(property.getType().getRawClass(), property);
                }

            } catch (IllegalStateException e) {
                // Spring上下文未初始化
                System.err.println("Spring应用上下文未初始化，跳过脱敏处理: " + e.getMessage());
                return prov.findValueSerializer(property.getType().getRawClass(), property);
            } catch (Exception e) {
                // 其他获取Bean的异常
                System.err.println("获取DesensitizeStrategyService失败，跳过脱敏处理: " + e.getMessage());
                return prov.findValueSerializer(property.getType().getRawClass(), property);
            }

            // 验证自定义脱敏参数
            if (annotation.type() == DesensitizeType.CUSTOM) {
                if (annotation.startKeep() < 0 || annotation.endKeep() < 0) {
                    System.err.println("自定义脱敏的开始保留字符数和结束保留字符数不能为负数，使用默认值");
                    return new DesensitizeSerializer(strategyService, annotation.type(),
                            annotation.customFormat(), 0, 0, annotation.maskChar(), annotation.enabled());
                }
            }

            return new DesensitizeSerializer(
                    strategyService,
                    annotation.type(),
                    annotation.customFormat(),
                    annotation.startKeep(),
                    annotation.endKeep(),
                    annotation.maskChar(),
                    annotation.enabled(),
                    annotation.fieldConfigs()
            );

        } catch (Exception e) {
            // 处理createContextual过程中的其他异常
            System.err.println("创建脱敏序列化器上下文时发生错误，使用默认序列化器: " + e.getMessage());

            try {
                return prov.findValueSerializer(property.getType().getRawClass(), property);
            } catch (Exception fallbackException) {
                System.err.println("获取默认序列化器也失败: " + fallbackException.getMessage());
                // 最后的保险措施：返回一个简单的序列化器
                return new JsonSerializer<Object>() {
                    @Override
                    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                        gen.writeObject(value);
                    }
                };
            }
        }
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try {
            if (value == null) {
                gen.writeNull();
                return;
            }

            // 如果没有启用脱敏，直接序列化原值
            if (!enabled || strategyService == null) {
                gen.writeObject(value);
                return;
            }

            try {
                String originalValue = value.toString();
                String desensitizedValue;

                // 针对JSON字段脱敏类型进行特殊处理
                if (type == DesensitizeType.JSON_FIELD) {
                    try {
                        JsonFieldDesensitizeStrategy jsonFieldStrategy =
                            (JsonFieldDesensitizeStrategy) ((com.xjt.desensitize.enumservice.impl.DesensitizeStrategyServiceImpl) strategyService).getStrategy(type);
                        if (jsonFieldStrategy != null) {
                            desensitizedValue = jsonFieldStrategy.desensitize(originalValue, fieldConfigs, maskChar);
                        } else {
                            desensitizedValue = strategyService.desensitize(
                                originalValue, type, customFormat, startKeep, endKeep, maskChar);
                        }
                    } catch (ClassCastException e) {
                        System.err.println("JSON字段脱敏策略类型转换失败，使用通用脱敏处理");
                        desensitizedValue = strategyService.desensitize(
                            originalValue, type, customFormat, startKeep, endKeep, maskChar);
                    }
                } else {
                    desensitizedValue = strategyService.desensitize(
                        originalValue, type, customFormat, startKeep, endKeep, maskChar);
                }

                // 确保脱敏后的值不为null
                String finalValue = desensitizedValue != null ? desensitizedValue : originalValue;
                gen.writeString(finalValue);

            } catch (Exception e) {
                // 脱敏处理失败时，记录错误并使用原值
                System.err.println("数据脱敏处理失败，使用原值。错误信息: " + e.getMessage() +
                    ", 原始值: " + (value != null ? value.getClass().getSimpleName() : "null"));

                // 回退到原值的序列化
                gen.writeObject(value);
            }

        } catch (IOException ioException) {
            // IO异常直接抛出，让上层处理
            throw ioException;
        } catch (Exception exception) {
            // 其他异常包装为IOException
            System.err.println("序列化过程中发生未知错误: " + exception.getMessage());
            throw new IOException("序列化失败", exception);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DesensitizeSerializer that = (DesensitizeSerializer) obj;

        if (startKeep != that.startKeep) return false;
        if (endKeep != that.endKeep) return false;
        if (maskChar != that.maskChar) return false;
        if (enabled != that.enabled) return false;
        if (!Objects.equals(type, that.type)) return false;
        if (!Objects.equals(customFormat, that.customFormat)) return false;
        if (!Objects.equals(fieldConfigs, that.fieldConfigs)) return false;
        return Objects.equals(strategyService, that.strategyService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyService, type, customFormat, startKeep, endKeep, maskChar, enabled, fieldConfigs);
    }
}