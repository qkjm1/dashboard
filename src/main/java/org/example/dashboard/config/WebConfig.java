package org.example.dashboard.config;

import org.example.dashboard.support.DateTimeParamConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZoneId;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 서버 기본 타임존: Asia/Seoul (원하면 설정값에서 주입)
        registry.addConverter(new DateTimeParamConverter(ZoneId.of("Asia/Seoul")));
    }
}
