package com.hpwit.modules.flowable.service;

import cn.dev33.satoken.stp.StpUtil;
import com.hpwit.common.constant.SysConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.hpwit.modules.flowable.service.ApprovalBpmn.require;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
public class ApprovalAccess {

    public static final String MODEL = "flowable:model";
    public static final String WORK = "flowable:work";
    public static final String PAGES = "flowable:pages";
    public static final String ADMIN = "flowable:admin";
    public static final String MONITOR = "flowable:monitor";

    private final ApprovalStore store;

    public record Actor(long id, String name, List<String> roles) {
        String engineId() {
            return Long.toString(id);
        }

        ApprovalStore.User user() {
            return new ApprovalStore.User(id, name);
        }
    }

    public Actor current(String permission) {
        var actor = current();
        require(can(actor, permission), "当前用户没有审批模块的相应权限");
        return actor;
    }

    public Actor current() {
        long id = StpUtil.getLoginIdAsLong();
        var user = store.user(id).orElseThrow(() -> new ApprovalException("当前系统用户不可用"));
        return new Actor(id, user.name(), store.roles(id));
    }

    public boolean can(Actor actor, String permission) {
        return actor.roles().stream().anyMatch(SysConstant.ROOT_ROLE_CODE::equalsIgnoreCase)
                || store.hasPermission(actor.id(), permission);
    }
}
