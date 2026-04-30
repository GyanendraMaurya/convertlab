package com.convertlab.convertlab_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.proxy")
public class ProxyConfig {
    private List<String> trustedCidrs = List.of("127.0.0.0/8", "::1/128");
}
