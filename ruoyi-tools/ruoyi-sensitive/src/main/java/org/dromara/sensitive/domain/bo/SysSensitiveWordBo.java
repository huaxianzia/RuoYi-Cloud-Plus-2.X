package org.dromara.sensitive.domain.bo;

import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 敏感词业务对象
 * 简化分页参数，直接暴露pageNum/pageSize（适配多数项目的分页规范）
 */
@Data
public class SysSensitiveWordBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（编辑/删除/状态修改时用）
     */
    private Long id;

    /**
     * 敏感词内容（查询/新增/修改时用）
     */
    private String word;

    /**
     * 备注（查询/新增/修改时用）
     */
    private String remark;

    /**
     * 状态（0启用 1禁用），Integer类型，默认值1
     */
    private Integer status;

    // ========== 直接定义分页参数（替代PageQuery，解决buildPage报错） ==========
    /**
     * 页码（默认第1页）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10条）
     */
    private Integer pageSize = 10;

    // 时间范围字段（按需启用）
    private String beginTime;
     private String endTime;
}
