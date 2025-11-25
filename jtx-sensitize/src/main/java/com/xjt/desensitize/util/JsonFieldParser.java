package com.xjt.desensitize.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON字段解析器
 * 用于在JSON字符串中定位和修改特定字段，支持复杂嵌套结构和数组类型
 *
 * @author JTX
 * @since 1.0.0
 */
public class JsonFieldParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 字段路径分隔符
     */
    private static final String PATH_SEPARATOR = ".";

    /**
     * 数组索引开始标记
     */
    private static final String ARRAY_INDEX_START = "[";

    /**
     * 数组索引结束标记
     */
    private static final String ARRAY_INDEX_END = "]";

    /**
     * 解析JSON字符串并对指定字段进行脱敏处理
     *
     * @param jsonString JSON字符串
     * @param fieldPaths 需要脱敏的字段路径列表，支持嵌套路径如 "user.profile.phone"
     * @param maskChar 脱敏字符
     * @return 处理后的JSON字符串
     */
    public static String desensitizeFields(String jsonString, List<String> fieldPaths, char maskChar) {
        if (!StringUtils.hasText(jsonString) || fieldPaths == null || fieldPaths.isEmpty()) {
            return jsonString;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            if (rootNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) rootNode;
                desensitizeFieldsInObject(objectNode, fieldPaths, maskChar, "");
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (IOException e) {
            System.err.println("JSON解析失败，返回原始字符串: " + e.getMessage());
            return jsonString;
        }

        return jsonString;
    }

    /**
     * 从JSON字符串中提取指定字段的值用于敏感词检测
     *
     * @param jsonString JSON字符串
     * @param fieldPath 字段路径
     * @return 字段值的字符串列表
     */
    public static List<String> extractFieldValues(String jsonString, String fieldPath) {
        List<String> values = new ArrayList<>();
        if (!StringUtils.hasText(jsonString) || !StringUtils.hasText(fieldPath)) {
            return values;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            extractFieldValuesFromNode(rootNode, fieldPath, "", values);
        } catch (IOException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
        }

        return values;
    }

    /**
     * 在对象中递归处理字段脱敏
     */
    private static void desensitizeFieldsInObject(ObjectNode objectNode, List<String> fieldPaths, char maskChar, String currentPath) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            String fullPath = currentPath.isEmpty() ? fieldName : currentPath + PATH_SEPARATOR + fieldName;

            if (fieldValue.isObject()) {
                // 递归处理嵌套对象
                desensitizeFieldsInObject((ObjectNode) fieldValue, fieldPaths, maskChar, fullPath);
            } else if (fieldValue.isArray()) {
                // 处理数组
                desensitizeFieldsInArray((ArrayNode) fieldValue, fieldPaths, maskChar, fullPath);
            } else {
                // 检查是否是需要脱敏的字段
                if (isFieldToDesensitize(fullPath, fieldPaths)) {
                    String desensitizedValue = desensitizeValue(fieldValue.asText(), maskChar);
                    objectNode.put(fieldName, desensitizedValue);
                }
            }
        }
    }

    /**
     * 在数组中递归处理字段脱敏
     */
    private static void desensitizeFieldsInArray(ArrayNode arrayNode, List<String> fieldPaths, char maskChar, String currentPath) {
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
            String elementPath = currentPath + ARRAY_INDEX_START + i + ARRAY_INDEX_END;

            if (element.isObject()) {
                desensitizeFieldsInObject((ObjectNode) element, fieldPaths, maskChar, elementPath);
            } else if (element.isArray()) {
                desensitizeFieldsInArray((ArrayNode) element, fieldPaths, maskChar, elementPath);
            } else {
                // 数组中的基本类型元素
                if (isFieldToDesensitize(elementPath, fieldPaths)) {
                    String desensitizedValue = desensitizeValue(element.asText(), maskChar);
                    arrayNode.set(i, objectMapper.getNodeFactory().textNode(desensitizedValue));
                }
            }
        }
    }

    /**
     * 从节点中提取字段值
     */
    private static void extractFieldValuesFromNode(JsonNode node, String targetPath, String currentPath, List<String> values) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                String fullPath = currentPath.isEmpty() ? fieldName : currentPath + PATH_SEPARATOR + fieldName;

                if (fullPath.equals(targetPath) || fullPath.startsWith(targetPath + PATH_SEPARATOR) ||
                    fullPath.startsWith(targetPath + ARRAY_INDEX_START)) {
                    if (fieldValue.isValueNode()) {
                        values.add(fieldValue.asText());
                    } else {
                        extractFieldValuesFromNode(fieldValue, targetPath, fullPath, values);
                    }
                } else if (fieldValue.isObject() || fieldValue.isArray()) {
                    extractFieldValuesFromNode(fieldValue, targetPath, fullPath, values);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode element = arrayNode.get(i);
                String elementPath = currentPath + ARRAY_INDEX_START + i + ARRAY_INDEX_END;
                extractFieldValuesFromNode(element, targetPath, elementPath, values);
            }
        }
    }

    /**
     * 检查字段路径是否在需要脱敏的字段列表中
     */
    private static boolean isFieldToDesensitize(String fullPath, List<String> fieldPaths) {
        return fieldPaths.stream().anyMatch(pattern -> matchPath(fullPath, pattern));
    }

    /**
     * 路径匹配，支持通配符
     */
    private static boolean matchPath(String fullPath, String pattern) {
        if (pattern.equals(fullPath)) {
            return true;
        }

        // 支持通配符匹配
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return fullPath.matches(regex);
        }

        // 支持前缀匹配（用于嵌套对象）
        if (fullPath.startsWith(pattern + PATH_SEPARATOR)) {
            return true;
        }

        return false;
    }

    /**
     * 对单个值进行脱敏处理
     */
    private static String desensitizeValue(String value, char maskChar) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 简单的脱敏逻辑：保留首尾字符，中间用脱敏字符替换
        int length = value.length();
        if (length <= 2) {
            return repeatMask(maskChar, length);
        }

        int keepLength = Math.max(1, length / 4); // 保留前后1/4的字符
        if (keepLength * 2 >= length) {
            keepLength = 1;
        }

        String start = value.substring(0, keepLength);
        String end = value.substring(length - keepLength);
        String middle = repeatMask(maskChar, length - keepLength * 2);

        return start + middle + end;
    }

    /**
     * 重复脱敏字符 - JDK 1.8兼容实现
     *
     * @param maskChar 脱敏字符
     * @param count    重复次数
     * @return 重复后的字符串
     */
    private static String repeatMask(char maskChar, int count) {
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
     * 验证JSON字符串格式是否正确
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

    /**
     * 格式化JSON字符串
     */
    public static String formatJson(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return jsonString;
        }

        try {
            JsonNode node = objectMapper.readTree(jsonString);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException e) {
            return jsonString;
        }
    }
}