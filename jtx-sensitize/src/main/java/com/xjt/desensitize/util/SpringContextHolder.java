package com.xjt.desensitize.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring上下文工具类，用于在非Spring管理的类中获取Spring容器中的Bean
 *
 * @author JTX
 * @since 1.0.0
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取Spring容器中的Bean
     *
     * @param clazz Bean的类型
     * @param <T>   泛型类型
     * @return 对应的Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring应用上下文未初始化");
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 获取Spring容器中的Bean
     *
     * @param name  Bean的名称
     * @param clazz Bean的类型
     * @param <T>   泛型类型
     * @return 对应的Bean实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring应用上下文未初始化");
        }
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext实例
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}