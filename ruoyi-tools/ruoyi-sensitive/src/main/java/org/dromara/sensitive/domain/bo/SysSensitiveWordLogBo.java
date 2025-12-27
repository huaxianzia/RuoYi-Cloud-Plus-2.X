package org.dromara.sensitive.domain.bo;

import lombok.Data;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.time.LocalDateTime;

/**
 * 敏感词日志查询业务对象
 *
 * @author 你的名字
 */
@Data
public class SysSensitiveWordLogBo extends PageQuery {

    /** 主键 */
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

    /** 创建时间开始 */
    private LocalDateTime createTimeStart;

    /** 创建时间结束 */
    private LocalDateTime createTimeEnd;

}
