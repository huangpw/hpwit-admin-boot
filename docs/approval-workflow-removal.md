# 审批管理与流程管理拆分手册

适用于 `hpwit-admin-boot`（后端）和同级目录下的 `hpwit-admin-vue`（前端）。依据两仓当前工作区核对，后端基线为 `cba19f2`，前端为 `604ea4b`，范围包含各工作区尚未提交的 Flowable 集成。数据库清单依据仓库中的 MySQL 8 / Flowable 7.2.0 SQL。部署环境中的扩展菜单、业务接入和表归属以实际盘点为准。 执行前取得实际包含该集成的前后端源码和后端 `sql/flowable/` 安装脚本。

「审批管理」和「流程管理」共用 `/v1/flowable`、Flowable 引擎、已发布模型和审批实例。将两者作为一个整体从管理平台拆出。原平台继续提供登录、动态菜单、角色权限及其余业务。临时下线走第 2 节。正式拆除时，先停用，再清理菜单并拆除前后端代码（第 3 至 5 节），最后验收主应用（第 7 节）。第 6 节用于盘点和归档数据，删表可在主应用验收后独立安排。第 8 节用于回滚或重新接入模块。

## 1. 模块边界

| 功能 | 菜单、路由与权限 |
| --- | --- |
| 审批管理 | 目录 `flowableApproval` 对应 `/flowable/approval`。审批中心、待我审批、我发起的、我已审批、抄送我的分别对应 `/flowable/work`、`/flowable/todo`、`/flowable/mine`、`/flowable/done`、`/flowable/cc`，使用 `flowable:work`。 |
| 流程管理 | 目录 `flowable` 对应 `/flowable`。流程模型 `/flowable/models` 使用 `flowable:model`，流程监控 `/flowable/monitor` 使用 `flowable:monitor` 和 `flowable:admin`，业务页面 `/flowable/business-pages` 使用 `flowable:pages`。 |
| 隐藏路由 | 前端动态补入 `flowableOrder`（`/flowable/order`）和 `flowableDetail`（`/flowable/detail/:id`）。默认菜单 SQL 未创建这两条记录，已自定义入库的环境需要一起清理。 |
| 主应用依赖 | 审批复用 Sa-Token、统一响应类型、MySQL 数据源及 Spring 事务，同时按 `sys_user`、`sys_user_role`、`sys_role`、`sys_role_menu`、`sys_menu` 解析用户及权限。这些系统表和基础能力归主应用。 |
| 业务接入 | `BusinessApprovalStatusHandler` 在同库事务里校验并回写业务状态。`hpwit.flowable.callbacks.endpoints` 配置异步 HTTP 操作回写。业务模块自行维护业务单据、状态和基础查询 API。 |

## 2. 盘点、备份与停用

1. 为前后端各留一份包含 Flowable 文件的可恢复源码快照，并保存当前部署产物、审批配置和数据库迁移执行记录。若工作区存在未跟踪文件，快照必须包含这些文件。
2. 盘点发起入口和调用方，包括主应用其他页面、接入应用、HTTP 回写接收方、定时任务、网关以及部署配置。搜索建议见第 7 节。检查 `flow_approval_business_page` 已登记的组件与业务路由，核对 `flow_approval_business_binding` 的业务主键和已发布版本关联。早期实例可用 `sql/flowable/audit-business-approval-status.sql` 对账。
3. 先停止新申请入口。对在途实例继续办理、办理终止，或约定迁移后接续。逐一核对业务单据的当前审批实例和状态。回写任务会重试，先修复并投递，或登记未完成事件及接收方处理方式；保留 `eventId`、`sequence` 和业务主键。模块停用后后台投递会停止。
4. 所有后端实例设置 `HPWIT_FLOWABLE_ENABLED=false` 和 `HPWIT_FLOWABLE_SCHEMA_UPDATE=false` 后重启。检查外置 Spring 配置、环境变量和启动参数的实际生效值。关闭后，流程引擎、审批 API、回写任务停止，两个菜单从用户动态路由中隐藏。让用户退出并重新登录，清理仍打开的审批标签页。
5. 在审批写入停止后留存一致性备份，并在隔离实例验证可恢复。备份包含审批业务表、引擎表、`sys_menu` 与 `sys_role_menu`，以及实际接入的业务状态数据。多个数据库或远端业务回写有独立写入时，记录切换点和事件进度。执行涉及菜单删除的第 3 节时，另外导出本次命中的菜单与授权记录。

