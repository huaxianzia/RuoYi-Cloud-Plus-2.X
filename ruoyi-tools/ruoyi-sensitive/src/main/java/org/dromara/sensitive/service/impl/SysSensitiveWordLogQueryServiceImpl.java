package org.dromara.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.SysSensitiveWordLog;
import org.dromara.sensitive.domain.bo.SysSensitiveWordLogBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordLogVo;
import org.dromara.sensitive.mapper.SysSensitiveWordLogMapper;
import org.dromara.sensitive.service.ISysSensitiveWordLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词日志查询专用实现类（仅处理查询逻辑，简洁无冗余）
 *
 * @author 你的名字
 */
@Service
public class SysSensitiveWordLogQueryServiceImpl implements ISysSensitiveWordLogQueryService {

    @Autowired
    private SysSensitiveWordLogMapper sysSensitiveWordLogMapper;

    @Override
    public TableDataInfo<SysSensitiveWordLogVo> selectPageList(SysSensitiveWordLogBo bo) {
        // 1. 构建分页参数（默认页码1，每页10条）
        Page<SysSensitiveWordLog> page = new Page<>(
            bo.getPageNum() == null ? 1 : bo.getPageNum(),
            bo.getPageSize() == null ? 10 : bo.getPageSize()
        );

        // 2. 构建查询条件（支持多维度筛选）
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
            // 敏感词模糊查询
            .like(StringUtils.isNotBlank(bo.getSensitiveWord()), SysSensitiveWordLog::getSensitiveWord, bo.getSensitiveWord())
            // 触发字段精确查询
            .eq(StringUtils.isNotBlank(bo.getTriggerField()), SysSensitiveWordLog::getTriggerField, bo.getTriggerField())
            // 操作人名称模糊查询
            .like(StringUtils.isNotBlank(bo.getOperatorName()), SysSensitiveWordLog::getOperatorName, bo.getOperatorName())
            // 操作人ID精确查询
            .eq(bo.getOperatorId() != null, SysSensitiveWordLog::getOperatorId, bo.getOperatorId())
            // 状态精确查询（1=拦截，0=替换）
            .eq(bo.getStatus() != null, SysSensitiveWordLog::getStatus, bo.getStatus())
            // 时间范围查询
            .ge(bo.getCreateTimeStart() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeStart())
            .le(bo.getCreateTimeEnd() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeEnd())
            // 按创建时间倒序（最新日志在前）
            .orderByDesc(SysSensitiveWordLog::getCreateTime);

        // 3. 执行分页查询
        IPage<SysSensitiveWordLog> iPage = sysSensitiveWordLogMapper.selectPage(page, queryWrapper);

        // 4. 实体转VO（适配前端展示）
        List<SysSensitiveWordLogVo> voList = iPage.getRecords().stream()
            .map(this::convertToVo)
            .collect(Collectors.toList());

        // 5. 封装分页结果
        return new TableDataInfo<>(voList, iPage.getTotal());
    }

    @Override
    public List<SysSensitiveWordLogVo> selectList(SysSensitiveWordLogBo bo) {
        // 复用查询条件逻辑
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
            .like(StringUtils.isNotBlank(bo.getSensitiveWord()), SysSensitiveWordLog::getSensitiveWord, bo.getSensitiveWord())
            .eq(StringUtils.isNotBlank(bo.getTriggerField()), SysSensitiveWordLog::getTriggerField, bo.getTriggerField())
            .like(StringUtils.isNotBlank(bo.getOperatorName()), SysSensitiveWordLog::getOperatorName, bo.getOperatorName())
            .eq(bo.getOperatorId() != null, SysSensitiveWordLog::getOperatorId, bo.getOperatorId())
            .eq(bo.getStatus() != null, SysSensitiveWordLog::getStatus, bo.getStatus())
            .ge(bo.getCreateTimeStart() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeStart())
            .le(bo.getCreateTimeEnd() != null, SysSensitiveWordLog::getCreateTime, bo.getCreateTimeEnd())
            .orderByDesc(SysSensitiveWordLog::getCreateTime);

        // 查询并转VO
        return sysSensitiveWordLogMapper.selectList(queryWrapper).stream()
            .map(this::convertToVo)
            .collect(Collectors.toList());
    }

    /**
     * 实体类转VO（简洁映射，仅保留前端需要的字段）
     */
    private SysSensitiveWordLogVo convertToVo(SysSensitiveWordLog entity) {
        SysSensitiveWordLogVo vo = new SysSensitiveWordLogVo();
        vo.setId(entity.getId());
        vo.setTriggerField(entity.getTriggerField());
        vo.setSensitiveWord(entity.getSensitiveWord());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setRequestIp(entity.getRequestIp());
        vo.setRequestUrl(entity.getRequestUrl());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

}
