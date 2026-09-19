package com.nova.platform.mail.model.dto;

import lombok.Data;

/**
 * @Author huangpw
 * @Date 2026/2/26 18:23
 */
@Data
public class MailDto {
    private String to;
    private String subject;
    private String content;
    private String filePath;
}
