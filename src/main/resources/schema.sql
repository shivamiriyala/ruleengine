CREATE TABLE IF NOT EXISTS dependency_upload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ci_upload_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status ENUM('IN_PROGRESS', 'COMPLETED') NOT NULL,
    INDEX idx_status (status)  --create an index on the status column
);
