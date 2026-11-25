package com.hpwit.system.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项查询参数
 * @author AlbertHPW
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysDictItemQuery  {

    /** 关键字(字典项值/字典项名称) */
    private String keywordValue;

    /** 字典编码 */
    private String dictCode;

}
