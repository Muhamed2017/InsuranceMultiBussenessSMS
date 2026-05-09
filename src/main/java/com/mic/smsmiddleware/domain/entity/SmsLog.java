package com.mic.smsmiddleware.domain.entity;

import com.mic.smsmiddleware.domain.enums.SmsStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "SMS_LOG",
    indexes = {
        @Index(name = "IDX_SMS_LOG_DEDUP",   columnList = "business_type, reference_key, normalized_phone, status"),
        @Index(name = "IDX_SMS_LOG_RETRY",   columnList = "status, retry_count"),
        @Index(name = "IDX_SMS_LOG_CREATED", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_type", nullable = false, length = 100)
    private String businessType;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "normalized_phone", nullable = false, length = 15)
    private String normalizedPhone;

    @Column(name = "contact_name", length = 300)
    private String contactName;

    @Column(name = "reference_key", nullable = false, length = 300)
    private String referenceKey;

    @Column(name = "message_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SmsStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
