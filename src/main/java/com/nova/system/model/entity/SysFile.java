package com.nova.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件管理类
 * @Author huangpw
 * @Date 2026/2/27 17:31
 */
@Data
@TableName("sys_file")
public class SysFile {

    /** 主键Id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务Id */
    private String bizId;

    /** 文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型/后缀名 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 显示文件大小（如 130KB） */
    private String sizeDisplay;

    /** 服务器存储路径 */
    private String storagePath;

    /** 文件访问URL */
    private String fileUrl;

    /** 文件来源：二维码生成、用户上传等 */
    private String sourceType;

    /** 文件状态，1=有效，0=删除 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
