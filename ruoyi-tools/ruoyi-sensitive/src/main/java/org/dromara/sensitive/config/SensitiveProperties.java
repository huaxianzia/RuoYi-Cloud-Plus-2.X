package org.dromara.sensitive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sensitive.word")
public class SensitiveProperties {
    // 字段名：enable（boolean类型）
    private boolean enable = true;            // 是否启用敏感词检查
    private boolean checkProducer = true;     // 生产端检查开关
    private boolean checkConsumer = true;     // 消费端检查开关
    private String strategy = "intercept";    // 处理策略：intercept(替换)/log(仅日志)
    private char replaceChar = '*';           // 替换字符
    private String checkFields = "content";   // 需要检查的消息字段

    // 关键：boolean类型的getter必须是isEnable()，而非getEnable()
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    // 其他字段的getter/setter（保持不变）
    public boolean isCheckProducer() {
        return checkProducer;
    }

    public void setCheckProducer(boolean checkProducer) {
        this.checkProducer = checkProducer;
    }

    public boolean isCheckConsumer() {
        return checkConsumer;
    }

    public void setCheckConsumer(boolean checkConsumer) {
        this.checkConsumer = checkConsumer;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public char getReplaceChar() {
        return replaceChar;
    }

    public void setReplaceChar(char replaceChar) {
        this.replaceChar = replaceChar;
    }

    public String getCheckFields() {
        return checkFields;
    }

    public void setCheckFields(String checkFields) {
        this.checkFields = checkFields;
    }
}
