package org.dromara.system;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.ComponentScan;

/**
 * 系统模块
 *
 * @author ruoyi
 */
@EnableDubbo
@SpringBootApplication//(exclude = RabbitAutoConfiguration.class)
@ComponentScan(basePackages = {
    "org.dromara.system",        // 系统模块自身包
    "org.dromara.sensitive"      // 敏感词工具类所在包
})
public class RuoYiSystemApplication implements CommandLineRunner {

    @Autowired
    private SensitiveWordUtils sensitiveWordUtils;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiSystemApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }

    @Override
    public void run(String... args) {
        // 启动时强制刷新敏感词库，确保初始化
        sensitiveWordUtils.refresh();
    }
}
