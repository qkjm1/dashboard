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


ALTER TABLE click_log
  ADD COLUMN country_code CHAR(2) NULL AFTER channel;
  
ALTER TABLE click_log
  ADD COLUMN is_bot TINYINT(1) DEFAULT 0,
  ADD COLUMN bot_type VARCHAR(32) NULL,         -- 'crawler' | 'preview' | 'healthcheck' | 'unknown'         -- CF-IPCountry 등
  ADD COLUMN referrer_host VARCHAR(128) NULL;


  
  --- ######### ClickLogRepository 쿼리 순수 db용 --
  
  
-- referrer 호스트 추출 표현식 ClickLogRepository -
LOWER(
  REPLACE(
    REPLACE(
      SUBSTRING_INDEX(SUBSTRING_INDEX(COALESCE(c.referrer,''), '/', 3), '/', -1),
      'www.', ''
    ),
    'm.', ''
  )
) AS referrerHost

  
--  국가 분포 (slug 기준) ClickLogRepository --
  SELECT
  COALESCE(NULLIF(c.country_code, ''), 'UN') AS countryCode,
  COUNT(*) AS cnt
FROM click_log c
JOIN link l ON l.id = c.link_id
WHERE l.slug = 'your-slug-here'
  -- AND c.clicked_at >= '2025-09-01 00:00:00'
  -- AND c.clicked_at <  '2025-09-12 00:00:00'
GROUP BY countryCode
ORDER BY cnt DESC;

-- 총 클릭  ClickLogRepository --
SELECT COUNT(*) AS total
FROM click_log c
JOIN link l ON l.id = c.link_id
WHERE l.slug = 'your-slug-here';

-- 유니크(대략적 사용자 수)  ClickLogRepository --
SELECT COUNT(*) AS uniqueApprox
FROM (
  SELECT c.ip_hash, COALESCE(NULLIF(c.user_agent,''), 'NA') AS ua
  FROM click_log c
  JOIN link l ON l.id = c.link_id
  WHERE l.slug = 'your-slug-here'
  GROUP BY c.ip_hash, COALESCE(NULLIF(c.user_agent,''), 'NA')
) t;


 -- 짧은 간격 필터 유니크  ClickLogRepository --
SELECT COUNT(*) AS uniqueWindowed
FROM (
  SELECT
    CASE
      WHEN TIMESTAMPDIFF(
             MINUTE,
             LAG(c.clicked_at) OVER (
               PARTITION BY c.ip_hash, COALESCE(NULLIF(c.user_agent,''), 'NA')
               ORDER BY c.clicked_at
             ),
             c.clicked_at
           ) IS NULL THEN 1
      WHEN TIMESTAMPDIFF(
             MINUTE,
             LAG(c.clicked_at) OVER (
               PARTITION BY c.ip_hash, COALESCE(NULLIF(c.user_agent,''), 'NA')
               ORDER BY c.clicked_at
             ),
             c.clicked_at
           ) >= 10 THEN 1
      ELSE 0
    END AS session_start
  FROM click_log c
  JOIN link l ON l.id = c.link_id
  WHERE l.slug = 'your-slug-here'
) x
WHERE session_start = 1;

