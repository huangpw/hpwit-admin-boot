package com.hpwit.modules.flowable.config;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hpwit.flowable.callbacks")
public record ApprovalCallbackProperties(Map<String, Endpoint> endpoints) {
    public ApprovalCallbackProperties {
        endpoints = endpoints == null ? Map.of() : Map.copyOf(endpoints);
    }

    public enum Method { POST, PATCH }

    public record Endpoint(String label, String businessPage, URI url, Method method, String bearerToken) {
        public Endpoint {
            method = method == null ? Method.POST : method;
            if (businessPage == null || !businessPage.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw new IllegalArgumentException("回写接口须绑定有效的业务页面编码");
            }
            if (url == null || url.getHost() == null || url.getUserInfo() != null || url.getFragment() != null
                    || !("https".equalsIgnoreCase(url.getScheme())
                        || ("http".equalsIgnoreCase(url.getScheme()) && "127.0.0.1".equals(url.getHost())))) {
                throw new IllegalArgumentException("回写接口须使用 HTTPS，本机调试可使用 http://127.0.0.1");
            }
        }
    }
}
