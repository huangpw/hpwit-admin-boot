# Flowable 审批模块

后端使用 Spring Boot 3、Java 17 和 Flowable 7.2.0，复用项目的 MySQL 数据源与事务管理器。Vue 使用 bpmn-js 设计、导入和预览 BPMN。草稿原文保存在 `flow_approval_model.bpmn_xml`，发布后 BPMN 文件与表单配置作为 Flowable 部署资源保存。发起时绑定已发布版本，后续发布只影响新申请。

## 初始化与启用

1. 在已有系统表的 MySQL 数据库中依次执行 `schema.sql`、`upgrade-workbench.sql`、`upgrade-order-approval-status.sql`、`upgrade-status-callback.sql`、`upgrade-business-approval-binding.sql`、`menu.sql`、`menu-workbench.sql` 和 `menu-approval-center.sql`。旧版已有回写任务表且缺少 `operation_id` 字段时，执行一次 `upgrade-approval-operation-callback.sql`。再重启后端并刷新前端。
2. 在尚未初始化 Flowable 的目标库执行 `engine-mysql-7.2.0.sql`。该脚本包含 Flowable 7.2.0 的官方 MySQL 建表语句，用于首次初始化。开发环境也可以通过同时设置 `HPWIT_FLOWABLE_ENABLED=true` 和 `HPWIT_FLOWABLE_SCHEMA_UPDATE=true` 在首次启动时自动创建引擎表。初始化完成后使用 `HPWIT_FLOWABLE_ENABLED=true` 和默认的 `HPWIT_FLOWABLE_SCHEMA_UPDATE=false` 启动后端。
3. 在「系统管理 → 角色管理」为业务角色分配「审批管理」或「流程管理」目录、所需页面及对应按钮权限。也可在同一 MySQL 会话中设置 `@flowable_role_code` 和 `@flowable_permission`，再执行 `grant_menu.sql`。每次授予一种权限及其所属目录和页面。系统启用的 `ROOT` 角色可访问全部页面。审批任务仍按实际分配的用户或角色办理。
4. Vue 使用项目现有的动态路由模式 `VITE_ROUTE_LOAD_MODE=dynamic`。开发环境运行 `pnpm install` 和 `pnpm dev`。如后端地址与默认值不同，在构建或启动 Vite 时设置 `VITE_API_BASE_URL`，例如 `VITE_API_BASE_URL=https://api.example.com pnpm build`。
5. 用户重新登录后，从「审批管理」办理审批，从「流程管理」配置模型。关闭时设置 `HPWIT_FLOWABLE_ENABLED=false` 并重启后端。开关关闭时，流程引擎和审批 API 停用，动态路由隐藏，数据库里的草稿、部署与审批记录保留。

| 顺序 | Maven JAR | JAR 内的 SQL 资源 |
| --- | --- | --- |
| 1 | `flowable-engine-common-7.2.0.jar` | `org/flowable/common/db/create/flowable.mysql.create.common.sql` |
| 2 | `flowable-engine-7.2.0.jar` | `org/flowable/db/create/flowable.mysql.create.engine.sql` |
| 3 | `flowable-engine-7.2.0.jar` | `org/flowable/db/create/flowable.mysql.create.history.sql` |

| 页面 | 按钮权限 | 功能 |
| --- | --- | --- |
| 审批中心 | `flowable:work` | 发起、待办、我的申请、已办和处理本人有权办理的任务 |
| 流程模型 | `flowable:model` | 设计、导入、下载 BPMN、配置申请字段和发布版本 |
| 流程监控 | `flowable:monitor` | 查看全部审批、已结束审批、流程图和操作日志 |
| 流程监控 | `flowable:admin` | 调整审批人和审批节点，强制通过／驳回，终止或重启 |
| 业务页面 | `flowable:pages` | 新增、编辑、启用或停用可关联的业务页面 |

发布时，指定审批人须拥有审批中心权限，指定角色须至少有一名启用且拥有审批中心权限的成员。角色候选任务由首位办理的成员认领，之后从其他成员的待办移除。如果新进入的节点已无人可办，本次发起或流转会回滚。

