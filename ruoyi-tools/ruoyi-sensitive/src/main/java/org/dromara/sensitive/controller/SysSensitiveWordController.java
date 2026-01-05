package org.dromara.sensitive.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.sensitive.domain.bo.SysSensitiveWordBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordVo;
import org.dromara.sensitive.service.ISysSensitiveWordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 敏感词管理控制器
 *
 * @author dromara
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sensitive/word")
public class SysSensitiveWordController extends BaseController {

    private final ISysSensitiveWordService sensitiveWordService;

    /**
     * 查询敏感词列表
     */
    @SaCheckPermission("sensitive:word:list")
    @GetMapping("/list")
    public TableDataInfo<SysSensitiveWordVo> list(SysSensitiveWordBo bo) {
        return sensitiveWordService.selectPageList(bo);
    }

    /**
     * 导出敏感词列表
     */
    @Log(title = "敏感词管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("sensitive:word:export")
    @PostMapping("/export")
    public void export(SysSensitiveWordBo bo, HttpServletResponse response) {
        List<SysSensitiveWordVo> list = sensitiveWordService.selectList(bo);
        ExcelUtil.exportExcel(list, "敏感词数据", SysSensitiveWordVo.class, response);
    }

    /**
     * 获取敏感词详细信息
     */
    @SaCheckPermission("sensitive:word:list")
    @GetMapping("/{id}")
    public R<SysSensitiveWordVo> getInfo(@PathVariable Long id) {
        return R.ok(sensitiveWordService.selectVoById(id));
    }

    /**
     * 新增敏感词
     */
    @Log(title = "敏感词管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("sensitive:word:add")
    @PostMapping
    public R<Boolean> add(@Validated @RequestBody SysSensitiveWordBo bo) {
        return R.ok(sensitiveWordService.insertByBo(bo));
    }

    /**
     * 修改敏感词
     */
    @Log(title = "敏感词管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("sensitive:word:edit")
    @PutMapping
    public R<Boolean> edit(@Validated @RequestBody SysSensitiveWordBo bo) {
        return R.ok(sensitiveWordService.updateByBo(bo));
    }

    /**
     * 修改敏感词状态
     */
    @Log(title = "敏感词管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("sensitive:word:edit")
    @PutMapping("/changeStatus")
    public R<Boolean> changeStatus(@RequestBody SysSensitiveWordBo bo) {
        return R.ok(sensitiveWordService.updateStatusById(bo.getId(), bo.getStatus()));
    }

    /**
     * 删除敏感词 (批量)
     */
    @Log(title = "敏感词管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("sensitive:word:remove")
    @DeleteMapping("/remove")
    public R<Boolean> remove(@RequestBody List<Long> ids) {
        return R.ok(sensitiveWordService.deleteWithValidByIds(ids));
    }

    /**
     * 删除敏感词 (单条)
     */
    @Log(title = "敏感词管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("sensitive:word:remove")
    @DeleteMapping("/{id}")
    public R<Boolean> remove(@PathVariable Long id) {
        return R.ok(sensitiveWordService.deleteWithValidByIds(List.of(id)));
    }
}
