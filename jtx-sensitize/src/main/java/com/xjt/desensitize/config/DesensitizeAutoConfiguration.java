package com.xjt.desensitize.config;

import com.xjt.desensitize.enumservice.DesensitizeStrategyService;
import com.xjt.desensitize.enumservice.impl.DesensitizeStrategyServiceImpl;
import com.xjt.desensitize.strategy.impl.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据脱敏自动配置类
 *
 * @author JTX
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(DesensitizeProperties.class)
@ConditionalOnProperty(prefix = "jtx.desensitize", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DesensitizeAutoConfiguration {

    /**
     * 配置用户名脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public UsernameDesensitizeStrategy usernameDesensitizeStrategy() {
        return new UsernameDesensitizeStrategy();
    }

    /**
     * 配置身份证号脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public IdCardDesensitizeStrategy idCardDesensitizeStrategy() {
        return new IdCardDesensitizeStrategy();
    }

    /**
     * 配置手机号脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public PhoneDesensitizeStrategy phoneDesensitizeStrategy() {
        return new PhoneDesensitizeStrategy();
    }

    /**
     * 配置邮箱脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailDesensitizeStrategy emailDesensitizeStrategy() {
        return new EmailDesensitizeStrategy();
    }

    /**
     * 配置银行卡号脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public BankCardDesensitizeStrategy bankCardDesensitizeStrategy() {
        return new BankCardDesensitizeStrategy();
    }

    /**
     * 配置中文姓名脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public ChineseNameDesensitizeStrategy chineseNameDesensitizeStrategy() {
        return new ChineseNameDesensitizeStrategy();
    }

    /**
     * 配置密码脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordDesensitizeStrategy passwordDesensitizeStrategy() {
        return new PasswordDesensitizeStrategy();
    }

    /**
     * 配置地址脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public AddressDesensitizeStrategy addressDesensitizeStrategy() {
        return new AddressDesensitizeStrategy();
    }

    /**
     * 配置自定义脱敏策略
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomDesensitizeStrategy customDesensitizeStrategy() {
        return new CustomDesensitizeStrategy();
    }

    /**
     * 配置脱敏策略服务
     */
    @Bean
    @ConditionalOnMissingBean
    public DesensitizeStrategyService desensitizeStrategyService(
            UsernameDesensitizeStrategy usernameStrategy,
            IdCardDesensitizeStrategy idCardStrategy,
            PhoneDesensitizeStrategy phoneStrategy,
            EmailDesensitizeStrategy emailStrategy,
            BankCardDesensitizeStrategy bankCardStrategy,
            ChineseNameDesensitizeStrategy chineseNameStrategy,
            PasswordDesensitizeStrategy passwordStrategy,
            AddressDesensitizeStrategy addressStrategy,
            CustomDesensitizeStrategy customStrategy) {

        return new DesensitizeStrategyServiceImpl(
                usernameStrategy,
                idCardStrategy,
                phoneStrategy,
                emailStrategy,
                bankCardStrategy,
                chineseNameStrategy,
                passwordStrategy,
                addressStrategy,
                customStrategy
        );
    }
}