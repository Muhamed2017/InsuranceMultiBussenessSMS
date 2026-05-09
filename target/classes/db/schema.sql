-- IF NOT EXISTS (
--     SELECT 1 FROM sys.tables WHERE name = 'SMS_LOG' AND type = 'U'
-- )
-- BEGIN
--     CREATE TABLE SMS_LOG (
--         id               BIGINT          IDENTITY(1,1)  NOT NULL,
--         business_type    VARCHAR(100)                   NOT NULL,
--         contact_phone    VARCHAR(30)                    NOT NULL,
--         normalized_phone VARCHAR(15)                    NOT NULL,
--         contact_name     NVARCHAR(300)                  NULL,
--         reference_key    VARCHAR(300)                   NOT NULL,
--         message_content  NVARCHAR(MAX)                  NOT NULL,
--         status           VARCHAR(10)                    NOT NULL,
--         failure_reason   NVARCHAR(500)                  NULL,
--         retry_count      INT             DEFAULT 0      NOT NULL,
--         sent_at          DATETIME                       NULL,
--         created_at       DATETIME        DEFAULT GETDATE() NOT NULL,

--         CONSTRAINT PK_SMS_LOG PRIMARY KEY (id),
--         CONSTRAINT CHK_SMS_LOG_STATUS CHECK (status IN ('SENT', 'FAILED'))
--     );

--     CREATE INDEX IDX_SMS_LOG_DEDUP
--         ON SMS_LOG (business_type, reference_key, normalized_phone, status);

--     CREATE INDEX IDX_SMS_LOG_RETRY
--         ON SMS_LOG (status, retry_count);

--     CREATE INDEX IDX_SMS_LOG_CREATED_AT
--         ON SMS_LOG (created_at);
-- END


IF OBJECT_ID('dbo.SMS_LOG', 'U') IS NULL
CREATE TABLE SMS_LOG (
    id BIGINT IDENTITY(1,1) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    normalized_phone VARCHAR(15) NOT NULL,
    contact_name NVARCHAR(300) NULL,
    reference_key VARCHAR(300) NOT NULL,
    message_content NVARCHAR(MAX) NOT NULL,
    status VARCHAR(10) NOT NULL,
    failure_reason NVARCHAR(500) NULL,
    retry_count INT DEFAULT 0 NOT NULL,
    sent_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE() NOT NULL,
    CONSTRAINT PK_SMS_LOG PRIMARY KEY (id),
    CONSTRAINT CHK_SMS_LOG_STATUS CHECK (status IN ('SENT', 'FAILED'))
);