## 从模型到实际审批

1. 打开「流程模型 → 新建模型」，填写模型名称和场景。从画布左侧添加审批节点和结束事件，连接开始事件、审批节点、结束事件。选中审批节点，填写名称并选择一名启用的系统用户或一个启用的角色。也可导入带图形信息且符合约束的 `.bpmn` 或 `.xml` 文件，文件最大 512 KB。
2. 在「流程表单字段」中用「添加字段」定义申请表单。字段支持短文本、长文本、数字、日期和是／否。然后保存草稿并发布。
3. 在「审批中心 → 发起申请」选择已发布流程，填写标题和表单。已有业务系统可通过业务单号关联采购单、报销单等记录。模块处理通用申请和审批轨迹，各业务单据的库存、财务或人事状态需由对应业务系统接入。
4. 由审批人在「我的待办」中处理。通过会推进流程，最后一关通过后申请结束。驳回须填写原因并终止当前申请。如需更正，可重新发起。发起人在申请处于审批中时可撤销。
5. 在申请详情查看原发布版本的流程图、当前与已执行节点，以及操作人、意见和时间。监控页可查看全部申请。

| 场景 | 表单字段示例 | 流程示例 |
| --- | --- | --- |
| 请假 | 请假起止日期、天数、原因 | 直属主管审批 → 人事审批 |
| 报销 | 金额、费用说明 | 部门主管审批 → 财务审批 |
| 采购 | 品名、数量、金额 | 主管审批 → 按金额分支 → 采购或财务审批 |
| 用章 | 印章名称、用途、日期 | 部门负责人审批 → 印章管理员审批 |

流程支持开始事件、结束事件、人工审批和排他网关。各路径至少经过一个审批节点。排他网关分支需要一条默认连线，其他连线配置数字比较表达式，例如 `${amount >= 1000}`，其中 `amount` 是必填数字字段。多个条件同时为真时，Flowable 按 BPMN 中的连线顺序取首条成立的分支。合并路径时保留一条普通出线。流程支持无环串行路径和互斥分支。

## 查看历史与恢复版本

在「流程管理 → 流程模型 → 版本历史」可查看当前发布版本、历史版本、发布时间、BPMN 图、表单字段及关联业务页面。版本导出为 JSON，包含 BPMN XML 和发布配置。

选择旧版本后点击「恢复为草稿」，确认后会覆盖当前草稿。可以继续编辑，再通过模型列表发布为下一个版本。未发布的草稿只对模型编辑可见。发布后，新申请使用新版本，在途申请继续按发起时绑定的版本办理。历史业务页面配置随版本恢复，审批人及角色成员仍按发布和办理时的系统权限校验。

新版本将原始 BPMN 与可执行 BPMN 一同存入 Flowable 部署资源。早期版本可使用其可执行 BPMN 恢复。此功能复用原表和 `flowable:model` 权限。

## 关联业务页面

1. 在「流程管理 → 业务页面」新增或编辑记录。业务页面编码创建后固定，名称、审批组件、站内路由、排序和启用状态可维护。
2. 审批组件使用前端随项目构建的 `src/views/**/approval.vue`，接收 `businessNo` 和 `instanceId`。组件可据此查询业务明细，业务接口应校验当前用户的查看权限。测试订单对应的组件和注册记录已提供。
3. 流程模型下拉从 `flow_approval_business_page` 读取启用记录。发布时保存页面快照，停用或编辑登记只影响后续选用和发布。旧版本按原记录打开业务页面。
4. 测试订单号为 `SO-DEMO-001`。数字字段 `amount` 由订单明细计算。订单按账号记录最近一次已提交的审批及其状态，发起与结束都在原流程事务内同步写回。保存申请草稿时保留当前订单审批状态。其他业务页面可从工作台选择对应流程并填写业务单号。

### 业务审批状态回写

业务模块实现 `BusinessApprovalStatusHandler` 并注册为 Spring Bean。`businessPage()` 返回业务页面编码。流程模型发布时绑定该编码，运行时根据发布版本调用对应处理器。已有发布版本关联了同一编码时，可直接接入处理器。每个编码注册一个处理器。

