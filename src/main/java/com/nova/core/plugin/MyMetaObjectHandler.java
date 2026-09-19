package com.nova.core.plugin;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * mybatis-plus 字段自动填充类
 * @Author huangpw
 * @Date 2025/11/21 9:59
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 新增填充创建时间
     * @param metaObject 元数据
     */
    @Override
    public void insertFill(MetaObject metaObject) {

        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 自动填充 createTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);

        // 自动填充 updateTime
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 自动填充 userId（非 Web 线程无 Sa-Token 上下文，视为未登录）
        if(SaHolder.getContext().isValid() && StpUtil.isLogin()){
            //Long userId = StpUtil.getLoginIdAsLong();
            //this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            //this.strictInsertFill(metaObject, "updateBy", Long.class, userId);

            Object loginId = StpUtil.getLoginId();
            Long userId = null;
            if (loginId instanceof Number) {
                userId = ((Number) loginId).longValue();
            }
            else if (loginId instanceof String value) {
                if (!value.isEmpty() && value.chars().allMatch(Character::isDigit)) {
                    userId = Long.parseLong(value);
                }
            }
            if(userId != null){
                this.strictInsertFill(metaObject, "createBy", Long.class, userId);
                this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
            }
        }

    }

    /**
     * 更新填充修改时间
     * @param metaObject 元数据
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        // 自动填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 自动填充修改人（非 Web 线程无 Sa-Token 上下文，视为未登录）
        if(SaHolder.getContext().isValid() && StpUtil.isLogin()){
            //Long userId = StpUtil.getLoginIdAsLong();
            //this.strictInsertFill(metaObject, "updateBy", Long.class, userId);

            Object loginId = StpUtil.getLoginId();
            Long userId = null;
            if (loginId instanceof Number) {
                userId = ((Number) loginId).longValue();
            }
            else if (loginId instanceof String value) {
                // 不用正则 性能更好 更优雅
                if (!value.isEmpty() && value.chars().allMatch(Character::isDigit)) {
                    userId = Long.parseLong(value);
                }
            }
            if(userId != null){
                this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }
        }
    }
}
