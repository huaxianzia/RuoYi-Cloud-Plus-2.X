package org.dromara.sensitive.annotation;

import java.lang.annotation.*;

/**
 * 标记需要进行敏感词检查的方法（通用注解，无依赖）
 */
@Target(ElementType.METHOD)  // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME)  // 运行时生效（切面可反射获取）
@Documented  // 生成JavaDoc时包含此注解
public @interface SensitiveCheck {
}
