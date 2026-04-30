package com.convertlab.convertlab_backend.service_email;

import java.time.Instant;
import java.util.UUID;

public record ContactInquiryEmail(
        UUID inquiryId,
        String fullName,
        String email,
        String phone,
        String inquiryType,
        String budgetRange,
        String message,
        Instant submittedAt
) {
}
