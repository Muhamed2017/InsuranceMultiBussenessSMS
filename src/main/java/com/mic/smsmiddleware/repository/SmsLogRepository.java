package com.mic.smsmiddleware.repository;

import com.mic.smsmiddleware.domain.entity.SmsLog;
import com.mic.smsmiddleware.domain.enums.SmsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    boolean existsByBusinessTypeAndReferenceKeyAndNormalizedPhoneAndStatus(
            String businessType,
            String referenceKey,
            String normalizedPhone,
            SmsStatus status
    );

    @Query("""
            SELECT l FROM SmsLog l
            WHERE l.status = 'FAILED'
              AND l.retryCount < :maxRetryCount
            ORDER BY l.createdAt ASC
            """)
    List<SmsLog> findRetryable(@Param("maxRetryCount") int maxRetryCount);
}
