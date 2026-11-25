# zyxhit-admin-boot

## 防重复提交注解

`@RepeatSubmit` 是项目自带的防重复提交注解（定义在 ![img](./material-icons/java.svg)RepeatSubmit.java），直接标在写接口的方法上即可。目前项目里所有用法都被注释掉了（如 `SysRoleController.java:52` 的 `//@RepeatSubmit`)，把注释打开就能生效。

### 基本用法

```java
import com.hpwit.core.annotation.RepeatSubmit;

@Operation(summary = "新增角色")
@PostMapping
@RepeatSubmit   // 默认 5 秒内同一用户同一请求只允许提交一次
public Result<?> addRole(@Valid @RequestBody SysRoleForm roleForm) {
    ...
}
```

两个可选参数：

```java
@RepeatSubmit(expire = 10)          // 自定义防重窗口为 10 秒

@RepeatSubmit(key = "#form.name")   // 用 SpEL 指定区分请求的字段，
                                    // 同一用户提交 name 不同的表单互不拦截

@PostMapping
@RepeatSubmit(key = "#roleForm.name")   // 参数名叫 roleForm，表达式必须同名
public Result<?> addRole(@Valid @RequestBody SysRoleForm roleForm) {
    ...
}
```

### 工作原理

由 RepeatSubmitAspect.java 切面实现，用的是 Redis `SETNX + TTL` 原子占位：

1. 拦截标注了@RepeatSubmit的方法，拼出占位 key：lock:resubmit:{用户标识}:{请求方式+URI}:{参数指纹}
   - **用户标识**：优先取 Authorization 头里 Bearer Token 的 SHA-256，没有 token 则降级用客户端 IP。
   - **参数指纹**：配了 `key` 就哈希表达式值；没配就对方法参数 JSON 序列化后哈希（自动排除 `ServletRequest`、`MultipartFile`、`BindingResult` 等参数）。
2. 第一次请求用 `setIfAbsent` 写入占位并设置 `expire` 秒过期；窗口期内的第二次请求占位已存在，直接抛出 `BusinessException`，返回错误码 **425「请勿重复提交」**（`ResultCode.DUPLICATE_SUBMISSION`）。
3. 占位到期自动释放，无需手动解锁。



## 限流过滤器

## 新增文件

| 文件                                                         | 说明                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [RateLimiterFilter.java](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\core\filter\RateLimiterFilter.java) | IP 限流过滤器本体，逻辑与 youlai 一致：Redis `INCR` + 1 秒过期窗口计数同一 IP 的请求数，超过阈值返回 423；阈值从 `sysConfigService.getSystemConfig("IP_QPS_THRESHOLD_LIMIT")` 实时读取，未配置则记 warn 跳过限流，转换失败回退默认 10 |
| [FilterConfig.java](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\config\FilterConfig.java) | 过滤器注册。这是本项目与 youlai 唯一的架构差异：youlai 注册在 Spring Security 过滤器链里，nova 用的是 Sa-Token（MVC 拦截器），所以改用 `FilterRegistrationBean`，order=-100 排在 CORS（-101）之后——预检请求不会在 CorsFilter 终止前被计数，也不会被限流挡掉 |
| [WebUtil.java](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\common\utils\WebUtil.java) | 过滤器里无法走 `@RestControllerAdvice`，用它直接向响应体写 `Result` JSON；HTTP 状态码与业务码保持一致（都是 423），符合 `ResultCode`「对应HTTP错误码」的既有约定 |
| [seed_config.sql](F:\Projects\zyxhit-admin-boot\sql\seed_config.sql) | 项目里原本没有 `sys_config` 的建表脚本和预置数据，缺了它过滤器会一直跳过限流。按 nova 的 SQL 风格建表并预置 `IP_QPS_THRESHOLD_LIMIT = 10`，可重复执行 |

## 放开的既有代码

- [ResultCode.java:33](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\common\result\ResultCode.java#L33) — 423 `REQUEST_CONCURRENCY_LIMIT_EXCEEDED` 错误码（原本被注释）。
- [SysConfigServiceImpl.java:44](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\system\service\impl\SysConfigServiceImpl.java#L44) — `@PostConstruct init()`，启动时把 `sys_config` 全表加载进 Redis Hash `system:config`。这是过滤器能读到配置的前提。
- [SysConfigController.java:58](F:\Projects\zyxhit-admin-boot\src\main\java\com\hpwit\system\controller\SysConfigController.java#L58) — `PUT /v1/config/refresh` 手动刷新缓存接口。

## 生效链路

```
请求 → CORS(-101) → RateLimiterFilter(-100) → ... → Sa-Token 拦截器 → Controller
              │
              ├─ INCR rate_limiter:ip:{ip}，1 秒窗口计数
              ├─ HGET system:config IP_QPS_THRESHOLD_LIMIT（实时读缓存）
              └─ 超阈值 → HTTP 423 {"code":423,"message":"请求并发数超出限制"}
```

## 部署与使用注意

1. **先执行 `sql/seed_config.sql`**（建表 + 预置阈值），再重启应用——`@PostConstruct` 只在启动时加载一次缓存，库里有表有数据才能命中。
2. 后台改了阈值后**缓存不会自动更新**（与 youlai 行为一致，`edit()` 不刷缓存），需要调一次 `PUT /v1/config/refresh` 或重启才生效。
3. 限流粒度是**固定 1 秒窗口**的简单计数（非滑动窗口、非令牌桶），适合防单 IP 高频刷接口；如果以后要更精确的限流，可以考虑换 Redis + Lua 或 Bucket4j，当前的实现和参考项目保持一致。
4. 删除配置行或缓存缺失时过滤器只是跳过限流（不拦截任何请求），不会影响服务可用性。