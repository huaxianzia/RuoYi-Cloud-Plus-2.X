package org.dromara.sensitive.mq;

import org.dromara.sensitive.config.RabbitMqSimpleConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * RabbitMQ 生产者（发送测试消息到敏感词识别队列）
 * 存放位置：org/dromara/sensitive/mq/SimpleMqProducer.java
 */
@Component
public class SimpleMqProducer {
    // 注入Spring AMQP核心模板（自动适配application.yml中的RabbitMQ配置）
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到测试队列
     * @param message 待检测敏感词的文本消息
     */
    public void sendMessage(String message) {
        try {
            // 发送消息：默认交换机、路由键=队列名、消息体
            rabbitTemplate.convertAndSend(RabbitMqSimpleConfig.TEST_QUEUE_NAME, message);
            System.out.println("✅ RabbitMQ 生产者 - 已发送消息：" + message);
        } catch (Exception e) {
            System.err.println("❌ RabbitMQ 生产者 - 发送消息失败：" + e.getMessage());
            throw new RuntimeException("消息发送失败", e);
        }
    }
}
