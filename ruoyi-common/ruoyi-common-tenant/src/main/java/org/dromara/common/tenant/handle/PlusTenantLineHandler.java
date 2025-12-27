package org.dromara.common.tenant.handle;

import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.tenant.properties.TenantProperties;

import java.util.List;

/**
 * 自定义租户处理器（已添加敏感词日志表的忽略逻辑）
 *
 * @author Lion Li
 */
@Slf4j
@AllArgsConstructor
public class PlusTenantLineHandler implements TenantLineHandler {

    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.error("无法获取有效的租户id -> Null");
            return new NullValue();
        }
        // 返回固定租户
        return new StringValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String tenantId = TenantHelper.getTenantId();
        // 判断是否有租户
        if (StringUtils.isNotBlank(tenantId)) {
            // 不需要过滤租户的表（添加sys_sensitive_word_log到忽略列表）
            List<String> tables = ListUtil.toList(
                "gen_table",
                "gen_table_column",
                "sys_sensitive_word_log" // 关键：添加敏感词日志表，忽略多租户处理
            );
            // 合并配置文件中的排除表
            tables.addAll(tenantProperties.getExcludes());
            // 忽略表名匹配（不区分大小写）
            return StringUtils.equalsAnyIgnoreCase(tableName, tables.toArray(new String[0]));
        }
        // 无租户信息时，所有表都忽略多租户处理
        return true;
    }

}
