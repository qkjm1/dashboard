package org.example.dashboard.controller;

import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.Link;
import org.example.dashboard.vo.ClickLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Mock
    LinkService linkService;

    @Mock
    ClickLogService clickLogService;

    // 1️⃣ 링크 생성 성공
    @Test
    void createLink_get_shouldReturnJson() throws Exception {
        Link mock = Link.builder()
                .id(1L)
                .slug("abc123")
                .originalUrl("https://www.google.com")
                .active(true)
                .build();

        when(linkService.createLink("https://www.google.com")).thenReturn(mock);

        mockMvc.perform(get("/links").param("url", "https://www.google.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"));
    }

    // 2️⃣ 리다이렉트 성공
    @Test
    void redirect_shouldReturn302_andRedirectToOriginalUrl() throws Exception {
        Link mock = Link.builder()
                .id(1L)
                .slug("abc123")
                .originalUrl("https://www.google.com")
                .active(true)
                .build();

        when(linkService.getLink("abc123")).thenReturn(mock);

        mockMvc.perform(get("/r/{slug}", "abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://www.google.com"));

        verify(clickLogService, times(1)).saveClick(any(ClickLog.class));
    }

    // 3️⃣ 존재하지 않는 slug → 404
    @Test
    void redirect_nonExistentSlug_shouldReturn404() throws Exception {
        when(linkService.getLink("notfound")).thenReturn(null);

        mockMvc.perform(get("/r/{slug}", "notfound"))
                .andExpect(status().isNotFound());
    }

    // 4️⃣ 비활성 링크 → 404
    @Test
    void redirect_inactiveLink_shouldReturn404() throws Exception {
        Link mock = Link.builder()
                .id(1L)
                .slug("abc123")
                .originalUrl("https://www.google.com")
                .active(false)
                .build();

        when(linkService.getLink("abc123")).thenReturn(mock);

        mockMvc.perform(get("/r/{slug}", "abc123"))
                .andExpect(status().isNotFound());
    }

    // 5️⃣ 잘못된 URL 입력 → 예외 처리
    @Test
    void createLink_invalidUrl_shouldReturn400() throws Exception {
        when(linkService.createLink("invalid-url")).thenThrow(new IllegalArgumentException("Invalid URL"));

        mockMvc.perform(get("/links").param("url", "invalid-url"))
                .andExpect(status().isBadRequest());
    }
}
