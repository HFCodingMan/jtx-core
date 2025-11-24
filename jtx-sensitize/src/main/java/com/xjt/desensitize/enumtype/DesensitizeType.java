package com.xjt.desensitize.enumtype;

/**
 * 脱敏类型枚举
 *
 * @author JTX
 * @since 1.0.0
 */
public enum DesensitizeType {

    /**
     * 用户名脱敏 - 隐藏首字符，如：张三 -> *三
     */
    USERNAME,

    /**
     * 身份证号脱敏 - 保留前6后4，如：11010119900307899X -> 110101********99X
     */
    ID_CARD,

    /**
     * 手机号脱敏 - 保留前3后4，如：13812345678 -> 138****5678
     */
    PHONE,

    /**
     * 邮箱脱敏 - 隐藏@前部分前3位，如：zhangsan@example.com -> ***@example.com
     */
    EMAIL,

    /**
     * 银行卡号脱敏 - 保留后4，如：6222021234567890123 -> ************0123
     */
    BANK_CARD,

    /**
     * 地址脱敏 - 保留前6后4，如：北京市朝阳区建国门外大街1号 -> 北京市朝阳区建国门******1号
     */
    ADDRESS,

    /**
     * 密码脱敏 - 全部隐藏，如：123456 -> ******
     */
    PASSWORD,

    /**
     * 姓名脱敏 - 隐藏首字符，如：张三 -> *三
     */
    CHINESE_NAME,

    /**
     * 自定义脱敏
     */
    CUSTOM
}