### 停用前的核对 SQL

在目标 MySQL schema 执行。表未安装时跳过对应语句。回写查询适用于含 `operation_id` 的结构。旧结构先查列定义，缺列时将历史回写记录纳入人工对账。

```sql
SELECT DATABASE() AS current_schema;

SELECT status, COUNT(*) AS instance_count
FROM flow_approval_instance
GROUP BY status;

SELECT id, business_no, applicant_id, definition_id, status
FROM flow_approval_instance
WHERE status IN ('PENDING', 'RUNNING', 'DRAFT')
ORDER BY status, id;

SELECT id, instance_id, business_page, business_no, endpoint, operation_id,
       attempts, next_attempt_at, lease_until, last_error
FROM flow_approval_status_callback
WHERE operation_id <> '' AND delivered_at IS NULL
ORDER BY id;

SELECT code, name, component, route_path, enabled
FROM flow_approval_business_page
ORDER BY code;
```

备份示例使用已配置的 `mysql_config_editor` 登录路径。按目标环境替换数据库名。备份期间暂停目标库的 schema 变更。外置配置和密钥引用用部署环境的备份机制保存。

```bash
umask 077
approval_db='替换为目标数据库名'
mysqldump --login-path=hpwit --single-transaction --skip-lock-tables \
  --routines --events --triggers --hex-blob --set-gtid-purged=OFF \
  --databases "$approval_db" > hpwit-before-approval-removal.sql
```

## 3. 清理共享菜单与角色授权

在部署已去掉菜单过滤逻辑的后端前完成本节。数据库中的路由记录决定动态路由结果。菜单种子 SQL 使用动态分配的 ID，历史版本的审批中心也可能挂在「流程管理」下。

### 3.1 查找并核对目标菜单

该查询列出标准路由、模块权限以及候选菜单的后代。根据 `parent_id`、路径、组件和权限逐条核对。遇到挂在模块目录下但归其他业务的菜单，先用系统菜单管理把它迁到合适目录，核验其 `tree_path` 和角色授权，再重新盘点。对已改名或移动且无法自动命中的模块菜单，使用实际 ID 补入下节清单。

```sql
WITH RECURSIVE candidate_menu (id) AS (
    SELECT id
    FROM sys_menu
    WHERE route_name IN (
        'flowable', 'flowableApproval', 'flowableWork', 'flowableTodo',
        'flowableMine', 'flowableDone', 'flowableCc', 'flowableModels',
        'flowableMonitor', 'flowableBusinessPages', 'flowableOrder', 'flowableDetail'
    )
       OR route_path = '/flowable'
       OR route_path LIKE '/flowable/%'
       OR COALESCE(TRIM(LEADING '/' FROM component), '') LIKE 'flowable/%'
       OR perm LIKE 'flowable:%'
    UNION DISTINCT
    SELECT child.id
    FROM sys_menu AS child
    JOIN candidate_menu AS parent ON child.parent_id = parent.id
)
SELECT m.id, m.parent_id, m.name, m.type, m.route_name, m.route_path,
       m.component, m.perm, m.tree_path,
       (SELECT COUNT(*) FROM sys_role_menu AS rm WHERE rm.menu_id = m.id) AS role_grants
FROM candidate_menu AS c
JOIN sys_menu AS m ON m.id = c.id
ORDER BY m.tree_path, m.parent_id, m.id;
```

将确认后的一组 ID 记录到变更单。暂停系统菜单编辑和角色分配，按该组 ID 分别导出菜单行及授权行，保留原主键。将下例的 `101,102` 替换为同一组真实 ID。重新导入前检查是否与现有 ID 或角色菜单唯一键冲突。

