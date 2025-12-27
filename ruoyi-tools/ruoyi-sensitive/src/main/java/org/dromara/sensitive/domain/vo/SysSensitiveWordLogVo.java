package org.dromara.sensitive.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感词日志视图对象
 *
 * @author 你的名字
 */
@Data
public class SysSensitiveWordLogVo {

    /** 日志主键 */
    private Long id;

    /** 触发敏感词的字段名（如companyName） */
    private String triggerField;

    /** 敏感词内容 */
    private String sensitiveWord;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 请求IP */
    private String requestIp;

    /** 请求URL */
    private String requestUrl;

    /** 状态（1=拦截，0=替换） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

}
