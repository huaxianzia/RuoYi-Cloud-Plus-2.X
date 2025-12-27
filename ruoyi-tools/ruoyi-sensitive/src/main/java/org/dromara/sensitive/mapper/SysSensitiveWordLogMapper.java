package org.dromara.sensitive.mapper;

import org.dromara.sensitive.domain.SysSensitiveWordLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词日志Mapper接口
 */
@Mapper
public interface SysSensitiveWordLogMapper extends BaseMapper<SysSensitiveWordLog> {
}
