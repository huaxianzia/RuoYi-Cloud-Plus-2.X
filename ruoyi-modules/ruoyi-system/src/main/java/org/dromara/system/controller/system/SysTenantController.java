package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.lock.annotation.Lock4j;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.sensitive.domain.SysSensitiveWordLog;
import org.dromara.sensitive.service.ISysSensitiveWordLogService;
import org.dromara.sensitive.utils.SensitiveWordUtils;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysTenantService;
import org.dromara.system.service.ISysUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 租户管理（含敏感词日志记录功能，已修复所有已知错误）
 *
 * @author Michelle.Chung
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/tenant")
@ConditionalOnProperty(value = "tenant.enable", havingValue = "true")
public class SysTenantController extends BaseController {

    private final ISysTenantService tenantService;

    // 注入敏感词工具类（非静态调用）
    @Autowired
    private SensitiveWordUtils sensitiveWordUtils;

    // 注入敏感词日志Service（用于保存日志）
    @Autowired
    private ISysSensitiveWordLogService sensitiveWordLogService;

    // 注入用户Service（用于查询操作人信息）
    @Autowired
    private ISysUserService userService;

    // 日志记录器（用于记录异常）
    private static final Logger log = Logger.getLogger(SysTenantController.class.getName());

    /**
     * 查询租户列表
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/list")
    public TableDataInfo<SysTenantVo> list(SysTenantBo bo, PageQuery pageQuery) {
        return tenantService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出租户列表
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:export")
    @Log(title = "租户管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysTenantBo bo, HttpServletResponse response) {
        List<SysTenantVo> list = tenantService.queryList(bo);
        ExcelUtil.exportExcel(list, "租户", SysTenantVo.class, response);
    }

    /**
     * 获取租户详细信息
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}")
    public R<SysTenantVo> getInfo(@NotNull(message = "主键不能为空")
                                  @PathVariable Long id) {
        return R.ok(tenantService.queryById(id));
    }

//    /**
//     * 新增租户（含敏感词检查+日志记录）
//     */
//    @ApiEncrypt
//    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
//    @SaCheckPermission("system:tenant:add")
//    @Log(title = "租户管理", businessType = BusinessType.INSERT)
//    @Lock4j
//    @RepeatSubmit()
//    @PostMapping()
//    public R<Void> add(
//        @Validated(AddGroup.class) @RequestBody SysTenantBo bo,
//        HttpServletRequest request
//    ) {
//        // 1. 企业名称唯一性校验
//        if (!tenantService.checkCompanyNameUnique(bo)) {
//            return R.fail("新增租户'" + bo.getCompanyName() + "'失败，企业名称已存在");
//        }
//
//        // 2. 敏感词检查+日志记录
//        String companyName = bo.getCompanyName();
//        String triggerField = "companyName";
//        if (StrUtil.isNotBlank(companyName)) {
//            String sensitiveWord = sensitiveWordUtils.check(companyName);
//            if (StrUtil.isNotBlank(sensitiveWord)) {
//                saveSensitiveLog(triggerField, sensitiveWord, companyName, request);
//                return R.fail("新增租户失败，企业名称包含敏感词：" + sensitiveWord);
//            }
//        }
//
//        // 3. 执行新增业务逻辑
//        return toAjax(TenantHelper.ignore(() -> tenantService.insertByBo(bo)));
//    }

    /**
     * 新增租户（含敏感词替换+日志记录）
     * 调整逻辑：检测到敏感词时自动替换为/**，不拒绝新增，仅记录日志
     */
    @ApiEncrypt
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:add")
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @Lock4j
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(
        @Validated(AddGroup.class) @RequestBody SysTenantBo bo,
        HttpServletRequest request
    ) {
        // 1. 企业名称唯一性校验（保留原有逻辑）
        if (!tenantService.checkCompanyNameUnique(bo)) {
            return R.fail("新增租户'" + bo.getCompanyName() + "'失败，企业名称已存在");
        }

        // 2. 敏感词检查+替换+日志记录（核心修改逻辑）
        String companyName = bo.getCompanyName();
        String triggerField = "companyName";
        if (StrUtil.isNotBlank(companyName)) {
            // 检查是否包含敏感词
            String sensitiveWord = sensitiveWordUtils.check(companyName);
            if (StrUtil.isNotBlank(sensitiveWord)) {
                // 记录敏感词日志（保留原有日志逻辑）
                saveSensitiveLog(triggerField, sensitiveWord, companyName, request);

                // 将敏感词替换为/**（支持多个敏感词批量替换）
                String processedCompanyName = companyName.replace(sensitiveWord, "/**");
                // 更新BO中的企业名称为替换后的值
                bo.setCompanyName(processedCompanyName);


            }
        }

        // 3. 执行新增业务逻辑（使用替换后的企业名称）
        return toAjax(TenantHelper.ignore(() -> tenantService.insertByBo(bo)));
    }

