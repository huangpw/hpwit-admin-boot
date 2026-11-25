package com.hpwit.core.plugin;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.hpwit.common.enums.DataScopeEnum;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class MyDataPermissionHandlerTest {

    @ParameterizedTest
    @CsvSource(value = {
            "SELF|7|u.create_by = 42",
            "SELF|NULL|u.create_by = 42",
            "DEPT|7|u.dept_id = 7",
            "DEPT|NULL|1 = 0",
            "DEPT_AND_SUB|7|u.dept_id IN (SELECT id FROM sys_dept WHERE id = 7 OR FIND_IN_SET(7, tree_path))",
            "DEPT_AND_SUB|NULL|1 = 0",
            "ALL|NULL|"
    }, delimiter = '|', nullValues = "NULL")
    void filtersUsingCurrentIdentity(DataScopeEnum scope, Long deptId, String expected) throws Exception {
        SaSession session = new SaSession("data-permission-test");
        if (deptId != null) {
            session.set("deptId", deptId);
        }
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getSession).thenReturn(session);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);

            Expression actual = MyDataPermissionHandler.dataScopeFilter(
                    "u", "dept_id", "u", "create_by", scope, null);
            assertEquals(expected == null ? null : CCJSqlParserUtil.parseCondExpression(expected).toString(),
                    actual == null ? null : actual.toString());

            Expression where = CCJSqlParserUtil.parseCondExpression("u.status = 1");
            Expression filtered = MyDataPermissionHandler.dataScopeFilter(
                    "u", "dept_id", "u", "create_by", scope, where);
            assertEquals(expected == null ? where.toString() : where + " AND " + actual, filtered.toString());
        }
    }
}
