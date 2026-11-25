package com.hpwit.config;

import com.hpwit.auth.controller.AuthController;
import com.hpwit.platform.file.controller.FileController;
import com.hpwit.platform.mail.controller.MailController;
import com.hpwit.platform.qrcode.controller.QrCodeController;
import com.hpwit.platform.sms.controller.SmsController;
import com.hpwit.system.controller.SysRoleController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author huangpw
 * @Date 2026/1/6 14:33
 */
@Configuration
public class Knife4jConfig {

    private static final List<String> PLATFORM_CONTROLLER_PACKAGES = List.of(
            FileController.class.getPackageName(),
            MailController.class.getPackageName(),
            QrCodeController.class.getPackageName(),
            SmsController.class.getPackageName()
    );

    @Autowired
    private Environment environment;

    /**
     * 认证管理接口文档配置
     */
    @Bean
    public GroupedOpenApi authOpenApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/**")
                .packagesToScan(AuthController.class.getPackageName())
                .build();
    }

    /**
     * 系统管理接口文档配置
     */
    @Bean
    public GroupedOpenApi systemOpenApi() {
        List<String> packages = new ArrayList<>();
        packages.add(SysRoleController.class.getPackageName());
        packages.addAll(PLATFORM_CONTROLLER_PACKAGES);

        return GroupedOpenApi.builder()
                .group("system")
                .pathsToMatch("/**")
                .packagesToScan(packages.toArray(String[]::new))
                .build();
    }

    /**
     * 平台管理接口文档配置
     */
    @Bean
    public GroupedOpenApi platformOpenApi() {
        return GroupedOpenApi.builder()
                .group("platform")
                .pathsToMatch("/**")
                .packagesToScan(PLATFORM_CONTROLLER_PACKAGES.toArray(String[]::new))
                .build();
    }

    /**
     * 接口文档信息
     */
    @Bean
    public OpenAPI openApi() {

        String appVersion = environment.getProperty("project.version", "1.0.0");

        return new OpenAPI()
                .info(new Info()
                        .title("管理系统 API 文档")
                        .description("本文档涵盖管理系统的所有API接口，包括登录认证、用户管理、角色管理、部门管理等功能模块，提供详细的接口说明和使用指南。")
                        .version(appVersion)
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")
                        )
                        .contact(new Contact()
                                .name("Hpwit Admin")
                                .email("hpwit-admin@example.com")
                                .url("https://github.com/your-org/hpwit-admin")
                        )
                )
                // 配置全局鉴权参数-Authorize
                .components(new Components()
                        .addSecuritySchemes(HttpHeaders.AUTHORIZATION,
                                new SecurityScheme()
                                        .name(HttpHeaders.AUTHORIZATION)
                                        .type(SecurityScheme.Type.HTTP)
                                        .in(SecurityScheme.In.HEADER)
                                        .scheme("Bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

}
