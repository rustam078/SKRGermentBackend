package com.skr.erp.service;

import com.skr.erp.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {

    DashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate);
}
