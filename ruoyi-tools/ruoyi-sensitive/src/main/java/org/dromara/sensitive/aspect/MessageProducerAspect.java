package org.dromara.sensitive.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dromara.sensitive.config.SensitiveProperties;
import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * 敏感词检查切面：拦截需要进行敏感词检查的业务方法（通用适配）
 */
@Aspect
@Component
public class MessageProducerAspect {

    @Autowired
    private SensitiveWordUtils sensitiveWordUtils;

    @Autowired
    private SensitiveProperties sensitiveProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 拦截需要进行敏感词检查的方法（可根据实际业务调整切点表达式）
     * 示例：拦截所有标有@SensitiveCheck注解的方法，或特定包下的服务方法
     */
    @Around("execution(* org.dromara.sensitive.service..*(..))") // 示例：拦截敏感词服务包下的所有方法
    public Object interceptSensitiveCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 检查是否启用敏感词检查
        if (!sensitiveProperties.isEnable()) {
            return joinPoint.proceed(); // 不启用则直接放行
        }

        // 2. 提取方法参数中的消息/业务对象（根据实际参数结构调整）
        Object[] originalArgs = joinPoint.getArgs();
        Object targetObject = extractTargetObject(originalArgs);
        if (targetObject == null) {
            return joinPoint.proceed(originalArgs); // 无目标对象，直接放行
        }

        // 3. 提取需要检查的文本内容
        String content = extractContent(targetObject);
        if (content == null || content.isEmpty()) {
            return joinPoint.proceed(originalArgs); // 无文本内容，直接放行
        }

        // 4. 检查敏感词
        String sensitiveWord = sensitiveWordUtils.check(content);
        if (sensitiveWord == null) {
            return joinPoint.proceed(originalArgs); // 无敏感词，直接放行
        }

        // 5. 根据策略处理敏感词
        switch (sensitiveProperties.getStrategy()) {
            case "intercept":
                // 策略1：拦截操作（不执行原方法），可添加日志记录
                throw new RuntimeException("内容包含敏感词[" + sensitiveWord + "]，已拦截");
            case "replace":
                // 策略2：替换敏感词后继续执行
                String replacedContent = sensitiveWordUtils.replace(content);
                Object newObject = replaceContentInObject(targetObject, replacedContent);
                Object[] newArgs = replaceObjectInArgs(originalArgs, targetObject, newObject);
                return joinPoint.proceed(newArgs); // 用替换后的参数执行原方法
            default:
                // 策略3：仅记录日志，不拦截也不替换（继续执行原方法）
                return joinPoint.proceed(originalArgs);
        }
    }

    /**
     * 从方法参数中提取需要检查的目标对象（根据实际业务调整，如消息体、DTO等）
     */
    private Object extractTargetObject(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        // 示例：取第一个非基础类型的参数作为目标对象（可根据实际参数结构调整）
        for (Object arg : args) {
            if (arg != null && !isBaseType(arg.getClass())) {
                return arg;
            }
        }
        return args[0]; // 默认取第一个参数
    }

    /**
     * 判断是否为基础数据类型（包括包装类和String）
     */
    private boolean isBaseType(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz == String.class
            || Number.class.isAssignableFrom(clazz)
            || clazz == Boolean.class;
    }

    /**
     * 从目标对象中提取需要检查的文本内容（支持String、Map、JavaBean）
     */
    private String extractContent(Object target) {
        // 情况1：目标本身是字符串
        if (target instanceof String) {
            return (String) target;
        }

        // 情况2：目标是Map或JavaBean，解析JSON后提取配置的字段（如content、title）
        try {
            String json = objectMapper.writeValueAsString(target);
            JsonNode rootNode = objectMapper.readTree(json);
            // 遍历配置的需要检查的字段（从SensitiveProperties中获取）
            for (String field : sensitiveProperties.getCheckFields().split(",")) {
                JsonNode fieldNode = rootNode.get(field);
                if (fieldNode != null && fieldNode.isTextual()) {
                    return fieldNode.asText(); // 返回第一个匹配的字段值
                }
            }
            // 若未匹配到指定字段，返回整个对象的JSON字符串（避免遗漏）
            return json;
        } catch (JsonProcessingException e) {
            return ""; // 解析失败时返回空字符串
        }
    }

    /**
     * 替换目标对象中的敏感内容（支持Map和JavaBean）
     */
    private Object replaceContentInObject(Object originalObject, String replacedContent) throws IllegalAccessException {
        // 情况1：目标是Map类型
        if (originalObject instanceof Map) {
            Map<String, Object> objectMap = (Map<String, Object>) originalObject;
            for (String field : sensitiveProperties.getCheckFields().split(",")) {
                objectMap.put(field, replacedContent); // 覆盖字段值
            }
            return objectMap;
        }

        // 情况2：目标是JavaBean（通过反射替换字段）
        Class<?> clazz = originalObject.getClass();
        for (String fieldName : sensitiveProperties.getCheckFields().split(",")) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true); // 允许访问私有字段
                field.set(originalObject, replacedContent); // 替换字段值
            } catch (NoSuchFieldException e) {
                // 字段不存在则跳过（不影响其他字段）
            }
        }
        return originalObject;
    }

    /**
     * 将原始参数中的旧对象替换为新对象，返回新的参数数组
     */
    private Object[] replaceObjectInArgs(Object[] originalArgs, Object oldObject, Object newObject) {
        Object[] newArgs = Arrays.copyOf(originalArgs, originalArgs.length);
        for (int i = 0; i < newArgs.length; i++) {
            if (newArgs[i] == oldObject) { // 引用匹配时替换
                newArgs[i] = newObject;
                break;
            }
        }
        return newArgs;
    }
}
