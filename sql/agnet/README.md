# AI 聊天助手

使用 Spring Boot 现有 MySQL 数据源和 DeepSeek 配置。在目标数据库依次执行 `ai-chat-schema.sql` 和 `menu-ai-chat.sql`，然后重启后端并让用户重新登录以刷新动态路由。第二个脚本会将「AI 聊天助手」添加到「功能示例」，并沿用父目录已授权角色的菜单权限。

DeepSeek 读取应用现有配置：`DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL` 和 `DEEPSEEK_API_KEY`。API Key 只在服务端使用。

会话和消息按登录用户隔离。回复通过 SSE 流式传输，用户消息和完整的助手回复会持久化。AI 请求上下文最多带最近 24 条消息；会话列表最多返回最近 100 个会话，每个会话接口最多返回最近 200 条消息。每个用户每分钟最多发送 12 条消息。

| 请求 | 用途 |
| --- | --- |
| `GET /v1/agnet/chat/sessions` | 当前用户的会话列表 |
| `POST /v1/agnet/chat/sessions` | 新建会话 |
| `GET /v1/agnet/chat/sessions/{id}/messages` | 读取最近消息 |
| `PATCH /v1/agnet/chat/sessions/{id}` | 重命名会话 |
| `DELETE /v1/agnet/chat/sessions/{id}` | 删除会话及消息 |
| `POST /v1/agnet/chat/sessions/{id}/messages` | 发送 `{ "content": "..." }`，响应为 `chunk`、`error` 或 `done` SSE 事件 |
