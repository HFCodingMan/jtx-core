# JTX Data Desensitize Spring Boot Starter

一个基于JDK 1.8的Spring Boot Starter，提供简单易用的数据脱敏功能。通过自定义注解方式，在JSON序列化时自动对敏感字段进行脱敏处理。

## 功能特性

- **多种脱敏类型支持**：用户名、身份证号、手机号、邮箱、银行卡号、密码、地址、中文姓名等
- **自定义脱敏策略**：支持自定义脱敏规则和格式
- **基于注解的使用方式**：简单易用，只需在字段上添加注解即可
- **Spring Boot自动配置**：无需手动配置，开箱即用
- **高度可扩展**：支持自定义脱敏策略
- **兼容JDK 1.8**：确保与现有系统的兼容性

## 支持的脱敏类型

| 类型 | 说明 | 示例 | 脱敏后 |
|------|------|------|--------|
| USERNAME | 用户名脱敏 | zhangsan | *******an |
| CHINESE_NAME | 中文姓名脱敏 | 张三 | *三 |
| ID_CARD | 身份证号脱敏 | 11010119900307899X | 110101********99X |
| PHONE | 手机号脱敏 | 13812345678 | 138****5678 |
| EMAIL | 邮箱脱敏 | zhangsan@example.com | z****@example.com |
| BANK_CARD | 银行卡号脱敏 | 6222021234567890123 | ************0123 |
| PASSWORD | 密码脱敏 | password123 | ****** |
| ADDRESS | 地址脱敏 | 北京市朝阳区建国门外大街1号 | 北京市朝阳区******1号 |
| CUSTOM | 自定义脱敏 | customfield | cu******ue |

## 快速开始

### 1. 添加依赖

在你的Spring Boot项目的`pom.xml`中添加依赖：

```xml
<dependency>
    <groupId>com.xjt</groupId>
    <artifactId>jtx-sensitize</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 使用注解

在你的实体类字段上添加`@Desensitize`注解：

```java
import com.xjt.desensitize.annotation.Desensitize;
import com.xjt.desensitize.enumtype.DesensitizeType;
import lombok.Data;

@Data
public class User {

    private Long id;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;

    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;

    @Desensitize(type = DesensitizeType.CUSTOM, startKeep = 2, endKeep = 2)
    private String customField;

    // 普通字段不会被脱敏
    private String normalField;
}
```

### 3. 使用示例

```java
@RestController
public class UserController {

    @GetMapping("/user")
    public User getUser() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13812345678");
        user.setIdCard("11010119900307899X");
        user.setEmail("zhangsan@example.com");
        user.setCustomField("customValue");
        user.setNormalField("普通字段");
        return user;
    }
}
```

返回的JSON结果：

```json
{
  "id": 1,
  "phone": "138****5678",
  "idCard": "110101********99X",
  "email": "z****@example.com",
  "customField": "cu****ue",
  "normalField": "普通字段"
}
```

## 配置选项

在`application.yml`中可以配置脱敏功能：

```yaml
jtx:
  desensitize:
    enabled: true              # 是否启用脱敏功能
    default-mask: '*'          # 默认脱敏字符
    global-enabled: true       # 全局脱敏开关
```

## 注解详细说明

### @Desensitize注解参数

```java
@Desensitize(
    type = DesensitizeType.PHONE,    # 脱敏类型（必填）
    customFormat = "",               # 自定义脱敏格式
    startKeep = 0,                   # 开始保留字符数
    endKeep = 0,                     # 结尾保留字符数
    maskChar = '*',                  # 脱敏字符
    enabled = true                   # 是否启用脱敏
)
private String phone;
```

### 自定义脱敏示例

```java
// 保留前3后4位
@Desensitize(type = DesensitizeType.CUSTOM, startKeep = 3, endKeep = 4)
private String field1;  // 138****5678

// 使用自定义脱敏字符
@Desensitize(type = DesensitizeType.CUSTOM, startKeep = 1, endKeep = 1, maskChar = '#')
private String field2;  // 1#######8

// 禁用脱敏
@Desensitize(type = DesensitizeType.PHONE, enabled = false)
private String phone;   // 13812345678 (不脱敏)
```

## 高级功能

### 自定义脱敏策略

1. 实现`DesensitizeStrategy`接口：

```java
@Component
public class MyCustomDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String origin) {
        // 自定义脱敏逻辑
        if (origin == null) return null;
        return "自定义脱敏：" + origin.substring(0, 1) + "***";
    }
}
```

2. 注册为Spring Bean即可使用。

### 条件化脱敏

可以通过配置动态控制脱敏：

```java
@Desensitize(type = DesensitizeType.PHONE, enabled = ${desensitize.phone.enabled})
private String phone;
```

## 构建和安装

### 构建项目

```bash
mvn clean install
```

### 本地测试

```bash
mvn test
```

## 版本要求

### 当前支持的版本
- JDK 1.8+
- Spring Boot 1.5.12.RELEASE
- Jackson 2.8.11
- Lombok 1.18.8
- Maven 3.6+

### 版本兼容性说明
本项目已专门适配 Spring Boot 1.5.12.RELEASE 版本，确保在旧版本环境中的稳定运行。

#### Spring Boot 1.5.12 兼容性调整：
- ✅ Spring Boot 版本：1.5.12.RELEASE
- ✅ Jackson 版本：2.8.11（与Spring Boot 1.5.12兼容）
- ✅ Lombok 版本：1.18.8（最高兼容版本）
- ✅ Maven Compiler Plugin 版本：3.6.2
- ✅ 所有使用的Spring API都是1.5.x兼容的

#### 兼容性验证：
- 自动配置类使用标准的Spring Boot 1.5.x注解
- 条件化Bean使用`@ConditionalOnMissingBean`和`@ConditionalOnProperty`
- 配置属性类使用`@ConfigurationProperties`（1.5.x兼容）
- Jackson序列化器使用2.8.x兼容API
- 所有Spring框架API调用都兼容1.5.x版本

## 注意事项

1. 脱敏功能只在JSON序列化时生效，不会修改原始对象数据
2. 对于null值，脱敏处理会直接返回null
3. 对于长度不足的字段，会自动调整脱敏策略
4. 自定义脱敏格式功能可以根据需要扩展
5. 建议在生产环境中谨慎使用，确保符合数据安全要求

## 问题反馈

如果您在使用过程中遇到问题，欢迎提交Issue或Pull Request。

## 许可证

本项目采用MIT许可证，详情请参考LICENSE文件。