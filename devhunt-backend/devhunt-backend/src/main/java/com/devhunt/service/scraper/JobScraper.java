package com.devhunt.service.scraper;

import com.devhunt.model.Vacancy;
import java.util.List;

public interface JobScraper {
    List<Vacancy> scrapeJobs();
    String getSourceName();
}