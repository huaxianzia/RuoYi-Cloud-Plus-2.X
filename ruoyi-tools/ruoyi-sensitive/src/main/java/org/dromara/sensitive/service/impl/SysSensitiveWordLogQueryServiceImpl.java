package org.dromara.sensitive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.SysSensitiveWordLog;
import org.dromara.sensitive.domain.bo.SysSensitiveWordLogBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordLogVo;
import org.dromara.sensitive.mapper.SysSensitiveWordLogMapper;
import org.dromara.sensitive.service.ISysSensitiveWordLogQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感词日志查询服务
 */
@Service
@RequiredArgsConstructor
public class SysSensitiveWordLogQueryServiceImpl implements ISysSensitiveWordLogQueryService {

    private final SysSensitiveWordLogMapper sensitiveWordLogMapper;

    @Override
    public TableDataInfo<SysSensitiveWordLogVo> selectPageList(SysSensitiveWordLogBo bo) {
        // 1. 构建分页对象 (处理默认值逻辑建议放在Bo或者前端，此处保持简洁)
        Page<SysSensitiveWordLog> page = new Page<>(bo.getPageNum(), bo.getPageSize());

        // 2. 查询并转换
        sensitiveWordLogMapper.selectPage(page, buildQueryWrapper(bo));

        // 3. 利用 Page 的 convert 方法配合 BeanUtil 转换，TableDataInfo.build 自动封装总数
        return TableDataInfo.build(page.convert(this::convertToVo));
    }

    @Override
    public List<SysSensitiveWordLogVo> selectList(SysSensitiveWordLogBo bo) {
        List<SysSensitiveWordLog> list = sensitiveWordLogMapper.selectList(buildQueryWrapper(bo));
        // 使用 Hutool 快速转换列表
        return BeanUtil.copyToList(list, SysSensitiveWordLogVo.class);
    }

    /**
     * 构建通用查询条件（DRY原则：一次编写，到处复用）
     */
    private LambdaQueryWrapper<SysSensitiveWordLog> buildQueryWrapper(SysSensitiveWordLogBo bo) {
        return Wrappers.<SysSensitiveWordLog>lambdaQuery()
            .like(StringUtils.isNotBlank(bo.getSensitiveWord()), SysSensitiveWordLog::getSensitiveWord, bo.getSensitiveWord())
            .eq(StringUtils.isNotBlank(bo.getTriggerField()), SysSensitiveWordLog::getTriggerField, bo.getTriggerField())
            .like(StringUtils.isNotBlank(bo.getOperatorName()), SysSensitiveWordLog::getOperatorName, bo.getOperatorName())
            .eq(bo.getOperatorId() != null, SysSensitiveWordLog::getOperatorId, bo.getOperatorId())
            .eq(bo.getStatus() != null, SysSensitiveWordLog::getStatus, bo.getStatus())
            // 范围查询：开始时间
            .ge(bo.getCreateTimeStart() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeStart())
            // 范围查询：结束时间
            .le(bo.getCreateTimeEnd() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeEnd())
            .orderByDesc(SysSensitiveWordLog::getCreateTime);
    }

    /**
     * 单个对象转换
     */
    private SysSensitiveWordLogVo convertToVo(SysSensitiveWordLog entity) {
        return entity == null ? null : BeanUtil.toBean(entity, SysSensitiveWordLogVo.class);
    }
}
