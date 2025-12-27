package org.dromara.sensitive.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词实体类
 * 对应数据库表：sys_sensitive_word
 */
@Data
@TableName("sys_sensitive_word")
public class SysSensitiveWord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词内容 */
    private String word;

    /**
     * 状态（0启用 1禁用），Integer类型，默认值1
     */
    private Integer status;

    /** 创建者 */
    private String createBy = "admin";

    /** 创建时间 */
    private Date createTime = new Date();

    /** 更新者 */
    private String updateBy = "";

    /** 更新时间 */
    private Date updateTime = new Date();

    /** 备注 */
    private String remark;
}
