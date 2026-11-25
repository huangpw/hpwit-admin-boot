package com.hpwit.modules.flowable.service;

import com.hpwit.modules.flowable.model.ApprovalData.BusinessPage;
import com.hpwit.modules.flowable.model.ApprovalData.Page;
import com.hpwit.modules.flowable.model.ApprovalData.SaveBusinessPage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.hpwit.modules.flowable.model.ApprovalData.DEMO_ORDER;
import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;
import static com.hpwit.modules.flowable.service.ApprovalStore.searchPattern;
import static com.hpwit.modules.flowable.service.ApprovalStore.text;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalPages {
    private static final String SELECT = "SELECT code, name, component, route_path, enabled, sort_order FROM flow_approval_business_page";
    private static final RowMapper<BusinessPage> PAGE = (rs, row) -> new BusinessPage(rs.getString("code"),
            rs.getString("name"), rs.getString("component"), rs.getString("route_path"),
            rs.getBoolean("enabled"), rs.getInt("sort_order"));

    private final JdbcClient jdbc;

    public Page<BusinessPage> list(int offset, int limit, String keyword) {
        Map<String, String> filters = text(keyword).isEmpty() ? Map.of()
                : Map.of("keyword", searchPattern(keyword).toLowerCase(Locale.ROOT));
        String where = filters.isEmpty() ? "" : """
                 WHERE LOWER(code) LIKE :keyword ESCAPE '!' OR LOWER(name) LIKE :keyword ESCAPE '!'
                """;
        long total = jdbc.sql("SELECT COUNT(*) FROM flow_approval_business_page" + where)
                .params(filters).query(Long.class).single();
        return new Page<>(jdbc.sql(SELECT + where + " ORDER BY sort_order, code LIMIT :limit OFFSET :offset")
                .params(filters).param("limit", limit).param("offset", offset).query(PAGE).list(), total);
    }

    public List<BusinessPage> enabled() {
        return jdbc.sql(SELECT + " WHERE enabled = TRUE ORDER BY sort_order, code").query(PAGE).list();
    }

    public BusinessPage resolve(String code) {
        if (code == null) {
            return null;
        }
        return jdbc.sql(SELECT + " WHERE code = ?").param(code).query(PAGE).optional().orElse(null);
    }

    BusinessPage legacy(String code) {
        if (DEMO_ORDER.equals(code)) {
            return new BusinessPage(DEMO_ORDER, "测试订单详情", "flowable/order/approval.vue", "/flowable/order", true, 0);
        }
        return resolve(code);
    }

    BusinessPage active(String code) {
        if (code == null) {
            return null;
        }
        BusinessPage page = resolve(code);
        require(page != null && page.enabled(), "关联业务页面不存在或已停用");
        return page;
    }

    public BusinessPage save(String code, SaveBusinessPage form) {
        require(!form.component().startsWith("/") && !form.component().contains("//"), "请输入相对于 src/views 的审批组件路径");
        try {
            if (code == null) {
                jdbc.sql("""
                        INSERT INTO flow_approval_business_page (code, name, component, route_path, enabled, sort_order)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """).params(form.code(), form.name().strip(), form.component(), text(form.routePath()),
                        form.enabled(), form.sortOrder()).update();
            } else {
                require(code.equals(form.code()), "页面编码创建后不可修改");
                int updated = jdbc.sql("""
                        UPDATE flow_approval_business_page SET name = ?, component = ?, route_path = ?, enabled = ?, sort_order = ?
                        WHERE code = ?
                        """).params(form.name().strip(), form.component(), text(form.routePath()), form.enabled(), form.sortOrder(), code)
                        .update();
                require(updated == 1, "业务页面不存在");
            }
        } catch (DuplicateKeyException e) {
            throw new ApprovalException("业务页面编码已存在");
        }
        return resolve(form.code());
    }
}
