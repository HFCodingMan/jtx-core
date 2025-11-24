[根目录](../../../../../CLAUDE.md) > [src](../../../../) > [main](../../../) > [java](../../) > [com](../) > [xjt](../../) > **desensitize** > **enumtype**

# 枚举类型模块 (enumtype)

## 变更记录 (Changelog)
- **2025-11-21 09:05:28** - 完成枚举类型模块文档初始化

## 模块职责

枚举类型模块定义了数据脱敏功能支持的所有脱敏类型。该模块提供了类型安全的枚举定义，确保脱敏配置的准确性和可维护性，同时为策略路由和配置元数据提供类型支持。

## 核心组件

### DesensitizeType 枚举

**文件位置**：`DesensitizeType.java`

**枚举定义**：
```java
public enum DesensitizeType {
    USERNAME,        // 用户名脱敏
    ID_CARD,         // 身份证号脱敏
    PHONE,           // 手机号脱敏
    EMAIL,           // 邮箱脱敏
    BANK_CARD,       // 银行卡号脱敏
    ADDRESS,         // 地址脱敏
    PASSWORD,        // 密码脱敏
    CHINESE_NAME,    // 姓名脱敏
    CUSTOM           // 自定义脱敏
}
```

## 脱敏类型详解

### 1. USERNAME (用户名脱敏)
- **描述**：隐藏首字符，显示末尾部分
- **示例**：zhangsan → *******an
- **适用场景**：用户登录名、用户昵称

### 2. CHINESE_NAME (中文姓名脱敏)
- **描述**：隐藏首字符，显示末尾字符
- **示例**：张三 → *三
- **适用场景**：真实姓名、联系人姓名

### 3. ID_CARD (身份证号脱敏)
- **描述**：保留前6位地区码和后4位校验码
- **示例**：11010119900307899X → 110101********99X
- **适用场景**：身份证号码、证件号码

### 4. PHONE (手机号脱敏)
- **描述**：保留前3位运营商号段和后4位
- **示例**：13812345678 → 138****5678
- **适用场景**：手机号码、联系电话

### 5. EMAIL (邮箱脱敏)
- **描述**：隐藏@前部分的部分字符，保留域名
- **示例**：zhangsan@example.com → z****@example.com
- **适用场景**：电子邮件地址

### 6. BANK_CARD (银行卡号脱敏)
- **描述**：仅显示后4位卡号
- **示例**：6222021234567890123 → ************0123
- **适用场景**：银行卡号、信用卡号

### 7. ADDRESS (地址脱敏)
- **描述**：保留前6位和后4位，中间脱敏
- **示例**：北京市朝阳区建国门外大街1号 → 北京市朝阳区******1号
- **适用场景**：详细地址、联系地址

### 8. PASSWORD (密码脱敏)
- **描述**：完全隐藏所有字符
- **示例**：password123 → ******
- **适用场景**：密码、PIN码、安全码

### 9. CUSTOM (自定义脱敏)
- **描述**：根据用户自定义参数进行脱敏
- **参数**：startKeep、endKeep、maskChar
- **示例**：startKeep=2, endKeep=2 → cu******ue
- **适用场景**：自定义格式的敏感字段

## 使用示例

### 注解中使用
```java
public class UserInfo {
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;

    @Desensitize(type = DesensitizeType.CUSTOM, startKeep = 3, endKeep = 4)
    private String customField;
}
```

### 配置中使用
```java
// 策略服务中的类型路由
switch (type) {
    case PHONE:
        return phoneStrategy.desensitize(origin);
    case EMAIL:
        return emailStrategy.desensitize(origin);
    case CUSTOM:
        return customStrategy.desensitize(origin, startKeep, endKeep, maskChar);
    default:
        return origin;
}
```

### 配置元数据关联
枚举值与spring-configuration-metadata.json中的配置选项对应，为IDE提供配置提示。

## 枚举设计原则

### 1. 类型安全
- 使用强类型枚举，避免字符串常量错误
- 编译时类型检查，减少运行时错误

### 2. 可扩展性
- 新增脱敏类型只需添加枚举值
- 配套的策略实现可以独立开发

### 3. 语义明确
- 枚举命名清晰表达脱敏意图
- 每个类型都有明确的适用场景

### 4. 文档完整
- 每个枚举值都有详细的JavaDoc注释
- 包含具体的脱敏规则和示例

## 相关文件清单

- **核心文件**：
  - `DesensitizeType.java` - 脱敏类型枚举定义

## 依赖关系

**上游依赖**：
- 无（纯Java枚举，独立模块）

**下游依赖**：
- `@Desensitize` 注解（作为注解参数类型）
- `DesensitizeStrategyService`（策略路由判断）
- `DesensitizeAutoConfiguration`（配置元数据）

## 扩展新类型

### 添加新脱敏类型的步骤

1. **添加枚举值**
```java
public enum DesensitizeType {
    // 现有类型...
    NEW_TYPE,  // 新增类型
}
```

2. **添加JavaDoc注释**
```java
/**
 * 新类型脱敏 - 描述脱敏规则
 * 示例：原值 -> 脱敏后值
 */
NEW_TYPE,
```

3. **实现对应策略**
```java
@Component
public class NewTypeDesensitizeStrategy extends AbstractDesensitizeStrategy {
    @Override
    public String desensitize(String origin) {
        // 实现具体的脱敏逻辑
        return mask(origin, startKeep, endKeep, DEFAULT_MASK);
    }
}
```

4. **注册策略映射**
```java
// 在DesensitizeStrategyServiceImpl中添加
strategyMap.put(DesensitizeType.NEW_TYPE, newTypeStrategy);
```

5. **更新自动配置**
```java
// 在DesensitizeAutoConfiguration中添加Bean
@Bean
@ConditionalOnMissingBean
public NewTypeDesensitizeStrategy newTypeDesensitizeStrategy() {
    return new NewTypeDesensitizeStrategy();
}
```

## 常见问题 (FAQ)

1. **Q**: 枚举值可以重复吗？
   **A**: 不可以，Java枚举天然保证唯一性。

2. **Q**: 可以动态添加新的脱敏类型吗？
   **A**: 不行，枚举类型在编译时确定，需要重新编译。

3. **Q**: 如何选择合适的脱敏类型？
   **A**: 根据数据的敏感程度和业务需求选择，参考类型描述和示例。

4. **Q**: 自定义类型和其他类型有什么区别？
   **A**: 自定义类型支持运行时参数配置，其他类型使用固定规则。

## 注意事项

- 枚举值命名采用大写加下划线的标准Java约定
- 每个新增类型都需要配套实现对应的策略
- 保持枚举值的语义明确性，避免含义模糊
- 新增类型时需要考虑向后兼容性
- 类型变更可能会影响现有配置和注解使用