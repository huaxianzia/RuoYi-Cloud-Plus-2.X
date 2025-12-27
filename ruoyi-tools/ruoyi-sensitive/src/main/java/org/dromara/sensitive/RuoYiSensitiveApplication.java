package org.dromara.sensitive;

import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

/**
 * 敏感词模块启动类
 */
@EnableDiscoveryClient // 若需注册到服务发现（如Nacos），保留此注解；否则删除
@MapperScan("org.dromara.sensitive.mapper") // 扫描MyBatis Mapper接口
@SpringBootApplication(scanBasePackages = "org.dromara.sensitive") // 明确扫描当前模块的所有组件
public class RuoYiSensitiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuoYiSensitiveApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  工具模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }

    /**
     * 异步初始化敏感词库（避免阻塞启动流程）
     */
//    @PostConstruct
//    public void initSensitiveWords() {
//        CompletableFuture.runAsync(SensitiveWordUtils::init)
//            .exceptionally(e -> {
//                System.err.println("敏感词库初始化失败：" + e.getMessage());
//                e.printStackTrace(); // 打印异常堆栈，便于排查问题
//                return null;
//            });
//    }
}
