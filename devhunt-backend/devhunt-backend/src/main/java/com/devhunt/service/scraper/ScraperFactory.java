package com.devhunt.service.scraper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScraperFactory {
    private final List<JobScraper> scrapers;

    public List<JobScraper> getAllScrapers() {
        return scrapers;
    }
}