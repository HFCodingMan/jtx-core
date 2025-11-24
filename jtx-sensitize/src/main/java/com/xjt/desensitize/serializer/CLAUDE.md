[根目录](../../../../../CLAUDE.md) > [src](../../../../) > [main](../../../) > [java](../../) > [com](../) > [xjt](../../) > **desensitize** > **serializer**

# 序列化器模块 (serializer)

## 变更记录 (Changelog)
- **2025-11-21 09:05:28** - 完成序列化器模块文档初始化

## 模块职责

序列化器模块是整个脱敏功能的核心执行层，负责在Jackson序列化过程中拦截带有 `@Desensitize` 注解的字段，并调用相应的脱敏策略进行处理。该模块确保脱敏操作与JSON序列化无缝集成。

## 核心组件

### DesensitizeSerializer

**文件位置**：`DesensitizeSerializer.java`

**继承关系**：
```java
public class DesensitizeSerializer extends StdSerializer<Object> implements ContextualSerializer
```

**主要功能**：
- 实现 `ContextualSerializer` 接口，支持动态上下文创建
- 解析字段上的 `@Desensitize` 注解参数
- 调用脱敏策略服务进行数据处理
- 提供完善的异常处理和降级机制

### 序列化流程

```mermaid
graph TD
    A[Jackson序列化开始] --> B[检查字段注解]
    B --> C{有@Desensitize注解?}
    C -->|否| D[使用默认序列化器]
    C -->|是| E[创建上下文序列化器]
    E --> F[获取策略服务]
    F --> G{策略服务存在?}
    G -->|否| H[降级到默认序列化]
    G -->|是| I[解析注解参数]
    I --> J[参数验证]
    J --> K{参数合法?}
    K -->|否| L[使用默认参数]
    K -->|是| M[创建专用序列化器]
    M --> N[执行脱敏处理]
    N --> O{脱敏成功?}
    O -->|是| P[输出脱敏结果]
    O -->|否| Q[降级到原始数据]
```

### 关键方法

#### createContextual() 方法
**功能**：创建序列化器上下文，解析注解参数
**处理逻辑**：
1. 获取字段上的 `@Desensitize` 注解
2. 验证注解参数有效性
3. 从Spring容器获取脱敏策略服务
4. 返回配置好的序列化器实例

#### serialize() 方法
**功能**：执行实际的序列化和脱敏处理
**处理逻辑**：
1. 检查是否启用脱敏
2. 调用策略服务进行脱敏
3. 处理异常情况，确保数据不丢失

## 异常处理策略

### 多层异常处理机制

1. **Spring上下文异常**
```java
try {
    strategyService = SpringContextHolder.getBean(DesensitizeStrategyService.class);
} catch (IllegalStateException e) {
    // Spring上下文未初始化
    return prov.findValueSerializer(property.getType().getRawClass(), property);
}
```

2. **脱敏处理异常**
```java
try {
    String desensitizedValue = strategyService.desensitize(/* 参数 */);
    gen.writeString(desensitizedValue);
} catch (Exception e) {
    // 脱敏失败，使用原值
    gen.writeObject(value);
}
```

3. **参数验证异常**
```java
if (annotation.startKeep() < 0 || annotation.endKeep() < 0) {
    // 参数错误，使用默认值
    return new DesensitizeSerializer(strategyService, annotation.type(),
            annotation.customFormat(), 0, 0, annotation.maskChar(), annotation.enabled());
}
```

## 性能优化

### 实例缓存
- 实现 `equals()` 和 `hashCode()` 方法，支持Jackson的序列化器缓存机制
- 避免为相同配置重复创建序列化器实例

### 延迟加载
- 策略服务通过Spring上下文延迟获取，避免循环依赖
- 只在需要时才创建脱敏策略服务实例

## 相关文件清单

- **核心文件**：
  - `DesensitizeSerializer.java` - Jackson序列化器实现

## 依赖关系

**上游依赖**：
- `com.fasterxml.jackson` - Jackson核心库
- `com.xjt.desensitize.annotation.Desensitize` - 脱敏注解
- `com.xjt.desensitize.enumservice.DesensitizeStrategyService` - 策略服务
- `com.xjt.desensitize.util.SpringContextHolder` - Spring上下文工具

**下游依赖**：
- 调用各种脱敏策略实现

## 使用场景

### 1. REST API响应
```java
@RestController
public class UserController {
    @GetMapping("/user")
    public User getUser() {
        // 脱敏自动在JSON序列化时执行
        return user;
    }
}
```

### 2. 日志序列化
```java
// 审计日志输出时的敏感数据脱敏
log.info("User data: {}", objectMapper.writeValueAsString(user));
```

### 3. 缓存序列化
```java
// Redis缓存时的数据脱敏
redisTemplate.opsForValue().set("user:" + id, objectMapper.writeValueAsString(user));
```

## 常见问题 (FAQ)

1. **Q**: 序列化器是线程安全的吗？
   **A**: 是的，序列化器设计为无状态，多线程安全。

2. **Q**: 为什么需要实现 ContextualSerializer？
   **A**: 为了动态获取字段上的注解参数，为不同字段创建不同的序列化配置。

3. **Q**: 脱敏失败会丢失数据吗？
   **A**: 不会，所有异常情况都会降级使用原始数据，确保数据不丢失。

4. **Q**: 可以用于非String类型字段吗？
   **A**: 可以，任何类型都会先转换为String再进行脱敏处理。

## 注意事项

- 序列化器只在JSON序列化时生效，不影响原始对象
- 异常处理完善，确保系统稳定性
- 支持Jackson 2.8.x版本，兼容Spring Boot 1.5.12
- 性能优化良好，适合高频调用场景