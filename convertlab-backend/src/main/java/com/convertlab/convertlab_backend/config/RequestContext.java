package com.convertlab.convertlab_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Setter
@Getter
public class RequestContext {
    private String sessionId;
    private String userId;

}
