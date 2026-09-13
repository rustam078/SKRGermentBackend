package com.skr.erp.service;

import com.skr.erp.dto.response.SystemSettingResponse;

import java.util.List;

public interface SystemSettingService {

    List<SystemSettingResponse> getAll();

    SystemSettingResponse get(String key);

    SystemSettingResponse update(String key, String value);

    Integer getLowStockThreshold();

}