package com.hpwit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

/**
 * @Author huangpw
 * @Date 2026/1/6 14:33
 */
@Configuration
public class Knife4jConfig {

    @Autowired
    private Environment environment;

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
                                .name("Zyxhit Admin")
                                .email("zyxhit-admin@example.com")
                                .url("https://github.com/your-org/zyxhit-admin")
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
