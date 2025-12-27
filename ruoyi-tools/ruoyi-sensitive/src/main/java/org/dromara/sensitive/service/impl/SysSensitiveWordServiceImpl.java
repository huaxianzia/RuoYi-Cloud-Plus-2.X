package org.dromara.sensitive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil; // 导入Hutool的字符串工具类
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.SysSensitiveWord;
import org.dromara.sensitive.domain.bo.SysSensitiveWordBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordVo;
import org.dromara.sensitive.mapper.SysSensitiveWordMapper;
import org.dromara.sensitive.service.ISysSensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysSensitiveWordServiceImpl extends ServiceImpl<SysSensitiveWordMapper, SysSensitiveWord> implements ISysSensitiveWordService {

    private final SysSensitiveWordMapper sensitiveWordMapper;

    // ========== 原有方法（无修改） ==========
    @Override
    public List<SysSensitiveWord> selectEnabledSensitiveWords() {
        LambdaQueryWrapper<SysSensitiveWord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysSensitiveWord::getStatus, 0); // 0=启用（Integer类型）
        return sensitiveWordMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysSensitiveWordBo bo) {
        // 校验敏感词是否已存在
        if (exists(new LambdaQueryWrapper<SysSensitiveWord>().eq(SysSensitiveWord::getWord, bo.getWord()))) {
            throw new ServiceException("敏感词【" + bo.getWord() + "】已存在，请勿重复添加");
        }
        // BO转实体
        SysSensitiveWord sensitiveWord = BeanUtil.toBean(bo, SysSensitiveWord.class);
        // 填充创建人（替换为项目实际登录工具类）
        sensitiveWord.setCreateBy(getLoginUserId().toString());
        return save(sensitiveWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysSensitiveWordBo bo) {
        // 校验ID是否存在
        SysSensitiveWord oldWord = getById(bo.getId());
        if (oldWord == null) {
            throw new ServiceException("敏感词不存在，无法修改");
        }
        // 校验敏感词内容重复（排除自身）
        if (exists(new LambdaQueryWrapper<SysSensitiveWord>()
            .eq(SysSensitiveWord::getWord, bo.getWord())
            .ne(SysSensitiveWord::getId, bo.getId()))) {
            throw new ServiceException("敏感词【" + bo.getWord() + "】已存在，请勿重复修改");
        }
        // BO转实体
        SysSensitiveWord sensitiveWord = BeanUtil.toBean(bo, SysSensitiveWord.class);
        sensitiveWord.setUpdateBy(getLoginUserId().toString());
        return updateById(sensitiveWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatusById(Long id, Integer status) {
        // 校验状态合法性（Integer类型：0=启用，1=禁用）
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态值非法，仅支持0（启用）/1（禁用）");
        }
        // 校验ID存在
        SysSensitiveWord sensitiveWord = getById(id);
        if (sensitiveWord == null) {
            throw new ServiceException("敏感词不存在，无法修改状态");
        }
        // 更新状态
        sensitiveWord.setStatus(status);
        sensitiveWord.setUpdateBy(getLoginUserId().toString());
        return updateById(sensitiveWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的敏感词");
        }
        // 校验ID是否存在
        List<SysSensitiveWord> wordList = listByIds(ids);
        if (wordList.size() != ids.size()) {
            throw new ServiceException("部分敏感词不存在，删除失败");
        }
        return removeByIds(ids);
    }

    // ========== 修复分页查询方法（核心修改） ==========
    @Override
    public TableDataInfo<SysSensitiveWordVo> selectPageList(SysSensitiveWordBo bo) {
        // 1. 直接构建MyBatis-Plus分页对象
        Page<SysSensitiveWord> page = new Page<>(bo.getPageNum(), bo.getPageSize());
        // 2. 构建查询条件
        LambdaQueryWrapper<SysSensitiveWord> queryWrapper = buildQueryWrapper(bo);

        // ========== 调试用：打印SQL（生产环境可删除） ==========
        // System.out.println("=== 敏感词查询SQL ===" + sensitiveWordMapper.getSqlRunner().sqlSelect(queryWrapper));

        // 3. 执行分页查询
        sensitiveWordMapper.selectPage(page, queryWrapper);
        // 4. 实体转VO并封装分页结果
        return TableDataInfo.build(page.convert(this::convertVo));
    }

    @Override
    public List<SysSensitiveWordVo> selectList(SysSensitiveWordBo bo) {
        LambdaQueryWrapper<SysSensitiveWord> queryWrapper = buildQueryWrapper(bo);
        return sensitiveWordMapper.selectList(queryWrapper)
            .stream()
            .map(this::convertVo)
            .collect(Collectors.toList());
    }

    @Override
    public SysSensitiveWordVo selectVoById(Long id) {
        SysSensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        return convertVo(sensitiveWord);
    }

    // ========== 私有辅助方法（核心修改：替换为Hutool的StrUtil） ==========
    /**
     * 构建查询条件（增强空值+合法性判断，兼容所有环境）
     */
    private LambdaQueryWrapper<SysSensitiveWord> buildQueryWrapper(SysSensitiveWordBo bo) {
        LambdaQueryWrapper<SysSensitiveWord> queryWrapper = Wrappers.lambdaQuery();

        // 1. 模糊查询：敏感词内容（仅非空且非空白时筛选，用Hutool的StrUtil）
        if (StrUtil.isNotBlank(bo.getWord())) {
            queryWrapper.like(SysSensitiveWord::getWord, StrUtil.trim(bo.getWord()));
        }

        // 2. 模糊查询：备注（仅非空且非空白时筛选，用Hutool的StrUtil）
        if (StrUtil.isNotBlank(bo.getRemark())) {
            queryWrapper.like(SysSensitiveWord::getRemark, StrUtil.trim(bo.getRemark()));
        }

        // 3. 状态筛选：仅当status为0/1时才添加筛选（否则查全部）
        if (bo.getStatus() != null && (bo.getStatus() == 0 || bo.getStatus() == 1)) {
            queryWrapper.eq(SysSensitiveWord::getStatus, bo.getStatus());
        }

        // 4. 时间范围：创建时间（仅开始+结束都非空时筛选）
        if (bo.getBeginTime() != null && bo.getEndTime() != null) {
            queryWrapper.between(SysSensitiveWord::getCreateTime, bo.getBeginTime(), bo.getEndTime());
        }

        // 5. 排序：创建时间倒序
        queryWrapper.orderByDesc(SysSensitiveWord::getCreateTime);

        return queryWrapper;
    }

    /**
     * 实体转VO
     */
    private SysSensitiveWordVo convertVo(SysSensitiveWord sensitiveWord) {
        if (sensitiveWord == null) {
            return null;
        }
        return BeanUtil.toBean(sensitiveWord, SysSensitiveWordVo.class);
    }

    /**
     * 获取当前登录用户ID（替换为项目实际工具类）
     */
    private Long getLoginUserId() {
        // 示例：Sa-Token获取登录ID（实际替换为项目真实逻辑）
        // return StpUtil.getLoginIdAsLong();
        return 1L; // 测试用占位
    }
}
