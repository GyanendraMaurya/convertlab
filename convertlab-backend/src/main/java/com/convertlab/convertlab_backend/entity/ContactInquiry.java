package com.convertlab.convertlab_backend.entity;

import com.convertlab.convertlab_backend.api.enums.ContactInquiryStatus;
import com.convertlab.convertlab_backend.api.enums.EmailNotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_inquiry")
@AllArgsConstructor
@Getter
@Setter
public class ContactInquiry {

    @Id
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    private String phone;

    private String message;

    @Column(name = "inquiry_type")
    private String inquiryType;

    @Column(name = "budget_range")
    private String budgetRange;

    @Enumerated(EnumType.STRING)
    private ContactInquiryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_notification_status")
    private EmailNotificationStatus emailNotificationStatus;

    @Column(name = "email_notification_error")
    private String emailNotificationError;

    @Column(name = "ip_hash")
    private String ipHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ContactInquiry() {
        // JPA only
    }
}
