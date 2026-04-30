package com.convertlab.convertlab_backend.service_util;

import com.convertlab.convertlab_backend.config.ProxyConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final ProxyConfig proxyConfig;

    public String extractClientIp(HttpServletRequest request) {
        return IpUtil.extractClientIp(request, proxyConfig.getTrustedCidrs());
    }
}
