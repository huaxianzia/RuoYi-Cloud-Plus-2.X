package org.dromara.sensitive.mq;

import com.rabbitmq.client.Channel;
import org.dromara.sensitive.config.RabbitMqSimpleConfig;
import org.dromara.sensitive.domain.SysSensitiveWordLog;
import org.dromara.sensitive.service.ISysSensitiveWordLogService;
import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 仅测试，之后还是使用自带
 * RabbitMQ 消费者（监听消息 → 敏感词识别 → 记录日志）
 * 适配现有 SensitiveWordUtils 工具类（check() 方法）
 * 存放位置：org/dromara/sensitive/mq/SimpleMqConsumer.java
 */
@Component
public class SimpleMqConsumer {
    // 注入项目中已有的敏感词识别工具类
    @Resource
    private SensitiveWordUtils sensitiveWordUtils;

    // 注入原有敏感词日志服务
    @Resource
    private ISysSensitiveWordLogService sensitiveWordLogService;

    /**
     * 监听测试队列，自动消费消息
     * @param message 队列中的文本消息（待检测敏感词）
     * @param channel RabbitMQ通道（用于手动ACK）
     * @param msg Spring AMQP封装的消息对象（获取deliveryTag）
     */
    @RabbitListener(queues = RabbitMqSimpleConfig.TEST_QUEUE_NAME)
    public void consumeMessage(String message, Channel channel, Message msg) throws IOException {
        // 消息标签（用于ACK确认）
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();
        try {
            System.out.println("\n📥 RabbitMQ 消费者 - 接收到消息：" + message);

            // 1. 调用现有敏感词识别工具类的 check() 方法
            String matchWord = sensitiveWordUtils.check(message); // 返回第一个匹配的敏感词（null=无）
            boolean hasSensitive = matchWord != null; // 是否包含敏感词
            int status = hasSensitive ? 1 : 0; // 1=拦截，0=替换（和原有日志状态一致）
            // 适配日志显示：null 转为 "无"
            String showWord = hasSensitive ? matchWord : "无";

            // 2. 封装敏感词日志对象（适配原有日志表结构）
            SysSensitiveWordLog log = new SysSensitiveWordLog();
            log.setTriggerField("mq_message"); // 标记触发字段为MQ消息
            log.setSensitiveWord(showWord);    // 匹配到的敏感词（无则显示"无"）
            log.setOperatorName("MQ消费者");     // 标记操作人
            log.setRequestIp("127.0.0.1");      // MQ消费默认IP
            log.setRequestUrl("/mq/consumer");  // 标记请求URL
            log.setStatus(status);              // 识别状态（1=含敏感词，0=无）
            log.setCreateTime(LocalDateTime.now()); // 创建时间

            // 3. 调用原有服务保存日志
            boolean saveResult = sensitiveWordLogService.save(log);
            if (saveResult) {
                System.out.println("📝 RabbitMQ 消费者 - 敏感词日志保存成功：" + log);
            } else {
                throw new RuntimeException("敏感词日志保存失败");
            }

            // 4. 手动ACK确认（RabbitMQ移除该消息，避免重复消费）
            channel.basicAck(deliveryTag, false);
            System.out.println("✅ RabbitMQ 消费者 - 消息ACK确认完成，deliveryTag：" + deliveryTag);

        } catch (Exception e) {
            System.err.println("❌ RabbitMQ 消费者 - 处理消息失败：" + e.getMessage());
            // 异常时拒绝消息并重新入队（false=不批量拒绝，true=重新入队）
            channel.basicNack(deliveryTag, false, true);
            System.err.println("🔄 RabbitMQ 消费者 - 消息已拒绝并重新入队，deliveryTag：" + deliveryTag);
        }
    }
}
