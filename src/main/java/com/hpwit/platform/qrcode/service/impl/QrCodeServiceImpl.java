package com.hpwit.platform.qrcode.service.impl;

import cn.hutool.core.util.IdUtil;
import com.hpwit.common.result.Result;
import com.hpwit.common.utils.FileUtil;
import com.hpwit.common.utils.QrCodeUtil;
import com.hpwit.platform.qrcode.service.QrCodeService;
import com.hpwit.system.mapper.SysFileMapper;
import com.hpwit.system.model.entity.SysFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @Author huangpw
 * @Date 2026/2/25 18:43
 */
@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${oss.local.storage-path}")
    private String storagePath;

    @Autowired
    private SysFileMapper sysFileMapper;

    @Override
    public Result<?> generateQrCode(String bizId, String code) {

        String fileName =  code + "_" + IdUtil.simpleUUID() + ".png";

        String savePath = storagePath + fileName;

        QrCodeUtil.generateQrCode(code,
                300,
                300,
                savePath);

        SysFile file = new SysFile();
        file.setBizId(bizId);
        file.setFileName(fileName);
        file.setOriginalName(fileName);
        file.setFileType("png");
        file.setFileSize(new File(savePath).length());
        file.setSizeDisplay(FileUtil.formatSize(new File(savePath).length()));
        file.setStoragePath(storagePath);
        file.setFileUrl(fileName);
        file.setSourceType("二维码生成");
        file.setStatus(1);

        sysFileMapper.insert(file);

        return Result.success(file);
    }

    @Override
    public SysFile generateQrCode(String code, int width, int height) {
        String fileName =  code + "_" + IdUtil.simpleUUID() + ".png";

        String savePath = storagePath + fileName;

        QrCodeUtil.generateQrCode(code,
                width,
                height,
                savePath
        );

        SysFile sysFile = new SysFile();

        File file = new File(savePath);
        sysFile.setFileName(fileName);
        sysFile.setOriginalName(fileName);
        sysFile.setFileType("png");
        sysFile.setFileSize(file.length());
        sysFile.setSizeDisplay(FileUtil.formatSize(file.length()));
        sysFile.setStoragePath(storagePath);
        sysFile.setFileUrl(fileName);
        sysFile.setSourceType("二维码生成");
        sysFile.setStatus(1);

        return sysFile;
    }
}
