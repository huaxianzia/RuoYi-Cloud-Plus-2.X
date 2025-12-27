package org.dromara.sensitive.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列配置类（敏感词识别测试队列）
 * 存放位置：org/dromara/sensitive/config/RabbitMqSimpleConfig.java
 */
@Configuration
public class RabbitMqSimpleConfig {
    // 测试队列名称（和生产者/消费者保持一致）
    public static final String TEST_QUEUE_NAME = "test_queue";

    /**
     * 声明测试队列（幂等性：重复声明不会创建新队列）
     * @return 队列实例
     */
    @Bean
    public Queue testQueue() {
        // 参数说明：队列名、是否持久化、是否独占、是否自动删除、额外参数
        return new Queue(TEST_QUEUE_NAME, false, false, false, null);
    }
}
