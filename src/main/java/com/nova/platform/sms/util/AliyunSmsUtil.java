package com.nova.platform.sms.util;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.nova.common.enums.SmsTypeEnum;
import com.nova.common.property.AliyunSmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author huangpw
 * @Date 2026/4/1 14:28
 */
@Component
@RequiredArgsConstructor
public class AliyunSmsUtil {

    private final Client client;
    private final AliyunSmsProperties prop;
    private final AliyunSmsProperties aliyunSmsProperties;

    /**
     * 发送登录验证码
     * @param phone 手机号
     * @param code 验证码
     * @param smsType 短信模板
     * @return 布尔结果
     */
    public boolean sendLoginCode(String phone, String code, SmsTypeEnum smsType) {
        try {

            String templateCode = aliyunSmsProperties.getTemplates().get(smsType.getValue());

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(prop.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.sendSms(request);

            return "OK".equals(response.getBody().getCode());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *  发送通知短信
     * @param phone 手机号
     * @param smsType 短信模板
     * @return 布尔结果
     */
    public boolean sendNotice(String phone, SmsTypeEnum smsType) {

        try {
            String templateCode = aliyunSmsProperties.getTemplates().get(smsType.getValue());
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(prop.getSignName())
                    .setTemplateCode(templateCode);
            // ❗不需要 TemplateParam

            SendSmsResponse response = client.sendSms(request);

            return "OK".equals(response.getBody().getCode());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
