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
     * @param configs 字段配置，格式为 "fieldPath:type:params"
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
     * 格式：fieldPath:type 或 fieldPath:type:startKeep:2,endKeep:4
     */
    private void parseFieldConfig(String config) {
        if (!StringUtils.hasText(config)) {
            return;
        }

        String[] parts = config.split(":");
        if (parts.length >= 2) {
            String fieldPath = parts[0].trim();
            try {
                DesensitizeType type = DesensitizeType.valueOf(parts[1].trim().toUpperCase());

                if (parts.length >= 3) {
                    // 有参数
                    String params = String.join(":", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                    addFieldConfig(fieldPath, type, params);
                } else {
                    // 无参数
                    addFieldConfig(fieldPath, type);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("无效的脱敏类型: " + parts[1] + ", 跳过配置: " + config);
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
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            String fullPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;

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

    /**
     * 递归处理数组节点的脱敏
     */
    private void desensitizeArray(ArrayNode arrayNode, String currentPath) {
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
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
        }
    }

    /**
     * 查找匹配的字段脱敏类型
     */
    private DesensitizeType findMatchingType(String fullPath) {
        // 精确匹配
        DesensitizeType exactMatch = fieldConfigs.get(fullPath);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 数组通配符匹配
        for (Map.Entry<String, DesensitizeType> entry : fieldConfigs.entrySet()) {
            String pattern = entry.getKey().replace("[*]", "[" + extractIndex(fullPath) + "]");
            if (matchesPath(fullPath, pattern)) {
                return entry.getValue();
            }
        }

        // 模式匹配
        for (Map.Entry<String, DesensitizeType> entry : fieldConfigs.entrySet()) {
            if (matchesPath(fullPath, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 从路径中提取数组索引
     */
    private String extractIndex(String path) {
        int bracketStart = path.lastIndexOf('[');
        int bracketEnd = path.lastIndexOf(']');
        if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
            return path.substring(bracketStart + 1, bracketEnd);
        }
        return "*";
    }

    /**
     * 路径匹配
     */
    private boolean matchesPath(String fullPath, String pattern) {
        if (pattern.equals(fullPath)) {
            return true;
        }

        // 通配符匹配
        if (pattern.contains("[*]")) {
            String wildcardPattern = pattern.replace("[*]", "[*]");
            return fullPath.startsWith(wildcardPattern.replace("[*]", ""));
        }

        return false;
    }

    /**
     * 对单个值进行脱敏处理 - 完全自定义实现
     */
    private String desensitizeValue(String value, DesensitizeType type, String fieldPath) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 解析字段参数
        FieldDesensitizeParams params = parseFieldParams(fieldPath);

        // 根据脱敏类型执行相应的脱敏逻辑
        switch (type) {
            case USERNAME:
                return desensitizeUsername(value, params.maskChar);
            case ID_CARD:
                return desensitizeIdCard(value, params.maskChar);
            case PHONE:
                return desensitizePhone(value, params.maskChar);
            case EMAIL:
                return desensitizeEmail(value, params.maskChar);
            case BANK_CARD:
                return desensitizeBankCard(value, params.maskChar);
            case CHINESE_NAME:
                return desensitizeChineseName(value, params.maskChar);
            case PASSWORD:
                return desensitizePassword(value, params.maskChar);
            case ADDRESS:
                return desensitizeAddress(value, params.maskChar);
            case CUSTOM:
                return desensitizeCustom(value, params.startKeep, params.endKeep, params.maskChar);
            default:
                return value;
        }
    }

    /**
     * 用户名脱敏 - 隐藏首字符
     */
    private String desensitizeUsername(String value, char maskChar) {
        if (value.length() <= 1) {
            return repeatMask(maskChar, value.length());
        }
        return maskChar + value.substring(1);
    }

    /**
     * 身份证号脱敏 - 保留前6后4
     */
    private String desensitizeIdCard(String value, char maskChar) {
        if (value.length() <= 10) {
            return repeatMask(maskChar, value.length());
        }
        return value.substring(0, 6) + repeatMask(maskChar, value.length() - 10) + value.substring(value.length() - 4);
    }

    /**
     * 手机号脱敏 - 保留前3后4
     */
    private String desensitizePhone(String value, char maskChar) {
        if (value.length() <= 7) {
            return repeatMask(maskChar, value.length());
        }
        return value.substring(0, 3) + repeatMask(maskChar, value.length() - 7) + value.substring(value.length() - 4);
    }

    /**
     * 邮箱脱敏 - 隐藏@前部分的部分字符
     */
    private String desensitizeEmail(String value, char maskChar) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return repeatMask(maskChar, atIndex) + value.substring(atIndex);
        }
        return value.charAt(0) + repeatMask(maskChar, atIndex - 1) + value.substring(atIndex);
    }

    /**
     * 银行卡号脱敏 - 保留后4
     */
    private String desensitizeBankCard(String value, char maskChar) {
        if (value.length() <= 4) {
            return repeatMask(maskChar, value.length());
        }
        return repeatMask(maskChar, value.length() - 4) + value.substring(value.length() - 4);
    }

    /**
     * 中文姓名脱敏 - 隐藏首字符
     */
    private String desensitizeChineseName(String value, char maskChar) {
        if (value.length() <= 1) {
            return repeatMask(maskChar, value.length());
        }
        return maskChar + value.substring(1);
    }

    /**
     * 密码脱敏 - 全部隐藏
     */
    private String desensitizePassword(String value, char maskChar) {
        return repeatMask(maskChar, value.length());
    }

    /**
     * 地址脱敏 - 保留前6后4
     */
    private String desensitizeAddress(String value, char maskChar) {
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

        // 如果字符串长度小于等于要保留的字符总数，直接返回原字符串或全部脱敏
        if (length <= startKeep + endKeep) {
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
            result.append(value.substring(0, startKeep));
        }

        // 添加脱敏部分
        result.append(repeatMask(maskChar, maskLength));

        // 添加结尾保留部分
        if (endKeep > 0) {
            result.append(value.substring(length - endKeep));
        }

        return result.toString();
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

        String paramStr = fieldParams.get(fieldPath.replaceFirst("\\[\\d+\\]$", "[*]"));
        if (StringUtils.hasText(paramStr)) {
            String[] paramPairs = paramStr.split("@");
            for (String pair : paramPairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    switch (key) {
                        case "startKeep":
                            try {
                                params.startKeep = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                // 忽略无效数字
                            }
                            break;
                        case "endKeep":
                            try {
                                params.endKeep = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                // 忽略无效数字
                            }
                            break;
                        case "maskChar":
                            if (value.length() == 1) {
                                params.maskChar = value.charAt(0);
                            }
                            break;
                    }
                }
            }
        }

        return params;
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