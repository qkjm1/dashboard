package org.example.dashboard.dto;

import lombok.Data;
import org.example.dashboard.vo.Link;


@Data
public class CountryCountDTO {
    private String countryCode; // "KR","US",...
    private long cnt;
    
}