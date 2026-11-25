package com.hpwit.config;

import com.hpwit.platform.file.service.FileService;
import com.hpwit.platform.file.service.impl.TencentCosFileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "oss.type", havingValue = "tencent")
public class TencentCosConfig {

    @Bean(destroyMethod = "shutdown")
    public COSClient tencentCosClient(
            @Value("${oss.tencent.region}") String region,
            @Value("${oss.tencent.secret-id}") String secretId,
            @Value("${oss.tencent.secret-key}") String secretKey) {
        Assert.hasText(region, "使用腾讯云 COS 时需设置 oss.tencent.region");
        Assert.hasText(secretId, "使用腾讯云 COS 时需设置 oss.tencent.secret-id");
        Assert.hasText(secretKey, "使用腾讯云 COS 时需设置 oss.tencent.secret-key");

        ClientConfig config = new ClientConfig(new Region(region));
        config.setHttpProtocol(HttpProtocol.https);
        return new COSClient(new BasicCOSCredentials(secretId, secretKey), config);
    }

    @Bean
    public FileService tencentCosFileService(
            COSClient tencentCosClient,
            @Value("${oss.tencent.bucket-name}") String bucketName,
            @Value("${oss.tencent.custom-domain:}") String customDomain) {
        return new TencentCosFileService(tencentCosClient, bucketName, customDomain);
    }
}
