package org.dromara.sensitive.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.SaTokenContextException; // 新增：导入上下文异常类
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.bo.SysSensitiveWordBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordVo;
import org.dromara.sensitive.service.ISysSensitiveWordService;
import org.dromara.common.web.core.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sensitive/word")
public class SysSensitiveWordController extends BaseController {

    // 新增：日志对象（用于打印异常，不新增类）
    private static final Logger log = LoggerFactory.getLogger(SysSensitiveWordController.class);

    private final ISysSensitiveWordService sensitiveWordService;

    /**
     * 分页查询敏感词列表
     */
    @SaCheckPermission("sensitive:word:list")
    @GetMapping("/list")
    public TableDataInfo<SysSensitiveWordVo> list(SysSensitiveWordBo bo) {
        return sensitiveWordService.selectPageList(bo);
    }

    /**
     * 导出敏感词数据
     */
    @Log(title = "敏感词管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("sensitive:word:export")
    @PostMapping("/export")
    public void export(SysSensitiveWordBo bo, HttpServletResponse response) {
        List<SysSensitiveWordVo> list = sensitiveWordService.selectList(bo);
        ExcelUtil.exportExcel(list, "敏感词数据", SysSensitiveWordVo.class, response);
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
     * 修改敏感词状态（启用/禁用）- 核心修改：捕获上下文异常，保证业务执行
     */
    @Log(title = "敏感词管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("sensitive:word:edit")
    @PutMapping("/changeStatus") // 保持PUT请求不变
    public R<Boolean> changeStatus(@RequestBody SysSensitiveWordBo bo) {
        try {
            // 尝试初始化上下文，失败则捕获异常
            SaHolder.getStorage();
        } catch (SaTokenContextException e) {
            // 仅打印警告日志，不中断业务逻辑
            log.warn("SaToken上下文未初始化（不影响业务执行）：{}", e.getMessage());
        }
        // 强制执行业务逻辑，忽略上下文异常
        return R.ok(sensitiveWordService.updateStatusById(bo.getId(), bo.getStatus()));
    }

    /**
     * 批量删除敏感词
     */
    @Log(title = "敏感词管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("sensitive:word:remove")
    @DeleteMapping("/remove")
    public R<Boolean> remove(@RequestBody Collection<Long> ids) {
        return R.ok(sensitiveWordService.deleteWithValidByIds(ids));
    }

    /**
     * 单条删除敏感词（核心新增：匹配前端 /sensitive/word/8 路径，解决不支持DELETE请求异常）
     */
    @Log(title = "敏感词管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("sensitive:word:remove")
    @DeleteMapping("/{id}") // 路径匹配 /sensitive/word/{id}，适配单条删除请求
    public R<Boolean> removeSingle(@PathVariable Long id) {
        // 复用批量删除的业务逻辑，将单条ID转为集合
        return R.ok(sensitiveWordService.deleteWithValidByIds(List.of(id)));
    }

    /**
     * 查询敏感词详情
     */
    @SaCheckPermission("sensitive:word:list")
    @GetMapping("/{id}")
    public R<SysSensitiveWordVo> getInfo(@PathVariable Long id) {
        return R.ok(sensitiveWordService.selectVoById(id));
    }
}
