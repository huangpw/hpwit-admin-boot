package com.nova.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nova.common.constant.SysConstant;
import com.nova.system.model.dto.CurrentUserDto;
import com.nova.system.model.entity.SysUser;
import com.nova.system.model.form.SysUserForm;
import com.nova.system.model.vo.SysUserExportVo;
import com.nova.system.model.vo.SysUserImportVo;
import com.nova.system.model.vo.SysUserPageVo;
import org.mapstruct.*;

/**
 * 用户对象转换器
 * @Author huangpw
 * @Date 2026/1/6 16:34
 */
@Mapper(componentModel = "spring")
public interface SysUserConverter {

    /** 实体(SysUser)转对象(SysUserPageVo) */
    SysUserPageVo toPageVo(SysUser user);

    /** 分页实体(SysUser)转分页对象(SysUserPageVo) */
    Page<SysUserPageVo> toPageVo(Page<SysUser> userPage);

    /** 对象(SysUserForm)转实体(SysUser) */
    @InheritInverseConfiguration(name = "toForm")
    @Mapping(target = "password", defaultValue = SysConstant.DEFAULT_PASSWORD) // 如果 source 为空，则使用默认值
    SysUser toEntity(SysUserForm entity);

    @Mappings({@Mapping(target = "userId", source = "id")})
    CurrentUserDto toCurrentUserDto(SysUser entity);

    /* ========= Export ========= */
    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderIntToStr")
    SysUserExportVo toExportVo(SysUser entity);


    /** 数字转中文 */
    @Named("genderIntToStr")
    default String genderIntToStr(Integer gender) {
        if (gender == null) {
            return "保密";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "保密";
        };
    }

    /* ========= Import ========= */
    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderStrToInt")
    @Mapping(target = "status", source = "status", qualifiedByName = "genderStrToInt")
    SysUser toEntity(SysUserImportVo vo);

    /** 中文转数字 */
    @Named("genderStrToInt")
    default Integer genderStrToInt(String gender) {
        if (gender == null) return 0;
        return switch (gender) {
            case "男" -> 1;
            case "女" -> 2;
            default -> 0;
        };
    }

    @Named("statusStrToInt")
    default Integer statusStrToInt(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "启用" -> 1;
            case "禁用" -> 2;
            default -> 0;
        };
    }


}
