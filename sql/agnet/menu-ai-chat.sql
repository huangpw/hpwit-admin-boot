START TRANSACTION;

SET @demo_id = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu
    WHERE parent_id = 0 AND type = 2 AND route_name = 'demo' AND route_path = '/demo'
);

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, component, visible, sort, icon, tree_path, requires_auth)
SELECT @demo_id, 'AI 聊天助手', 1, 'aiChat', '/demo/ai-chat',
       '/demo/ai-chat/index', 1, 12, 'icon-park-outline:robot-one', CONCAT('0,', @demo_id), 1
WHERE @demo_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE route_name = 'aiChat' OR route_path = '/demo/ai-chat');

UPDATE sys_menu
SET visible = 1, component = '/demo/ai-chat/index'
WHERE parent_id = @demo_id AND type = 1 AND route_name = 'aiChat' AND route_path = '/demo/ai-chat';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, @demo_id
FROM sys_role role
WHERE @demo_id IS NOT NULL AND role.code = 'ADMIN' AND role.status = 1 AND role.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing WHERE existing.role_id = role.id AND existing.menu_id = @demo_id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT parent_grant.role_id, page.id
FROM sys_role_menu parent_grant
JOIN sys_menu page ON page.parent_id = @demo_id AND page.type = 1 AND page.route_name = 'aiChat'
WHERE parent_grant.menu_id = @demo_id
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu page_grant
      WHERE page_grant.role_id = parent_grant.role_id AND page_grant.menu_id = page.id
  );

COMMIT;
SET @demo_id = NULL;
