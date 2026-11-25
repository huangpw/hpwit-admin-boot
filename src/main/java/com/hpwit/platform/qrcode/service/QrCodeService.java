package com.hpwit.platform.qrcode.service;

import com.hpwit.common.result.Result;
import com.hpwit.system.model.entity.SysFile;

/**
 * @Author huangpw
 * @Date 2026/2/25 18:42
 */
public interface QrCodeService {

    Result<?> generateQrCode(String bizId, String code);

    SysFile generateQrCode(String code, int width, int height);
}
