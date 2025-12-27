package org.dromara.sensitive.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感词日志实体（仅依赖MyBatis-Plus基础注解）
 */
@Data
@TableName("sys_sensitive_word_log") // 绑定表名
public class SysSensitiveWordLog {

    /** 日志ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID) // 自动生成雪花ID
    private Long id;

    /** 触发敏感词的字段 */
    private String triggerField;

    /** 敏感词内容 */
    private String sensitiveWord;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 请求IP */
    private String requestIp;

    /** 接口URL */
    private String requestUrl;

    /** 状态（1：拦截 0：替换） */
    private Integer status;

    /** 创建时间（复合主键之一） */
    private LocalDateTime createTime; // 无需@TableId，查询/更新时手动指定条件
}
