package org.dromara.sensitive.service;
import org.dromara.sensitive.domain.SysSensitiveWordLog;

import java.util.Collection;
import java.time.LocalDateTime; // 新增导入
import java.util.List;

public interface ISysSensitiveWordLogService {

    // 新增日志（不变）
    boolean save(SysSensitiveWordLog log);

    // 修正：createTime参数类型从String改为LocalDateTime（与实体类一致）
    SysSensitiveWordLog getById(Long id, LocalDateTime createTime);

    // 更新日志（不变）
    boolean update(SysSensitiveWordLog log);

    // 修正：createTime参数类型从String改为LocalDateTime
    boolean removeById(Long id, LocalDateTime createTime);

    // 条件查询列表（不变）
    List<SysSensitiveWordLog> list(SysSensitiveWordLog query);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
