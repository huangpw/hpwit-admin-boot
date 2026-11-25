package com.hpwit.system.model.query;

import com.hpwit.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置查询对象
 *
 * @author huangpw
 */
@Getter
@Setter
@Schema(description = "系统配置分页查询")
public class SysConfigPageQuery extends BasePageQuery {

    @Schema(description="关键字(配置项名称/配置项值)")
    private String keywords;
}
