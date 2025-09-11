CREATE TABLE link (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      slug VARCHAR(50) NOT NULL UNIQUE,
                      original_url VARCHAR(2048) NOT NULL,
                      expiration_date DATETIME NULL,
                      active BOOLEAN DEFAULT TRUE,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE click_log (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           link_id BIGINT NOT NULL,
                           clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           ip_hash CHAR(64),
                           referrer VARCHAR(512),
                           channel VARCHAR(100),
                           device_type ENUM('MOBILE','DESKTOP','TABLET','OTHER') DEFAULT 'OTHER',
                           os VARCHAR(50),
                           browser VARCHAR(50),
                           user_agent VARCHAR(512),
                           FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);
