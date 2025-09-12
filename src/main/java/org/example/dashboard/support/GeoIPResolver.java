package org.example.dashboard.support;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;


/*
 * 국가 ip를 코드로 바꿔주는 메서드 (kr...us...)
 */
@Component
public class GeoIPResolver {
    private DatabaseReader reader;

    @PostConstruct
    public void init() {
        try {
            var db = new ClassPathResource("GeoLite2-Country.mmdb"); // resources/ 에 파일 배치
            if (db.exists()) {
                reader = new DatabaseReader.Builder(db.getInputStream()).build();
            }
        } catch (Exception ignore) { /* graceful fallback */ }
    }

    /** IP → ISO2(예: "KR"). 실패 시 null 반환 */
    public String countryCode(String ip) {
        if (reader == null || ip == null || ip.isBlank()) return null;
        try {
            InetAddress addr = InetAddress.getByName(ip);
            CountryResponse resp = reader.country(addr);
            if (resp != null && resp.getCountry() != null) {
                String code = resp.getCountry().getIsoCode();
                return (code != null && !code.isBlank()) ? code : null;
            }
        } catch (IOException | GeoIp2Exception ignore) { }
        return null;
    }
}
