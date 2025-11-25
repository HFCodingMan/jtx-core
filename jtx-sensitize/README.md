# JTX Data Desensitize Spring Boot Starter

一个基于JDK 1.8的Spring Boot Starter，提供简单易用的数据脱敏功能。通过自定义注解方式，在JSON序列化时自动对敏感字段进行脱敏处理。

## 功能特性

- **多种脱敏类型支持**：用户名、身份证号、手机号、邮箱、银行卡号、密码、地址、中文姓名等
- **JSON字段级脱敏**：支持对JSON字符串中的特定字段进行不同类型的脱敏处理
- **复杂结构支持**：支持嵌套对象、数组元素的字段级脱敏
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
| JSON_FIELD | JSON字段脱敏 | 复杂JSON结构 | 按字段配置脱敏 |
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

   @Desensitize(type = DesensitizeType.PASSWORD)
   private String password;

   @Desensitize(type = DesensitizeType.ID_CARD)
   private String idCard;

   @Desensitize(type = DesensitizeType.EMAIL)
   private String email;

   @Desensitize(type = DesensitizeType.CUSTOM, startKeep = 2, endKeep = 2)
   private String customField;

   @Desensitize(type = DesensitizeType.JSON_FIELD,  fieldConfigs = "secrets[*]:PASSWORD:maskChar:#,data.apiKeys[*]:CUSTOM:startKeep:2@endKeep:3")
   private String json;

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
      user.setPassword("9999999999");
      user.setIdCard("11010119900307899X");
      user.setEmail("zhangsan@example.com");
      user.setCustomField("customValue");
      user.setNormalField("普通字段");
      String json = "{\"data\":{\"apiKeys\":[\"张啦啦啦啦啦啦啦啦三\"]},\"secrets\":[\"safljsalkfjlkasdklf232j4\",\"3245436578658756778\"]}";
      user.setJson(json);
      return user;
   }
}
```

返回的JSON结果：

```json
{
   "id": 1,
   "phone": "138****5678",
   "password": "******",
   "idCard": "110101********899X",
   "email": "z*******@example.com",
   "customField": "cu*******ue",
   "json": "{\"data\":{\"apiKeys\":[\"张啦*****啦啦三\"]},\"secrets\":[\"########################\",\"###################\"]}",
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
    type = DesensitizeType.PHONE,          # 脱敏类型（必填）
    customFormat = "",                      # 自定义脱敏格式
    startKeep = 0,                        # 开始保留字符数
    endKeep = 0,                          # 结尾保留字符数
    maskChar = '*',                       # 脱敏字符
    fieldConfigs = "",                    # JSON字段级脱敏配置（仅JSON_FIELD类型）
    enabled = true                        # 是否启用脱敏
)
private String sensitiveField;
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

// JSON字段级脱敏
@Desensitize(type = DesensitizeType.JSON_FIELD,
             fieldConfigs = "user.phone:PHONE,user.email:EMAIL,user.idCard:ID_CARD")
private String userProfileData;  // JSON字符串中的特定字段会被脱敏

// 带参数的JSON字段脱敏
@Desensitize(type = DesensitizeType.JSON_FIELD,
             fieldConfigs = "secrets[*]:PASSWORD:maskChar:#,apiKeys[*]:CUSTOM:startKeep:2@endKeep:4")
private String sensitiveJson;  // 数组元素使用不同的脱敏参数
```

## JSON字段脱敏

### 功能介绍

JSON字段脱敏功能允许您对JSON字符串中的**特定字段**应用不同的脱敏类型。这对于处理复杂的数据结构、API响应或配置文件中的敏感信息非常有用。

### 基本语法

```java
@Desensitize(type = DesensitizeType.JSON_FIELD,
             fieldConfigs = "字段路径:脱敏类型[:参数],字段路径2:脱敏类型2[:参数2]")
private String jsonData;
```

### 使用示例

#### 1. 基础字段脱敏

```java
public class UserProfile {
    @Desensitize(type = DesensitizeType.JSON_FIELD,
                fieldConfigs = "phone:PHONE,email:EMAIL")
    private String contactInfo;

    // JSON示例：{"phone":"13812345678","email":"user@example.com"}
    // 脱敏后：{"phone":"138****5678","email":"u****@example.com"}
}
```

#### 2. 嵌套对象脱敏

```java
public class UserData {
    @Desensitize(type = DesensitizeType.JSON_FIELD,
                fieldConfigs = "personalInfo.phone:PHONE,personalInfo.idCard:ID_CARD," +
                             "bankInfo.cardNumber:BANK_CARD,bankInfo.cvv:PASSWORD")
    private String jsonData;

    // 原始JSON：
    // {
    //   "personalInfo": {
    //     "name": "张三",
    //     "phone": "13812345678",
    //     "idCard": "11010119900307899X"
    //   },
    //   "bankInfo": {
    //     "bankName": "中国银行",
    //     "cardNumber": "6222021234567890123",
    //     "cvv": "123"
    //   }
    // }

    // 脱敏后：
    // {
    //   "personalInfo": {
    //     "name": "张三",
    //     "phone": "138****5678",
    //     "idCard": "110101********99X"
    //   },
    //   "bankInfo": {
    //     "bankName": "中国银行",
    //     "cardNumber": "************0123",
    //     "cvv": "***"
    //   }
    // }
}
```

#### 3. 数组元素脱敏

```java
public class ContactList {
    @Desensitize(type = DesensitizeType.JSON_FIELD,
                fieldConfigs = "contacts[*].phone:PHONE,contacts[*].email:EMAIL," +
                             "addresses[*].detail:ADDRESS,addresses[*].zipCode:PHONE")
    private String jsonData;

