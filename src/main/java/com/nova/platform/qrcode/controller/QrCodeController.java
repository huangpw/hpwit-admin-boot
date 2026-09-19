package com.nova.platform.qrcode.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nova.common.result.Result;
import com.nova.platform.qrcode.service.QrCodeService;
import com.nova.system.model.entity.SysFile;
import com.nova.system.service.SysFileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author huangpw
 * @Date 2026/2/25 18:42
 */
@Tag(name = "10.二维码接口")
@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final SysFileService sysFileService;

    /** 生成二维码 */
    @GetMapping("/generate")
    public Result<?> generate(@RequestParam String bizId, @RequestParam String code) {

        LambdaQueryWrapper<SysFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysFile::getBizId, bizId)
                        .last("limit 1");
        SysFile file = sysFileService.getOne(queryWrapper);

        if(file != null) {
            return Result.success(file);
        }

        return qrCodeService.generateQrCode(bizId, code);
    }
}
