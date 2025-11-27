package com.xjt.desensitize.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xjt.desensitize.enumtype.DesensitizeType;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON字段脱敏处理器
 * 支持对JSON字符串中的特定字段应用不同类型的脱敏规则
 * 完全自定义实现，不依赖外部策略映射
 *
 * 配置格式：字段路径:脱敏类型[:参数]
 * 多个字段配置用分号(;)分隔，参数用逗号(,)分隔
 *
 * 示例配置：
 * "user.phone:PHONE;user.idCard:ID_CARD"
 * "items[*].phone:PHONE:startKeep:2,endKeep:3;items[*].idCard:ID_CARD:startKeep:4,endKeep:2"
 *
 * @author JTX
 * @since 1.0.0
 */
public class JsonFieldDesensitizer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 字段路径配置
     * Map<字段路径, 脱敏类型>
     */
    private final Map<String, DesensitizeType> fieldConfigs = new HashMap<>();

    /**
     * 字段路径参数配置
     * Map<字段路径, 脱敏参数字符串>
     */
    private final Map<String, String> fieldParams = new HashMap<>();

    /**
     * 默认脱敏字符
     */
    private char defaultMaskChar = '*';

    /**
     * 添加字段脱敏配置
     *
     * @param fieldPath 字段路径，如 "user.phone", "items[*].idCard"
     * @param type      脱敏类型
     */
    public void addFieldConfig(String fieldPath, DesensitizeType type) {
        if (StringUtils.hasText(fieldPath) && type != null) {
            fieldConfigs.put(fieldPath, type);
        }
    }

    /**
     * 添加字段脱敏配置（带参数）
     *
     * @param fieldPath 字段路径
     * @param type      脱敏类型
     * @param params    脱敏参数，格式为 "startKeep:2,endKeep:4,maskChar:#"
     */
    public void addFieldConfig(String fieldPath, DesensitizeType type, String params) {
        if (StringUtils.hasText(fieldPath) && type != null) {
            fieldConfigs.put(fieldPath, type);
            if (StringUtils.hasText(params)) {
                fieldParams.put(fieldPath, params);
            }
        }
    }

    /**
     * 批量添加字段配置
     *
     * @param configs 字段配置列表，每项格式为 "fieldPath:type[:params]"
     *                参数格式为 "startKeep:2,endKeep:3,maskChar:#"
     */
    public void addFieldConfigs(List<String> configs) {
        if (configs != null) {
            for (String config : configs) {
                parseFieldConfig(config);
            }
        }
    }

    /**
     * 解析字段配置字符串
     * 格式：fieldPath:type[:params]
     * 其中params格式为：startKeep:2,endKeep:3,maskChar:#
     */
    private void parseFieldConfig(String config) {
        if (!StringUtils.hasText(config)) {
            return;
        }

        // 先分割前两部分（fieldPath和type），最多分割3部分
        String[] firstSplit = config.split(":", 3);
        if (firstSplit.length >= 2) {
            String fieldPath = firstSplit[0].trim();
            try {
                DesensitizeType type = DesensitizeType.valueOf(firstSplit[1].trim().toUpperCase());

                if (firstSplit.length == 3) {
                    // 有参数，直接使用第三部分
                    addFieldConfig(fieldPath, type, firstSplit[2]);
                } else {
                    // 无参数
                    addFieldConfig(fieldPath, type);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("无效的脱敏类型: " + firstSplit[1] + ", 跳过配置: " + config);
            }
        }
    }

    /**
     * 对JSON字符串进行字段级脱敏处理
     *
     * @param jsonString JSON字符串
     * @param defaultMaskChar 默认脱敏字符
     * @return 脱敏后的JSON字符串
     */
    public String desensitize(String jsonString, char defaultMaskChar) {
        if (!StringUtils.hasText(jsonString) || fieldConfigs.isEmpty()) {
            return jsonString;
        }

        this.defaultMaskChar = defaultMaskChar;

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            if (rootNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) rootNode;
                desensitizeObject(objectNode, "");
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (IOException e) {
            System.err.println("JSON解析失败，返回原始字符串: " + e.getMessage());
            return jsonString;
        }

        return jsonString;
    }

    /**
     * 递归处理对象节点的脱敏
     */
    private void desensitizeObject(ObjectNode objectNode, String currentPath) {
        if (objectNode == null) {
            return;
        }

        try {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                String fullPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;

                if (fieldValue != null) {
                    if (fieldValue.isObject()) {
                        // 递归处理嵌套对象
                        desensitizeObject((ObjectNode) fieldValue, fullPath);
                    } else if (fieldValue.isArray()) {
                        // 处理数组
                        desensitizeArray((ArrayNode) fieldValue, fullPath);
                    } else if (fieldValue.isValueNode()) {
                        // 处理值节点（字符串、数字等）
                        DesensitizeType type = findMatchingType(fullPath);
                        if (type != null) {
                            String desensitizedValue = desensitizeValue(fieldValue.asText(), type, fullPath);
                            objectNode.put(fieldName, desensitizedValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("处理对象节点时发生错误: " + e.getMessage());
        }
    }

    /**
     * 递归处理数组节点的脱敏
     */
    private void desensitizeArray(ArrayNode arrayNode, String currentPath) {
        if (arrayNode == null || arrayNode.size() == 0) {
            return;
        }

        for (int i = 0; i < arrayNode.size(); i++) {
            try {
                JsonNode element = arrayNode.get(i);
                if (element == null) {
                    continue; // 跳过null元素
                }

                String elementPath = currentPath + "[" + i + "]";

                if (element.isObject()) {
                    desensitizeObject((ObjectNode) element, elementPath);
                } else if (element.isArray()) {
                    desensitizeArray((ArrayNode) element, elementPath);
                } else if (element.isValueNode()) {
                    // 处理数组中的值节点
                    DesensitizeType type = findMatchingType(currentPath + "[*]");
                    if (type != null) {
                        String desensitizedValue = desensitizeValue(element.asText(), type, elementPath);
                        arrayNode.set(i, objectMapper.getNodeFactory().textNode(desensitizedValue));
                    }
                }
            } catch (Exception e) {
                System.err.println("处理数组元素时发生错误: " + e.getMessage());
                continue;
            }
        }
    }

    /**
     * 查找匹配的字段脱敏类型
     */
    private DesensitizeType findMatchingType(String fullPath) {
        // 将路径标准化，将数组索引替换为通配符
        String normalizedPath = normalizeFieldPath(fullPath);

        // 精确匹配（使用标准化路径）
        DesensitizeType exactMatch = fieldConfigs.get(normalizedPath);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 模式匹配
        for (Map.Entry<String, DesensitizeType> entry : fieldConfigs.entrySet()) {
            if (matchesPath(normalizedPath, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 路径匹配
     */
    private boolean matchesPath(String fullPath, String pattern) {
        // 精确匹配
        if (pattern.equals(fullPath)) {
            return true;
        }

        // 通配符匹配
        if (pattern.contains("[*]")) {
            // 转换为正则表达式
            String regex = pattern.replace(".", "\\.")
                               .replace("[*]", "\\[\\d+\\]")
                               .replace("(", "\\(")
                               .replace(")", "\\)")
                               .replace("[", "\\[")
                               .replace("]", "\\]")
                               .replace("?", "\\?")
                               .replace("+", "\\+")
                               .replace("*", "\\*");

            // 确保正则表达式完整匹配
            regex = "^" + regex + "$";

            return fullPath.matches(regex);
        }

        // 前缀匹配（用于嵌套对象）
        if (fullPath.startsWith(pattern + ".")) {
            return true;
        }

        return false;
    }

    /**
     * 对单个值进行脱敏处理 - 完全自定义实现
     */
    private String desensitizeValue(String value, DesensitizeType type, String fieldPath) {
        // 加强null和空值检查
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        // 解析字段参数
        FieldDesensitizeParams params = parseFieldParams(fieldPath);

        // 根据脱敏类型执行相应的脱敏逻辑
        switch (type) {
            case USERNAME:
                return desensitizeUsername(value, params.startKeep, params.endKeep, params.maskChar);
            case ID_CARD:
                return desensitizeIdCard(value, params.startKeep, params.endKeep, params.maskChar);
            case PHONE:
                return desensitizePhone(value, params.startKeep, params.endKeep, params.maskChar);
            case EMAIL:
                return desensitizeEmail(value, params.startKeep, params.endKeep, params.maskChar);
            case BANK_CARD:
                return desensitizeBankCard(value, params.startKeep, params.endKeep, params.maskChar);
            case CHINESE_NAME:
                return desensitizeChineseName(value, params.startKeep, params.endKeep, params.maskChar);
            case PASSWORD:
                return desensitizePassword(value, params.startKeep, params.endKeep, params.maskChar);
            case ADDRESS:
                return desensitizeAddress(value, params.startKeep, params.endKeep, params.maskChar);
            case CUSTOM:
                return desensitizeCustom(value, params.startKeep, params.endKeep, params.maskChar);
            default:
                return value;
        }
    }

    /**
     * 用户名脱敏 - 隐藏首字符
     */
    private String desensitizeUsername(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：隐藏首字符
        if (value.length() <= 1) {
            return repeatMask(maskChar, value.length());
        }
        return maskChar + value.substring(1);
    }

    /**
     * 身份证号脱敏 - 保留前6后4
     */
    private String desensitizeIdCard(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：保留前6后4
        if (value.length() <= 10) {
            return repeatMask(maskChar, value.length());
        }
        return value.substring(0, 6) + repeatMask(maskChar, value.length() - 10) + value.substring(value.length() - 4);
    }

    /**
     * 手机号脱敏 - 保留前3后4
     */
    private String desensitizePhone(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：保留前3后4
        if (value.length() <= 7) {
            return repeatMask(maskChar, value.length());
        }
        return value.substring(0, 3) + repeatMask(maskChar, value.length() - 7) + value.substring(value.length() - 4);
    }

    /**
     * 邮箱脱敏 - 隐藏@前部分的部分字符
     */
    private String desensitizeEmail(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：隐藏@前部分的部分字符
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return repeatMask(maskChar, atIndex) + value.substring(atIndex);
        }
        return value.charAt(0) + repeatMask(maskChar, atIndex - 1) + value.substring(atIndex);
    }

    /**
     * 银行卡号脱敏 - 保留后4
     */
    private String desensitizeBankCard(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：保留后4
        if (value.length() <= 4) {
            return repeatMask(maskChar, value.length());
        }
        return repeatMask(maskChar, value.length() - 4) + value.substring(value.length() - 4);
    }

    /**
     * 中文姓名脱敏 - 隐藏首字符
     */
    private String desensitizeChineseName(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：隐藏首字符
        if (value.length() <= 1) {
            return repeatMask(maskChar, value.length());
        }
        return maskChar + value.substring(1);
    }

    /**
     * 密码脱敏 - 全部隐藏
     */
    private String desensitizePassword(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：全部隐藏
        return repeatMask(maskChar, value.length());
    }

    /**
     * 地址脱敏 - 保留前6后4
     */
    private String desensitizeAddress(String value, int startKeep, int endKeep, char maskChar) {
        if (startKeep > 0 || endKeep > 0) {
            // 如果有参数，使用通用脱敏方法
            return desensitizeCustom(value, startKeep, endKeep, maskChar);
        }
        // 默认行为：保留前6后4
        if (value.length() <= 10) {
            return repeatMask(maskChar, value.length());
        }
        return value.substring(0, 6) + repeatMask(maskChar, value.length() - 10) + value.substring(value.length() - 4);
    }

    /**
     * 自定义脱敏 - 根据参数保留前后字符
     */
    private String desensitizeCustom(String value, int startKeep, int endKeep, char maskChar) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        int length = value.length();

        // 如果字符串长度小于等于要保留的字符总数
        if (length <= startKeep + endKeep) {
            // 如果要保留的总字符数等于或超过字符串长度
            if (startKeep >= length) {
                return repeatMask(maskChar, length);
            }
            if (endKeep >= length) {
                return repeatMask(maskChar, length);
            }
            if (length <= 2) {
                return repeatMask(maskChar, length);
            }
            // 保留首尾，中间脱敏
            return value.charAt(0) + repeatMask(maskChar, length - 2) + value.charAt(length - 1);
        }

        // 计算需要脱敏的长度
        int maskLength = length - startKeep - endKeep;

        // 构建脱敏字符串
        StringBuilder result = new StringBuilder();

        // 添加开始保留部分
        if (startKeep > 0) {
            result.append(safeSubstring(value, 0, startKeep));
        }

        // 添加脱敏部分
        result.append(repeatMask(maskChar, maskLength));

        // 添加结尾保留部分
        if (endKeep > 0) {
            result.append(safeSubstring(value, length - endKeep, length));
        }

        return result.toString();
    }

    /**
     * 创建安全的substring方法，防止边界异常
     */
    private String safeSubstring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (start < 0) start = 0;
        if (end > length) end = length;
        if (start >= end) return "";
        return str.substring(start, end);
    }

    /**
     * 重复脱敏字符 - JDK 1.8兼容方法
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

    /**
     * 解析字段脱敏参数
     */
    private FieldDesensitizeParams parseFieldParams(String fieldPath) {
        FieldDesensitizeParams params = new FieldDesensitizeParams();
        params.maskChar = this.defaultMaskChar;

        // 将字段路径中的所有数组索引替换为通配符，以便匹配配置
        String normalizedPath = normalizeFieldPath(fieldPath);

        // 尝试精确匹配
        String paramStr = fieldParams.get(normalizedPath);

        // 如果精确匹配失败，尝试查找匹配的模式
        if (!StringUtils.hasText(paramStr)) {
            for (Map.Entry<String, String> entry : fieldParams.entrySet()) {
                if (matchesPath(normalizedPath, entry.getKey())) {
                    paramStr = entry.getValue();
                    break;
                }
            }
        }

        if (StringUtils.hasText(paramStr)) {
            String[] paramPairs = paramStr.split(",");
            for (String pair : paramPairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    switch (key) {
                        case "startKeep":
                            try {
                                int parsedValue = Integer.parseInt(value);
                                if (parsedValue >= 0 && parsedValue <= 1000) { // 合理范围限制
                                    params.startKeep = parsedValue;
                                } else {
                                    System.err.println("startKeep参数超出有效范围[0-1000]: " + value);
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("startKeep参数格式错误: " + value + ", 使用默认值0");
                            }
                            break;
                        case "endKeep":
                            try {
                                int parsedValue = Integer.parseInt(value);
                                if (parsedValue >= 0 && parsedValue <= 1000) { // 合理范围限制
                                    params.endKeep = parsedValue;
                                } else {
                                    System.err.println("endKeep参数超出有效范围[0-1000]: " + value);
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("endKeep参数格式错误: " + value + ", 使用默认值0");
                            }
                            break;
                        case "maskChar":
                            if (value.length() == 1) {
                                params.maskChar = value.charAt(0);
                            } else {
                                System.err.println("maskChar参数必须是单个字符: " + value + ", 使用默认值*");
                            }
                            break;
                    }
                }
            }
        }

        return params;
    }

    /**
     * 将字段路径标准化，将所有数组索引替换为通配符
     * 例如: items[0].phone -> items[*].phone
     *      user.items[2].contacts[5].email -> user.items[*].contacts[*].email
     */
    private String normalizeFieldPath(String fieldPath) {
        if (!StringUtils.hasText(fieldPath)) {
            return fieldPath;
        }
        // 将所有 [数字] 替换为 [*]
        return fieldPath.replaceAll("\\[\\d+\\]", "[*]");
    }

    /**
     * 字段脱敏参数
     */
    private static class FieldDesensitizeParams {
        int startKeep = 0;
        int endKeep = 0;
        char maskChar = '*';
    }

    /**
     * 设置默认脱敏字符
     */
    public void setDefaultMaskChar(char maskChar) {
        this.defaultMaskChar = maskChar;
    }

    /**
     * 清空所有配置
     */
    public void clearConfigs() {
        fieldConfigs.clear();
        fieldParams.clear();
    }

    /**
     * 获取配置的字段数量
     */
    public int getConfigCount() {
        return fieldConfigs.size();
    }

    /**
     * 验证JSON字符串格式
     */
    public static boolean isValidJson(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return false;
        }

        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}