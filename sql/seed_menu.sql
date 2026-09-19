-- ============================================================
-- zyxhit-admin-boot 菜单/角色/权限 seed
-- 对应前端 zyxhit-admin 的 routes.static.ts 现有菜单结构
-- 重跑前请先清表：TRUNCATE sys_role_menu; TRUNCATE sys_user_role;
--                  DELETE FROM sys_menu; DELETE FROM sys_role;
-- 适用 MySQL 8.x（utf8mb4 / InnoDB）
-- ============================================================

-- ----- 1. sys_role 角色表 -----
CREATE TABLE IF NOT EXISTS sys_role (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    name         VARCHAR(64)  NOT NULL COMMENT '角色名称',
    code         VARCHAR(64)  NOT NULL COMMENT '角色编码',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    data_scope   TINYINT      NOT NULL DEFAULT 1 COMMENT '1全部 2本部门 3本部门及下 4本部门及上 5仅本人 6自定义',
    is_deleted   TINYINT      NOT NULL DEFAULT 0,
    create_by    BIGINT                DEFAULT NULL,
    update_by    BIGINT                DEFAULT NULL,
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----- 2. sys_menu 菜单表 -----
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id       BIGINT        NOT NULL DEFAULT 0 COMMENT '父菜单ID，0=根',
    name            VARCHAR(64)   NOT NULL COMMENT '菜单名称',
    type            TINYINT       NOT NULL COMMENT '1菜单 2目录 3外链 4按钮',
    route_name      VARCHAR(64)            DEFAULT NULL COMMENT '路由name',
    route_path      VARCHAR(255)           DEFAULT NULL COMMENT '路由path',
    component       VARCHAR(255)           DEFAULT NULL COMMENT '组件路径',
    perm            VARCHAR(128)           DEFAULT NULL COMMENT '权限标识',
    visible         TINYINT       NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
    sort            INT           NOT NULL DEFAULT 0 COMMENT '排序',
    icon            VARCHAR(64)            DEFAULT NULL,
    redirect        VARCHAR(255)           DEFAULT NULL,
    tree_path       VARCHAR(255)           DEFAULT NULL COMMENT '祖先路径，逗号分隔',
    keep_alive      TINYINT       NOT NULL DEFAULT 0 COMMENT '1开启缓存',
    always_show     TINYINT       NOT NULL DEFAULT 0 COMMENT '目录始终显示',
    requires_auth   TINYINT       NOT NULL DEFAULT 1 COMMENT '1需要登录',
    params          VARCHAR(1024)          DEFAULT NULL COMMENT 'JSON 路由参数',
    is_deleted      TINYINT       NOT NULL DEFAULT 0,
    create_time     DATETIME               DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ----- 3. sys_role_menu 角色-菜单关联 -----
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- ----- 4. sys_user_role 用户-角色关联 -----
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ============================================================
-- 菜单 seed（id 固定便于关联，type=1菜单 2目录 3外链 4按钮）
-- 完整还原前端 routes.static.ts 8 个一级菜单 + 个人中心
-- ============================================================
INSERT INTO sys_menu
  (id, parent_id, name, type, route_name, route_path, component, visible, sort, icon, redirect, keep_alive, always_show, requires_auth, tree_path)
VALUES
  -- 仪表盘
  (1,    0, '仪表盘',          2, 'dashboard',      NULL,                    NULL,                                  1, 1,  'icon-park-outline:analysis',         NULL, 0, 0, 1, '0'),
  (101,  1, '工作台',          1, 'workbench',      '/dashboard/workbench',  '/dashboard/workbench/index.vue',       1, 1,  'icon-park-outline:alarm',            NULL, 0, 0, 1, '0,1'),
  (102,  1, '监控页',          1, 'monitor',        '/dashboard/monitor',    '/dashboard/monitor/index.vue',         1, 2,  'icon-park-outline:anchor',           NULL, 0, 0, 1, '0,1'),

  -- 多级菜单演示
  (2,    0, '多级菜单演示',    2, 'multi',          NULL,                    NULL,                                  1, 2,  'icon-park-outline:list',             NULL, 0, 0, 1, '0'),
  (201,  2, '多级菜单子页',    1, 'multi2',         '/multi/multi-2',        '/demo/multi/multi-2/index.vue',       1, 1,  'icon-park-outline:list',             NULL, 0, 0, 1, '0,2'),
  (20101,2, '菜单详情页',      1, 'multi2-detail',  '/multi/multi-2/detail','/demo/multi/multi-2/detail/index.vue', 0, 2,  'icon-park-outline:list',             NULL, 0, 0, 0, '0,2'),
  (202,  2, '多级菜单',        2, 'multi3',         '/multi/multi-3',        NULL,                                  1, 3,  'icon-park-outline:list',             NULL, 0, 0, 1, '0,2'),
  (20201,202,'多级菜单3-1',    1, 'multi4',         '/multi/multi-3/multi-4','/demo/multi/multi-3/multi-4/index.vue', 1, 1,  'icon-park-outline:list',             NULL, 0, 0, 1, '0,2,202'),

  -- 列表页
  (3,    0, '列表页',          2, 'list',           NULL,                    NULL,                                  1, 3,  'icon-park-outline:list-two',         NULL, 0, 0, 1, '0'),
  (301,  3, '常用列表',        1, 'commonList',     '/list/common-list',     '/demo/list/common-list/index.vue',    1, 1,  'icon-park-outline:list-view',        NULL, 0, 0, 1, '0,3'),
  (302,  3, '卡片列表',        1, 'cardList',       '/list/card-list',       '/demo/list/card-list/index.vue',      1, 2,  'icon-park-outline:view-grid-list',   NULL, 0, 0, 1, '0,3'),
  (303,  3, '拖拽列表',        1, 'draggableList',  '/list/draggable-list',  '/demo/list/draggable-list/index.vue', 1, 3,  'icon-park-outline:menu-fold',        NULL, 0, 0, 1, '0,3'),

  -- 功能示例
  (4,    0, '功能示例',        2, 'demo',           NULL,                    NULL,                                  1, 4,  'icon-park-outline:application-one',  NULL, 0, 0, 1, '0'),
  (401,  4, '请求示例',        1, 'fetch',          '/demo/fetch',           '/demo/fetch/index.vue',               1, 1,  'icon-park-outline:international',    NULL, 0, 0, 1, '0,4'),
  (402,  4, 'ECharts',         1, 'echarts',        '/demo/echarts',         '/demo/echarts/index.vue',             1, 2,  'icon-park-outline:chart-proportion', NULL, 0, 0, 1, '0,4'),
  (403,  4, '地图',            1, 'map',            '/demo/map',             '/demo/map/index.vue',                 1, 3,  'carbon:map',                         NULL, 1, 0, 1, '0,4'),
  (404,  4, '编辑器',          2, 'editor',         '/demo/editor',          NULL,                                  1, 4,  'icon-park-outline:editor',           NULL, 0, 0, 1, '0,4'),
  (40401,404,'MarkDown',       1, 'editorMd',       '/demo/editor/md',       '/demo/editor/md/index.vue',           1, 1,  'ri:markdown-line',                   NULL, 0, 0, 1, '0,4,404'),
  (40402,404,'富文本',         1, 'editorRich',     '/demo/editor/rich',     '/demo/editor/rich/index.vue',         1, 2,  'icon-park-outline:edit-one',         NULL, 0, 0, 1, '0,4,404'),
  (405,  4, '剪贴板',          1, 'clipboard',      '/demo/clipboard',       '/demo/clipboard/index.vue',           1, 5,  'icon-park-outline:clipboard',        NULL, 0, 0, 1, '0,4'),
  (406,  4, '图标',            1, 'icons',          '/demo/icons',           '/demo/icons/index.vue',               1, 6,  'local:cool',                         NULL, 0, 0, 1, '0,4'),
  (407,  4, '二维码',          1, 'QRCode',         '/demo/qr-code',         '/demo/qr-code/index.vue',             1, 7,  'icon-park-outline:two-dimensional-code', NULL, 0, 0, 1, '0,4'),
  (408,  4, '省市区联动',      1, 'cascader',       '/demo/cascader',        '/demo/cascader/index.vue',            1, 8,  'icon-park-outline:add-subset',       NULL, 0, 0, 1, '0,4'),
  (409,  4, '字典示例',        1, 'dict',           '/demo/dict',            '/demo/dict/index.vue',                1, 9,  'icon-park-outline:book-one',         NULL, 0, 0, 1, '0,4'),

  -- 外链文档
  (5,    0, '外链文档',        2, 'documents',      NULL,                    NULL,                                  1, 5,  'icon-park-outline:file-doc',         NULL, 0, 0, 1, '0'),
  (501,  5, 'Vue',             1, 'documentsVue',   '/documents/vue',        '/demo/documents/vue/index.vue',       1, 1,  'logos:vue',                          NULL, 0, 0, 1, '0,5'),
  (502,  5, 'Vite',            1, 'documentsVite',  '/documents/vite',       '/demo/documents/vite/index.vue',      1, 2,  'logos:vitejs',                       NULL, 0, 0, 1, '0,5'),
  (503,  5, 'VueUse（外链）',  3, 'documentsVueuse','/documents/vue-use',    NULL,                                  1, 3,  'logos:vueuse',                       NULL, 0, 0, 1, '0,5'),
  (504,  5, 'Nova docs',       3, 'documentsNova',  '/documents/nova',       NULL,                                  1, 4,  'local:logo',                         NULL, 0, 0, 1, '0,5'),
  (505,  5, '公共示例页（外链）', 3, 'documentsPublic', '/documents/public',  NULL,                                  1, 5,  'local:logo',                         NULL, 0, 0, 1, '0,5'),

  -- 权限
  (6,    0, '权限',            2, 'permission',     NULL,                    NULL,                                  1, 6,  'icon-park-outline:people-safe',      NULL, 0, 0, 1, '0'),
  (601,  6, '权限示例',        1, 'permissionDemo', '/permission/permission','/demo/permission/permission/index.vue',1,1,'icon-park-outline:right-user',     NULL, 0, 0, 1, '0,6'),
  (602,  6, 'super可见',      1, 'justSuper',      '/permission/just-super','/demo/permission/just-super/index.vue',1,2,'icon-park-outline:wrong-user',   NULL, 0, 0, 1, '0,6'),

  -- 系统设置
  (7,    0, '系统设置',        2, 'setting',        NULL,                    NULL,                                  1, 7,  'icon-park-outline:setting',          NULL, 0, 0, 1, '0'),
  (701,  7, '用户设置',        1, 'accountSetting', '/setting/account',      '/setting/account/index.vue',         1, 1,  'icon-park-outline:user',             NULL, 0, 0, 1, '0,7'),
  (702,  7, '字典设置',        1, 'dictionarySetting','/setting/dictionary', '/setting/dictionary/index.vue',      1, 2,  'icon-park-outline:book-one',         NULL, 0, 0, 1, '0,7'),
  (703,  7, '菜单设置',        1, 'menuSetting',    '/setting/menu',         '/setting/menu/index.vue',             1, 3,  'icon-park-outline:application-menu', NULL, 0, 0, 1, '0,7'),

  -- 关于
  (8,    0, '关于',            1, 'about',          '/about',                '/demo/about/index.vue',               1, 8,  'icon-park-outline:info',             NULL, 0, 0, 1, '0'),

  -- 系统管理
  (10,   0, '系统管理',        2, 'system',         NULL,                    NULL,                                  1, 9,  'icon-park-outline:setting-two',      NULL, 0, 0, 1, '0'),
  (1001, 10, '用户管理',       1, 'systemUser',     '/system/user',          '/system/user/index.vue',              1, 1,  'icon-park-outline:every-user',       NULL, 0, 0, 1, '0,10'),
  (1002, 10, '角色管理',       1, 'systemRole',     '/system/role',          '/system/role/index.vue',              1, 2,  'icon-park-outline:user-positioning', NULL, 0, 0, 1, '0,10'),
  (1003, 10, '菜单管理',       1, 'systemMenu',     '/system/menu',          '/system/menu/index.vue',              1, 3,  'icon-park-outline:application-menu', NULL, 0, 0, 1, '0,10'),
  (1004, 10, '部门管理',       1, 'systemDept',     '/system/dept',          '/system/dept/index.vue',              1, 4,  'icon-park-outline:branch-one',       NULL, 0, 0, 1, '0,10'),
  (1005, 10, '字典管理',       1, 'systemDict',     '/system/dict',          '/system/dict/index.vue',              1, 5,  'icon-park-outline:book-one',         NULL, 0, 0, 1, '0,10'),
  (1006, 10, '操作日志',       1, 'systemLog',      '/system/log',           '/system/log/index.vue',               1, 6,  'icon-park-outline:log',              NULL, 0, 0, 1, '0,10'),
  (1007, 10, '系统配置',       1, 'systemConfig',   '/system/config',        '/system/config/index.vue',            1, 7,  'icon-park-outline:config',           NULL, 0, 0, 1, '0,10'),
  (1008, 10, '通知公告',       1, 'systemNotice',   '/system/notice',        '/system/notice/index.vue',            1, 8,  'icon-park-outline:volume-notice',    NULL, 0, 0, 1, '0,10'),

  -- 个人中心（隐藏）
  (999,  0, '个人中心',        1, 'userCenter',     '/user-center',          '/build-in/user-center/index.vue',     0, 99, 'carbon:user-avatar-filled-alt',     NULL, 0, 0, 1, '0');

-- 外链的 route_path 改成实际跳转地址（CurrentMenuDto 转换时 link 类型会复制到 href）
UPDATE sys_menu SET route_path = 'https://vueuse.org/guide/'           WHERE id = 503;
UPDATE sys_menu SET route_path = 'https://zyxhit-admin-docs.netlify.app/' WHERE id = 504;
UPDATE sys_menu SET route_path = '/public'                              WHERE id = 505;

-- ============================================================
-- 角色 seed
-- ============================================================
INSERT INTO sys_role (id, name, code, sort, status, data_scope) VALUES
  (1, '超级管理员', 'super',  1, 1, 1),
  (2, '普通用户',   'user',   2, 1, 1),
  (3, '测试员',     'tester', 3, 1, 1);

-- ============================================================
-- 角色-菜单关联
-- super：所有菜单
-- user：仪表盘 + 个人中心
-- tester：仪表盘 + 字典示例 + 系统设置 + 个人中心（不能进系统管理）
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE is_deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (2, 1), (2, 101), (2, 102), (2, 999);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (3, 1), (3, 101), (3, 102),  -- 仪表盘
  (3, 409),                    -- 字典示例
  (3, 7), (3, 701), (3, 702), (3, 703),  -- 系统设置
  (3, 999);                    -- 个人中心

-- ============================================================
-- 用户-角色关联
-- 前提：sys_user 表里已经有 userId=1/2/3 的用户（自己 seed）
-- 如果没有，把 user_id 改成实际已有的 id
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3);
