package com.hpwit.platform.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.hpwit.core.exception.BusinessException;
import com.hpwit.platform.file.model.FileInfo;
import com.hpwit.platform.file.service.FileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TencentCosFileService implements FileService {

    private static final Pattern BUCKET_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*-[0-9]+");
    private static final Pattern FILE_EXTENSION = Pattern.compile("[a-zA-Z0-9]{1,16}");
    private static final Pattern OBJECT_KEY =
            Pattern.compile("uploads/[0-9]{8}/[a-f0-9]{32}(?:\\.[a-zA-Z0-9]{1,16})?");

    private final COSClient cosClient;
    private final String bucketName;
    private final URI bucketUrl;
    private final URI publicUrl;

    public TencentCosFileService(COSClient cosClient, String bucketName, String customDomain) {
        Assert.isTrue(BUCKET_NAME.matcher(bucketName).matches(),
                "oss.tencent.bucket-name 必须为包含 APPID 的完整存储桶名称，例如 hpwit-admin-1250000000");
        this.cosClient = cosClient;
        this.bucketName = bucketName;
        this.bucketUrl = URI.create(cosClient.getObjectUrl(bucketName, "/").toExternalForm());
        this.publicUrl = StringUtils.hasText(customDomain) ? parseCustomDomain(customDomain) : bucketUrl;
    }

    @Override
    public FileInfo uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.getSuffix(originalFilename);
        String key = "uploads/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/" + IdUtil.simpleUUID();
        if (extension != null && FILE_EXTENSION.matcher(extension).matches()) {
            key += "." + extension;
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        if (StringUtils.hasText(file.getContentType())) {
            metadata.setContentType(file.getContentType());
        }
        try (InputStream input = file.getInputStream()) {
            cosClient.putObject(bucketName, key, input, metadata);
        } catch (IOException | CosClientException e) {
            throw new BusinessException("文件上传失败", e);
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(publicUrl.resolve(key).toASCIIString());
        return fileInfo;
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return false;
        }

        URI url;
        try {
            url = URI.create(filePath);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String path = url.getRawPath();
        if (path == null || !path.startsWith("/")) {
            return false;
        }
        String key = path.substring(1);
        if (!OBJECT_KEY.matcher(key).matches()
                || !(url.equals(publicUrl.resolve(key)) || url.equals(bucketUrl.resolve(key)))) {
            return false;
        }

        try {
            cosClient.deleteObject(bucketName, key);
            return true;
        } catch (CosClientException e) {
            throw new BusinessException("文件删除失败", e);
        }
    }

    @Override
    public List<FileInfo> uploadFiles(MultipartFile[] files) {
        List<FileInfo> fileInfos = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            fileInfos.add(uploadFile(file));
        }
        return fileInfos;
    }

    private static URI parseCustomDomain(String customDomain) {
        String domain = customDomain.trim();
        URI uri = URI.create(domain.endsWith("/") ? domain : domain + "/");
        Assert.isTrue("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                        && uri.getRawUserInfo() == null && "/".equals(uri.getRawPath())
                        && uri.getRawQuery() == null && uri.getRawFragment() == null,
                "oss.tencent.custom-domain 必须为 HTTPS 域名，例如 https://files.example.com");
        return uri;
    }
}
