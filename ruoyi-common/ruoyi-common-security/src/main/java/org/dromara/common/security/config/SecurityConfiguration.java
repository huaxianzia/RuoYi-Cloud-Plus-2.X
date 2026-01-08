package org.dromara.common.security.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.util.SaResult;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限安全配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class SecurityConfiguration implements WebMvcConfigurer {

    /**
     * 注册sa-token的拦截器（修复SaRouter.free()传参问题）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则（忽略rabbit相关接口）
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 核心：新版Sa-Token的free()需要传入空函数参数，实现放行
            SaRouter.match("/rabbit/**")
                .free(r -> {}) // 传入空函数，适配新版语法：放行该路径，不执行任何校验
                .back();
        })).addPathPatterns("/**");
    }

    /**
     * 校验是否从网关转发（添加rabbit接口到放行列表）
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
            // 拦截所有请求
            .addInclude("/**")
            // 放行路径：actuator + rabbit相关接口
            .addExclude(
                "/actuator", "/actuator/**",
                "/rabbit/send", "/rabbit/sendDelay"
            )
            .setAuth(obj -> {
                if (SaManager.getConfig().getCheckSameToken()) {
                    SaSameUtil.checkCurrentRequestToken();
                }
            })
            .setError(e -> SaResult.error("认证失败，无法访问系统资源").setCode(HttpStatus.UNAUTHORIZED));
    }

    /**
     * 对 actuator 健康检查接口 做账号密码鉴权
     */
    @Bean
    public SaServletFilter actuatorFilter() {
        String username = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.username");
        String password = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.userpassword");
        return new SaServletFilter()
            .addInclude("/actuator", "/actuator/**")
            .setAuth(obj -> {
                SaHttpBasicUtil.check(username + ":" + password);
            })
            .setError(e -> SaResult.error(e.getMessage()).setCode(HttpStatus.UNAUTHORIZED));
    }

}
