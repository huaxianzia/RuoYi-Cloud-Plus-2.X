package org.dromara.system.controller; // 必须在启动类的扫描包下（如org.dromara.system的子包）

import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/mq/sensitive") // 一级路径
public class TestMqSensitiveController {

    @Autowired
    private SensitiveWordUtils sensitiveWordUtils;

    @GetMapping("/producer/intercept") // 二级路径，完整路径：/test/mq/sensitive/producer/intercept
    public String interceptSensitive(@RequestParam String content) {
        String sensitiveWord = sensitiveWordUtils.check(content);
        return sensitiveWord != null
            ? "【拦截成功】敏感词：" + sensitiveWord
            : "【放行】无敏感词：" + content;
    }
}