```bash
umask 077
approval_db='替换为目标数据库名'
approval_menu_ids='101,102'
mysqldump --login-path=hpwit --single-transaction --skip-lock-tables \
  --no-create-info --skip-add-locks --skip-disable-keys --skip-triggers --complete-insert \
  --set-gtid-purged=OFF --where="id IN ($approval_menu_ids)" \
  "$approval_db" sys_menu > approval-menu-rows.sql
mysqldump --login-path=hpwit --single-transaction --skip-lock-tables \
  --no-create-info --skip-add-locks --skip-disable-keys --skip-triggers --complete-insert \
  --set-gtid-purged=OFF --where="menu_id IN ($approval_menu_ids)" \
  "$approval_db" sys_role_menu > approval-role-menu-rows.sql
```

### 3.2 在同一 MySQL 会话删除已确认的记录

将示例 ID 替换为已备份的列表，每个 ID 一行。已有菜单关闭写入后执行。若查询仍有未选中的子菜单、选中行少于预期，或角色授权与备份不符，先 `ROLLBACK`，核对后再执行。没有待清理 ID 时跳过删除。

```sql
CREATE TEMPORARY TABLE approval_menu_remove (
    id BIGINT UNSIGNED PRIMARY KEY
);

-- 替换为已核对并备份的 ID。
INSERT INTO approval_menu_remove (id) VALUES (101), (102);

START TRANSACTION;

SELECT m.id, m.parent_id, m.route_name, m.route_path, m.perm
FROM sys_menu AS m
JOIN approval_menu_remove AS target ON target.id = m.id
ORDER BY m.id;

SELECT child.id, child.parent_id, child.name, child.route_path
FROM sys_menu AS child
JOIN approval_menu_remove AS parent ON parent.id = child.parent_id
WHERE child.id NOT IN (
    -- 填入同一组 ID，避免 MySQL 在同一语句重复引用临时表。
    101, 102
);

SELECT rm.id, rm.role_id, rm.menu_id
FROM sys_role_menu AS rm
JOIN approval_menu_remove AS target ON target.id = rm.menu_id
ORDER BY rm.role_id, rm.menu_id;
```

确认子菜单检查结果为空且记录与备份相符，再执行以下删除语句。对比删除行数，并确认两个 `remaining_*` 均为 0 后单独执行 `COMMIT`。发现异常执行 `ROLLBACK`。退出会话时临时表自动移除，重用原会话时执行 `DROP TEMPORARY TABLE approval_menu_remove;`。

```sql
DELETE rm
FROM sys_role_menu AS rm
JOIN approval_menu_remove AS target ON target.id = rm.menu_id;
SELECT ROW_COUNT() AS deleted_grants;

DELETE m
FROM sys_menu AS m
JOIN approval_menu_remove AS target ON target.id = m.id;
SELECT ROW_COUNT() AS deleted_menus;

SELECT COUNT(*) AS remaining_grants
FROM sys_role_menu AS rm
JOIN approval_menu_remove AS target ON target.id = rm.menu_id;

SELECT COUNT(*) AS remaining_menus
FROM sys_menu AS m
JOIN approval_menu_remove AS target ON target.id = m.id;
```

如目标环境给 `sys_menu.parent_id` 添加了自引用外键，按实际树层级逐级删除菜单，从按钮和最深层子菜单删到根目录。保留整张 `sys_menu`、`sys_role_menu` 及其他系统表。部署后用 ROOT 和普通角色重新登录核对动态路由、角色编辑和菜单管理。

## 4. 后端拆除

在 `hpwit-admin-boot` 中执行。先移除其他模块对审批的调用，再去掉 Flowable 包和构建依赖。

