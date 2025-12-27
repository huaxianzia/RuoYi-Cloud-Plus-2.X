package org.dromara.common.mybatis.config;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.handlers.PostInitTableInfoHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mybatis.aspect.DataPermissionPointcutAdvisor;
import org.dromara.common.mybatis.handler.InjectionMetaObjectHandler;
import org.dromara.common.mybatis.handler.MybatisExceptionHandler;
import org.dromara.common.mybatis.handler.PlusPostInitTableInfoHandler;
import org.dromara.common.mybatis.interceptor.PlusDataPermissionInterceptor;
import org.dromara.common.mybatis.service.SysDataScopeService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis-plus配置类（已删除重复Bean，解决冲突）
 *
 * @author Lion Li
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("${mybatis-plus.mapperPackage}")
@PropertySource(value = "classpath:common-mybatis.yml", factory = YmlPropertySourceFactory.class)
public class MybatisPlusConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件 必须放到第一位（复用项目原有Bean，不再重复定义）
        try {
            TenantLineInnerInterceptor tenant = SpringUtils.getBean(TenantLineInnerInterceptor.class);
            interceptor.addInnerInterceptor(tenant);
        } catch (BeansException ignore) {
        }
        // 数据权限处理
        interceptor.addInnerInterceptor(dataPermissionInterceptor());
        // 分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        // 乐观锁插件
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 数据权限拦截器
     */
    public PlusDataPermissionInterceptor dataPermissionInterceptor() {
        return new PlusDataPermissionInterceptor();
    }

    /**
     * 数据权限切面处理器
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataPermissionPointcutAdvisor dataPermissionPointcutAdvisor() {
        return new DataPermissionPointcutAdvisor();
    }

    /**
     * 分页插件，自动识别数据库类型
     */
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        // 分页合理化
        paginationInnerInterceptor.setOverflow(true);
        return paginationInnerInterceptor;
    }

    /**
     * 乐观锁插件
     */
    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    /**
     * 元对象字段填充控制器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new InjectionMetaObjectHandler();
    }

    /**
     * 使用网卡信息绑定雪花生成器
     * 防止集群雪花ID重复
     */
    @Bean
    public IdentifierGenerator idGenerator() {
        return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
    }

    /**
     * 异常处理器
     */
    @Bean
    public MybatisExceptionHandler mybatisExceptionHandler() {
        return new MybatisExceptionHandler();
    }

    /**
     * 数据权限处理实现
     */
    @Bean("sdss")
    public SysDataScopeService sysDataScopeService() {
        return new SysDataScopeService();
    }

    /**
     * 初始化表对象处理器
     */
    @Bean
    public PostInitTableInfoHandler postInitTableInfoHandler() {
        return new PlusPostInitTableInfoHandler();
    }

}