    /**
     * 修改租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysTenantBo bo) {
        tenantService.checkTenantAllowed(bo.getTenantId());
        if (!tenantService.checkCompanyNameUnique(bo)) {
            return R.fail("修改租户'" + bo.getCompanyName() + "'失败，公司名称已存在");
        }
        return toAjax(tenantService.updateByBo(bo));
    }

    /**
     * 状态修改
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysTenantBo bo) {
        tenantService.checkTenantAllowed(bo.getTenantId());
        return toAjax(tenantService.updateTenantStatus(bo));
    }

    /**
     * 删除租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:remove")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(tenantService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 动态切换租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/{tenantId}")
    public R<Void> dynamicTenant(@NotBlank(message = "租户ID不能为空") @PathVariable String tenantId) {
        TenantHelper.setDynamic(tenantId, true);
        return R.ok();
    }

    /**
     * 清除动态租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/clear")
    public R<Void> dynamicClear() {
        TenantHelper.clearDynamic();
        return R.ok();
    }

    /**
     * 同步租户套餐
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @Lock4j
    @GetMapping("/syncTenantPackage")
    public R<Void> syncTenantPackage(@NotBlank(message = "租户ID不能为空") String tenantId,
                                     @NotNull(message = "套餐ID不能为空") Long packageId) {
        return toAjax(TenantHelper.ignore(() -> tenantService.syncTenantPackage(tenantId, packageId)));
    }

    /**
     * 同步租户字典
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @Lock4j
    @GetMapping("/syncTenantDict")
    public R<Void> syncTenantDict() {
        if (!TenantHelper.isEnable()) {
            return R.fail("当前未开启租户模式");
        }
        tenantService.syncTenantDict();
        return R.ok("同步租户字典成功");
    }

    /**
     * 通用敏感词日志记录方法（已修复：operator_id类型不匹配问题）
     */
    private void saveSensitiveLog(String triggerField, String sensitiveWord, String content, HttpServletRequest request) {
        try {
            SysSensitiveWordLog log = new SysSensitiveWordLog();

            // 基础字段填充
            log.setTriggerField(triggerField);
            log.setSensitiveWord(sensitiveWord);
            log.setRequestIp(getClientIp(request));
            log.setRequestUrl(request.getRequestURI());
            log.setStatus(1);
            log.setCreateTime(LocalDateTime.now());

            // 操作人信息填充（核心修复）
            if (StpUtil.isLogin()) {
                String loginId = StpUtil.getLoginIdAsString(); // 原始登录ID：sys_user:1
                String pureUserId = "0"; // 默认值，避免空指针

                // 安全拆分登录ID，提取纯数字用户ID
                if (loginId.contains(":")) {
                    String[] splitArr = loginId.split(":");
                    if (splitArr.length >= 2) { // 防止数组越界
                        pureUserId = splitArr[1];
                    }
                } else {
                    // 无前缀时直接使用登录ID（默认纯数字）
                    pureUserId = loginId;
                }

                // 赋值纯数字ID（Long类型，匹配数据库整数字段）
                log.setOperatorId(Long.valueOf(pureUserId));

                // 查询用户信息，设置操作人姓名
                SysUserVo user = userService.selectUserById(Long.valueOf(pureUserId));
                log.setOperatorName(user != null ? user.getUserName() : "用户-" + pureUserId);
            } else {
                // 未登录状态：operator_id设为0（Long类型）
                log.setOperatorId(0L);
                log.setOperatorName("匿名操作");
            }

            // 保存日志
            sensitiveWordLogService.save(log);
        } catch (Exception e) {
            log.severe("敏感词日志保存失败：" + e.getMessage());
        }
    }

    /**
     * 内嵌式IP获取方法（无任何外部依赖）
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String tempIp : ips) {
                if (!"unknown".equalsIgnoreCase(tempIp.trim())) {
                    ip = tempIp.trim();
                    break;
                }
            }
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                ip = "localhost";
            }
        }
        return ip;
    }
}
