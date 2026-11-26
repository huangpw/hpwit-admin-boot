START TRANSACTION;

SET @demo_id = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu
    WHERE parent_id = 0 AND type = 2 AND route_name = 'demo' AND route_path = '/demo'
);

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, component, visible, sort, icon, tree_path, requires_auth)
SELECT @demo_id, 'AI 识图录入', 1, 'aiProductRecognition', '/demo/product',
       '/demo/product/index', 1, 10, 'icon-park-outline:upload-picture', CONCAT('0,', @demo_id), 1
WHERE @demo_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM sys_menu WHERE route_name = 'aiProductRecognition' OR route_path = '/demo/product'
    );

UPDATE sys_menu
SET visible = 1
WHERE parent_id = @demo_id AND type = 1 AND route_name = 'aiProductRecognition'
    AND route_path = '/demo/product' AND component = '/demo/product/index' AND visible = 0;

-- 系统管理员需要父目录和页面授权，侧栏才能展示入口。
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
JOIN sys_menu page ON page.parent_id = @demo_id AND page.type = 1
    AND page.route_name = 'aiProductRecognition' AND page.route_path = '/demo/product'
    AND page.component = '/demo/product/index'
WHERE parent_grant.menu_id = @demo_id
    AND NOT EXISTS (
        SELECT 1 FROM sys_role_menu page_grant
        WHERE page_grant.role_id = parent_grant.role_id AND page_grant.menu_id = page.id
    );

COMMIT;
SET @demo_id = NULL;
