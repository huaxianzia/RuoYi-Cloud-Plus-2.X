package org.dromara.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.dromara.sensitive.domain.SysSensitiveWordLog;
import org.dromara.sensitive.mapper.SysSensitiveWordLogMapper;
import org.dromara.sensitive.service.ISysSensitiveWordLogService;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 敏感词日志Service实现（完整功能：包含批量删除，无框架基类依赖）
 */
@Service
public class SysSensitiveWordLogServiceImpl implements ISysSensitiveWordLogService {

    @Autowired
    private SysSensitiveWordLogMapper sysSensitiveWordLogMapper;

    /**
     * 新增日志
     */
    @Override
    public boolean save(SysSensitiveWordLog log) {
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        return sysSensitiveWordLogMapper.insert(log) > 0;
    }

    /**
     * 根据复合主键查询
     */
    @Override
    public SysSensitiveWordLog getById(Long id, LocalDateTime createTime) {
        if (id == null || createTime == null) {
            return null;
        }
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysSensitiveWordLog::getId, id)
            .eq(SysSensitiveWordLog::getCreateTime, createTime);
        return sysSensitiveWordLogMapper.selectOne(queryWrapper);
    }

    /**
     * 更新日志（基于复合主键）
     */
    @Override
    public boolean update(SysSensitiveWordLog log) {
        if (log.getId() == null || log.getCreateTime() == null) {
            return false;
        }
        LambdaQueryWrapper<SysSensitiveWordLog> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(SysSensitiveWordLog::getId, log.getId())
            .eq(SysSensitiveWordLog::getCreateTime, log.getCreateTime());
        return sysSensitiveWordLogMapper.update(log, updateWrapper) > 0;
    }

    /**
     * 根据复合主键删除单条日志
     */
    @Override
    public boolean removeById(Long id, LocalDateTime createTime) {
        if (id == null || createTime == null) {
            return false;
        }
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysSensitiveWordLog::getId, id)
            .eq(SysSensitiveWordLog::getCreateTime, createTime);
        return sysSensitiveWordLogMapper.delete(queryWrapper) > 0;
    }

    /**
     * 条件查询日志列表
     */
    @Override
    public List<SysSensitiveWordLog> list(SysSensitiveWordLog query) {
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(query)) {
            // 敏感词精确匹配
            if (query.getSensitiveWord() != null && !query.getSensitiveWord().isEmpty()) {
                queryWrapper.eq(SysSensitiveWordLog::getSensitiveWord, query.getSensitiveWord());
            }
            // 触发字段匹配
            if (query.getTriggerField() != null && !query.getTriggerField().isEmpty()) {
                queryWrapper.eq(SysSensitiveWordLog::getTriggerField, query.getTriggerField());
            }
            // 状态匹配（1=拦截，0=替换）
            if (query.getStatus() != null) {
                queryWrapper.eq(SysSensitiveWordLog::getStatus, query.getStatus());
            }
            // 时间范围（大于等于传入时间）
            if (query.getCreateTime() != null) {
                queryWrapper.ge(SysSensitiveWordLog::getCreateTime, query.getCreateTime());
            }
        }
        return sysSensitiveWordLogMapper.selectList(queryWrapper);
    }

    /**
     * 批量删除日志（修正：自己实现批量删除逻辑，避免依赖框架方法）
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        // 空集合直接返回false
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 构造批量删除条件：id在传入的ids集合中
        LambdaQueryWrapper<SysSensitiveWordLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysSensitiveWordLog::getId, ids);
        // 执行删除并返回是否成功（删除数量>0则为true）
        int deleteCount = sysSensitiveWordLogMapper.delete(queryWrapper);
        return deleteCount > 0;
    }
}
