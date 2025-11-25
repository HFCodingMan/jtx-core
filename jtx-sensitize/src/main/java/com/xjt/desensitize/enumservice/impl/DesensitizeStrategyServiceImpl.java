package com.xjt.desensitize.enumservice.impl;

import com.xjt.desensitize.enumservice.DesensitizeStrategyService;
import com.xjt.desensitize.enumtype.DesensitizeType;
import com.xjt.desensitize.strategy.DesensitizeStrategy;
import com.xjt.desensitize.strategy.impl.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 脱敏策略服务实现类
 *
 * @author JTX
 * @since 1.0.0
 */
@Service
public class DesensitizeStrategyServiceImpl implements DesensitizeStrategyService {

    /**
     * 脱敏策略映射
     */
    private final Map<DesensitizeType, DesensitizeStrategy> strategyMap = new HashMap<>();

    /**
     * 自定义脱敏策略
     */
    private final CustomDesensitizeStrategy customStrategy;

    public DesensitizeStrategyServiceImpl(UsernameDesensitizeStrategy usernameStrategy,
                                         IdCardDesensitizeStrategy idCardStrategy,
                                         PhoneDesensitizeStrategy phoneStrategy,
                                         EmailDesensitizeStrategy emailStrategy,
                                         BankCardDesensitizeStrategy bankCardStrategy,
                                         ChineseNameDesensitizeStrategy chineseNameStrategy,
                                         PasswordDesensitizeStrategy passwordStrategy,
                                         AddressDesensitizeStrategy addressStrategy,
                                         JsonFieldDesensitizeStrategy jsonFieldDesensitizeStrategy,
                                         CustomDesensitizeStrategy customStrategy) {
        this.customStrategy = customStrategy;

        strategyMap.put(DesensitizeType.USERNAME, usernameStrategy);
        strategyMap.put(DesensitizeType.ID_CARD, idCardStrategy);
        strategyMap.put(DesensitizeType.PHONE, phoneStrategy);
        strategyMap.put(DesensitizeType.EMAIL, emailStrategy);
        strategyMap.put(DesensitizeType.BANK_CARD, bankCardStrategy);
        strategyMap.put(DesensitizeType.CHINESE_NAME, chineseNameStrategy);
        strategyMap.put(DesensitizeType.PASSWORD, passwordStrategy);
        strategyMap.put(DesensitizeType.JSON_FIELD, jsonFieldDesensitizeStrategy);
        strategyMap.put(DesensitizeType.ADDRESS, addressStrategy);
    }

    @Override
    public String desensitize(String origin, DesensitizeType type, String customFormat,
                             int startKeep, int endKeep, char maskChar) {

        try {
            if (origin == null) {
                return null;
            }

            // 验证输入参数
            if (type == null) {
                System.err.println("脱敏类型不能为null，返回原字符串");
                return origin;
            }

            if (origin.isEmpty()) {
                return origin;
            }

            // 验证参数合法性
            if (startKeep < 0 || endKeep < 0) {
                System.err.println("保留字符数不能为负数，使用默认值");
                startKeep = Math.max(0, startKeep);
                endKeep = Math.max(0, endKeep);
            }

            // 对于自定义脱敏类型，使用特殊处理
            if (DesensitizeType.CUSTOM.equals(type)) {
                try {
                    if (customStrategy == null) {
                        System.err.println("自定义脱敏策略未初始化，返回原字符串");
                        return origin;
                    }
                    return customStrategy.desensitize(origin, startKeep, endKeep, maskChar);
                } catch (Exception e) {
                    System.err.println("自定义脱敏处理失败: " + e.getMessage() + "，返回原字符串");
                    return origin;
                }
            }

            // 如果有自定义格式，优先使用自定义格式
            if (customFormat != null && !customFormat.trim().isEmpty()) {
                try {
                    return processCustomFormat(origin, customFormat, startKeep, endKeep, maskChar);
                } catch (Exception e) {
                    System.err.println("自定义格式处理失败: " + e.getMessage() + "，使用默认策略");
                    // 继续使用默认策略
                }
            }

            // 根据类型获取对应的策略
            DesensitizeStrategy strategy = strategyMap.get(type);
            if (strategy != null) {
                try {
                    String result = strategy.desensitize(origin);
                    // 确保返回结果不为null
                    return result != null ? result : origin;
                } catch (Exception e) {
                    System.err.println("脱敏策略执行失败 (" + type + "): " + e.getMessage() + "，返回原字符串");
                    return origin;
                }
            }

            // 如果没有找到对应策略，返回原字符串
            System.err.println("未找到对应的脱敏策略: " + type + "，返回原字符串");
            return origin;

        } catch (Exception e) {
            // 捕获所有未预期的异常
            System.err.println("脱敏处理过程中发生未知错误: " + e.getMessage() + "，返回原字符串");
            return origin;
        }
    }

    /**
     * 处理自定义格式
     * 格式示例：${start:3}${mask:****}${end:4}
     *
     * @param origin       原始字符串
     * @param customFormat 自定义格式
     * @param startKeep    开始保留字符数
     * @param endKeep      结尾保留字符数
     * @param maskChar     脱敏字符
     * @return 脱敏后的字符串
     */
    private String processCustomFormat(String origin, String customFormat, int startKeep, int endKeep, char maskChar) {
        // 简单的自定义格式处理
        // 这里可以根据需要扩展更复杂的格式解析逻辑
        return customStrategy.desensitize(origin, startKeep, endKeep, maskChar);
    }

    /**
     * 获取指定类型的脱敏策略
     * 用于序列化器中的特殊处理
     *
     * @param type 脱敏类型
     * @return 脱敏策略，如果没有找到则返回null
     */
    public DesensitizeStrategy getStrategy(DesensitizeType type) {
        return strategyMap.get(type);
    }
}