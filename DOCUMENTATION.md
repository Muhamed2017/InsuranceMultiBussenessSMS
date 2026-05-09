# MIC Multi-Business SMS Middleware — Full Project Documentation

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Project Structure](#3-project-structure)
4. [Core Concepts](#4-core-concepts)
5. [Configuration Reference](#5-configuration-reference)
6. [Database Schema](#6-database-schema)
7. [Stored Procedure Contract](#7-stored-procedure-contract)
8. [Business Types](#8-business-types)
9. [SMS Provider Integration](#9-sms-provider-integration)
10. [Scheduling](#10-scheduling)
11. [Deduplication](#11-deduplication)
12. [Retry Mechanism](#12-retry-mechanism)
13. [Egyptian Mobile Validation](#13-egyptian-mobile-validation)
14. [Adding a New Business Type](#14-adding-a-new-business-type)
15. [Component Reference](#15-component-reference)
16. [Setup & Deployment](#16-setup--deployment)
17. [Logging](#17-logging)

---

## 1. Project Overview

**MIC Multi-Business SMS Middleware** is a Spring Boot application that proactively sends SMS notifications to insurance customers at key moments in their policy lifecycle. The application is driven entirely by configuration — new notification types (business types) are added through YAML without changing any code.

**Key capabilities:**
- Executes configurable Microsoft SQL Server stored procedures on a schedule
- Validates and normalizes Egyptian mobile numbers
- Composes dynamic SMS messages from YAML-defined templates
- Prevents duplicate SMS delivery via database-backed deduplication
- Automatically retries failed SMS deliveries every 5 minutes
- Logs every send attempt (successful or failed) to the database

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    BusinessProcessingScheduler                   │
│          Runs every hour between 10:00 AM and 11:00 PM          │
└────────────────────────────┬────────────────────────────────────┘
                             │ triggers each active business type
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BusinessProcessorService                      │
│  1. Calls StoredProcedureService to execute the configured SP   │
│  2. Applies field mappings (info1→InsuredName, etc.)            │
│  3. Validates phone number via MobileValidationService          │
│  4. Builds ContactRecord, delegates to SmsDispatchService       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SmsDispatchService                          │
│  1. Checks DeduplicationService (already sent? → skip)          │
│  2. Composes message via MessageTemplateService                  │
│  3. Calls SmsProvider.send()                                     │
│  4. Records result via SmsLogService → SMS_LOG table            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        RetryScheduler                            │
│          Runs every 5 minutes                                    │
│  Queries FAILED logs where retry_count < max_retry_count        │
│  Re-sends each via SmsProvider; updates status on success       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Project Structure

```
sms-middleware/
├── pom.xml
├── DOCUMENTATION.md
└── src/
    ├── main/
    │   ├── java/com/mic/smsmiddleware/
    │   │   ├── MicSmsMiddlewareApplication.java
    │   │   ├── config/
    │   │   │   ├── RestTemplateConfig.java
    │   │   │   └── SchedulingConfig.java
    │   │   ├── domain/
    │   │   │   ├── entity/
    │   │   │   │   └── SmsLog.java
    │   │   │   ├── enums/
    │   │   │   │   └── SmsStatus.java
    │   │   │   └── model/
    │   │   │       ├── ContactRecord.java
    │   │   │       └── SmsDeliveryResult.java
    │   │   ├── exception/
    │   │   │   ├── InvalidMobileNumberException.java
    │   │   │   ├── SmsProviderException.java
    │   │   │   └── StoredProcedureException.java
    │   │   ├── properties/
    │   │   │   ├── AppProperties.java
    │   │   │   ├── BusinessTypeConfig.java
    │   │   │   ├── SchedulingProperties.java
    │   │   │   ├── SmsProviderProperties.java
    │   │   │   └── SpParameterConfig.java
    │   │   ├── provider/
    │   │   │   ├── SmsProvider.java
    │   │   │   └── HttpSmsProvider.java
    │   │   ├── repository/
    │   │   │   └── SmsLogRepository.java
    │   │   ├── scheduler/
    │   │   │   ├── BusinessProcessingScheduler.java
    │   │   │   └── RetryScheduler.java
    │   │   └── service/
    │   │       ├── BusinessProcessorService.java
    │   │       ├── DeduplicationService.java
    │   │       ├── MessageTemplateService.java
    │   │       ├── MobileValidationService.java
    │   │       ├── SmsDispatchService.java
    │   │       ├── SmsLogService.java
    │   │       └── StoredProcedureService.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │           └── schema.sql
    └── test/
        └── java/com/mic/smsmiddleware/
            └── service/
                ├── MobileValidationServiceTest.java
                └── MessageTemplateServiceTest.java
```

---

## 4. Core Concepts

### Business Type
A named notification scenario (e.g., `issuance`, `due-installment`, `customer-birthday`). Each business type has its own stored procedure, field mappings, and SMS template, all defined in `application.yml`.

### Stored Procedure Output Contract
Every stored procedure — regardless of business type — **must** return a result set with generic column names `info1`, `info2`, `info3`, ..., `infoN`. The semantic meaning of each column is defined per business type in the YAML `field-mappings` section.

### Field Mappings
The bridge between the generic SP output (`info1`, `info2`, ...) and meaningful names used in the SMS template (`InsuredName`, `PolicyNumber`, etc.).

Example mapping in YAML:
```yaml
field-mappings:
  info1: InsuredName
  info2: InsuredPhone
  info3: PolicyNumber
  info4: IssuanceDate
  info5: ProductName
```

### Reference Key
A unique identifier for the business event, used to prevent duplicate SMS delivery. For issuance, it is the policy number. For due installments, it is a composite of policy number and due date. The reference key is assembled from the fields listed in `reference-fields`.

### SMS Template
A plain-text string with `{PlaceholderName}` tokens that correspond to semantic field names from `field-mappings`.

---

## 5. Configuration Reference

All application configuration lives in `src/main/resources/application.yml` under the `mic` prefix.

### Provider Configuration (`mic.provider`)

| Property | Description | Default |
|---|---|---|
| `base-url` | SMS gateway HTTP endpoint | *(required)* |
| `username` | Gateway authentication username | *(required)* |
| `password` | Gateway authentication password | *(required)* |
| `sender-id` | Sender name shown to recipient | `MIC` |
| `language` | Message language code (1=Arabic, 2=English) | `2` |
| `environment` | Gateway environment flag | `1` |
| `success-response-code` | String in response body indicating success | `0000` |
| `connect-timeout` | HTTP connection timeout in milliseconds | `5000` |
| `read-timeout` | HTTP read timeout in milliseconds | `10000` |

### Scheduling Configuration (`mic.scheduling`)

| Property | Description | Default |
|---|---|---|
| `processing-cron` | Cron expression for the hourly processing job | `0 0 10-23 * * *` |
| `retry-cron` | Cron expression for the retry job | `0 */5 * * * *` |
| `max-retry-count` | Maximum retry attempts before giving up | `3` |
| `active-business-types` | List of business type keys to process on schedule | *(required)* |

### Business Type Configuration (`mic.business-types.<key>`)

| Property | Description |
|---|---|
| `stored-procedure` | Name of the MSSQL stored procedure to call |
| `parameters` | List of SP input parameters (see below) |
| `phone-field` | SP output column key (e.g., `info2`) that holds the phone number |
| `name-field` | SP output column key that holds the contact name (used in logging) |
| `reference-fields` | List of SP output column keys whose values form the deduplication reference key |
| `template` | SMS message template with `{PlaceholderName}` tokens |
| `field-mappings` | Map of `infoN` → semantic name used in the template |

### SP Parameter Configuration (`mic.business-types.<key>.parameters[]`)

| Property | Description | Allowed Values |
|---|---|---|
| `name` | Parameter name as expected by the stored procedure | any string |
| `value` | Parameter value (always defined as a string in YAML) | any string |
| `type` | Data type for JDBC binding | `STRING`, `INTEGER`, `DECIMAL`, `DATE` |

---

## 6. Database Schema

The application uses a single table: `SMS_LOG`.

```sql
CREATE TABLE SMS_LOG (
    id               BIGINT       IDENTITY(1,1) NOT NULL,
    business_type    VARCHAR(100)               NOT NULL,
    contact_phone    VARCHAR(30)                NOT NULL,
    normalized_phone VARCHAR(15)                NOT NULL,
    contact_name     NVARCHAR(300)              NULL,
    reference_key    VARCHAR(300)               NOT NULL,
    message_content  NVARCHAR(MAX)              NOT NULL,
    status           VARCHAR(10)                NOT NULL,  -- 'SENT' or 'FAILED'
    failure_reason   NVARCHAR(500)              NULL,
    retry_count      INT          DEFAULT 0     NOT NULL,
    sent_at          DATETIME                   NULL,
    created_at       DATETIME     DEFAULT GETDATE() NOT NULL,

    CONSTRAINT PK_SMS_LOG PRIMARY KEY (id),
    CONSTRAINT CHK_SMS_LOG_STATUS CHECK (status IN ('SENT', 'FAILED'))
);
```

**Indexes:**

| Index Name | Columns | Purpose |
|---|---|---|
| `IDX_SMS_LOG_DEDUP` | `business_type, reference_key, normalized_phone, status` | Fast deduplication lookups |
| `IDX_SMS_LOG_RETRY` | `status, retry_count` | Fast retry queue queries |
| `IDX_SMS_LOG_CREATED` | `created_at` | Time-range reporting queries |

**Column descriptions:**

- `contact_phone`: Raw phone number exactly as returned by the stored procedure.
- `normalized_phone`: Egyptian mobile number after validation and normalization (always 11 digits, e.g., `01012345678`).
- `reference_key`: Composite key assembled from the configured `reference-fields`. Used for deduplication.
- `retry_count`: Incremented on each failed retry. When it reaches `max-retry-count`, the record is excluded from future retry cycles.

---

## 7. Stored Procedure Contract

### Output Shape

Every stored procedure used by this application **must** return a result set using a consistent generic column naming convention:

```sql
SELECT
    col_a AS info1,
    col_b AS info2,
    col_c AS info3,
    ...
FROM your_table
WHERE your_conditions;
```

The column aliases must be lowercase `info1`, `info2`, ..., `infoN`. The YAML `field-mappings` for that business type defines the semantic meaning of each column.

### Example: Issuance SP

```sql
CREATE PROCEDURE SP_ISSUANCE_SMS
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        p.InsuredName       AS info1,
        p.InsuredPhone      AS info2,
        p.PolicyNumber      AS info3,
        CONVERT(VARCHAR, p.IssuanceDate, 103) AS info4,
        pr.ProductName      AS info5
    FROM Policies p
    INNER JOIN Products pr ON pr.ProductId = p.ProductId
    WHERE CAST(p.IssuanceDate AS DATE) = CAST(GETDATE() AS DATE);
END
```

### Example: Due Installment SP

```sql
CREATE PROCEDURE SP_DUE_INSTALLMENT_SMS
    @DaysAhead INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        c.FullName                          AS info1,
        c.MobilePhone                       AS info2,
        p.PolicyNumber                      AS info3,
        CAST(i.InstallmentAmount AS VARCHAR) AS info4,
        CONVERT(VARCHAR, i.DueDate, 103)    AS info5
    FROM Installments i
    INNER JOIN Policies p ON p.PolicyId = i.PolicyId
    INNER JOIN Customers c ON c.CustomerId = p.CustomerId
    WHERE CAST(i.DueDate AS DATE) = CAST(DATEADD(DAY, @DaysAhead, GETDATE()) AS DATE)
      AND i.Status = 'PENDING';
END
```

### Reference Key for Due Installments

The `reference-fields` for `due-installment` are configured as `[info3, info5]` (PolicyNumber + DueDate). The application joins these values with `_` to produce a composite reference key such as `POL-2024-001_15/03/2024`. This prevents re-sending a reminder for the same installment if the scheduler runs again before the payment is processed, while still allowing reminders for different installments of the same policy.

---

## 8. Business Types

### Currently Active

| Key | Stored Procedure | Schedule | Description |
|---|---|---|---|
| `issuance` | `SP_ISSUANCE_SMS` | Hourly, 10am–11pm | Notifies customers whose policies were issued today |
| `due-installment` | `SP_DUE_INSTALLMENT_SMS` | Hourly, 10am–11pm | Reminds customers with installments due in 10 days |

### SMS Templates

**Issuance:**
```
Dear {InsuredName}, your policy No. {PolicyNumber} has been successfully
issued on {IssuanceDate}. Product: {ProductName}.
Welcome to MIC Insurance Family.
```

**Due Installment:**
```
Dear {InsuredName}, this is a reminder that your installment for
policy No. {PolicyNumber} amounting to {InstallmentAmount} EGP is
due on {DueDate}. Please pay on time to keep your coverage active.
```

---

## 9. SMS Provider Integration

The application uses a single `SmsProvider` interface:

```java
public interface SmsProvider {
    SmsDeliveryResult send(String normalizedPhone, String message);
}
```

The production implementation is `HttpSmsProvider`, which sends an HTTP POST request with form-encoded parameters to the configured `mic.provider.base-url`. The gateway response body is checked for the configured `success-response-code`. If the code is found, the delivery is considered successful.

**To switch SMS providers:** implement the `SmsProvider` interface and annotate the new class with `@Primary` (or remove the `@Component` from `HttpSmsProvider`). No other code changes are required.

**Provider credentials** in `application.yml`:
```yaml
mic:
  provider:
    base-url: https://sms-gateway.placeholder.com/api/send
    username: PLACEHOLDER_USERNAME
    password: PLACEHOLDER_PASSWORD
    sender-id: MIC
```
Replace all three placeholder values with your actual credentials before deploying.

---

## 10. Scheduling

### Business Processing Job

- **Cron:** `0 0 10-23 * * *` (configurable via `mic.scheduling.processing-cron`)
- **Fires at:** 10:00, 11:00, 12:00, ..., 23:00 every day
- **Behavior:** Iterates `mic.scheduling.active-business-types` in order. For each type, the full SP-execute → validate → deduplicate → send → log cycle runs. If one business type fails entirely, the error is logged and processing continues with the next type.

### Retry Job

- **Cron:** `0 */5 * * * *` (configurable via `mic.scheduling.retry-cron`)
- **Fires at:** every 5 minutes, 24 hours a day
- **Behavior:** Queries all `FAILED` log entries where `retry_count < max-retry-count`. Attempts resend for each. On success, updates `status` to `SENT` and sets `sent_at`. On continued failure, increments `retry_count`. Once `retry_count` reaches `max-retry-count` (default 3), the record is permanently excluded from retries.

---

## 11. Deduplication

Before dispatching any SMS, `DeduplicationService` checks whether a row already exists in `SMS_LOG` with:
- `business_type` = current business type
- `reference_key` = assembled reference key for this record
- `normalized_phone` = validated phone number
- `status` = `SENT`

If a match is found, the dispatch is silently skipped. This guarantees that:
- A customer is never notified twice about the same policy issuance.
- A customer is never reminded twice about the same installment due date.

**Important:** FAILED entries do **not** block deduplication, because they represent unsuccessful attempts that will be retried.

---

## 12. Retry Mechanism

The retry system is separate from the main processing pipeline. It operates exclusively on records already in the `SMS_LOG` table with `status = FAILED`.

| Scenario | Outcome |
|---|---|
| Retry succeeds | `status` → `SENT`, `sent_at` → now, `failure_reason` → cleared |
| Retry fails | `retry_count` incremented, `failure_reason` updated |
| `retry_count` = `max-retry-count` | Record excluded from all future retry cycles |

The maximum retry count is configured at `mic.scheduling.max-retry-count` (default: 3).

---

## 13. Egyptian Mobile Validation

`MobileValidationService` validates and normalizes mobile numbers to the Egyptian 11-digit local format (`01XXXXXXXXX`).

**Accepted input formats:**
| Input Format | Example | Normalized Output |
|---|---|---|
| Local 11-digit | `01012345678` | `01012345678` |
| With `+20` country code | `+201012345678` | `01012345678` |
| With `0020` country code | `00201012345678` | `01012345678` |
| With `20` prefix (12 digits) | `201012345678` | `01012345678` |
| With spaces or dashes | `0101 234 5678` | `01012345678` |

**Valid operator prefixes:** `010` (Vodafone), `011` (Etisalat), `012` (Orange), `015` (WE)

Any number that does not match these criteria results in an `InvalidMobileNumberException`. Records with invalid phone numbers are **skipped** (not logged as failures), and a warning is written to the application log.

---

## 14. Adding a New Business Type

No Java code changes are required. Follow these steps:

**Step 1 — Create the stored procedure** in MSSQL, following the output contract (generic `info1`, `info2`, ... column aliases).

**Step 2 — Add a business type block** to `application.yml`:

```yaml
mic:
  business-types:
    customer-birthday:
      stored-procedure: SP_CUSTOMER_BIRTHDAY_SMS
      parameters: []
      phone-field: info2
      name-field: info1
      reference-fields:
        - info3
        - info4
      template: >-
        Dear {CustomerName}, MIC Insurance wishes you a very happy birthday!
        Your policy {PolicyNumber} remains active to protect what matters most.
      field-mappings:
        info1: CustomerName
        info2: CustomerPhone
        info3: PolicyNumber
        info4: BirthYear
```

**Step 3 — Add the key to `active-business-types`** if it should run on the scheduled processing job:

```yaml
mic:
  scheduling:
    active-business-types:
      - issuance
      - due-installment
      - customer-birthday
```

The new business type will be active at the next application restart.

---

## 15. Component Reference

### `MicSmsMiddlewareApplication`
Spring Boot entry point. Triggers `@ConfigurationPropertiesScan` to register all `@ConfigurationProperties` beans.

### `AppProperties`
Root `@ConfigurationProperties` bean bound to the `mic` prefix. Holds provider, scheduling, and business type configuration.

### `StoredProcedureService`
Executes any named MSSQL stored procedure via JDBC `CallableStatement`. Accepts a list of `SpParameterConfig` for dynamic parameter binding. Returns a `List<Map<String, String>>` where each map represents one row with lowercase column names as keys.

### `MobileValidationService`
Stateless service. `normalize(rawPhone)` throws `InvalidMobileNumberException` for invalid numbers. `isValid(rawPhone)` returns a boolean without throwing.

### `MessageTemplateService`
Stateless service. `compose(template, semanticData)` performs simple string replacement of `{Key}` tokens with values from the data map.

### `DeduplicationService`
Delegates to `SmsLogRepository` to check for an existing `SENT` record with the same `businessType`, `referenceKey`, and `normalizedPhone`.

### `SmsLogService`
Creates and updates `SmsLog` entities. `record()` creates a new log entry. `markSent()` transitions a failed entry to sent. `incrementRetry()` increments the retry counter.

### `SmsDispatchService`
Orchestrates a single SMS dispatch: deduplication check → template composition → provider send → log result.

### `BusinessProcessorService`
Orchestrates a full business type processing run: SP execution → field mapping → phone validation → ContactRecord construction → dispatch via `SmsDispatchService`. Handles `InvalidMobileNumberException` gracefully per-record without aborting the batch.

### `HttpSmsProvider`
`SmsProvider` implementation. Sends an HTTP POST with form-encoded credentials and message. Parses the response body for the configured `success-response-code`.

### `BusinessProcessingScheduler`
Spring `@Scheduled` component. Fires on `mic.scheduling.processing-cron`. Iterates active business types and calls `BusinessProcessorService.process()` for each.

### `RetryScheduler`
Spring `@Scheduled` component. Fires on `mic.scheduling.retry-cron`. Queries retryable failed logs and re-attempts sending via `SmsProvider`.

### `SmsLogRepository`
Spring Data JPA repository for `SmsLog`. Provides `existsByBusinessTypeAndReferenceKeyAndNormalizedPhoneAndStatus` for deduplication and `findRetryable` for the retry queue.

---

## 16. Setup & Deployment

### Prerequisites

- Java 17+
- Apache Maven 3.8+
- Microsoft SQL Server 2016+ (or Azure SQL)

### Database Setup

1. Create a database named `MIC_SMS_DB` (or update the JDBC URL in `application.yml`).
2. The application runs `src/main/resources/db/schema.sql` on startup via `spring.sql.init`. The script is idempotent — it only creates the table if it does not already exist.
3. Create the stored procedures in the same database (see section 7 for examples).

### Configuration

Edit `src/main/resources/application.yml`:

1. Update the datasource URL, username, and password.
2. Replace `PLACEHOLDER_USERNAME`, `PLACEHOLDER_PASSWORD`, and `base-url` under `mic.provider` with real SMS gateway credentials.
3. Confirm `mic.scheduling.active-business-types` lists the business types you want to activate.

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
java -jar target/sms-middleware-1.0.0.jar
```

### Environment-specific Configuration (optional)

Override any `application.yml` property at runtime using environment variables or a separate profile:

```bash
java -jar sms-middleware-1.0.0.jar \
  --spring.datasource.password=SecurePass \
  --mic.provider.username=realUser \
  --mic.provider.password=realPass
```

---

## 17. Logging

Application logs are written to both the console and `logs/sms-middleware.log` (rolling, max 50 MB per file, 30-day history).

### Log Levels by Event

| Event | Level | Logger |
|---|---|---|
| SP returned N records | INFO | `BusinessProcessorService` |
| Invalid phone skipped | WARN | `BusinessProcessorService` |
| Duplicate skipped | DEBUG | `DeduplicationService` |
| SMS sent successfully | INFO | `SmsLogService` |
| SMS failed | WARN | `SmsLogService` |
| Provider HTTP error | ERROR | `HttpSmsProvider` |
| Retry succeeded | INFO | `RetryScheduler` |
| Retry failed | WARN | `RetryScheduler` |
| Business type error | ERROR | `BusinessProcessingScheduler` |

To enable debug-level logging for this application add the following to `application.yml`:

```yaml
logging:
  level:
    com.mic.smsmiddleware: DEBUG
```
