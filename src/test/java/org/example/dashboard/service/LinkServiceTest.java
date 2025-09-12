package org.example.dashboard.service;

import org.example.dashboard.repository.LinkRepository;
import org.example.dashboard.vo.Link;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private LinkService linkService;

    @org.junit.jupiter.api.BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createLink_shouldSetOriginalUrl_andGenerateSlug_thenCallInsert() {
        // arrange
        String url = "https://www.google.com";
        when(linkRepository.countBySlug(anyString())).thenReturn(0);

        // act
        Link created = linkService.createLink(url);

        // assert
        assertThat(created).isNotNull();
        assertThat(created.getOriginalUrl()).isEqualTo(url);
        assertThat(created.getSlug()).isNotNull().hasSize(6);
        verify(linkRepository, times(1)).insertLink(any(Link.class));
    }

    @Test
    void getLink_shouldDelegateToRepository() {
        // arrange
        Link link = Link.builder().id(1L).slug("abc123").originalUrl("https://x").active(true).build();
        when(linkRepository.selectBySlug("abc123")).thenReturn(link);

        // act
        Link found = linkService.getLink("abc123");

        // assert
        assertThat(found).isEqualTo(link);
        verify(linkRepository, times(1)).selectBySlug("abc123");
    }
}
