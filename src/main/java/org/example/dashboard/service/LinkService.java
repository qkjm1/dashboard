package org.example.dashboard.service;

import org.example.dashboard.repository.LinkRepository;
import org.example.dashboard.vo.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class LinkService {

    @Autowired
    private LinkRepository linkRepository;

    private final Random random = new Random();

    // 링크 생성
    public Link createLink(String originalUrl) {
    	System.out.println(originalUrl);
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setActive(true);

        String slug;
        int attempt = 0;
        do {
            slug = generateSlug();
            attempt++;
        } while(linkRepository.countBySlug(slug) > 0 && attempt < 3);
        link.setSlug(slug);

        System.out.println(link);
        linkRepository.insertLink(link);
        return link;
    }

    // 리다이렉트 조회
    public Link getLink(String slug) {
        return linkRepository.selectBySlug(slug);
    }

    private String generateSlug() {
        int len = 6;
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
