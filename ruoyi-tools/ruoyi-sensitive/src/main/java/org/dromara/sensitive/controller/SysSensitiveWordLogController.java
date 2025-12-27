package org.dromara.sensitive.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.sensitive.domain.bo.SysSensitiveWordLogBo;
import org.dromara.sensitive.domain.vo.SysSensitiveWordLogVo;
import org.dromara.sensitive.service.ISysSensitiveWordLogQueryService;
import org.dromara.sensitive.service.ISysSensitiveWordLogService;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sensitive/log")
public class SysSensitiveWordLogController extends BaseController {

    // 原有：查询服务（负责列表、导出）
    private final ISysSensitiveWordLogQueryService sensitiveWordLogQueryService;
    // 新增：操作服务（负责删除）
    private final ISysSensitiveWordLogService sensitiveWordLogService;

    /**
     * 分页查询敏感词日志
     */
    @SaCheckPermission("sensitive:log:list")
    @GetMapping("/list")
    public TableDataInfo<SysSensitiveWordLogVo> list(SysSensitiveWordLogBo bo) {
        return sensitiveWordLogQueryService.selectPageList(bo);
    }

    /**
     * 导出敏感词日志
     */
    @Log(title = "敏感词日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("sensitive:log:export")
    @PostMapping("/export")
    public void export(SysSensitiveWordLogBo bo, HttpServletResponse response) {
        List<SysSensitiveWordLogVo> list = sensitiveWordLogQueryService.selectList(bo);
        ExcelUtil.exportExcel(list, "敏感词日志", SysSensitiveWordLogVo.class, response);
    }

    /**
     * 批量删除敏感词日志（匹配前端请求路径/sensitive/log/remove）
     */
    @SaCheckPermission("sensitive:log:remove")
    @Log(title = "敏感词日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove")
    public R<Boolean> remove(@RequestBody Collection<Long> ids) {
        // 调用服务层删除方法，确保日志删除后逻辑正确
        return R.ok(sensitiveWordLogService.deleteWithValidByIds(ids));
    }

}
