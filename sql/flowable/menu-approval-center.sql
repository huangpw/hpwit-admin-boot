-- 在 menu.sql、menu-workbench.sql 之后执行。保留 /flowable/work 及其菜单 ID。
START TRANSACTION;

UPDATE sys_menu
SET name = '流程管理'
WHERE parent_id = 0 AND type = 2 AND route_name = 'flowable' AND route_path = '/flowable';

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, visible, sort, icon, tree_path, always_show, requires_auth)
SELECT 0, '审批管理', 2, 'flowableApproval', '/flowable/approval', 1, 69, 'icon-park-outline:checklist', '0', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE route_name = 'flowableApproval' OR route_path = '/flowable/approval'
);

SET @approval_catalog = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu
    WHERE parent_id = 0 AND type = 2 AND route_name = 'flowableApproval' AND route_path = '/flowable/approval'
);

UPDATE sys_menu
SET parent_id = @approval_catalog, name = '审批中心', sort = 1, tree_path = CONCAT('0,', @approval_catalog)
WHERE @approval_catalog IS NOT NULL AND route_name = 'flowableWork' AND route_path = '/flowable/work' AND type = 1;

SET @approval_center = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu WHERE route_name = 'flowableWork' AND route_path = '/flowable/work' AND type = 1
);

UPDATE sys_menu
SET name = '使用审批中心', tree_path = CONCAT('0,', @approval_catalog, ',', @approval_center)
WHERE @approval_catalog IS NOT NULL AND parent_id = @approval_center AND type = 4 AND perm = 'flowable:work';

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, component, visible, sort, icon, tree_path, requires_auth)
SELECT @approval_catalog, p.title, 1, p.route_name, p.route_path, p.component, 1, p.position,
    p.icon, CONCAT('0,', @approval_catalog), 1
FROM (
    SELECT '待我审批' AS title, 'flowableTodo' AS route_name, '/flowable/todo' AS route_path,
        '/flowable/todo/index' AS component, 2 AS position, 'icon-park-outline:time' AS icon
    UNION ALL SELECT '我发起的', 'flowableMine', '/flowable/mine', '/flowable/mine/index', 3, 'icon-park-outline:send'
    UNION ALL SELECT '我已审批', 'flowableDone', '/flowable/done', '/flowable/done/index', 4, 'icon-park-outline:check-one'
    UNION ALL SELECT '抄送我的', 'flowableCc', '/flowable/cc', '/flowable/cc/index', 5, 'icon-park-outline:inbox'
) p
WHERE @approval_catalog IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.route_name = p.route_name OR m.route_path = p.route_path);

-- 按既有工作台页面及使用权限迁移关联。流程管理目录下的授权仍由角色配置。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT old.role_id, m.id
FROM sys_role_menu old
JOIN sys_menu permission ON permission.parent_id = @approval_center AND permission.type = 4 AND permission.perm = 'flowable:work'
JOIN sys_role_menu granted ON granted.role_id = old.role_id AND granted.menu_id = permission.id
JOIN sys_menu m ON m.id = @approval_catalog OR (m.parent_id = @approval_catalog AND m.type = 1
    AND m.route_name IN ('flowableTodo','flowableMine','flowableDone','flowableCc'))
WHERE old.menu_id = @approval_center
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu existing WHERE existing.role_id = old.role_id AND existing.menu_id = m.id);

COMMIT;
SET @approval_catalog = NULL;
SET @approval_center = NULL;
