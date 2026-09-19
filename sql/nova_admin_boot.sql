/*
 Navicat Premium Dump SQL

 Source Server         : local(127.0.0.1)
 Source Server Type    : MySQL
 Source Server Version : 80030 (8.0.30)
 Source Host           : 127.0.0.1:3306
 Source Schema         : nova_admin_boot

 Target Server Type    : MySQL
 Target Server Version : 80030 (8.0.30)
 File Encoding         : 65001

 Date: 19/09/2026 20:42:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置值',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述、备注',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '系统限流QPS', 'IP_QPS_THRESHOLD_LIMIT', '10', '单个IP请求的最大每秒查询数（QPS）阈值Key', 0, 1, NULL, '2026-09-18 01:29:31', '2026-09-18 01:29:31');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部门编码',
  `parent_id` bigint UNSIGNED NULL DEFAULT 0 COMMENT '父节点id',
  `tree_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父节点id路径',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除(0-否 1-是)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, '珠海市智域星河科技有限公司', 'MAKFF045-5', 0, '0', 1, 1, 0, 1, 1, '2026-09-12 08:49:13', '2026-09-12 20:02:28');
INSERT INTO `sys_dept` VALUES (2, '研发部', 'Development', 1, '0,1', 1, 1, 0, 1, NULL, '2026-09-12 20:06:16', '2026-09-12 20:06:16');
INSERT INTO `sys_dept` VALUES (3, '设计部', 'Design', 1, '0,1', 2, 1, 0, 1, NULL, '2026-09-12 20:06:41', '2026-09-12 20:06:41');
INSERT INTO `sys_dept` VALUES (4, '市场部', 'Market', 1, '0,1', 3, 1, 0, 1, NULL, '2026-09-12 20:07:22', '2026-09-12 20:07:22');
INSERT INTO `sys_dept` VALUES (5, '财务部', 'Finance', 1, '0,1', 4, 1, 0, 1, NULL, '2026-09-12 20:07:50', '2026-09-12 20:07:50');
INSERT INTO `sys_dept` VALUES (6, '‌运营部', 'Operation', 1, '0,1', 5, 1, 0, 1, NULL, '2026-09-12 20:08:24', '2026-09-12 20:08:24');

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典名称',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(1:启用, 0:停用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_code`(`dict_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, 'gender', '性别', 1, '', 0, 1, NULL, '2026-09-17 23:19:41', '2026-09-17 23:19:41');
INSERT INTO `sys_dict` VALUES (2, 'notice_type', '通知类型', 1, '', 0, 1, NULL, '2026-09-18 00:15:10', '2026-09-18 00:15:10');
INSERT INTO `sys_dict` VALUES (3, 'notice_level', '通知级别', 1, '', 0, 1, NULL, '2026-09-18 01:15:17', '2026-09-18 01:15:17');

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码(关联 sys_dict.dict_code)',
  `label` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典项名称',
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典项值',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(1-正常, 0-禁用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `tag_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签类型',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dict_code`(`dict_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES (1, 'gender', '男', '1', 1, 1, NULL, 'default', 0, 1, NULL, '2026-09-17 23:20:13', '2026-09-17 23:20:13');
INSERT INTO `sys_dict_item` VALUES (2, 'gender', '女', '2', 2, 1, NULL, 'default', 0, 1, NULL, '2026-09-17 23:20:27', '2026-09-17 23:20:27');
INSERT INTO `sys_dict_item` VALUES (3, 'gender', '保密', '0', 3, 1, NULL, 'default', 0, 1, NULL, '2026-09-17 23:20:38', '2026-09-17 23:20:38');
INSERT INTO `sys_dict_item` VALUES (4, 'notice_type', '系统升级', '1', 1, 1, NULL, 'primary', 0, 1, NULL, '2026-09-18 00:54:57', '2026-09-18 00:54:57');
INSERT INTO `sys_dict_item` VALUES (5, 'notice_type', '系统维护', '2', 2, 1, NULL, 'tertiary', 0, 1, NULL, '2026-09-18 00:55:22', '2026-09-18 00:55:22');
INSERT INTO `sys_dict_item` VALUES (6, 'notice_type', '安全警告', '3', 3, 1, NULL, 'warning', 0, 1, 1, '2026-09-18 00:55:33', '2026-09-18 00:55:40');
INSERT INTO `sys_dict_item` VALUES (7, 'notice_type', '假期通知', '4', 4, 1, NULL, 'success', 0, 1, NULL, '2026-09-18 00:55:59', '2026-09-18 00:55:59');
INSERT INTO `sys_dict_item` VALUES (8, 'notice_type', '公司新闻', '5', 5, 1, NULL, 'info', 0, 1, NULL, '2026-09-18 00:56:17', '2026-09-18 00:56:17');
INSERT INTO `sys_dict_item` VALUES (9, 'notice_type', '其他', '99', 99, 1, NULL, 'default', 0, 1, NULL, '2026-09-18 00:56:27', '2026-09-18 00:56:27');
INSERT INTO `sys_dict_item` VALUES (10, 'notice_level', '低', 'L', 1, 1, NULL, 'info', 0, 1, NULL, '2026-09-18 01:15:36', '2026-09-18 01:15:36');
INSERT INTO `sys_dict_item` VALUES (11, 'notice_level', '中', 'M', 2, 1, NULL, 'warning', 0, 1, NULL, '2026-09-18 01:15:51', '2026-09-18 01:15:51');
INSERT INTO `sys_dict_item` VALUES (12, 'notice_level', '高', 'H', 3, 1, NULL, 'error', 0, 1, NULL, '2026-09-18 01:16:03', '2026-09-18 01:16:03');

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务Id',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型/后缀名',
  `file_size` bigint UNSIGNED NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `size_display` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '显示文件大小(如 130KB)',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务器存储路径',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件访问URL',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件来源:二维码生成/用户上传 等',
  `status` tinyint NULL DEFAULT 1 COMMENT '文件状态(1=有效,0=删除)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_source_type`(`source_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_file
-- ----------------------------

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日志模块(LogModuleEnum:EXCEPTION/LOGIN/USER/DEPT/ROLE/MENU/DICT/SETTING/OTHER)',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求方式(GET/POST/...)',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数',
  `response_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应参数',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日志内容',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求路径',
  `method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求方法(Controller#Method)',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP 地址',
  `province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '城市',
  `browser` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浏览器',
  `browser_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浏览器版本',
  `os` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '终端系统',
  `execution_time` bigint NULL DEFAULT 0 COMMENT '执行时间(毫秒)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES (1, 'LOGIN', 'POST', '{\"username\":\"admin\",\"password\":\"123456\",\"captchaKey\":\"fb402acde2324476ac889d05792ee881\",\"captchaCode\":\"PFSD\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 87, 2, '2026-09-18 01:27:14');
INSERT INTO `sys_log` VALUES (2, 'LOGIN', 'POST', '{\"username\":\"superadmin\",\"password\":\"123456\",\"captchaKey\":\"76e864584f28415e860ac0071b1f48e8\",\"captchaCode\":\"9Z2S\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 80, 1, '2026-09-18 01:27:34');
INSERT INTO `sys_log` VALUES (3, 'SETTING', 'GET', '{\"pageNum\":1,\"pageSize\":10}', NULL, '系统配置分页列表', '/v1/config/page', 'page', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 11, 1, '2026-09-18 01:28:44');
INSERT INTO `sys_log` VALUES (4, 'SETTING', 'GET', '{\"pageNum\":1,\"pageSize\":10}', NULL, '系统配置分页列表', '/v1/config/page', 'page', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 12, 1, '2026-09-18 01:28:51');
INSERT INTO `sys_log` VALUES (5, 'SETTING', 'POST', '{\"configName\":\"系统限流QPS\",\"configKey\":\"IP_QPS_THRESHOLD_LIMIT\",\"configValue\":\"10\",\"remark\":\"单个IP请求的最大每秒查询数（QPS）阈值Key\"}', NULL, '新增系统配置', '/v1/config', 'save', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 12, 1, '2026-09-18 01:29:31');
INSERT INTO `sys_log` VALUES (6, 'SETTING', 'GET', '{\"pageNum\":1,\"pageSize\":10}', NULL, '系统配置分页列表', '/v1/config/page', 'page', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 31, 1, '2026-09-18 01:29:31');
INSERT INTO `sys_log` VALUES (7, 'SETTING', 'GET', '{\"pageNum\":1,\"pageSize\":10}', NULL, '系统配置分页列表', '/v1/config/page', 'page', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 25, 1, '2026-09-18 02:15:57');
INSERT INTO `sys_log` VALUES (8, 'SETTING', 'GET', '{\"pageNum\":1,\"pageSize\":10}', NULL, '系统配置分页列表', '/v1/config/page', 'page', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 14, 1, '2026-09-18 02:24:17');
INSERT INTO `sys_log` VALUES (9, 'LOGIN', 'POST', '{\"username\":\"huangpw\",\"password\":\"123456\",\"captchaKey\":\"43d259c547304017952def68b79fa409\",\"captchaCode\":\"KSB7\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 98, 3, '2026-09-18 03:18:08');
INSERT INTO `sys_log` VALUES (10, 'LOGIN', 'POST', '{\"username\":\"superadmin\",\"password\":\"123456\",\"captchaKey\":\"137afd3fb6544a329905ab99d598d744\",\"captchaCode\":\"Z7KM\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 91, 1, '2026-09-18 03:18:30');
INSERT INTO `sys_log` VALUES (11, 'LOGIN', 'POST', '{\"username\":\"huangpw\",\"password\":\"123456\",\"captchaKey\":\"06637656d2a74bf5a889e94ce9cb6d53\",\"captchaCode\":\"HZ4X\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 84, 3, '2026-09-18 03:19:15');
INSERT INTO `sys_log` VALUES (12, 'LOGIN', 'POST', '{\"username\":\"superadmin\",\"password\":\"123456\",\"captchaKey\":\"7b9fb38a2d904e21b2ce02392d9eeb8b\",\"captchaCode\":\"KMK3\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 0, NULL, '2026-09-18 03:19:49');
INSERT INTO `sys_log` VALUES (13, 'LOGIN', 'POST', '{\"username\":\"superadmin\",\"password\":\"123456\",\"captchaKey\":\"7b9fb38a2d904e21b2ce02392d9eeb8b\",\"captchaCode\":\"KMK8\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 76, 1, '2026-09-18 03:19:52');
INSERT INTO `sys_log` VALUES (14, 'LOGIN', 'POST', '{\"username\":\"huangpw\",\"password\":\"123456\",\"captchaKey\":\"17ad65c6775142a298e003ff506a011c\",\"captchaCode\":\"F5RN\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 82, 3, '2026-09-18 03:20:19');
INSERT INTO `sys_log` VALUES (15, 'LOGIN', 'POST', '{\"username\":\"superadmin\",\"password\":\"123456\",\"captchaKey\":\"01a75245c30d4e3f9f837452247bca58\",\"captchaCode\":\"ZPNS\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 80, 1, '2026-09-18 03:21:15');
INSERT INTO `sys_log` VALUES (16, 'LOGIN', 'POST', '{\"username\":\"huangpw\",\"password\":\"123456\",\"captchaKey\":\"538880b9b1d84cbc999db23c2d1150fc\",\"captchaCode\":\"6ZC3\"}', NULL, '登录', '/v1/auth/login', 'login', '192.168.31.95', '0', '内网IP', 'Chrome', '153.0.0.0', 'Windows 10 or Windows Server 2016', 79, 3, '2026-09-18 03:21:45');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint UNSIGNED NULL DEFAULT 0 COMMENT '父菜单ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '菜单类型(1-菜单;2-目录;3-外链;4-按钮权限)',
  `route_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路由名称(Vue Router name)',
  `route_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组件路径(省略 .vue 后缀)',
  `perm` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限标识',
  `visible` tinyint NULL DEFAULT 1 COMMENT '显示状态(1:显示;0:隐藏)',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `redirect` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '跳转路径',
  `tree_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父节点路径,以英文逗号(,)分割',
  `keep_alive` tinyint NULL DEFAULT 0 COMMENT '【菜单】是否开启页面缓存(1:开启;0:关闭)',
  `always_show` tinyint NULL DEFAULT 0 COMMENT '【目录】只有一个子路由是否始终显示(1:是 0:否)',
  `requires_auth` tinyint NULL DEFAULT 1 COMMENT '是否需要登录权限(1:是 0:否)',
  `params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路由参数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, '仪表盘', 2, 'dashboard', '/dashboard', 'Layout', NULL, 1, 1, 'icon-park-outline:analysis', '/dashboard/workbench', '0', 1, 1, 0, NULL, '2026-09-12 20:36:02', '2026-09-12 20:36:02');
INSERT INTO `sys_menu` VALUES (2, 1, '工作台', 1, 'workbench', '/dashboard/workbench', '/dashboard/workbench/index', NULL, 1, 1, 'icon-park-outline:alarm', NULL, '0,1', 1, 1, 1, NULL, '2026-09-12 20:39:20', '2026-09-12 20:39:20');
INSERT INTO `sys_menu` VALUES (3, 1, '监控页', 1, 'monitor', '/dashboard/monitor', '/dashboard/monitor/index', NULL, 1, 2, 'icon-park-outline:anchor', NULL, '0,1', 1, 1, 1, NULL, '2026-09-12 20:42:20', '2026-09-12 20:42:20');
INSERT INTO `sys_menu` VALUES (4, 0, '多级菜单演示', 2, 'multi', '/multi', 'Layout', NULL, 1, 81, 'icon-park-outline:list', '/multi/multi2', '0', 1, 1, 0, NULL, '2026-09-12 20:44:33', '2026-09-12 20:44:33');
INSERT INTO `sys_menu` VALUES (5, 4, '多级菜单子页', 1, 'multi2', '/multi/multi2', '/demo/multi/multi2/index', NULL, 1, 1, 'icon-park-outline:list', NULL, '0,4', 1, 1, 1, NULL, '2026-09-12 20:45:32', '2026-09-12 20:45:32');
INSERT INTO `sys_menu` VALUES (6, 4, '多级菜单的详情页', 1, 'multi2Detail', '/multi/multi2/detail', '/demo/multi/multi2/detail/index', NULL, 0, 2, 'icon-park-outline:list', NULL, '0,4', 1, 1, 1, NULL, '2026-09-12 20:47:18', '2026-09-12 20:50:32');
INSERT INTO `sys_menu` VALUES (7, 4, '多级菜单', 2, 'multi3', '/multi/multi3', 'Layout', NULL, 1, 3, 'icon-park-outline:list', '/multi/multi3/multi4', '0,4', 1, 1, 0, NULL, '2026-09-12 20:49:47', '2026-09-12 20:49:47');
INSERT INTO `sys_menu` VALUES (8, 7, '多级菜单3-1', 1, 'multi4', '/multi/multi3/multi4', '/demo/multi/multi3/multi4/index', NULL, 1, 2, 'icon-park-outline:list', NULL, '0,4,7', 1, 1, 1, NULL, '2026-09-12 20:52:05', '2026-09-12 20:52:05');
INSERT INTO `sys_menu` VALUES (9, 0, '列表页', 2, 'list', '/list', 'Layout', NULL, 1, 82, 'icon-park-outline:list-two', '/list/commonList', '0', 1, 1, 0, NULL, '2026-09-12 20:59:32', '2026-09-12 20:59:32');
INSERT INTO `sys_menu` VALUES (10, 9, '常用列表', 1, 'commonList', '/list/commonList', '/demo/list/commonList/index', NULL, 1, 1, 'icon-park-outline:list-view', NULL, '0,9', 1, 1, 1, NULL, '2026-09-12 21:01:59', '2026-09-12 21:02:20');
INSERT INTO `sys_menu` VALUES (11, 9, '卡片列表', 1, 'cardList', '/list/cardList', '/demo/list/cardList/index', NULL, 1, 2, 'icon-park-outline:view-grid-list', NULL, '0,9', 1, 1, 1, NULL, '2026-09-12 21:03:15', '2026-09-12 21:03:15');
INSERT INTO `sys_menu` VALUES (12, 0, '功能示例', 2, 'demo', '/demo', 'Layout', NULL, 1, 83, 'icon-park-outline:application-one', '/demo/fetch', '0', 1, 1, 0, NULL, '2026-09-12 21:20:43', '2026-09-12 21:21:03');
INSERT INTO `sys_menu` VALUES (13, 12, '请求示例', 1, 'fetch', '/demo/fetch', '/demo/fetch/index', NULL, 1, 1, 'icon-park-outline:international', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:22:07', '2026-09-12 21:22:07');
INSERT INTO `sys_menu` VALUES (14, 12, 'ECharts', 1, 'echarts', '/demo/echarts', '/demo/echarts/index', NULL, 1, 2, 'icon-park-outline:chart-proportion', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:23:04', '2026-09-12 21:23:04');
INSERT INTO `sys_menu` VALUES (15, 12, '地图', 1, 'map', '/demo/map', '/demo/map/index', NULL, 1, 3, 'icon-park-outline:map-distance', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:24:00', '2026-09-12 21:24:00');
INSERT INTO `sys_menu` VALUES (16, 12, '编辑器', 2, 'editor', '/demo/editor', 'Layout', NULL, 1, 4, 'icon-park-outline:editor', NULL, '0,12', 1, 1, 0, NULL, '2026-09-12 21:24:45', '2026-09-12 21:24:45');
INSERT INTO `sys_menu` VALUES (17, 16, 'MarkDown', 1, 'editorMd', '/demo/editor/md', '/demo/editor/md/index', NULL, 1, 1, 'ri:markdown-line', NULL, '0,12,16', 1, 1, 1, NULL, '2026-09-12 21:25:53', '2026-09-12 21:31:00');
INSERT INTO `sys_menu` VALUES (18, 16, '富文本', 1, 'editorRich', '/demo/editor/rich', '/demo/editor/rich/index', NULL, 1, 2, 'icon-park-outline:edit-one', NULL, '0,12,16', 1, 1, 1, NULL, '2026-09-12 21:27:02', '2026-09-12 21:31:39');
INSERT INTO `sys_menu` VALUES (19, 12, '剪贴板', 1, 'clipboard', '/demo/clipboard', '/demo/clipboard/index', NULL, 1, 5, 'icon-park-outline:clipboard', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:32:40', '2026-09-12 21:32:40');
INSERT INTO `sys_menu` VALUES (20, 12, '图标', 1, 'icons', '/demo/icons/index', '/demo/icons/index', NULL, 1, 6, 'local:cool', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:33:49', '2026-09-12 21:33:49');
INSERT INTO `sys_menu` VALUES (21, 12, '二维码', 1, 'QRCode', '/demo/QRCode', '/demo/QRCode/index', NULL, 1, 7, 'icon-park-outline:two-dimensional-code', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:35:05', '2026-09-12 21:35:05');
INSERT INTO `sys_menu` VALUES (22, 12, '省市区联动', 1, 'cascader', '/demo/cascader', '/demo/cascader/index', NULL, 1, 8, 'icon-park-outline:add-subset', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:36:12', '2026-09-12 21:36:12');
INSERT INTO `sys_menu` VALUES (23, 12, '字典示例', 1, 'dict', '/demo/dict', '/demo/dict/index', NULL, 1, 9, 'icon-park-outline:book-one', NULL, '0,12', 1, 1, 1, NULL, '2026-09-12 21:36:55', '2026-09-12 21:36:55');
INSERT INTO `sys_menu` VALUES (24, 0, '外链文档', 2, 'documents', '/documents', 'Layout', NULL, 1, 84, 'icon-park-outline:file-doc', NULL, '0', 1, 1, 0, NULL, '2026-09-12 21:37:57', '2026-09-12 21:37:57');
INSERT INTO `sys_menu` VALUES (25, 24, 'Vue', 1, 'documentsVue', '/documents/vue', '/demo/documents/vue/index', NULL, 1, 1, 'logos:vue', NULL, '0,24', 1, 1, 0, NULL, '2026-09-12 21:39:08', '2026-09-12 21:44:52');
INSERT INTO `sys_menu` VALUES (26, 24, 'Vite', 1, 'documentsVite', '/documents/vite', '/demo/documents/vite/index', NULL, 1, 2, 'logos:vitejs', NULL, '0,24', 1, 1, 0, NULL, '2026-09-12 21:45:28', '2026-09-12 21:45:50');
INSERT INTO `sys_menu` VALUES (27, 24, 'VueUse（外链）', 3, NULL, 'https://vueuse.org/guide/', NULL, NULL, 1, 3, 'logos:vueuse', NULL, '0,24', 1, 1, 0, NULL, '2026-09-12 21:47:06', '2026-09-12 21:47:27');
INSERT INTO `sys_menu` VALUES (28, 24, 'Nova docs', 3, NULL, 'https://nova-admin-docs.netlify.app/', NULL, NULL, 1, 4, 'local:logo', NULL, '0,24', 1, 1, 0, NULL, '2026-09-12 21:48:28', '2026-09-12 21:48:48');
INSERT INTO `sys_menu` VALUES (29, 24, '公共示例页（外链）', 3, NULL, 'https://nova-admin.pages.dev/public', NULL, NULL, 1, 5, 'local:logo', NULL, '0,24', 1, 1, 0, NULL, '2026-09-12 21:49:59', '2026-09-12 21:49:59');
INSERT INTO `sys_menu` VALUES (30, 0, '权限', 2, 'permission', '/permission', 'Layout', NULL, 1, 85, 'icon-park-outline:people-safe', NULL, '0', 1, 1, 0, NULL, '2026-09-12 21:50:55', '2026-09-12 21:50:55');
INSERT INTO `sys_menu` VALUES (31, 30, '权限示例', 1, 'permissionDemo', '/permission/permission', '/demo/permission/permission/index', NULL, 1, 1, 'icon-park-outline:right-user', NULL, '0,30', 1, 1, 0, NULL, '2026-09-12 21:51:48', '2026-09-12 21:51:48');
INSERT INTO `sys_menu` VALUES (32, 30, 'super可见', 1, 'justSuper', '/permission/justSuper', '/demo/permission/just-super/index', NULL, 1, 2, 'icon-park-outline:wrong-user', NULL, '0,30', 1, 1, 0, NULL, '2026-09-12 21:52:25', '2026-09-13 02:59:09');
INSERT INTO `sys_menu` VALUES (33, 0, '异常页', 2, 'error', '/error', 'Layout', NULL, 1, 86, 'icon-park-outline:error-computer', NULL, '0', 1, 1, 0, NULL, '2026-09-12 21:53:04', '2026-09-12 21:53:04');
INSERT INTO `sys_menu` VALUES (34, 33, '403', 1, 'demo403', '/error/403', '/error/403/index', NULL, 1, 1, 'carbon:error', NULL, '0,33', 0, 1, 0, NULL, '2026-09-12 21:53:39', '2026-09-13 03:44:32');
INSERT INTO `sys_menu` VALUES (35, 33, '404', 1, 'demo404', '/error/404', '/error/404/index', NULL, 1, 2, 'icon-park-outline:error', NULL, '0,33', 0, 1, 0, NULL, '2026-09-12 21:54:47', '2026-09-13 03:44:37');
INSERT INTO `sys_menu` VALUES (36, 33, '500', 1, 'demo500', '/error/500', '/error/500/index', NULL, 1, 3, 'carbon:data-error', NULL, '0,33', 0, 1, 0, NULL, '2026-09-12 21:55:19', '2026-09-13 03:44:43');
INSERT INTO `sys_menu` VALUES (37, 0, '系统设置', 2, 'setting', '/setting', 'Layout', NULL, 1, 87, 'icon-park-outline:setting', NULL, '0', 1, 1, 0, NULL, '2026-09-13 00:29:19', '2026-09-13 00:29:25');
INSERT INTO `sys_menu` VALUES (38, 37, '用户设置', 1, 'accountSetting', '/setting/account', '/setting/account/index', NULL, 1, 1, 'icon-park-outline:user', NULL, '0,37', 1, 1, 1, NULL, '2026-09-13 00:30:21', '2026-09-13 00:30:21');
INSERT INTO `sys_menu` VALUES (39, 37, '字典设置', 1, 'dictionarySetting', '/setting/dictionary', '/setting/dictionary/index', NULL, 1, 2, 'icon-park-outline:book-one', NULL, '0,37', 1, 1, 1, NULL, '2026-09-13 00:30:57', '2026-09-13 00:30:57');
INSERT INTO `sys_menu` VALUES (40, 37, '菜单设置', 1, 'menuSetting', '/setting/menu', '/setting/menu/index', NULL, 1, 3, 'icon-park-outline:application-menu', NULL, '0,37', 1, 1, 1, NULL, '2026-09-13 00:31:35', '2026-09-13 00:31:35');
INSERT INTO `sys_menu` VALUES (41, 0, '个人中心', 1, 'userCenter', '/user-center', '/build-in/user-center/index', NULL, 0, 88, 'carbon:user-avatar-filled-alt', NULL, '0', 1, 1, 1, NULL, '2026-09-13 00:32:31', '2026-09-13 02:54:45');
INSERT INTO `sys_menu` VALUES (42, 0, '关于', 1, 'about', '/about', '/demo/about/index', NULL, 1, 89, 'icon-park-outline:info', NULL, '0', 1, 1, 1, NULL, '2026-09-13 00:34:08', '2026-09-13 00:34:08');
INSERT INTO `sys_menu` VALUES (43, 0, '系统管理', 2, 'system', '/system', 'Layout', NULL, 1, 80, 'icon-park-outline:setting-two', NULL, '0', 1, 1, 0, NULL, '2026-09-13 00:35:22', '2026-09-13 00:36:03');
INSERT INTO `sys_menu` VALUES (44, 43, '用户管理', 1, 'systemUser', '/system/user', '/system/user/index', NULL, 1, 1, 'icon-park-outline:user', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:37:05', '2026-09-13 00:37:05');
INSERT INTO `sys_menu` VALUES (45, 43, '角色管理', 1, 'systemRole', '/system/role', '/system/role/index', NULL, 1, 2, 'icon-park-outline:user-business', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:37:44', '2026-09-13 00:37:44');
INSERT INTO `sys_menu` VALUES (46, 43, '菜单管理', 1, 'systemMenu', '/system/menu', '/system/menu/index', NULL, 1, 3, 'icon-park-outline:application-menu', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:40:23', '2026-09-13 00:40:23');
INSERT INTO `sys_menu` VALUES (47, 43, '部门管理', 1, 'systemDept', '/system/dept', '/system/dept/index', NULL, 1, 4, 'icon-park-outline:branch-one', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:41:03', '2026-09-13 00:41:03');
INSERT INTO `sys_menu` VALUES (48, 43, '字典管理', 1, 'systemDict', '/system/dict', '/system/dict/index', NULL, 1, 5, 'icon-park-outline:book-one', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:41:41', '2026-09-13 00:41:41');
INSERT INTO `sys_menu` VALUES (49, 43, '操作日志', 1, 'systemLog', '/system/log', '/system/log/index', NULL, 1, 6, 'icon-park-outline:log', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:43:12', '2026-09-13 00:43:12');
INSERT INTO `sys_menu` VALUES (50, 43, '系统配置', 1, 'systemConfig', '/system/config', '/system/config/index', NULL, 1, 7, 'carbon:settings-edit', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:44:18', '2026-09-13 00:44:18');
INSERT INTO `sys_menu` VALUES (51, 43, '通知公告', 1, 'systemNotice', '/system/notice', '/system/notice/index', NULL, 1, 8, 'icon-park-outline:volume-notice', NULL, '0,43', 1, 1, 1, NULL, '2026-09-13 00:45:12', '2026-09-13 00:45:12');
INSERT INTO `sys_menu` VALUES (52, 44, '搜索 / 重置', 4, NULL, '', NULL, 'sys:user:query', 1, 1, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:28:29', '2026-09-18 02:28:29');
INSERT INTO `sys_menu` VALUES (53, 44, '新建', 4, NULL, '', NULL, 'sys:user:add', 1, 2, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:28:59', '2026-09-18 02:28:59');
INSERT INTO `sys_menu` VALUES (54, 44, '编辑', 4, NULL, '', NULL, 'sys:user:edit', 1, 3, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:30:45', '2026-09-18 02:30:45');
INSERT INTO `sys_menu` VALUES (55, 44, '删除', 4, NULL, '', NULL, 'sys:user:delete', 1, 4, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:31:06', '2026-09-18 02:31:06');
INSERT INTO `sys_menu` VALUES (56, 44, '重置密码', 4, NULL, '', NULL, 'sys:user:resetPwd', 1, 5, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:31:24', '2026-09-18 02:31:24');
INSERT INTO `sys_menu` VALUES (57, 44, '启用/禁用', 4, NULL, '', NULL, 'sys:user:status', 1, 6, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:31:51', '2026-09-18 02:31:51');
INSERT INTO `sys_menu` VALUES (58, 44, '导入', 4, NULL, '', NULL, 'sys:user:import', 1, 7, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:32:10', '2026-09-18 02:32:10');
INSERT INTO `sys_menu` VALUES (59, 44, '导出', 4, NULL, '', NULL, 'sys:user:export', 1, 8, '', NULL, '0,43,44', 1, 1, 0, NULL, '2026-09-18 02:32:24', '2026-09-18 02:32:35');
INSERT INTO `sys_menu` VALUES (60, 45, '搜索 / 重置', 4, NULL, '', NULL, 'sys:role:query', 1, 1, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:33:10', '2026-09-18 02:33:18');
INSERT INTO `sys_menu` VALUES (61, 45, '新建', 4, NULL, '', NULL, 'sys:role:add', 1, 2, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:33:58', '2026-09-18 02:37:21');
INSERT INTO `sys_menu` VALUES (62, 45, '编辑', 4, NULL, '', NULL, 'sys:role:edit', 1, 3, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:34:16', '2026-09-18 02:34:16');
INSERT INTO `sys_menu` VALUES (63, 45, '删除 / 批量删除', 4, NULL, '', NULL, 'sys:role:delete', 1, 4, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:34:49', '2026-09-18 02:34:49');
INSERT INTO `sys_menu` VALUES (64, 45, '分配权限', 4, NULL, '', NULL, 'sys:role:assignPerm', 1, 5, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:35:07', '2026-09-18 02:35:07');
INSERT INTO `sys_menu` VALUES (65, 45, '下载', 4, NULL, '', NULL, 'sys:role:export', 1, 6, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:35:34', '2026-09-18 02:35:34');
INSERT INTO `sys_menu` VALUES (66, 45, '导入', 4, NULL, '', NULL, 'sys:role:import', 1, 7, '', NULL, '0,43,45', 1, 1, 0, NULL, '2026-09-18 02:35:58', '2026-09-18 02:35:58');
INSERT INTO `sys_menu` VALUES (67, 46, '搜索 / 重置', 4, NULL, '', NULL, 'sys:role:query', 1, 1, '', NULL, '0,43,46', 1, 1, 0, NULL, '2026-09-18 02:37:41', '2026-09-18 02:37:47');
INSERT INTO `sys_menu` VALUES (68, 46, '新建', 4, NULL, '', NULL, 'sys:menu:add', 1, 2, '', NULL, '0,43,46', 1, 1, 0, NULL, '2026-09-18 02:58:23', '2026-09-18 02:58:23');
INSERT INTO `sys_menu` VALUES (69, 46, '编辑', 4, NULL, '', NULL, 'sys:menu:edit', 1, 3, '', NULL, '0,43,46', 1, 1, 0, NULL, '2026-09-18 02:58:47', '2026-09-18 02:58:47');
INSERT INTO `sys_menu` VALUES (70, 46, '删除 / 批量删除', 4, NULL, '', NULL, 'sys:menu:delete', 1, 4, '', NULL, '0,43,46', 1, 1, 0, NULL, '2026-09-18 02:59:03', '2026-09-18 02:59:23');
INSERT INTO `sys_menu` VALUES (71, 47, '搜索 / 重置', 4, NULL, '', NULL, 'sys:dept:query', 1, 1, '', NULL, '0,43,47', 1, 1, 0, NULL, '2026-09-18 03:00:33', '2026-09-18 03:00:41');
INSERT INTO `sys_menu` VALUES (72, 47, '新建', 4, NULL, '', NULL, 'sys:dept:add', 1, 2, '', NULL, '0,43,47', 1, 1, 0, NULL, '2026-09-18 03:01:07', '2026-09-18 03:01:07');
INSERT INTO `sys_menu` VALUES (73, 47, '查看', 4, NULL, '', NULL, 'sys:dept:view', 1, 3, '', NULL, '0,43,47', 1, 1, 0, NULL, '2026-09-18 03:01:28', '2026-09-18 03:01:28');
INSERT INTO `sys_menu` VALUES (74, 47, '编辑', 4, NULL, '', NULL, 'sys:dept:edit', 1, 4, '', NULL, '0,43,47', 1, 1, 0, NULL, '2026-09-18 03:01:47', '2026-09-18 03:01:47');
INSERT INTO `sys_menu` VALUES (75, 47, '删除 / 批量删除', 4, NULL, '', NULL, 'sys:dept:delete', 1, 5, '', NULL, '0,43,47', 1, 1, 0, NULL, '2026-09-18 03:02:20', '2026-09-18 03:02:20');
INSERT INTO `sys_menu` VALUES (76, 48, '搜索 / 重置', 4, NULL, '', NULL, 'sys:dict:query', 1, 1, '', NULL, '0,43,48', 1, 1, 0, NULL, '2026-09-18 03:02:45', '2026-09-18 03:02:59');
INSERT INTO `sys_menu` VALUES (77, 48, '查看分类', 4, NULL, '', NULL, 'sys:dict:view', 1, 3, '', NULL, '0,43,48', 1, 1, 0, NULL, '2026-09-18 03:03:16', '2026-09-18 03:03:23');
INSERT INTO `sys_menu` VALUES (78, 48, '新建', 4, NULL, '', NULL, 'sys:dict:add', 1, 2, '', NULL, '0,43,48', 1, 1, 0, NULL, '2026-09-18 03:03:37', '2026-09-18 03:03:37');
INSERT INTO `sys_menu` VALUES (79, 48, '编辑', 4, NULL, '', NULL, 'sys:dict:edit', 1, 4, '', NULL, '0,43,48', 1, 1, 0, NULL, '2026-09-18 03:04:04', '2026-09-18 03:04:04');
INSERT INTO `sys_menu` VALUES (80, 48, '删除', 4, NULL, '', NULL, 'sys:dict:delete', 1, 5, '', NULL, '0,43,48', 1, 1, 0, NULL, '2026-09-18 03:04:24', '2026-09-18 03:04:24');
INSERT INTO `sys_menu` VALUES (81, 49, '搜索 / 重置', 4, NULL, '', NULL, 'sys:log:query', 1, 1, '', NULL, '0,43,49', 1, 1, 0, NULL, '2026-09-18 03:04:45', '2026-09-18 03:05:07');
INSERT INTO `sys_menu` VALUES (82, 50, '搜索 / 重置', 4, NULL, '', NULL, 'sys:config:query', 1, 1, '', NULL, '0,43,50', 1, 1, 0, NULL, '2026-09-18 03:05:36', '2026-09-18 03:05:58');
INSERT INTO `sys_menu` VALUES (83, 50, '新建', 4, NULL, '', NULL, 'sys:config:add', 1, 2, '', NULL, '0,43,50', 1, 1, 0, NULL, '2026-09-18 03:06:26', '2026-09-18 03:06:26');
INSERT INTO `sys_menu` VALUES (84, 50, '编辑', 4, NULL, '', NULL, 'sys:config:edit', 1, 3, '', NULL, '0,43,50', 1, 1, 0, NULL, '2026-09-18 03:06:40', '2026-09-18 03:06:40');
INSERT INTO `sys_menu` VALUES (85, 50, '删除', 4, NULL, '', NULL, 'sys:config:delete', 1, 4, '', NULL, '0,43,50', 1, 1, 0, NULL, '2026-09-18 03:07:22', '2026-09-18 03:07:22');
INSERT INTO `sys_menu` VALUES (86, 50, '下载', 4, NULL, '', NULL, 'sys:config:export', 1, 5, '', NULL, '0,43,50', 1, 1, 0, NULL, '2026-09-18 03:07:57', '2026-09-18 03:08:02');
INSERT INTO `sys_menu` VALUES (87, 51, '搜索 / 重置', 4, NULL, '', NULL, 'sys:notice:query', 1, 1, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:14:00', '2026-09-18 03:14:11');
INSERT INTO `sys_menu` VALUES (88, 51, '新建', 4, NULL, '', NULL, 'sys:notice:add', 1, 2, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:14:30', '2026-09-18 03:14:30');
INSERT INTO `sys_menu` VALUES (89, 51, '查看', 4, NULL, '', NULL, 'sys:notice:view', 1, 3, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:14:45', '2026-09-18 03:14:45');
INSERT INTO `sys_menu` VALUES (90, 51, '编辑', 4, NULL, '', NULL, 'sys:notice:edit', 1, 4, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:15:00', '2026-09-18 03:15:00');
INSERT INTO `sys_menu` VALUES (91, 51, '删除 / 批量删除', 4, NULL, '', NULL, 'sys:notice:delete', 1, 5, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:15:23', '2026-09-18 03:15:23');
INSERT INTO `sys_menu` VALUES (92, 51, '发布', 4, NULL, '', NULL, 'sys:notice:publish', 1, 6, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:15:41', '2026-09-18 03:15:41');
INSERT INTO `sys_menu` VALUES (93, 51, '撤回', 4, NULL, '', NULL, 'sys:notice:revoke', 1, 7, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:15:59', '2026-09-18 03:15:59');
INSERT INTO `sys_menu` VALUES (94, 51, '下载', 4, NULL, '', NULL, 'sys:notice:export', 1, 8, '', NULL, '0,43,51', 1, 1, 0, NULL, '2026-09-18 03:16:21', '2026-09-18 03:16:21');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '通知内容',
  `type` tinyint NULL DEFAULT 1 COMMENT '通知类型',
  `publisher_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '发布人',
  `level` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'M' COMMENT '通知等级(L:低, M:中, H:高)',
  `target_type` tinyint NULL DEFAULT 1 COMMENT '目标类型(1:全体, 2:指定)',
  `target_user_ids` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标用户ID集合(逗号分隔)',
  `publish_status` tinyint NULL DEFAULT 0 COMMENT '发布状态(0:未发布, 1:已发布, -1:已撤回)',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `revoke_time` datetime NULL DEFAULT NULL COMMENT '撤回时间',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_publish_status`(`publish_status` ASC) USING BTREE,
  INDEX `idx_publisher_id`(`publisher_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '新产品发布会邀请', '<p>🎊&nbsp;新产品发布会邀请</p><p>公司将于&nbsp;<strong>2025年1月15日下午14:00</strong>&nbsp;在总部会议室举办新产品发布会。</p><p>届时将展示最新研发的产品和技术成果，欢迎全体员工参加。</p><p>请各部门提前安排好工作，准时参加。</p>', 5, 1, 'M', 1, '', -1, '2026-09-18 02:24:06', '2026-09-18 02:24:13', 0, 1, 1, '2026-09-18 02:16:58', '2026-09-18 02:16:58');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NULL DEFAULT 1 COMMENT '角色状态(1-正常 0-停用)',
  `data_scope` tinyint NULL DEFAULT NULL COMMENT '数据权限(1-ALL 2-DEPT 3-DEPT_AND_CHILD 4-DEPT_AND_PARENT 5-SELF 6-CUSTOM)',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除(0-否 1-是)',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'ROOT', 1, 1, 1, 0, 1, 1, '2026-09-11 19:51:20', '2026-09-11 19:51:57');
INSERT INTO `sys_role` VALUES (2, '系统管理员', 'ADMIN', 2, 1, 1, 0, 1, 1, '2026-09-12 08:50:59', '2026-09-12 19:52:42');
INSERT INTO `sys_role` VALUES (3, '普通用户', 'USER', 3, 1, 4, 0, 1, 1, '2026-09-12 19:11:20', '2026-09-12 19:52:48');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` bigint UNSIGNED NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 169 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (134, 2, 1);
INSERT INTO `sys_role_menu` VALUES (135, 2, 2);
INSERT INTO `sys_role_menu` VALUES (136, 2, 3);
INSERT INTO `sys_role_menu` VALUES (137, 2, 33);
INSERT INTO `sys_role_menu` VALUES (138, 2, 34);
INSERT INTO `sys_role_menu` VALUES (139, 2, 35);
INSERT INTO `sys_role_menu` VALUES (140, 2, 36);
INSERT INTO `sys_role_menu` VALUES (141, 2, 41);
INSERT INTO `sys_role_menu` VALUES (142, 2, 42);
INSERT INTO `sys_role_menu` VALUES (143, 2, 43);
INSERT INTO `sys_role_menu` VALUES (144, 2, 44);
INSERT INTO `sys_role_menu` VALUES (145, 2, 45);
INSERT INTO `sys_role_menu` VALUES (146, 2, 46);
INSERT INTO `sys_role_menu` VALUES (147, 2, 47);
INSERT INTO `sys_role_menu` VALUES (148, 2, 48);
INSERT INTO `sys_role_menu` VALUES (149, 2, 49);
INSERT INTO `sys_role_menu` VALUES (150, 2, 50);
INSERT INTO `sys_role_menu` VALUES (151, 2, 51);
INSERT INTO `sys_role_menu` VALUES (161, 3, 1);
INSERT INTO `sys_role_menu` VALUES (162, 3, 2);
INSERT INTO `sys_role_menu` VALUES (168, 3, 3);
INSERT INTO `sys_role_menu` VALUES (163, 3, 37);
INSERT INTO `sys_role_menu` VALUES (164, 3, 38);
INSERT INTO `sys_role_menu` VALUES (165, 3, 39);
INSERT INTO `sys_role_menu` VALUES (166, 3, 40);
INSERT INTO `sys_role_menu` VALUES (167, 3, 41);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别(1-男 2-女 0-保密)',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(BCrypt)',
  `salt` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加密盐',
  `dept_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '部门ID',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系方式',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信 openid',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '修改人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_mobile`(`mobile` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'superadmin', '超级管理员', 1, '$2a$10$G3qDYSkxKP5SXHYs3VUz9e9QD4fg2JupoVBryfXzbWJPGxbXvWjQS', '$2a$10$G3qDYSkxKP5SXHYs3VUz9e', 1, NULL, '13232722998', 1, '13232722998@qq.com', NULL, 0, '初始化账号', NULL, 1, '2026-09-12 07:55:39', '2026-09-13 01:40:44');
INSERT INTO `sys_user` VALUES (2, 'admin', '管理员', 0, '$2a$10$KWiVw8Z7Zcifw0dNMsea/eX64gQHbJjyShrpfAdmmi3EOgKtO8Ivm', '$2a$10$KWiVw8Z7Zcifw0dNMsea/e', 1, NULL, NULL, 1, NULL, NULL, 0, '初始化账号', NULL, 1, '2026-09-12 07:55:39', '2026-09-13 01:40:49');
INSERT INTO `sys_user` VALUES (3, 'huangpw', '黄培文', 1, '$2a$10$DOlFAJL9hjcdRzcYQyw3NeoDoMiELlHVm3KIsSOP6ehtPFIEAgAIm', '$2a$10$DOlFAJL9hjcdRzcYQyw3Ne', 2, NULL, NULL, 1, NULL, NULL, 0, '初始化账号', NULL, 2, '2026-09-12 07:55:39', '2026-09-13 02:26:10');

-- ----------------------------
-- Table structure for sys_user_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_id` bigint UNSIGNED NOT NULL COMMENT '公共通知id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '读取状态(0-未读, 1-已读)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '用户阅读时间',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户-通知公告关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_notice
-- ----------------------------
INSERT INTO `sys_user_notice` VALUES (1, 1, 1, 0, NULL, 1, '2026-09-18 02:17:02', '2026-09-18 02:17:11');
INSERT INTO `sys_user_notice` VALUES (2, 1, 2, 0, NULL, 1, '2026-09-18 02:17:02', '2026-09-18 02:17:11');
INSERT INTO `sys_user_notice` VALUES (3, 1, 3, 0, NULL, 1, '2026-09-18 02:17:02', '2026-09-18 02:17:11');
INSERT INTO `sys_user_notice` VALUES (4, 1, 1, 0, NULL, 1, '2026-09-18 02:17:18', '2026-09-18 02:17:57');
INSERT INTO `sys_user_notice` VALUES (5, 1, 2, 0, NULL, 1, '2026-09-18 02:17:18', '2026-09-18 02:17:57');
INSERT INTO `sys_user_notice` VALUES (6, 1, 3, 0, NULL, 1, '2026-09-18 02:17:18', '2026-09-18 02:17:57');
INSERT INTO `sys_user_notice` VALUES (7, 1, 1, 0, NULL, 1, '2026-09-18 02:24:06', '2026-09-18 02:24:13');
INSERT INTO `sys_user_notice` VALUES (8, 1, 2, 0, NULL, 1, '2026-09-18 02:24:06', '2026-09-18 02:24:13');
INSERT INTO `sys_user_notice` VALUES (9, 1, 3, 0, NULL, 1, '2026-09-18 02:24:06', '2026-09-18 02:24:13');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2, 2);
INSERT INTO `sys_user_role` VALUES (3, 3, 3);

SET FOREIGN_KEY_CHECKS = 1;
