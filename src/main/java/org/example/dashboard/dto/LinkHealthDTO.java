package org.example.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkHealthDTO {
private LocalDateTime checkedAt;
private Integer httpStatus;
private Integer redirectHops;
private Boolean isLoop;
private Boolean ok;
private String message;
}