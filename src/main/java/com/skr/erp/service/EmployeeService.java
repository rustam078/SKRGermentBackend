package com.skr.erp.service;

import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.response.EmployeeCalendarResponse;
import com.skr.erp.dto.response.EmployeeDetailsResponse;
import com.skr.erp.dto.response.EmployeeResponse;
import com.skr.erp.dto.response.EmployeeStatsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    EmployeeResponse create(CreateEmployeeRequest request);

    List<EmployeeResponse> getAll();
    EmployeeResponse getById(UUID id);

    EmployeeResponse update(
            UUID id,
            UpdateEmployeeRequest request);

    void updateStatus(
            UUID id,
            Boolean active);

    EmployeeDetailsResponse getDetails(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate
    );

    public EmployeeStatsResponse getStats();
    List<EmployeeCalendarResponse> getCalendarData(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate);

}