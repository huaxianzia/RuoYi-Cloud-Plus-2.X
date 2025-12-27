package org.dromara.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.sensitive.domain.SysSensitiveWord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysSensitiveWordMapper extends BaseMapper<SysSensitiveWord> {
    /**
     * 查询所有启用的敏感词
     */
    List<SysSensitiveWord> selectEnabledSensitiveWords();
}