| 文件或目录 | 执行事项 |
| --- | --- |
| `src/main/java/com/hpwit/modules/flowable/` | 整包移出主应用。包含引擎与回写配置、`ApprovalController`、模型、审批办理、权限检查、JDBC 存储、排队回写和测试订单处理器。 |
| `src/test/java/com/hpwit/modules/flowable/` | 随模块移出 `ApprovalIntegrationTest`。 |
| `pom.xml` | 删除 `flowable.version` 和 `org.flowable:flowable-spring`。当前 `com.h2database:h2` 为审批集成测试引入，清点其他测试后移除未被使用的 H2 测试依赖。 |
| `src/main/resources/application.yml` | 删除 `hpwit.flowable` 配置节点。清理各 profile、`config/`、外置配置与部署环境中的 `HPWIT_FLOWABLE_ENABLED`、`HPWIT_FLOWABLE_SCHEMA_UPDATE`、模块回写配置和模块独用的密钥引用。 |
| `src/main/java/com/hpwit/system/service/impl/SysMenuServiceImpl.java` | 删除注入 `flowableEnabled` 的 `@Value` 字段和 `availableRoutes()`。在 `listCurrentUserRoutes()` 中用 `buildRoutes(SysConstant.ROOT_NODE_ID, menuList)`，在 `listCurrentUserMenuRoutes()` 中用 `menuList.stream()`。删除未使用的 import，保留 ROOT 和普通角色的菜单查询。 |
| `src/main/resources/mapper/system/SysMenuMapper.xml` | 保留 `getMenusByRoleCodes` 的 `t1.requires_auth` 字段，普通角色的路由认证标识由共享菜单服务读取。 |
| `sql/flowable/` | 整个目录随模块保存，含安装、升级、菜单授权、历史对账、引擎 DDL 与 `LICENSE-flowable.txt`。主应用安装和发布任务停止执行该目录的脚本。 |
| `sql/hpwit_admin_boot.sql`、`sql/seed_menu.sql`、`sql/seed_config.sql` | 当前仓库的主应用初始化脚本可独立使用。若环境自有全库基线或迁移包含审批表及菜单，从后续初始化流程剔除对应项。 |

已接入的业务如直接引用 `BusinessApprovalStatusHandler`、审批 DTO、审批表或旧 API，将审批适配代码移到模块侧，或在业务侧移除触发点并明确单据下一步操作及读写规则。实业务表及其审批状态列属于对应业务，由业务方按保留、迁移或状态交接的结果处理。示例订单 `DEMO_ORDER` 与 `SO-DEMO-001` 的代码和数据均属于审批模块。

## 5. 前端拆除

在 `hpwit-admin-vue` 中执行。

| 文件或目录 | 执行事项 |
| --- | --- |
| `src/views/flowable/` | 整目录移出主应用，包括审批中心、工作列表、监控、模型设计和版本历史、详情、业务页面设置、BPMN 组件及测试订单。 |
| `src/service/api/flowable.ts` | 随模块移出。清理外部页引用的审批请求、详情跳转、发起按钮、角标、待办提醒和类型引用。 |
| `src/store/router/index.ts` | 删除查找 `flowableWork` 并追加 `flowableOrder` 的代码，以及按 `approvalLists` 追加 `flowableDetail` 的代码。保留 `resolvedRoutes`、`userCenter` 补入以及通用动态路由生成和注册。 |
| `locales/zh_CN.json`、`locales/en_US.json` | 删除 `route` 下 12 个 Flowable 键，分别为 `flowable`、`flowableApproval`、`flowableWork`、`flowableTodo`、`flowableMine`、`flowableDone`、`flowableCc`、`flowableModels`、`flowableMonitor`、`flowableBusinessPages`、`flowableOrder`、`flowableDetail`。 |
| `package.json` | 用 `pnpm remove bpmn-js` 移除 18.33.1，更新本地依赖树。当前 `pnpm-lock.yaml` 被仓库忽略。若交付分支跟踪锁文件，随依赖变更一并更新。其他页面独立使用 BPMN 时保留相应依赖。 |
| `build/plugins.ts`、`src/typings/env.d.ts` | 保留现有 `VITE_API_BASE_URL`。它覆盖全应用的 API 地址，支持主应用单独部署。保留 `VITE_ROUTE_LOAD_MODE=dynamic` 和通用的 `fetchUserRoutes()`。 |

实际业务在 `src/views/**/approval.vue` 注册过嵌入详情时，核查它是否只被审批侧使用。审批适配组件可随模块迁移，业务自己的列表、详情和 API 留在业务侧。重新构建并整体发布 `dist/`，切换期间清理代理和 CDN 上引用旧审批资源的缓存。已有浏览器会话重新登录并关闭残留的审批标签页。

## 6. 审批数据表与移除顺序

可先保留原表作为历史和回滚来源，主应用在第 3 至 5 节完成后即可独立部署。确认备份恢复成功、主应用已通过第 7 节，并完成历史留存或迁移后，再移除专用表。

