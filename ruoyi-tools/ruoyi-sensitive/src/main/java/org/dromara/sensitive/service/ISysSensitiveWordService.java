package org.dromara.sensitive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.SysSensitiveWord;
import org.dromara.sensitive.domain.bo.SysSensitiveWordBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordVo;

import java.util.Collection;
import java.util.List;

public interface ISysSensitiveWordService extends IService<SysSensitiveWord> {
    /**
     * 查询所有启用的敏感词（原有方法）
     */
    List<SysSensitiveWord> selectEnabledSensitiveWords();

    /**
     * 分页查询敏感词列表
     */
    TableDataInfo<SysSensitiveWordVo> selectPageList(SysSensitiveWordBo bo);

    /**
     * 不分页查询敏感词列表（供导出用）
     */
    List<SysSensitiveWordVo> selectList(SysSensitiveWordBo bo);

    /**
     * 根据ID查询敏感词详情
     */
    SysSensitiveWordVo selectVoById(Long id);

    /**
     * 新增敏感词
     */
    Boolean insertByBo(SysSensitiveWordBo bo);

    /**
     * 修改敏感词
     */
    Boolean updateByBo(SysSensitiveWordBo bo);

    /**
     * 修改敏感词状态（status 改为 Integer 类型）
     */
    Boolean updateStatusById(Long id, Integer status);

    /**
     * 批量删除敏感词
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
