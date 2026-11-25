package com.hpwit.system.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: AlbertHPW
 * @Date: 2025/11/22 20:30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysDictQuery {

    /** 关键字(字典名称或字典编码) */
    private String keywordValue;
}