### 6.1 模块业务表（12 张）

| 表名 | 用途 | 建表脚本（`sql/flowable/`） |
| --- | --- | --- |
| `flow_approval_model` | 模型草稿、BPMN XML、版本信息 | `schema.sql` |
| `flow_approval_instance` | 审批申请、状态、定义和流程实例关联 | `schema.sql` |
| `flow_approval_action` | 审批操作历史 | `schema.sql` |
| `flow_approval_business_page` | 业务页面及审批组件登记 | `upgrade-workbench.sql` |
| `flow_approval_order` | 测试订单内容 | `upgrade-workbench.sql` |
| `flow_approval_instance_meta` | 重新提交来源、替换关系与可见状态 | `upgrade-workbench.sql` |
| `flow_approval_signature` | 加签及原办理人信息 | `upgrade-workbench.sql` |
| `flow_approval_notice` | 抄送和催办 | `upgrade-workbench.sql` |
| `flow_approval_attachment` | 审批附件及二进制数据 | `upgrade-workbench.sql` |
| `flow_approval_order_status` | 最近一次测试订单审批状态 | `upgrade-order-approval-status.sql` |
| `flow_approval_status_callback` | HTTP 操作回写、历史状态事件及投递状态 | `upgrade-status-callback.sql`，旧库用 `upgrade-approval-operation-callback.sql` 补操作事件字段 |
| `flow_approval_business_binding` | 实例与真实业务主键绑定及在途唯一性 | `upgrade-business-approval-binding.sql` |

### 6.2 引擎表（32 张）

以下集合来自 `sql/flowable/engine-mysql-7.2.0.sql`。已发布 BPMN、表单配置和部署资源分布在引擎表，备份或迁移时与 12 张业务表使用同一个数据切换点。

| 范围 | 表名 |
| --- | --- |
| 通用信息、资源与部署 | `ACT_GE_PROPERTY`、`ACT_GE_BYTEARRAY`、`ACT_RE_DEPLOYMENT`、`ACT_RE_MODEL`、`ACT_RE_PROCDEF`、`ACT_PROCDEF_INFO`、`ACT_EVT_LOG` |
| 运行时 | `ACT_RU_ENTITYLINK`、`ACT_RU_IDENTITYLINK`、`ACT_RU_JOB`、`ACT_RU_TIMER_JOB`、`ACT_RU_SUSPENDED_JOB`、`ACT_RU_DEADLETTER_JOB`、`ACT_RU_HISTORY_JOB`、`ACT_RU_EXTERNAL_JOB`、`ACT_RU_TASK`、`ACT_RU_VARIABLE`、`ACT_RU_EVENT_SUBSCR`、`ACT_RU_EXECUTION`、`ACT_RU_ACTINST` |
| 历史 | `ACT_HI_ENTITYLINK`、`ACT_HI_IDENTITYLINK`、`ACT_HI_TASKINST`、`ACT_HI_TSK_LOG`、`ACT_HI_VARINST`、`ACT_HI_PROCINST`、`ACT_HI_ACTINST`、`ACT_HI_DETAIL`、`ACT_HI_COMMENT`、`ACT_HI_ATTACHMENT` |
| 批处理 | `FLW_RU_BATCH_PART`、`FLW_RU_BATCH` |

### 6.3 核对后执行删除 SQL

先对照表清单、部署实例的数据源和数据库对象依赖。下列查询逐项检查现有表、视图和引用审批表的外键，外键结果包含其他 schema。另行检查目标库的触发器、存储过程、事件、ETL 和只在应用代码中声明的关系。表名前缀用于盘点，删除使用明确表名。若另一套 Flowable 使用同一 schema 中的引擎表，保留该套引擎表，先在模块支持的迁移或归档方式中处理自己的流程数据。额外安装的引擎表按实际归属另行处理。

