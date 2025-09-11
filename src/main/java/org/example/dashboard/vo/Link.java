package org.example.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Link {
    private Long id;
    private String slug;
    private String originalUrl;
    private LocalDateTime expirationDate;
    private Boolean active;
    private LocalDateTime createdAt;
}