提交时，`resolveBusinessId()` 按业务单号校验申请人的提交权限并返回稳定的业务主键。模块可在主键中包含租户范围。系统持久化该主键，并按业务页面和主键限制同一单据只有一笔在途审批。处理结束状态时，`updateStatus()` 使用持久化主键并匹配业务表的当前审批实例 ID。使用与审批相同的数据源和事务，写入失败抛出异常，流程和业务状态一起回滚。启用 `supportsStatusMapping()` 的处理器可在模型里打开「同库状态映射」，配置全部七种提交后状态，并通过 `businessStatus` 读取映射值。

升级前的实例可按原单号办理。历史对账使用 `audit-business-approval-status.sql`，其中列出可区分为退回的历史记录、测试订单状态差异和未绑定主键的在途实例。各接入模块可用自己的业务表和实例 ID 对照。

### 流程模型配置操作回写接口

1. 在后端的 Spring 配置中登记业务接口，例子使用业务页面编码 `PURCHASE_ORDER`。同一个页面可以登记多个接口供模型选择。接口键一旦被发布版本使用，应保持与目标地址的对应关系。更换目标时登记新键，并在模型中重新选择和发布。

   ```yaml
   hpwit:
     flowable:
       callbacks:
         endpoints:
           purchase-action:
             label: 采购审批操作通知
             business-page: PURCHASE_ORDER
             url: ${PURCHASE_APPROVAL_CALLBACK_URL}
             method: PATCH
             bearer-token: ${PURCHASE_APPROVAL_CALLBACK_TOKEN:}
   ```

   接口支持 `POST` 和 `PATCH`，默认 `POST`。URL 使用 HTTPS，本机联调可用 `http://127.0.0.1`。令牌由后端添加到 `Authorization`，模型和前端只保存接口键。

2. 在「流程模型 → 设计」关联业务页面，展开「操作回写接口」，给需要同步的转交、加签、指定审批人或退回等操作选接口。保存并发布后，审批实例沿用发起时发布版本的配置。
3. 接口接收 JSON。`businessId` 优先使用已绑定的业务主键。实例尚未绑定主键时，使用 `businessNo` 的值。接收端应鉴别调用方，校验申请人和单据的关系，并使用 `eventId` 或 `Idempotency-Key` 去重。允许重复提交的单据应记录当前 `instanceId`，并根据递增的 `sequence`（十进制整数字符串）判断事件新旧。业务更新成功并提交后返回 HTTP 2xx，写入失败时返回非 2xx。

   ```json
   {
     "eventId": "approval-123:e7c41a19-3759-4ed6-87e4-a1dcde8b52c1",
     "sequence": "42",
     "businessPage": "PURCHASE_ORDER",
     "businessNo": "PO-001",
     "businessId": "17",
     "instanceId": "approval-123",
     "applicantId": "7",
     "approvalStatus": "RETURNED",
     "eventType": "OPERATION",
     "operation": "RETURN",
     "actorId": "8"
   }
   ```

操作事件还可携带适用的任务、节点、接收人或来源实例 ID。重复发生的同一操作拥有各自的事件 ID。

HTTP 回写记录和流程状态在同一事务提交。后台按业务单据顺序投递操作事件，失败后退避重试。审批详情显示操作回写进度，`flow_approval_status_callback` 保留 `delivered_at`、`attempts` 和 `last_error`。历史 HTTP 状态事件留在表中供查询，后台跳过这些记录并继续投递操作事件。

## 工作台操作