```sql
SELECT DATABASE() AS current_schema, @@SESSION.foreign_key_checks AS fk_checks;

SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND REGEXP_LIKE(table_name, '^(flow_approval_|ACT_|FLW_)', 'i')
ORDER BY table_name;

SELECT table_schema, table_name, constraint_name,
       referenced_table_schema, referenced_table_name
FROM information_schema.key_column_usage
WHERE referenced_table_schema = DATABASE()
  AND REGEXP_LIKE(referenced_table_name, '^(flow_approval_|ACT_|FLW_)', 'i')
ORDER BY referenced_table_name, table_schema, table_name, constraint_name;

SELECT view_schema, view_name, table_name
FROM information_schema.view_table_usage
WHERE table_schema = DATABASE()
  AND REGEXP_LIKE(table_name, '^(flow_approval_|ACT_|FLW_)', 'i')
ORDER BY view_schema, view_name, table_name;
```

在选定 schema 中执行以下 DDL。Flowable 引擎段只适用于确认独占的 32 张引擎表。仓库 DDL 的外键按子表在前、父表在后的顺序处理，保持 `@@SESSION.foreign_key_checks = 1`。若线上新增了引用，先处理引用关系，失败时核对已执行的 DDL 后续跑。MySQL `DROP TABLE` 会隐式提交，数据恢复依赖备份。

```sql
-- 模块业务表。
DROP TABLE IF EXISTS
    flow_approval_status_callback,
    flow_approval_business_binding,
    flow_approval_attachment,
    flow_approval_notice,
    flow_approval_signature,
    flow_approval_instance_meta,
    flow_approval_order_status,
    flow_approval_order,
    flow_approval_action,
    flow_approval_instance,
    flow_approval_business_page,
    flow_approval_model;

-- 模块独占的 Flowable 引擎表。按仓库 DDL 的外键依赖分组执行。
DROP TABLE IF EXISTS
    FLW_RU_BATCH_PART,
    ACT_RU_IDENTITYLINK,
    ACT_RU_VARIABLE,
    ACT_RU_JOB,
    ACT_RU_TIMER_JOB,
    ACT_RU_SUSPENDED_JOB,
    ACT_RU_DEADLETTER_JOB,
    ACT_RU_EXTERNAL_JOB,
    ACT_RU_EVENT_SUBSCR,
    ACT_RU_ENTITYLINK,
    ACT_RU_HISTORY_JOB,
    ACT_RU_ACTINST,
    ACT_HI_ENTITYLINK,
    ACT_HI_IDENTITYLINK,
    ACT_HI_TASKINST,
    ACT_HI_TSK_LOG,
    ACT_HI_VARINST,
    ACT_HI_PROCINST,
    ACT_HI_ACTINST,
    ACT_HI_DETAIL,
    ACT_HI_COMMENT,
    ACT_HI_ATTACHMENT,
    ACT_EVT_LOG;

DROP TABLE IF EXISTS
    ACT_RU_TASK,
    ACT_RE_MODEL,
    ACT_PROCDEF_INFO;

DROP TABLE IF EXISTS ACT_RU_EXECUTION;
DROP TABLE IF EXISTS ACT_RE_PROCDEF;
DROP TABLE IF EXISTS ACT_GE_BYTEARRAY;
DROP TABLE IF EXISTS ACT_RE_DEPLOYMENT;
DROP TABLE IF EXISTS ACT_GE_PROPERTY;
DROP TABLE IF EXISTS FLW_RU_BATCH;
```

## 7. 验收

在两仓提交拆分前搜索残留引用。主应用所保留的文档及归档文件中可能存在模块关键词。运行时源文件、依赖、部署和初始化链路中的每个命中都应有明确归属。

```bash
# 后端仓库根目录。
rg -n -i 'org[.]flowable|flowable|flow_approval|BusinessApprovalStatusHandler' \
  src pom.xml config sql
bash mvnw clean test
bash mvnw -DskipTests package
bash mvnw dependency:tree '-Dincludes=org.flowable:*'

# 前端仓库根目录。
rg -n -i 'flowable|bpmn-js|审批管理|流程管理' src build locales package.json
pnpm lint
pnpm build
```

后端测试启用项目测试环境所需的 MySQL、Redis 等依赖；构建和验收使用同一拆分分支。以下各项在与目标环境结构一致的隔离环境先演练，再核对上线结果。

