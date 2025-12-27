package org.dromara.sensitive.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dromara.sensitive.annotation.SensitiveCheck; // 导入自定义注解（需提前创建）
import org.dromara.sensitive.config.SensitiveProperties;
import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 保留你原来的类名，仅移除Rabbit依赖，改为通用敏感词检查切面
 */
@Aspect
@Component
public class MesssgeConsumerAspect { // 恢复你原来的类名（与文件名一致）

    @Autowired
    private SensitiveWordUtils sensitiveWordUtils;

    @Autowired
    private SensitiveProperties sensitiveProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 拦截所有标记@SensitiveCheck注解的方法（替代原来的RabbitListener）
    @Around("@annotation(org.dromara.sensitive.annotation.SensitiveCheck)")
    public Object interceptConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 检查是否启用敏感词检查，或是否跳过消费端检查
        if (!sensitiveProperties.isEnable() || !sensitiveProperties.isCheckConsumer()) {
            return joinPoint.proceed();
        }

        // 2. 提取方法参数（假设第一个参数是需要检查的消息/业务对象）
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return joinPoint.proceed();
        }

        // 3. 提取消息内容并检查敏感词
        Object message = args[0];
        String content = extractContent(message);
        String sensitiveWord = sensitiveWordUtils.check(content);

        if (sensitiveWord != null) {
            // 4. 根据策略处理敏感词
            switch (sensitiveProperties.getStrategy()) {
                case "intercept":
                    // 拦截处理（不执行原方法），可添加日志记录
                    return null;
                case "replace":
                    // 替换敏感词后继续处理
                    String newContent = sensitiveWordUtils.replace(content);
                    Object newMessage = replaceContentInMessage(message, newContent);
                    args[0] = newMessage;
                    return joinPoint.proceed(args);
                default:
                    // 仅记录日志，继续处理原消息
                    return joinPoint.proceed();
            }
        }

        // 无敏感词，直接放行
        return joinPoint.proceed();
    }

    /**
     * 提取需要检查的文本内容（支持String、Map、JavaBean）
     */
    private String extractContent(Object message) {
        if (message instanceof String) {
            return (String) message;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            JsonNode rootNode = objectMapper.readTree(json);
            for (String field : sensitiveProperties.getCheckFields().split(",")) {
                JsonNode fieldNode = rootNode.get(field);
                if (fieldNode != null && fieldNode.isTextual()) {
                    return fieldNode.asText();
                }
            }
            return json;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 替换消息中的敏感内容（支持Map和JavaBean）
     */
    private Object replaceContentInMessage(Object message, String newContent) throws IllegalAccessException {
        if (message instanceof Map) {
            Map<String, Object> messageMap = (Map<String, Object>) message;
            for (String field : sensitiveProperties.getCheckFields().split(",")) {
                messageMap.put(field, newContent);
            }
            return messageMap;
        }

        Class<?> clazz = message.getClass();
        for (String fieldName : sensitiveProperties.getCheckFields().split(",")) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(message, newContent);
            } catch (NoSuchFieldException e) {
                // 字段不存在则跳过
            }
        }
        return message;
    }
}
