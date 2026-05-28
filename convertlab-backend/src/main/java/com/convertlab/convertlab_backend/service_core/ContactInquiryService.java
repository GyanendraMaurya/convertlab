package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.api.enums.ContactInquiryStatus;
import com.convertlab.convertlab_backend.api.enums.EmailNotificationStatus;
import com.convertlab.convertlab_backend.entity.ContactInquiry;
import com.convertlab.convertlab_backend.exception.ContactInquiryValidationException;
import com.convertlab.convertlab_backend.repository.ContactInquiryRepository;
import com.convertlab.convertlab_backend.service_email.ContactInquiryEmail;
import com.convertlab.convertlab_backend.service_email.EmailSender;
import com.convertlab.convertlab_backend.service_util.EmailUtil;
import com.convertlab.convertlab_backend.service_util.IpUtil;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ContactInquiryRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ContactInquiryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Log4j2
@Service
@RequiredArgsConstructor
public class ContactInquiryService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s().-]+$");

    private final ContactInquiryRepository contactInquiryRepository;
    private final EmailSender emailSender;
    private final FeatureFlagService featureFlagService;

    @Value("${app.contact.to-email:gmaurya973@gmail.com}")
    private String contactToEmail;

    @Transactional
    public ContactInquiryResponse createInquiry(
            ContactInquiryRequest request,
            String ipAddress,
            String userAgent
    ) {
        featureFlagService.requireEnabled(
                FeatureFlagService.SHOW_CONTACT_PAGE,
                "Contact page is currently unavailable"
        );

        ContactInquiryRequest sanitized = sanitizeAndValidate(request);
        Instant now = Instant.now();

        ContactInquiry inquiry = new ContactInquiry(
                UUID.randomUUID(),
                sanitized.fullName(),
                sanitized.email(),
                sanitized.phone(),
                sanitized.message(),
                sanitized.inquiryType(),
                sanitized.budgetRange(),
                ContactInquiryStatus.NEW,
                EmailNotificationStatus.PENDING,
                null,
                IpUtil.hashIp(ipAddress),
                trimToMax(userAgent, 512),
                now,
                now
        );

        ContactInquiry savedInquiry = contactInquiryRepository.save(inquiry);

        try {
            emailSender.sendContactInquiry(
                    contactToEmail,
                    new ContactInquiryEmail(
                            savedInquiry.getId(),
                            savedInquiry.getFullName(),
                            savedInquiry.getEmail(),
                            savedInquiry.getPhone(),
                            savedInquiry.getInquiryType(),
                            savedInquiry.getBudgetRange(),
                            savedInquiry.getMessage(),
                            savedInquiry.getCreatedAt()
                    )
            );
            savedInquiry.setEmailNotificationStatus(EmailNotificationStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send contact inquiry notification for id: {}", savedInquiry.getId(), e);
            savedInquiry.setEmailNotificationStatus(EmailNotificationStatus.FAILED);
            savedInquiry.setEmailNotificationError(trimToMax(e.getMessage(), 1000));
        }

        savedInquiry.setUpdatedAt(Instant.now());
        ContactInquiry updatedInquiry = contactInquiryRepository.save(savedInquiry);

        return new ContactInquiryResponse(
                updatedInquiry.getId(),
                updatedInquiry.getCreatedAt(),
                updatedInquiry.getEmailNotificationStatus()
        );
    }

    private ContactInquiryRequest sanitizeAndValidate(ContactInquiryRequest request) {
        if (request == null) {
            throw new ContactInquiryValidationException("Contact inquiry payload is required", "CONTACT_REQUEST_REQUIRED");
        }

        String fullName = trimToNull(request.fullName());
        String email = trimToNull(request.email());
        String phone = trimToNull(request.phone());
        String message = trimToNull(request.message());
        String inquiryType = trimToNull(request.inquiryType());
        String budgetRange = trimToNull(request.budgetRange());

        if (fullName == null) {
            throw new ContactInquiryValidationException("Full name is required", "FULL_NAME_REQUIRED");
        }
        if (fullName.length() < 2 || fullName.length() > 120) {
            throw new ContactInquiryValidationException("Full name must be between 2 and 120 characters", "INVALID_FULL_NAME");
        }
        if (message == null) {
            throw new ContactInquiryValidationException("Message is required", "MESSAGE_REQUIRED");
        }
        if (message.length() < 20 || message.length() > 1500) {
            throw new ContactInquiryValidationException("Message must be between 20 and 1500 characters", "INVALID_MESSAGE");
        }
        if (email == null && phone == null) {
            throw new ContactInquiryValidationException("Please provide either an email address or contact number", "CONTACT_METHOD_REQUIRED");
        }
        if (email != null && (!EmailUtil.isValid(email) || email.length() > 255)) {
            throw new ContactInquiryValidationException("Please enter a valid email address", "INVALID_EMAIL");
        }
        if (phone != null && !isValidPhone(phone)) {
            throw new ContactInquiryValidationException("Please enter a valid contact number", "INVALID_PHONE");
        }

        return new ContactInquiryRequest(
                fullName,
                email,
                phone,
                message,
                trimToMax(inquiryType, 60),
                trimToMax(budgetRange, 60)
        );
    }

    private static boolean isValidPhone(String phone) {
        int digitCount = phone.replaceAll("\\D", "").length();
        return digitCount >= 7 && digitCount <= 15 && PHONE_PATTERN.matcher(phone).matches() && phone.length() <= 40;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String trimToMax(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
