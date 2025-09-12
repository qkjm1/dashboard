package org.example.dashboard.service;

import org.example.dashboard.repository.ClickLogRepository;
import org.example.dashboard.vo.ClickLog;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClickLogServiceTest {

    @Mock private ClickLogRepository clickLogRepository;
    @InjectMocks private ClickLogService clickLogService;

    @org.junit.jupiter.api.BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void saveClick_callsRepositoryInsert() {
        ClickLog log = new ClickLog();
        log.setLinkId(1L);
        clickLogService.saveClick(log);
        verify(clickLogRepository, times(1)).insertClickLog(log);
    }

    @Test
    void getLogsByLinkId_delegates() {
        when(clickLogRepository.selectByLinkId(1L)).thenReturn(List.of());
        assertThat(clickLogService.getLogsByLinkId(1L)).isEmpty();
    }
}
