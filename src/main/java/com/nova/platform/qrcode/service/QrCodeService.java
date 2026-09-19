package com.nova.platform.qrcode.service;

import com.nova.common.result.Result;
import com.nova.system.model.entity.SysFile;

/**
 * @Author huangpw
 * @Date 2026/2/25 18:42
 */
public interface QrCodeService {

    Result<?> generateQrCode(String bizId, String code);

    SysFile generateQrCode(String code, int width, int height);
}
