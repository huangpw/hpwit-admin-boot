-- 在已有系统表的 MySQL 数据库执行。重复执行可补齐缺少的菜单项。
START TRANSACTION;

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, visible, sort, icon, tree_path, always_show, requires_auth)
SELECT 0, '流程管理', 2, 'flowable', '/flowable', 1, 70, 'icon-park-outline:connection-point-two', '0', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE route_name = 'flowable' OR route_path = '/flowable');

SET @flowable_menu_id = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu
    WHERE parent_id = 0 AND type = 2 AND route_name = 'flowable' AND route_path = '/flowable'
);

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, component, visible, sort, icon, tree_path, requires_auth)
SELECT @flowable_menu_id, routes.title, 1, routes.route_name, routes.route_path, routes.component, 1, routes.sort,
    routes.icon, CONCAT('0,', @flowable_menu_id), 1
FROM (
    SELECT '审批中心' AS title, 'flowableWork' AS route_name, '/flowable/work' AS route_path,
        '/flowable/work/index' AS component, 1 AS sort, 'icon-park-outline:checklist' AS icon
    UNION ALL SELECT '流程模型', 'flowableModels', '/flowable/models', '/flowable/models/index', 2, 'icon-park-outline:tree-diagram'
    UNION ALL SELECT '流程监控', 'flowableMonitor', '/flowable/monitor', '/flowable/monitor/index', 3, 'icon-park-outline:list-view'
) routes
WHERE @flowable_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.route_name = routes.route_name OR m.route_path = routes.route_path);

INSERT INTO sys_menu (parent_id, name, type, perm, visible, sort, tree_path, requires_auth)
SELECT MIN(page.id), permissions.title, 4, permissions.perm, 1, 1,
    CONCAT('0,', @flowable_menu_id, ',', MIN(page.id)), 1
FROM (
    SELECT 'flowableWork' AS route_name, '/flowable/work' AS route_path, '使用审批中心' AS title, 'flowable:work' AS perm
    UNION ALL SELECT 'flowableModels', '/flowable/models', '设计与发布流程', 'flowable:model'
    UNION ALL SELECT 'flowableMonitor', '/flowable/monitor', '查看全部审批', 'flowable:monitor'
) permissions
JOIN sys_menu page ON page.parent_id = @flowable_menu_id AND page.type = 1
    AND page.route_name = permissions.route_name AND page.route_path = permissions.route_path
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.type = 4 AND m.perm = permissions.perm)
GROUP BY permissions.title, permissions.perm
HAVING COUNT(*) = 1;

COMMIT;

-- 核对结果应包含 1 个目录、3 个页面和 3 个按钮权限。
SELECT id, parent_id, name, type, route_name, route_path, component, perm, requires_auth
FROM sys_menu
WHERE id = @flowable_menu_id
   OR (parent_id = @flowable_menu_id AND route_name IN ('flowableWork', 'flowableModels', 'flowableMonitor'))
   OR (type = 4 AND perm IN ('flowable:work', 'flowable:model', 'flowable:monitor'))
ORDER BY type, parent_id, sort, id;

SET @flowable_menu_id = NULL;