- 发起人可以保存、编辑、提交和删除草稿。审批中的申请可撤回、取消或催办。驳回或退回后可编辑并重新提交，已处理的申请保留在审批记录里。重新提交会生成新实例并记录来源。若最新版本关联了不同的业务页面，重新提交沿用原发布版本。
- 审批人可以同意、驳回、转交、加签或退回发起人。加签实现为当前节点先交由指定人员确认，确认后回到原审批人继续处理。退回结束本次流转并写入 `RETURNED`（已退回），发起人可编辑后生成新实例。
- 待处理申请的参与者可评论、上传附件或抄送。每份申请最多 10 个附件，每个最大 2 MB，下载入口校验实例查看权限。催办发送站内提醒，同一当前任务对同一接收人五分钟内只发送一次。
- 管理员操作由 `flowable:admin` 控制。流程干预只能移动到同一发布版本的其他审批节点。终止后可按原版本生成新实例，原记录保留。强制处理及干预都会写入操作日志。
- 详情顶部按权限和状态显示动作。审批中心可切换「待我审批」「我发起的」「我已审批」「抄送我的」「催办提醒」，前四项也有独立菜单。监控包括「全部审批」和「已结束」。列表可按分类、状态、关键字和日期筛选。待办支持批量同意、驳回和转交，每批最多 20 条并在同一事务内执行。抄送支持选中或全部标记已读。
- 已上线库运行升级 SQL 后，旧状态 `RUNNING` 转为 `PENDING`，新增 `DRAFT`、`CANCELLED` 和 `TERMINATED`。

## API 与数据一致性

所有接口使用项目现有认证，请求前缀为 `/v1/flowable`。列表返回 `data.list` 和 `data.total`。

| 请求 | 用途 |
| --- | --- |
| `GET /business-pages`、`POST /business-pages`、`PUT /business-pages/{code}` | 业务页面维护 |
| `GET /business-pages/options` | 模型设计下拉数据 |
| `POST /drafts`、`PUT /drafts/{id}` | 保存或编辑申请草稿 |
| `POST /instances/{id}/actions` | 按权限执行详情操作 |
| `GET /instances/{id}/edit-definition`、`POST /instances/{id}/resubmit` | 获取编辑字段配置并重新提交 |
| `GET /models`、`GET /models/{id}` | 查询模型与草稿 |
| `POST /models`、`PUT /models/{id}` | 创建与编辑草稿。编辑时提交当前 `revision` |
| `POST /models/{id}/publish?revision={revision}` | 校验并发布，重复发布同一草稿返回已有版本 |
| `GET /models/{id}/versions`、`GET /models/{id}/versions/{version}` | 查看发布历史和版本配置 |
| `POST /models/{id}/versions/{version}/restore` | 按请求中的当前草稿 `revision` 恢复旧版本 |
| `GET /approvers?roles=false&search=` | 查询可配置的账号或角色 |
| `GET /definitions` | 获取可发起的最新发布版本及表单 |
| `POST /instances` | 提交 `definitionId`、`requestId`、`title`、可选的 `businessNo` 与 `values`。重试同一提交时复用 UUID `requestId`。数字字段可传 JSON 数字或十进制字符串，最多 12 位整数和 6 位小数 |
| `GET /overview` | 查询审批中心各列表的统计 |
| `POST /instances/batch` | 批量处理当前用户的待办 |
| `POST /notices/read?kind=CC` | 将本人抄送记录标为已读。传 `kind=REMIND` 标记催办提醒 |
| `GET /instances?inbox=TODO` | 分页查看待办、本人申请 `MINE`、已办 `DONE` 或监控范围 `ALL`。查询 `MINE` 时可传 `businessNo` 筛选本人申请 |
| `GET /instances/{id}` | 返回申请、部署时的 BPMN、流转节点及审批记录 |
| `POST /instances/{id}/tasks/{taskId}/approve` | 通过，提交 `{"comment":"..."}` |
| `POST /instances/{id}/tasks/{taskId}/reject` | 驳回，须提交非空 `comment` |
| `POST /instances/{id}/withdraw` | 发起人撤销，提交可选的 `comment` |

金额分支使用精确的十进制比较。已有发布版本的条件沿用该版本的部署资源，重新保存并发布模型可让新申请使用新的比较规则。模型发布和发起使用模型行锁。任务处理与撤销使用申请行锁，并校验当前任务 ID。流程引擎、申请状态与操作记录在同一数据库事务内更新。备份或迁移时一并包含模块的 `flow_approval_*` 表和 Flowable 的 `ACT_*` 表。
