[根目录](../../../../../CLAUDE.md) > [src](../../../../) > [main](../../../) > [java](../../) > [com](../) > [xjt](../../) > **desensitize** > **annotation**

# 注解模块 (annotation)

## 变更记录 (Changelog)
- **2025-11-21 09:05:28** - 完成注解模块文档初始化

## 模块职责

注解模块定义了数据脱敏功能的核心注解 `@Desensitize`，作为用户使用脱敏功能的入口点。该注解结合Jackson的序列化机制，在JSON序列化时自动触发脱敏处理。

## 核心组件

### @Desensitize 注解

**文件位置**：`Desensitize.java`

**核心功能**：
- 标记需要进行脱敏处理的字段
- 配置脱敏类型和参数
- 集成Jackson序列化流程

**注解参数**：
```java
@Desensitize(
    type = DesensitizeType.PHONE,    // 脱敏类型（必填）
    customFormat = "",               // 自定义脱敏格式
    startKeep = 0,                   // 开始保留字符数
    endKeep = 0,                     // 结尾保留字符数
    maskChar = '*',                  // 脱敏字符
    enabled = true                   // 是否启用脱敏
)
```

**技术特性**：
- `@Target(ElementType.FIELD)`：只能用于字段
- `@Retention(RetentionPolicy.RUNTIME)`：运行时注解
- `@JacksonAnnotationsInside`：Jackson元注解
- `@JsonSerialize(using = DesensitizeSerializer.class)`：指定序列化器

## 使用示例

### 基础用法
```java
public class User {
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;  // 138****5678
}
```

### 自定义脱敏
```java
public class User {
    @Desensitize(type = DesensitizeType.CUSTOM, startKeep = 2, endKeep = 2)
    private String customField;  // cu****ue

    @Desensitize(type = DesensitizeType.EMAIL, enabled = false)
    private String email;  // 不脱敏
}
```

## 相关文件清单

- **核心文件**：
  - `Desensitize.java` - 主要注解定义

## 依赖关系

**上游依赖**：
- `com.xjt.desensitize.enumtype.DesensitizeType` - 脱敏类型枚举
- `com.xjt.desensitize.serializer.DesensitizeSerializer` - 序列化器

**下游依赖**：
- 被业务实体类使用

## 常见问题 (FAQ)

1. **Q**: 注解可以用于方法或类上吗？
   **A**: 不可以，`@Desensitize` 只能用于字段上。

2. **Q**: 为什么必须指定 type 参数？
   **A**: type 参数决定了使用哪种脱敏策略，是必需参数。

3. **Q**: 可以动态控制脱敏开关吗？
   **A**: 可以通过 `enabled` 参数或者全局配置 `jtx.desensitize.global-enabled` 控制。

## 注意事项

- 注解只在JSON序列化时生效，不会修改原始对象数据
- 对于 null 值，序列化器会直接返回 null
- 自定义脱敏参数需要合理设置，避免参数错误