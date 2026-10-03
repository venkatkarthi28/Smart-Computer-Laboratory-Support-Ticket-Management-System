package com.example.labsupport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.dto.response.LabelCount;
import com.example.labsupport.repository.AnalyticsRepository;

/** Chart data for the admin reports page. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public List<LabelCount> ticketsByCategory() {
        return analyticsRepository.ticketsByCategory();
    }

    public List<LabelCount> ticketsByPriority() {
        return analyticsRepository.ticketsByPriority();
    }
}
