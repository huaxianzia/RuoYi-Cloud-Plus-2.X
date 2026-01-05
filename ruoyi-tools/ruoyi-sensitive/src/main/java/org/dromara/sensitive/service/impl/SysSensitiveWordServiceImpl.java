package org.dromara.sensitive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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

@Service
@RequiredArgsConstructor
public class SysSensitiveWordServiceImpl extends ServiceImpl<SysSensitiveWordMapper, SysSensitiveWord> implements ISysSensitiveWordService {

    private final SysSensitiveWordMapper sensitiveWordMapper;

    @Override
    public List<SysSensitiveWord> selectEnabledSensitiveWords() {
        return sensitiveWordMapper.selectList(
            Wrappers.<SysSensitiveWord>lambdaQuery().eq(SysSensitiveWord::getStatus, 0)
        );
    }

    @Override
    public TableDataInfo<SysSensitiveWordVo> selectPageList(SysSensitiveWordBo bo) {
        Page<SysSensitiveWord> page = sensitiveWordMapper.selectPage(new Page<>(bo.getPageNum(), bo.getPageSize()), buildQueryWrapper(bo));
        return TableDataInfo.build(page.convert(this::convertVo));
    }

    @Override
    public List<SysSensitiveWordVo> selectList(SysSensitiveWordBo bo) {
        List<SysSensitiveWord> list = sensitiveWordMapper.selectList(buildQueryWrapper(bo));
        return BeanUtil.copyToList(list, SysSensitiveWordVo.class);
    }

    @Override
    public SysSensitiveWordVo selectVoById(Long id) {
        return convertVo(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysSensitiveWordBo bo) {
        if (checkWordUnique(bo.getWord(), null)) {
            throw new ServiceException("敏感词【" + bo.getWord() + "】已存在");
        }
        SysSensitiveWord sensitiveWord = BeanUtil.toBean(bo, SysSensitiveWord.class);
        sensitiveWord.setCreateBy(getLoginUserIdStr());
        return save(sensitiveWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysSensitiveWordBo bo) {
        SysSensitiveWord oldWord = getById(bo.getId());
        if (oldWord == null) {
            throw new ServiceException("敏感词不存在");
        }
        if (checkWordUnique(bo.getWord(), bo.getId())) {
            throw new ServiceException("敏感词【" + bo.getWord() + "】已存在");
        }
        SysSensitiveWord sensitiveWord = BeanUtil.toBean(bo, SysSensitiveWord.class);
        sensitiveWord.setUpdateBy(getLoginUserIdStr());
        return updateById(sensitiveWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatusById(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态值非法");
        }
        SysSensitiveWord exist = getById(id);
        if (exist == null) {
            throw new ServiceException("敏感词不存在");
        }
        exist.setStatus(status);
        exist.setUpdateBy(getLoginUserIdStr());
        return updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的数据");
        }
        // 校验逻辑可根据业务需求保留或简化，这里保持原逻辑严格性
        if (listByIds(ids).size() != ids.size()) {
            throw new ServiceException("部分数据不存在，删除失败");
        }
        return removeByIds(ids);
    }

    /**
     * 构建查询条件 (链式调用，简洁明了)
     */
    private LambdaQueryWrapper<SysSensitiveWord> buildQueryWrapper(SysSensitiveWordBo bo) {
        return Wrappers.<SysSensitiveWord>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getWord()), SysSensitiveWord::getWord, bo.getWord())
            .like(StrUtil.isNotBlank(bo.getRemark()), SysSensitiveWord::getRemark, bo.getRemark())
            .eq(bo.getStatus() != null, SysSensitiveWord::getStatus, bo.getStatus())
            .between(bo.getBeginTime() != null && bo.getEndTime() != null, SysSensitiveWord::getCreateTime, bo.getBeginTime(), bo.getEndTime())
            .orderByDesc(SysSensitiveWord::getCreateTime);
    }

    /**
     * 校验敏感词唯一性
     * @param word 敏感词
     * @param excludeId 需要排除的ID（修改时使用）
     * @return true=已存在
     */
    private boolean checkWordUnique(String word, Long excludeId) {
        return exists(Wrappers.<SysSensitiveWord>lambdaQuery()
            .eq(SysSensitiveWord::getWord, word)
            .ne(excludeId != null, SysSensitiveWord::getId, excludeId));
    }

    private SysSensitiveWordVo convertVo(SysSensitiveWord entity) {
        return entity == null ? null : BeanUtil.toBean(entity, SysSensitiveWordVo.class);
    }

    // 建议：实际项目中建议使用 LoginHelper.getUserId()，这里仅作兼容
    private String getLoginUserIdStr() {
        return "1";
    }
}
