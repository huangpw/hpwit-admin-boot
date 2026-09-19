package com.nova.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * @Author: AlbertHPW
 * @Date: 2026/1/10 0:34
 */
@Schema(description ="当前登录用户关联菜单对象")
@Data
public class CurrentMenuDto {

    /** 唯一ID */
    private Long id;

    /** 父级ID，根节点为null */
    private Long pid;

    /** 路由名称（唯一） */
    private String name;

    /** 路径 */
    private String path;

    /** 标题（用于菜单显示或页面标题） */
    private String title;

    /** 是否需要登录访问 */
    private Boolean requiresAuth;

    /** 图标（可选） */
    private String icon;

    private Integer type;
    /** 菜单类型：dir=目录, menu=菜单, btn=按钮, link=外链 */
    private String menuType;

    /** 对应前端组件路径（可为空） */
    private String componentPath;

    /** 是否隐藏 */
    private Boolean hide;

    /** 排序 */
    private Integer order;

    /** 排序 */
    private Integer sort;

    /** 嵌套外链 */
    private String href;

    /** 是否开启页面缓存 */
    private Boolean keepAlive;

    /** 重定向地址 */
    private String redirect;

    private Map<String,String> params;
}
