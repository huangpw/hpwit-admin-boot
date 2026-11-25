-- Run after menu.sql. Use the role editor to grant access to each page and its permissions.
START TRANSACTION;

SET @flow_workbench_catalog = (
    SELECT CASE WHEN COUNT(*) = 1 THEN MIN(id) END
    FROM sys_menu WHERE route_name = 'flowable' AND route_path = '/flowable' AND parent_id = 0 AND type = 2
);

INSERT INTO sys_menu (parent_id, name, type, route_name, route_path, component, visible, sort, icon, tree_path, requires_auth)
SELECT @flow_workbench_catalog, '业务页面', 1, 'flowableBusinessPages', '/flowable/business-pages',
    '/flowable/business-pages/index', 1, 5, 'icon-park-outline:application-menu', CONCAT('0,', @flow_workbench_catalog), 1
WHERE @flow_workbench_catalog IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE route_name = 'flowableBusinessPages' OR route_path = '/flowable/business-pages');

INSERT INTO sys_menu (parent_id, name, type, perm, visible, sort, tree_path, requires_auth)
SELECT p.id, x.name, 4, x.perm, 1, 2, CONCAT('0,', @flow_workbench_catalog, ',', p.id), 1
FROM (
    SELECT 'flowableBusinessPages' AS route_name, '/flowable/business-pages' AS route_path, '维护业务页面' AS name, 'flowable:pages' AS perm
    UNION ALL SELECT 'flowableMonitor', '/flowable/monitor', '流程管理员操作', 'flowable:admin'
) x
JOIN sys_menu p ON p.parent_id = @flow_workbench_catalog AND p.type = 1
    AND p.route_name = x.route_name AND p.route_path = x.route_path
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.type = 4 AND m.perm = x.perm);

COMMIT;
SET @flow_workbench_catalog = NULL;
