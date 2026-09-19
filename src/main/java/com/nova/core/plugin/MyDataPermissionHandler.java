package com.nova.core.plugin;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.nova.common.base.BaseEnum;
import com.nova.common.enums.DataScopeEnum;
import com.nova.core.annotation.DataPermission;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 数据权限控制器
 *
 * @author zc
 * @since 2021-12-10 13:28
 */
@Slf4j
public class MyDataPermissionHandler implements DataPermissionHandler {

    /**
     * 构建过滤条件
     *
     * @param where 当前查询条件
     * @return 构建后查询条件
     */
    @SneakyThrows
    public static Expression dataScopeFilter(String deptAlias, String deptIdColumnName, String userAlias, String userIdColumnName,DataScopeEnum dataScopeEnum, Expression where) {

        // 获取部门和用户的别名
        String deptColumnName = StrUtil.isNotBlank(deptAlias) ? (deptAlias + StringPool.DOT + deptIdColumnName) : deptIdColumnName;
        String userColumnName = StrUtil.isNotBlank(userAlias) ? (userAlias + StringPool.DOT + userIdColumnName) : userIdColumnName;

        Long userId = StpUtil.getLoginIdAsLong();
        Long deptId = (Long) StpUtil.getSession().get("deptId");

        String appendSqlStr;
        switch (dataScopeEnum) {
            case ALL:
                return where;
            case DEPT:
                appendSqlStr = deptId == null ? "1 = 0" : deptColumnName + StringPool.EQUALS + deptId;
                break;
            case SELF:
                appendSqlStr = userColumnName + StringPool.EQUALS + userId;
                break;
            // 默认部门及子部门数据权限
            default:
                appendSqlStr = deptId == null ? "1 = 0" : deptColumnName + " IN ( SELECT id FROM sys_dept WHERE id = " + deptId + " OR FIND_IN_SET( " + deptId + " , tree_path ) )";
                break;
        }

        if (StrUtil.isBlank(appendSqlStr)) {
            return where;
        }

        Expression appendExpression = CCJSqlParserUtil.parseCondExpression(appendSqlStr);

        if (where == null) {
            return appendExpression;
        }

        return new AndExpression(where, appendExpression);
    }

    /**
     * 获取数据权限的sql片段
     * @param where 查询条件
     * @param mappedStatementId mapper接口方法的全路径
     * @return sql片段
     */
    @Override
    @SneakyThrows
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 如果是未登录，或者是定时任务执行的SQL，或者是超级管理员，直接返回；
        // 非 Web 线程（启动加载、定时任务等）没有 Sa-Token 上下文，同样视为未登录，否则 isLogin 会抛 SaTokenContextException
        if (!SaHolder.getContext().isValid() || !StpUtil.isLogin()) {
            return where;
        }
        // 正在登录中
        Map<String, Object> dataMap = StpUtil.getSession().getDataMap();
        if(dataMap == null || dataMap.isEmpty()) {
            return where;
        }
        // 判断是否是超级管理员
        Boolean isRoot = (Boolean) dataMap.get("isRoot");
        if(isRoot) {
            return where;
        }

        log.info("StpUtil -> {}", StpUtil.getSession().getDataMap().toString());

        // 获取当前用户的数据权限
        Long dataScope = (Long) dataMap.get("dataScope");
        if(dataScope == null) {
            return where;
        }
        DataScopeEnum dataScopeEnum = BaseEnum.getEnumByValue(dataScope.intValue(), DataScopeEnum.class);

        // 如果是全部数据权限，直接返回
        if (DataScopeEnum.ALL.equals(dataScopeEnum)) {
            return where;
        }

        // 获取当前执行的接口类
        Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(StringPool.DOT)));

        // 获取当前执行的方法名称
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringPool.DOT) + 1);

        // 获取当前执行的接口类里所有的方法
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            //找到当前执行的方法
            if (method.getName().equals(methodName)) {
                DataPermission annotation = method.getAnnotation(DataPermission.class);
                // 判断当前执行的方法是否有权限注解，如果没有注解直接返回
                if (annotation == null ) {
                    return where;
                }
                // deptAlias:部门别名,deptIdColumnName:部门ID字段,userAlias:用户别名,userIdColumnName:用户ID字段
                return dataScopeFilter(annotation.deptAlias(), annotation.deptIdColumnName(), annotation.userAlias(), annotation.userIdColumnName(), dataScopeEnum,where);
            }
        }
        return where;
    }


}

