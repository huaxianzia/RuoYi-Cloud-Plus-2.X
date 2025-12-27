package org.dromara.sensitive.domain.vo;

import org.dromara.sensitive.domain.SysSensitiveWord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 敏感词视图对象 sys_sensitive_word
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysSensitiveWordVo extends SysSensitiveWord {
    // 若需扩展前端显示字段，可在此添加（如状态名称：statusName）
    private String statusName; // 用于前端显示“拦截”/“过滤”

    // 重写getStatus方法，自动填充statusName
    @Override
    public Integer getStatus() {
        Integer status = super.getStatus();
        this.statusName = status == 1 ? "拦截" : "过滤";
        return status;
    }
}
