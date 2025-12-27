package org.dromara.sensitive.controller;

import org.dromara.common.core.domain.R;
import org.dromara.sensitive.mq.SimpleMqProducer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * RabbitMQ 测试接口（发送敏感词检测消息）
 * 存放位置：org/dromara/sensitive/controller/MqTestController.java
 */
@RestController
@RequestMapping("/mq/test")
public class MqTestController {
    // 注入MQ生产者
    @Resource
    private SimpleMqProducer simpleMqProducer;

    /**
     * 发送测试消息到RabbitMQ队列
     * 访问示例：http://localhost:8088/mq/test/send?message=测试敏感词：赌博
     * @param message 待检测的文本消息
     * @return 操作结果
     */
    @GetMapping("/send")
    public R<String> sendTestMessage(@RequestParam(value = "message", defaultValue = "测试消息：无敏感词") String message) {
        try {
            // 调用生产者发送消息
            simpleMqProducer.sendMessage(message);
            return R.ok("✅ 消息已成功发送到RabbitMQ队列，消费者将自动识别敏感词并记录日志！\n发送的消息：" + message);
        } catch (Exception e) {
            return R.fail("❌ 消息发送失败：" + e.getMessage());
        }
    }
}
