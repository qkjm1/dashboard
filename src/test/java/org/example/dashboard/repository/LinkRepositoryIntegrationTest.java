package org.example.dashboard.repository;

import org.example.dashboard.vo.Link;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class LinkRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired LinkRepository linkRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS click_log");
        jdbcTemplate.execute("DROP TABLE IF EXISTS link");
        jdbcTemplate.execute("""
            CREATE TABLE link (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                slug VARCHAR(50) NOT NULL UNIQUE,
                original_url VARCHAR(2048) NOT NULL,
                expiration_date DATETIME NULL,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """);
        jdbcTemplate.execute("""
            CREATE TABLE click_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                link_id BIGINT NOT NULL,
                clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ip_hash CHAR(64),
                user_agent VARCHAR(512),
                referrer VARCHAR(512),
                channel VARCHAR(100),
                device_type ENUM('MOBILE','DESKTOP','TABLET','OTHER') DEFAULT 'OTHER',
                os VARCHAR(50),
                browser VARCHAR(50),
                user_agent VARCHAR(512),
                FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
            );
            """);
    }

    @Test
    void insert_and_select_by_slug() {
        Link l = new Link();
        l.setOriginalUrl("https://www.example.com");
        l.setActive(true);
        l.setSlug("tc1234");
        linkRepository.insertLink(l);

        Link found = linkRepository.selectBySlug("tc1234");
        assertThat(found).isNotNull();
        assertThat(found.getOriginalUrl()).isEqualTo("https://www.example.com");
        assertThat(found.getSlug()).isEqualTo("tc1234");
    }
}
