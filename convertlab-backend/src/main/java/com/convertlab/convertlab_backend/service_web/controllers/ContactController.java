package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.ContactInquiryService;
import com.convertlab.convertlab_backend.service_util.ClientIpResolver;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ContactInquiryRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ContactInquiryResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactInquiryService contactInquiryService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/inquiries")
    public ResponseEntity<ApiResponse<ContactInquiryResponse>> createInquiry(
            @RequestBody ContactInquiryRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpRequest
    ) {
        log.info("Recording contact inquiry for: {}", request == null ? "unknown" : request.fullName());
        String clientIp = clientIpResolver.extractClientIp(httpRequest);
        ContactInquiryResponse response = contactInquiryService.createInquiry(request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
