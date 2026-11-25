-- 在同一连接中设置角色编码和所需权限后执行本文件。
-- 示例
-- SET @flowable_role_code = 'USER';
-- SET @flowable_permission = 'flowable:work';
-- 每次授予一种权限，可选 flowable:work、flowable:model、flowable:monitor、flowable:pages、flowable:admin。

START TRANSACTION;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role.id, target.id
FROM sys_role role
JOIN sys_menu catalog ON catalog.parent_id = 0 AND catalog.type = 2 AND (
    (catalog.route_name = 'flowable' AND catalog.route_path = '/flowable')
    OR (catalog.route_name = 'flowableApproval' AND catalog.route_path = '/flowable/approval'))
JOIN sys_menu page ON page.parent_id = catalog.id AND page.type = 1
JOIN sys_menu button ON button.parent_id = page.id AND button.type = 4
JOIN sys_menu target ON target.id IN (catalog.id, page.id, button.id)
    OR (button.perm = 'flowable:work' AND target.parent_id = catalog.id AND target.type = 1
        AND target.route_name IN ('flowableTodo','flowableMine','flowableDone','flowableCc'))
WHERE role.status = 1 AND role.is_deleted = 0
  AND CAST(LOWER(CONVERT(role.code USING utf8mb4)) AS BINARY)
      = CAST(LOWER(CONVERT(@flowable_role_code USING utf8mb4)) AS BINARY)
  AND CAST(button.perm AS BINARY) = CAST(CONVERT(@flowable_permission USING utf8mb4) AS BINARY)
  AND ((button.perm = 'flowable:work' AND page.route_name = 'flowableWork' AND page.route_path = '/flowable/work')
    OR (button.perm = 'flowable:model' AND page.route_name = 'flowableModels' AND page.route_path = '/flowable/models')
    OR (button.perm IN ('flowable:monitor','flowable:admin') AND page.route_name = 'flowableMonitor' AND page.route_path = '/flowable/monitor')
    OR (button.perm = 'flowable:pages' AND page.route_name = 'flowableBusinessPages' AND page.route_path = '/flowable/business-pages'))
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing WHERE existing.role_id = role.id AND existing.menu_id = target.id
  );

SELECT ROW_COUNT() AS added_grants;

COMMIT;

SET @flowable_role_code = NULL;
SET @flowable_permission = NULL;
