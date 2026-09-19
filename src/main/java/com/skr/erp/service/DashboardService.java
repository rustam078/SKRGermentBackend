package com.skr.erp.service;

import com.skr.erp.dto.response.DashboardResponse;
import com.skr.erp.dto.response.DailyReportRow;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    DashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate);

    List<DailyReportRow> getDailyReport(LocalDate fromDate, LocalDate toDate);
}