- **基础运行**。后端使用现有主应用配置和数据库启动。新库执行主应用初始化流程即可启动，无审批建表和初始化要求。依赖树无 `org.flowable` 依赖，后台无审批回写任务。
- **动态菜单与权限**。ROOT 和普通角色均可登录。核对 `GET /v1/menu/routes/current`、`GET /v1/menu/routes`、系统菜单和角色授权页面。审批菜单和权限记录已移除，普通角色仍只能访问已有授权页面，`/user-center` 可直接打开。
- **路由与 API**。访问 `/flowable/work`、`/flowable/models`、`/flowable/order`、`/flowable/detail/<旧实例ID>` 应进入前端未找到页面。使用有效会话请求 `GET /v1/flowable/overview`、`GET /v1/flowable/models`，后端应返回 HTTP 404 和项目的 `INTERFACE_NOT_EXIST` 错误。
- **保留业务**。验证用户、角色、菜单、部门、字典、操作日志、系统配置、通知公告以及实际接入业务的关键操作。比对保留角色的授权和业务状态，确认旧的审批入口、跳转及回写依赖已按业务交接处理。
- **数据**。重新执行第 3.1 和第 6.3 的查询，确认审批菜单及其授权已清除。删表后按清单逐项复核，约定留档或由其他引擎使用的表应仍可核对。检查启动日志、接口日志和浏览器控制台有无意外的缺表或组件加载异常。

## 8. 回滚与重新接回

- **只停用**。先恢复审批配置和所有实例使用的回写端点，确保数据库包含对应版本的审批业务表和引擎表，再设置 `HPWIT_FLOWABLE_ENABLED=true`、`HPWIT_FLOWABLE_SCHEMA_UPDATE=false` 并重启。核对在途实例和待投递事件，再恢复新申请入口。
- **已清理菜单或移出源码**。前后端成对回退到包含模块的同一版本及相应配置。系统菜单若已删除，检查原 ID、角色 ID 与唯一键冲突，先恢复第 3 节导出的 `sys_menu` 行，再恢复 `sys_role_menu` 行。首次安装或确认采用新菜单 ID 时，按下方的装配顺序初始化并重配角色授权。
- **已删表**。先把停用时的数据库备份恢复到隔离实例，按第 6 节的表清单提取专属表及数据。将审批业务表与引擎表成组恢复到目标库，保留运行期的实例、部署、业务绑定、回写序号和相关标识。按记录好的切换点对账真实业务状态和回写投递结果，再开启审批。回滚线上环境时以定向恢复审批表和菜单行的方式保护主应用在切换之后新增的业务数据。
- **把模块接入同一套主应用的新环境**。装回两仓模块目录及第 4、5 节的审批接入部分。先创建主应用系统表，随后执行 `schema.sql` → `upgrade-workbench.sql` → `upgrade-order-approval-status.sql` → `upgrade-status-callback.sql` → `upgrade-business-approval-binding.sql`。仅当旧回写表存在且缺少 `operation_id`、仍使用 `uk_flow_status_callback` 时运行 `upgrade-approval-operation-callback.sql`。独占引擎首次建库执行 `engine-mysql-7.2.0.sql`。再按 `menu.sql` → `menu-workbench.sql` → `menu-approval-center.sql` 安装菜单，并在角色管理中授权或使用 `grant_menu.sql`。迁移已有数据时采用相同版本表结构和备份数据，核对用户、角色及业务主键映射。恢复开关后先验收一次完整的发起、待办、处理、监控和业务回写，再开放入口。安装与接口细节见包含该集成的后端源码中的 `sql/flowable/README.md`。
- **把模块部署为独立服务**。先在独立部署单元装配模块和必要的认证、角色、动态路由与统一响应依赖，并由同一个服务独占流程写入。要改为独立数据库，将 12 张模块业务表与 32 张引擎表作为整体迁移，替换当前对 `sys_*` 的直接查询或提供兼容的身份和权限数据。原同库 `BusinessApprovalStatusHandler` 的业务写入与流程共用事务；跨库时需要新的业务状态同步机制，明确提交校验、幂等、事件顺序、补偿和对账。现有 HTTP 回写投递面向审批操作事件，审批状态同步需按业务设计实现。切换网关和前端入口前，确认旧服务的流程写入及回写任务已停止，同一批在途实例只能由接手服务继续处理。
