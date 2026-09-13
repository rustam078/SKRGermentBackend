package com.skr.erp.service;

import com.skr.erp.dto.response.SystemSettingResponse;

public interface SystemSettingService {

    SystemSettingResponse get(String key);

    SystemSettingResponse update(String key, String value);

    Integer getLowStockThreshold();

}