package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.request.UpdateEmployeeStatusRequest;
import com.skr.erp.dto.response.EmployeeCalendarResponse;
import com.skr.erp.dto.response.EmployeeDetailsResponse;
import com.skr.erp.dto.response.EmployeeResponse;
import com.skr.erp.dto.response.EmployeeStatsResponse;
import com.skr.erp.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public CommonResponse<EmployeeResponse> create(
            @Valid
            @RequestBody
            CreateEmployeeRequest request) {

        return CommonResponse
                .<EmployeeResponse>builder()
                .success(true)
                .message(
                        "Employee created successfully")
                .data(
                        employeeService.create(request))
                .build();
    }

    @GetMapping
    public CommonResponse<List<EmployeeResponse>>
    getAll() {

        return CommonResponse
                .<List<EmployeeResponse>>builder()
                .success(true)
                .message(
                        "Employees fetched successfully")
                .data(
                        employeeService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public CommonResponse<EmployeeResponse>
    getById(
            @PathVariable UUID id) {

        return CommonResponse
                .<EmployeeResponse>builder()
                .success(true)
                .message(
                        "Employee fetched successfully")
                .data(
                        employeeService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    public CommonResponse<EmployeeResponse>
    update(
            @PathVariable UUID id,

            @Valid
            @RequestBody
            UpdateEmployeeRequest request) {

        return CommonResponse
                .<EmployeeResponse>builder()
                .success(true)
                .message(
                        "Employee updated successfully")
                .data(
                        employeeService.update(
                                id,
                                request))
                .build();
    }

    @PatchMapping("/{id}/status")
    public CommonResponse<String>
    updateStatus(
            @PathVariable UUID id,

            @RequestBody
            UpdateEmployeeStatusRequest request) {

        employeeService.updateStatus(
                id,
                request.getActive());

        return CommonResponse
                .<String>builder()
                .success(true)
                .message(
                        "Employee status updated successfully")
                .data("Success")
                .build();
    }

    @GetMapping("/{id}/details")
    public CommonResponse<EmployeeDetailsResponse>
    getDetails(

            @PathVariable UUID id,

            @RequestParam(required = false)
            LocalDate fromDate,

            @RequestParam(required = false)
            LocalDate toDate
    ) {

        return CommonResponse
                .<EmployeeDetailsResponse>builder()
                .success(true)
                .message(
                        "Employee details fetched successfully")
                .data(
                        employeeService.getDetails(
                                id,
                                fromDate,
                                toDate
                        ))
                .build();
    }

    @GetMapping("/stats")
    public CommonResponse<EmployeeStatsResponse> getStats() {

        return CommonResponse
                .<EmployeeStatsResponse>builder()
                .success(true)
                .message("Employee statistics fetched successfully")
                .data(employeeService.getStats())
                .build();

    }

    @GetMapping("/{employeeId}/calendar")
    public CommonResponse<List<EmployeeCalendarResponse>>
    getCalendarData(

            @PathVariable UUID employeeId,

            @RequestParam LocalDate fromDate,

            @RequestParam LocalDate toDate) {

        return CommonResponse
                .<List<EmployeeCalendarResponse>>builder()
                .success(true)
                .message("Calendar data fetched successfully")
                .data(
                        employeeService.getCalendarData(
                                employeeId,
                                fromDate,
                                toDate))
                .build();
    }

}