    // 原始JSON：
    // {
    //   "contacts": [
    //     {"name": "张三", "phone": "13812345678", "email": "zhang@example.com"},
    //     {"name": "李四", "phone": "13987654321", "email": "li@example.com"}
    //   ],
    //   "addresses": [
    //     {"type": "家庭", "detail": "北京市朝阳区建国门外大街1号", "zipCode": "100020"},
    //     {"type": "公司", "detail": "上海市浦东新区陆家嘴金融中心88号", "zipCode": "200120"}
    //   ]
    // }

    // 脱敏后：数组中的所有相关字段都会被脱敏处理
}
```

#### 4. 自定义参数脱敏

```java
public class SecureData {
    @Desensitize(type = DesensitizeType.JSON_FIELD,
                fieldConfigs = "secret:PASSWORD:maskChar:#," +
                             "token:CUSTOM:startKeep:3,endKeep:3,maskChar:*," +
                             "apiKey:CUSTOM:startKeep:4,endKeep:2")
    private String sensitiveData;

    // 可以为每个字段指定不同的脱敏参数
}
```

### 字段路径规则

#### 路径格式
- **简单字段**：`fieldName`
- **嵌套字段**：`parent.child.field`
- **数组元素**：`arrayName[*].field`
- **指定索引**：`arrayName[0].field`

#### 配置格式
```
字段路径:脱敏类型[:参数1:值1,参数2:值2]
```

#### 支持的参数
- `startKeep`：开始保留字符数
- `endKeep`：结尾保留字符数
- `maskChar`：脱敏字符
- `customFormat`：自定义格式（适用于CUSTOM类型）

### 实际应用场景

#### 1. API响应数据处理

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public UserProfile getUserProfile(@PathVariable Long id) {
        // 从数据库获取包含敏感信息的JSON数据
        String rawData = userRepository.getRawProfileData(id);

        UserProfile profile = new UserProfile();
        profile.setJsonData(rawData); // 自动应用字段级脱敏

        return profile;
    }
}
```

#### 2. 日志记录

```java
@Service
public class AuditService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void logUserActivity(UserActivity activity) {
        try {
            // 敏感字段自动脱敏后再记录日志
            String logData = objectMapper.writeValueAsString(activity);
            logger.info("用户活动: {}", logData);
        } catch (Exception e) {
            logger.error("日志记录失败", e);
        }
    }
}
```

#### 3. 配置文件敏感信息保护

```java
@Configuration
public class AppConfiguration {

    @Desensitize(type = DesensitizeType.JSON_FIELD,
                fieldConfigs = "database.password:PASSWORD," +
                             "redis.password:PASSWORD:maskChar:#," +
                             "api.keys[*]:CUSTOM:startKeep:4,endKeep:4")
    private String sensitiveConfig;

    // 配置文件中的敏感信息会被自动脱敏
}
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
4. JSON字段脱敏功能仅适用于 `type = DesensitizeType.JSON_FIELD` 时
5. JSON字段脱敏配置中的字段路径需要准确匹配JSON结构
6. 字段路径支持通配符 `[*]` 来匹配数组中的所有元素
7. 自定义脱敏格式功能可以根据需要扩展
8. 建议在生产环境中谨慎使用，确保符合数据安全要求
9. 对于复杂的JSON结构，请确保配置的字段路径正确，避免脱敏失败

## 最佳实践

### JSON字段脱敏配置建议

1. **字段路径命名**：使用清晰的字段路径，便于维护
   ```java
   // 推荐
   @Desensitize(type = DesensitizeType.JSON_FIELD,
               fieldConfigs = "userProfile.phone:PHONE,billingInfo.cardNumber:BANK_CARD")

   // 不推荐
   @Desensitize(type = DesensitizeType.JSON_FIELD,
               fieldConfigs = "p:PHONE,c:BANK_CARD")
   ```

2. **参数使用**：为敏感数据设置合适的脱敏参数
   ```java
   // 密码等敏感信息建议完全隐藏
   @Desensitize(type = DesensitizeType.JSON_FIELD,
               fieldConfigs = "password:PASSWORD,token:PASSWORD:maskChar:#")

   // 部分信息建议保留关键信息
   @Desensitize(type = DesensitizeType.JSON_FIELD,
               fieldConfigs = "phone:PHONE:startKeep:3,endKeep:4")
   ```

3. **性能考虑**：JSON字段脱敏会增加序列化时间，建议：
   - 只对必要的敏感字段配置脱敏
   - 避免对大型JSON文件配置过多的脱敏规则
   - 在日志场景中使用时注意性能影响

### 错误处理

如果JSON字段脱敏配置有问题：

1. **字段路径不匹配**：系统会忽略无法匹配的字段配置，不会报错
2. **脱敏类型不存在**：系统会在控制台输出警告，但JSON仍会正常序列化
3. **JSON格式错误**：如果JSON字符串格式不正确，会返回原始字符串
4. **参数错误**：无效的脱敏参数会被忽略，使用默认参数

## 问题反馈

如果您在使用过程中遇到问题，欢迎提交Issue或Pull Request。

## 许可证

本项目采用MIT许可证，详情请参考LICENSE文件。