[根目录](../../../../../CLAUDE.md) > [src](../../../../) > [main](../../../) > [java](../../) > [com](../) > [xjt](../../) > **desensitize** > **util**

# 工具模块 (util)

## 变更记录 (Changelog)
- **2025-11-21 09:05:28** - 完成工具模块文档初始化

## 模块职责

工具模块提供Spring应用上下文访问能力，用于在非Spring管理的类中获取Spring容器中的Bean。该模块解决了脱敏序列化器需要在Jackson序列化过程中访问Spring服务的问题，是连接Spring生态和Jackson序列化框架的桥梁。

## 核心组件

### SpringContextHolder

**文件位置**：`SpringContextHolder.java`

**设计模式**：
- 静态工具类模式
- Spring ApplicationContextAware 接口实现

**核心功能**：
- 存储Spring应用上下文引用
- 提供静态方法获取Bean
- 支持按类型和按名称获取Bean

## 技术实现

### 1. ApplicationContextAware 接口

```java
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
}
```

**工作原理**：
- Spring容器启动时会自动调用 `setApplicationContext()` 方法
- 将ApplicationContext引用保存到静态变量中
- 使得其他类可以通过静态方法访问Spring容器

### 2. 静态工具方法

#### 按类型获取Bean
```java
public static <T> T getBean(Class<T> clazz) {
    if (applicationContext == null) {
        throw new IllegalStateException("Spring应用上下文未初始化");
    }
    return applicationContext.getBean(clazz);
}
```

#### 按名称和类型获取Bean
```java
public static <T> T getBean(String name, Class<T> clazz) {
    if (applicationContext == null) {
        throw new IllegalStateException("Spring应用上下文未初始化");
    }
    return applicationContext.getBean(name, clazz);
}
```

#### 获取ApplicationContext
```java
public static ApplicationContext getApplicationContext() {
    return applicationContext;
}
```

## 使用场景

### 1. Jackson序列化器中的使用

**在DesensitizeSerializer中的应用**：
```java
@Override
public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
    try {
        // 获取脱敏策略服务
        DesensitizeStrategyService strategyService =
            SpringContextHolder.getBean(DesensitizeStrategyService.class);

        return new DesensitizeSerializer(strategyService, /* 其他参数 */);
    } catch (IllegalStateException e) {
        // Spring上下文未初始化时的处理
        System.err.println("Spring应用上下文未初始化，跳过脱敏处理");
        return prov.findValueSerializer(property.getType().getRawClass(), property);
    }
}
```

### 2. 其他非Spring管理类的使用

```java
public class ExternalService {
    public void processWithSpringBean() {
        // 在普通类中获取Spring管理的Bean
        DesensitizeStrategyService service =
            SpringContextHolder.getBean(DesensitizeStrategyService.class);

        // 使用服务进行脱敏处理
        String result = service.desensitize("13812345678",
            DesensitizeType.PHONE, "", 0, 0, '*');
    }
}
```

## 异常处理

### 上下文未初始化异常

**触发条件**：
- Spring应用尚未完全启动
- 在非Spring环境中调用工具方法

**异常处理**：
```java
if (applicationContext == null) {
    throw new IllegalStateException("Spring应用上下文未初始化");
}
```

**使用建议**：
```java
try {
    DesensitizeStrategyService service =
        SpringContextHolder.getBean(DesensitizeStrategyService.class);
    // 正常使用服务
} catch (IllegalStateException e) {
    // 降级处理：不执行脱敏或使用默认逻辑
    logger.warn("Spring上下文未初始化，跳过脱敏处理", e);
}
```

## 生命周期管理

### 初始化时机
- Spring容器启动过程中，`setApplicationContext()` 被调用
- 在所有Bean实例化之前，ApplicationContext就已可用

### 销毁处理
- 静态变量在应用关闭时自动清理
- 不需要显式的清理代码

## 线程安全性

### 并发访问分析
- `applicationContext` 是只读的，线程安全
- `getBean()` 方法依赖于Spring容器的线程安全性
- Spring容器本身是线程安全的，可以并发获取Bean

### 最佳实践
```java
// 推荐的使用方式
DesensitizeStrategyService service = SpringContextHolder.getBean(DesensitizeStrategyService.class);

// 避免重复获取，可以缓存引用（如果适用）
private static DesensitizeStrategyService cachedService;

public static String process(String data) {
    if (cachedService == null) {
        cachedService = SpringContextHolder.getBean(DesensitizeStrategyService.class);
    }
    return cachedService.desensitize(data, DesensitizeType.PHONE, "", 0, 0, '*');
}
```

## 相关文件清单

- **核心文件**：
  - `SpringContextHolder.java` - Spring上下文工具类

## 依赖关系

**上游依赖**：
- Spring Framework (ApplicationContext, ApplicationContextAware, BeansException)

**下游依赖**：
- 被需要访问Spring容器的非Spring管理类使用
- 主要被 `DesensitizeSerializer` 使用

## 设计优势

### 1. 解耦合
- 脱敏序列化器不直接依赖Spring API
- 通过工具类间接访问，降低耦合度

### 2. 便利性
- 提供简单的静态方法接口
- 避免复杂的依赖注入配置

### 3. 兼容性
- 支持在Jackson序列化框架中使用
- 与Spring Boot自动配置完美集成

## 常见问题 (FAQ)

1. **Q**: 为什么需要这个工具类？
   **A**: Jackson序列化器不是Spring管理的Bean，需要通过静态方式访问Spring容器。

2. **Q**: ApplicationContext什么时候可用？
   **A**: Spring容器完全启动后可用，通常在应用启动完成后。

3. **Q**: 会导致内存泄漏吗？
   **A**: 不会，静态引用在应用关闭时自动清理，Spring容器正常管理生命周期。

4. **Q**: 可以在测试中使用吗？
   **A**: 可以，但需要确保测试环境中有Spring上下文。

## 注意事项

- 只应在确实需要时使用，避免过度依赖静态访问
- 异常处理要完善，确保在非Spring环境中也能正常降级
- 适用于Spring Boot应用，纯Java应用中不需要
- 确保调用时序正确，避免在Spring初始化完成前调用
- 考虑在分布式环境中的应用上下文隔离问题