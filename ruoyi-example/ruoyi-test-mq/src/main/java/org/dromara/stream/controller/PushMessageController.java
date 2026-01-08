package org.dromara.stream.controller;

import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.sensitive.service.ISysSensitiveWordLogService;
import org.dromara.sensitive.domain.SysSensitiveWordLog; // 假设实体类在这个包下，请根据实际项目路径调整
import org.dromara.stream.producer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.dromara.sensitive.utils.SensitiveWordUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author xbhog
 */
@Slf4j
@RestController
@RequestMapping
public class PushMessageController {

    @Resource
    private SensitiveWordUtils sensitiveWordUtils;

    @Resource
    private ISysSensitiveWordLogService sensitiveWordLogService;

    @Autowired
    private NormalRabbitProducer normalRabbitProducer;

    @Autowired
    private DelayRabbitProducer delayRabbitProducer;


    @Autowired
    private KafkaNormalProducer normalKafkaProducer;

    /**
     * rabbitmq 普通消息
     */
    @SaIgnore
    @GetMapping("/rabbit/send")
    public void rabbitSend() {
        String message = "明天";
        normalRabbitProducer.send(message);
    }

    /**
     * rabbitmq 延迟队列消息
     */
    @SaIgnore
    @GetMapping("/rabbit/sendDelay")
    public void rabbitSendDelay(long delay) {
        // 如果这里也需要检测，可以复制上面的逻辑
        delayRabbitProducer.sendDelayMessage("Hello ttl RabbitMsg", delay);
    }

    /**
     * rocketmq 发送消息
     * 需要手动创建相关的Topic和group
     */
//    @GetMapping("/rocket/send")
//    public void rocketSend(){
//        normalRocketProducer.sendMessage();
//    }

    /**
     * rocketmq 事务消息
     */
//    @GetMapping("/rocket/transaction")
//    public void rocketTransaction(){
//        transactionRocketProducer.sendTransactionMessage();
//    }

    /**
     * kafka 发送消息
     */
//    @GetMapping("/kafka/send")
//    public void kafkaSend(){
//        normalKafkaProducer.sendKafkaMsg();
//    }
}
