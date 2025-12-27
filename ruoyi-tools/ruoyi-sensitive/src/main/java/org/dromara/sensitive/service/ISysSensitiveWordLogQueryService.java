package org.dromara.sensitive.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.bo.SysSensitiveWordLogBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordLogVo;

import java.util.List;

/**
 * 敏感词日志查询专用接口（仅含查询相关方法）
 *
 * @author 你的名字
 */
public interface ISysSensitiveWordLogQueryService {

    /**
     * 分页查询敏感词日志（支持多条件筛选）
     * @param bo 查询条件（含敏感词、操作人、时间范围等）
     * @return 分页结果（VO列表，适配前端展示）
     */
    TableDataInfo<SysSensitiveWordLogVo> selectPageList(SysSensitiveWordLogBo bo);

    /**
     * 不分页查询敏感词日志（用于导出）
     * @param bo 查询条件
     * @return 日志VO列表
     */
    List<SysSensitiveWordLogVo> selectList(SysSensitiveWordLogBo bo);

}
